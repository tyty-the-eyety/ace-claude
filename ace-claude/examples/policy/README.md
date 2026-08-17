# Policy project examples (verified working, ACE 13.0.2.2)

`POLICY_PROJECT_TEMPLATE/` is a copy of a policy project whose policies
packaged, deployed, and (where marked) demonstrably took effect at runtime
(2026-07-02, workspace projects `POLICY_DEMO_POLICIES` + `POLICY_DEMO_APP`).
Copy the whole directory, rename the project in `.project`, delete the
policies you don't need, and mutate the rest. Do not author the scaffold from
scratch. **The template's `.project` must never carry the name of a real
project** — ibmint scans the whole `--input-path` and duplicate project names
break packaging (BIP8081E `duplicate entry`).

`flows/` holds the verified **node-side attachment flows** for every policy in
the template (see `flows/README.md` for the per-flow map), and `tools/` holds
the WLM BAR-override file and the proxy-proof script. When building a flow that
uses a policy, copy the matching flow from `flows/` and mutate it.

## Contents

| File | Policy type | Verified | Notes |
|---|---|---|---|
| `.project`, `.settings/…`, `policy.descriptor` | scaffold | ✔ | fixed boilerplate; policy nature + policybuilder only |
| `BackendRoute.policyxml` | HTTPRequest | ✔ runtime | `endpointURL` override redirected a bogus node URL to a real backend |
| `DemoSettings.policyxml` | UserDefined | ✔ deploy+REST | arbitrary child elements become string properties |
| `TickTimer.policyxml` | Timer | ✔ runtime | policy `timeoutInterval=3` beat the node's `timeoutInterval=1` (ticks every 3s); works without MQ |
| `MaxRate2PerSec.policyxml` | WorkloadManagement | ✔ runtime | 6 requests throttled to ~3s at 2 msg/s; `processingTimeoutAction` must be lowercase `none` |
| `FlowActivity.policyxml` | ActivityLog | ✔ runtime | active on deploy, no attachment step; writes CSV activity entries to `fileName` |
| `DevQM.policyxml` | MQEndpoint | ✔ runtime | `connection=SERVER` local binding to TEST_QM; MQOutput node with NO qmgr config put a message on `TEST.POLICY.Q` purely via `policyUrl` |
| `DevMySQL.policyxml` | JDBCProviders | ✔ deploy+REST | non-dynamic type — live redeploy needs `--restart-all-applications` |
| `LocalAuthProfile.policyxml` | SecurityProfiles | ✔ runtime | inbound HTTP basic auth validated against a vault `local` credential: no/wrong creds → 401, correct → 200 |
| `AuthBackendRoute.policyxml` | HTTPRequest + credential | ✔ runtime | `credentialName` injected vault-stored basic auth into an outbound call — caller supplied no credentials and still got 200 from the secured endpoint |
| `TcpServer5555.policyxml` | TCPIPServer | ✔ runtime | port defined ONLY in the policy; listener bound on deploy, payload received over the socket |
| `TcpClient5555.policyxml` | TCPIPClient | ✔ runtime | client flow connected via policy hostname/port; **NOTE: TCPIP policy properties are PascalCase** (`Hostname`, `Port`) unlike every other policy type |
| `LocalProxy.policyxml` | HTTPProxy | ◐ deploy+REST | policy validates and deploys, but **13.0.2.2 has no attachment point**: `httpProxyLocation` on HTTPRequest nodes is literal-only (BIP2211E on a policy ref) and the connector-policy `proxyName` property arrived in a later fixpack. Proxy transit itself verified with a literal `httpProxyLocation="host:port"` |

## How each policy type attaches

| Policy type | Attachment |
|---|---|
| HTTPRequest | node attribute `policyUrl="{proj}:policy"` on HTTPRequest/WSRequest |
| MQEndpoint | node attribute `policyUrl="{proj}:policy"` on MQInput/MQOutput |
| Timer | node attribute `uniqueIdentifier="{proj}:policy"` on TimeoutNotification/TimeoutControl |
| WorkloadManagement | BAR override `<flowName>#wlmPolicy={proj}:policy` (NOT `workloadManagementPolicy`) via `ibmint apply overrides` |
| HTTPReply | server-wide default: `Defaults → Policies → HTTPReply: '{proj}:policy'` in server.conf.yaml (reply nodes have no policyUrl) |
| ActivityLog | none — active for the whole server once deployed |
| UserDefined | read from JavaCompute / Graphical Data Map, or via admin REST |
| JDBCProviders / JMSProviders | resolved by name from nodes/ESQL that name the provider; non-dynamic |
| SecurityProfiles | node attributes `securityProfileName="{proj}:policy"` + `identityType="usernameAndPassword"` on the input node |
| TCPIPClient / TCPIPServer | node attribute `connectionDetails="{proj}:policy"` on the TCPIP nodes (same attribute also accepts literal `host:port` — the policy form is what makes it environment-portable) |
| HTTPProxy | via another policy: set `proxyName={proj}:policy` in the connector's connection policy (later fixpacks; NOT available at 13.0.2.2). Plain HTTPRequest nodes at 13.0.2.2: literal `httpProxyLocation="host:port"` only |

## Vault + credentials (verified pattern)

```bash
mqsivault --work-dir <WORKDIR> --create --vault-key <KEY>
# inbound: what a Local-auth security profile validates against
mqsicredentials --work-dir <WORKDIR> --create --vault-key <KEY> \
  --credential-type local --credential-name demoUser --username demo --password s3cretpw
# outbound: what an HTTPRequest policy credentialName injects
mqsicredentials --work-dir <WORKDIR> --create --vault-key <KEY> \
  --credential-type http --auth-type basic --credential-name backendBasic --username demo --password s3cretpw
```

- SecurityProfiles policy: `authentication=Local`, `authenticationConfig=<local credential name>` — the incoming basic-auth username/password must match that vault credential.
- HTTPRequest policy: `credentialName=<http credential name>` — the node sends `Authorization: Basic` built from the vault credential.
- **Once the work dir has a vault, the server will not start without `--vault-key <KEY>`** (or MQSI_VAULT_KEY / .mqsivaultrc).
- Credential types list: `mqsicredentials --help` (notably `local`, `http`, `jdbc`, `odbc`, `mq`, `kafka`).

The authoritative list of BAR-overridable flow/node properties is the deployed
`run/<APP>/META-INF/broker.xml` — read it instead of guessing key names.
`ibmint apply overrides` does NOT validate property names (it writes anything,
BIP1140I), and the runtime silently ignores unknown ones.

## Rules that were verified the hard way

- Filename must be `<policyName>.policyxml`, `policyName` attribute matching.
- Property element names: trust shipped `server/adminservices/PolicyTemplates/`
  first, then `common/schemas/Policy/Policy.xsd`, then the docs. Confirmed
  docs/XSD errors: `endpointURL` (docs say `endpointUrl`),
  `userAgentCompressionBlockFilter` (XSD says `userAgentBlockCompressionFilter`),
  `processingTimeoutAction: none` (docs say `None`).
- A malformed policy body does NOT fail `ibmint package` or `deploy` — it fails
  at **flow start** (BIP9320E + often the cryptic BIP2328E "SQL datatype
  'UNKNOWN'"). Every flow with a node the policy applies to fails.
- HTTPRequest `endpointURLOverrideBehaviour`: `ProtocolHostAndPort` (default,
  node path kept) or `ProtocolHostPortAndPath` (policy URL wins entirely).
- Unqualified policy references resolve in the `DefaultPolicies` project
  (rename via `Defaults → policyProject` in server.conf.yaml); use
  `{project}:name` to be explicit.

## Build / deploy / verify

```bash
ibmint package --input-path <WORKSPACE> --output-bar-file bars/<APP>.bar \
  --project <APP> --project <POLICY_PROJECT>          # --project repeats
ibmint apply overrides <overrides.txt> --bar-file bars/<APP>.bar   # if attaching WLM etc.
ibmint deploy --input-bar-file bars/<APP>.bar --output-work-directory <WORKDIR>
```

Startup log confirms with `BIP9332I: PolicyProject '<name>' reloaded successfully`.
Admin REST (HTTPS, `-sk`):

```
GET /apiv2/policies                                  → policy projects
GET /apiv2/policies/<project>                        → policies in it
GET /apiv2/policies/<project>/policy/<policyName>    → full property dump
```

(segment is the literal word `policy`, not the policy type; `/apiv2/policy-projects` does not exist)

## Runtime proof patterns used

- **HTTPRequest**: node URL `http://unreachable.invalid:59999/wrong-path` +
  policy endpointURL to the real backend → call succeeded ⇒ policy decided.
- **Timer**: node interval 1s, policy 3s → ticks landed exactly 3s apart
  (check `<outputDirectory>/mqsitransit/` — FileOutput stages appends there).
- **WLM**: 6 sequential HTTP requests, unthrottled ~0.09s, with
  `maximumRateMsgsPerSec=2` → ~2.5–3.0s.
- **ActivityLog**: drive any file/resource flow, entries appear in `fileName`.
- **TCPIP**: HTTP flow → Compute (JSON→BLOB via ASBITSTREAM) → TCPIPClientOutput
  (`closeConnection="afterData"`) → socket → TCPIPServerInput
  (`recordDetection="wholeFile"`, BLOB) → FileOutput. Port exists only in the
  policies; `ss -tln` shows the listener as soon as the server flow starts.
