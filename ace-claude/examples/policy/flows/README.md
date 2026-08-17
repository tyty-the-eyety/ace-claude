# Verified policy-attachment flows

Byte-for-byte copies of the flows that runtime-proved each policy type on
2026-07-02 (from workspace app `POLICY_DEMO_APP`, ACE 13.0.2.2). Each one shows
the **node-side attachment** for a policy in `../POLICY_PROJECT_TEMPLATE/`.
Copy-mutate these rather than authoring node XML from scratch.

**Before reusing, change:** the plugin name in the two `colorGraphic` lines
(`POLICY_DEMO_APP` → your app), any absolute paths
(`/path/to/dev-server/...` in FileOutput/ActivityLog usage),
ports (7800/5555/8899), and the `{POLICY_DEMO_POLICIES}:...` policy references.

| Flow | Demonstrates | Attachment shown |
|---|---|---|
| `POLICY_BACKEND_MF` | plain HTTP echo backend (target for the client flows); WLM policy attaches to this flow via BAR override — see `../tools/wlm-overrides.txt`, applied with `ibmint apply overrides` | none in the msgflow — `wlmPolicy` lives in the BAR |
| `POLICY_CLIENT_MF` | HTTPRequest policy overriding the node URL (node points at `unreachable.invalid`) | `policyUrl="{proj}:BackendRoute"` on WSRequest |
| `POLICY_TIMER_MF` | Timer policy overriding the node interval (node 1s, policy 3s); FileOutput append pattern (`outputMode="append"`, `recordDefinition="delimited"` — file stays in `mqsitransit/` until finished) | `uniqueIdentifier="{proj}:TickTimer"` on TimeoutNotification (`operationMode="automatic"`) |
| `POLICY_MQ_MF` | MQEndpoint policy supplying the queue manager (node has none configured); HTTP→MQ bridge shape — NOTE: demo does NOT strip HTTPInputHeader before MQOutput, real flows must | `policyUrl="{proj}:DevQM"` on MQOutput |
| `POLICY_SECURE_MF` | inbound basic auth against a vault `local` credential (401 without/wrong creds, 200 with) | `securityProfileName="{proj}:LocalAuthProfile"` + `identityType="usernameAndPassword"` on HTTP Input |
| `POLICY_SECURECLIENT_MF` | outbound credential injection from the vault — caller sends no credentials, gets 200 from the secured endpoint | `policyUrl="{proj}:AuthBackendRoute"` (policy carries `credentialName`) on WSRequest |
| `POLICY_TCPIPSERVER_MF` | TCPIPServer policy defining the listen port (`recordDetection="wholeFile"`, BLOB domain) | `connectionDetails="{proj}:TcpServer5555"` on TCPIPServerInput |
| `POLICY_TCPIPCLIENT_MF` | TCPIPClient policy defining host/port; JSON→BLOB via ASBITSTREAM before the socket | `connectionDetails="{proj}:TcpClient5555"` on TCPIPClientOutput (`closeConnection="afterData"`) |
| `POLICY_PROXYCLIENT_MF` | HTTP proxy transit (unresolvable target host, proxy carries the traffic) — see `../tools/mini_proxy.py` | `httpProxyLocation="localhost:8899"` **literal only at 13.0.2.2** — policy refs fail with BIP2211E |

Run-time prerequisites for the full set: dev vault with `demoUser`/`backendBasic`
credentials (`--vault-key` at server start), queue manager `TEST_QM` with
`TEST.POLICY.Q`, and `mini_proxy.py` running for the proxy flow.

## `../tools/`

- `wlm-overrides.txt` — BAR override file attaching a WorkloadManagement policy
  to a flow (`<flowName>#wlmPolicy={proj}:policy`); apply with
  `ibmint apply overrides <file> --bar-file <bar>` after packaging.
- `mini_proxy.py` — ~50-line logging forward proxy (port 8899) used to prove
  proxy transit: it ignores the target host and forwards to `localhost:7800`,
  so an unresolvable node URL succeeding == traffic went through the proxy.
