## ACE Policy Project Structure

All policies (connector or otherwise) MUST be created in a **separate** Policy Project, NOT inside the Application project.

### Eclipse project scaffold

A Policy project is a specialised form of an Eclipse project. The directory **MUST** contain:

**`.project` file** with these exact sections:
```xml
<natures>
  <nature>com.ibm.etools.mft.policy.ui.Nature</nature>
</natures>
<buildSpec>
  <buildCommand>
    <name>com.ibm.etools.mft.policy.ui.policybuilder</name>
    <arguments>
    </arguments>
  </buildCommand>
</buildSpec>
```

**`.settings/org.eclipse.core.resources.prefs`**:
```
eclipse.preferences.version=1
encoding/<project>=UTF-8
```
(The literal string `encoding/<project>=UTF-8` — do NOT substitute the project name.)

**`policy.descriptor`** in the project root:
```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<ns2:policyProjectDescriptor xmlns="http://com.ibm.etools.mft.descriptor.base" xmlns:ns2="http://com.ibm.etools.mft.descriptor.policyProject">
  <references/>
</ns2:policyProjectDescriptor>
```

### Policy files

Policy files (`.policyxml` extension) are placed in the root of the Policy Project.

If the user has not specified a policy, create one. Derive the filename from the connector type followed by `1` (e.g. `Salesforce1.policyxml`, `AmazonS31.policyxml`).

The connector-specific `.md` file contains the example policy XML for each connector. The policy file structure MUST conform to the ACE Policy XML schema; optionally verify against `<ACE_INSTALL_DIR>/common/schemas/Policy/Policy.xsd`.

### General .policyxml format (verified ACE 13.0.2.2)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<policies>
  <policy policyType="<TYPE>" policyName="<NAME>" policyTemplate="<TYPE or empty>">
    <propertyName>value</propertyName>
    ...
  </policy>
</policies>
```

- The filename must be `<NAME>.policyxml` and `policyName` must match the filename stem.
- Property element names come from `Policy.xsd` — **the XSD is authoritative over the IBM docs property tables** (known docs typo: HTTPRequest policy `endpointURL` is listed there as `endpointUrl`).
- Copy-and-mutate `examples/policy/POLICY_PROJECT_TEMPLATE/` rather than authoring the scaffold from scratch; see `examples/policy/README.md`.

### Common non-connector policy types (all verified unless noted)

- **HTTPRequest** — attach to HTTPRequest/HTTPAsyncRequest nodes; key properties `endpointURL` (protocol/host/port[/path] override) and `endpointURLOverrideBehaviour` (`ProtocolHostAndPort` default, or `ProtocolHostPortAndPath` to replace the whole node URL). Also supports basic-auth/API-key/OAuth2 credential properties.
- **UserDefined** — no schema; arbitrary child elements become string properties. Readable from JavaCompute or Graphical Data Maps (not plain ESQL); also visible via the admin REST API.
- **Timer** — attach via the TimeoutNotification/TimeoutControl node's `uniqueIdentifier` attribute set to `{proj}:policy`; the policy's `timeoutInterval` (seconds, mandatory) overrides the node's own interval in automatic mode. Works without MQ on an independent server.
- **WorkloadManagement** — attach via BAR override `<flowName>#wlmPolicy={proj}:policy` (`ibmint apply overrides`); key properties `maximumRateMsgsPerSec`, `additionalInstances`, `processingTimeoutSec`, `processingTimeoutAction` (lowercase `none` — the docs' `None` breaks flow start with BIP7987E).
- **ActivityLog** — no attachment: active server-wide once deployed. `fileName`, `numberOfLogs`, `maxFileSizeMb`, `formatEntries`, `filter` (tag pairs like `RM=File;MSGFLOW=myflow`), `consoleLog`/`consoleLogFormat`.
- **HTTPReply** — reply nodes have no policyUrl; set a server-wide default in server.conf.yaml `Defaults → Policies → HTTPReply: '{proj}:policy'`. Properties `compressionType` (`allMimeTypes`/`compressibleMimeTypes`/`noCompression`), `minimumCompressionSize`, `userAgentCompressionBlockFilter` (that word order — Policy.xsd has it wrong). Caveat: structure validates, but no compression was observed from the 13.0.2.2 native listener.
- **MQEndpoint** — attach via `policyUrl` on MQInput/MQOutput; `connection` is `SERVER` (local bindings), `CLIENT`, or `CCDT`; with SERVER only `destinationQueueManagerName` matters, with CLIENT add `queueManagerHostname`/`listenerPortNumber`/`channelName`. Runtime-verified: an MQOutput with no queue manager configured put to a queue purely through the policy.
- **TCPIPClient / TCPIPServer** — attach via `connectionDetails="{proj}:policy"` on TCPIP nodes (attribute also accepts literal `host:port`). Properties are **PascalCase** (`Hostname`, `Port`, `MaximumConnections`) unlike all other policy types. Server policy's port binds a listener the moment the flow starts.
- **HTTPProxy** — `proxyUrl` (required) + `credentialName` (type `httpproxy`). Consumed by discovery-connector connection policies via a `proxyName={proj}:policy` property in later fixpacks — **not attachable at 13.0.2.2** (no `proxyName` in Policy.xsd; `httpProxyLocation` on HTTPRequest nodes rejects policy refs with BIP2211E and takes only literal `host:port`).
- **SecurityProfiles** — attach via `securityProfileName="{proj}:policy"` + `identityType="usernameAndPassword"` on the input node. For vault-backed inbound basic auth (no LDAP needed): `authentication=Local`, `authenticationConfig=<vault credential name of type local>`. Pair with an HTTPRequest policy `credentialName` (vault credential type `http`) for outbound injection — see `examples/policy/README.md` vault section for the exact `mqsivault`/`mqsicredentials` commands. A work dir with a vault requires `--vault-key` at server start.
- **JDBCProviders / JMSProviders / Kafka / Aggregation** etc. — full list and property names in `<ACE_INSTALL_DIR>/common/schemas/Policy/Policy.xsd`; shipped samples in `<ACE_INSTALL_DIR>/server/adminservices/PolicyTemplates/`.
- Non-dynamic types (`JDBCProviders`, `JMSProviders`, `JavaClassLoader`, `DotNetAppDomain`, `WXSServer`, policy sets/bindings) cannot be redeployed while flows use them — redeploy with `--restart-all-applications`. Deploying to a stopped server's work dir is unaffected. Dynamic types (e.g. HTTPRequest, MQEndpoint) auto-restart only the flows that use them.

### Attaching a policy to a message flow node

Set the node's Policy property in the msgflow XML:

```xml
<nodes xmi:type="ComIbmWSRequest.msgnode:FCMComposite_1" ...
    policyUrl="{PolicyProjectName}:PolicyName" .../>
```

The attribute name on HTTPRequest (WSRequest) and MQ nodes is `policyUrl`; on timeout nodes it is `uniqueIdentifier`. The value format `{project}:policy` has literal braces; unqualified names resolve in the `DefaultPolicies` project. The policy must be deployed before flows that reference it start. **A malformed policy body fails at flow start (BIP9320E + cryptic BIP2328E), not at package/deploy time** — diff against a shipped template when flows suddenly won't start.

For flow-level policies (WLM), the overridable property names are declared in the deployed `run/<APP>/META-INF/broker.xml` — read that file for the real key names; `ibmint apply overrides` writes any key without validation and the runtime ignores unknown ones.

### Packaging, deploying, verifying

```bash
ibmint package --input-path <WORKSPACE> --output-bar-file bars/<APP>.bar \
  --project <APP> --project <POLICY_PROJECT>   # --project repeats
```

Deploy as normal. Startup log confirms with `BIP9332I: PolicyProject '<name>' has been reloaded successfully`. Inspect deployed policies via the admin REST API:
`GET /apiv2/policies/<project>/policy/<policyName>` (segment is the literal word `policy`).
