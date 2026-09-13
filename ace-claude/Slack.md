## Slack node Requirements

> **Status: runtime-proven (ACE 13.0.2.2, workspace `myworkspace`, 2026-09-13).**
> Two actions driven end to end against a real Slack workspace and the results
> observed in Slack: `group`/`RETRIEVEALL` (list private channels) and
> `message`/`CREATE` (post a message). Copy-ready flows in `examples/slack/`,
> proof app `demo-apps/SLACK_DEMO_APP` + `demo-apps/SLACK_DEMO_POLICIES`.
> `message`/`RETRIEVEALL` is **proven impossible with a bot token** — see
> *Reading messages*. Everything else on this page is from the connector's own
> model files and is not runtime-verified.

1. Request nodes: `xmi:type="ComIbmApplicationConnectorRequest_slack.msgnode"`,
   `applicationConnectorType="slack"`. The event/input node is
   `ComIbmApplicationConnectorInput_slack.msgnode`.
2. **`xmlns` URI is the bare filename** — `xmlns:ComIbmApplicationConnectorRequest_slack.msgnode="ComIbmApplicationConnectorRequest_slack.msgnode"`.
   Both `.msgnode` files sit at the **root** of
   `tools/plugins/com.ibm.etools.mft.connectornodes.definitions_13.0.2.2.jar`,
   so unlike Kafka there is no slash path.
3. Set `schemaPrefix="gen/<FlowName>.Slack_Request"`. Which schema files must
   exist **depends on the action** — see *Schema files*.
4. Set `policyUrl="{<PolicyProjectName>}:Slack1"`.
5. Required node attributes (`lowerBound="1"`): `applicationConnectorType`,
   `action`, `businessObject`, `displayName`, `schemaPrefix`, `policyUrl`,
   `dataLocation` (default `$Body`), `outputDataLocation` (default
   `$OutputRoot`), `resultDataLocation` (default `$ResultRoot`),
   `mappingMode` (default `none`), `outputDestinationMode` (default
   `MessageAssembly`).
6. Terminals: `InTerminal.in`, `OutTerminal.out`, **`OutTerminal.noData`**,
   `OutTerminal.failure`. See *The noData terminal* — this one is not optional in
   practice.

---

## Objects and actions

The authoritative list is the connector's own object model, not the docs:

```bash
python3 -c "import json;[print(o['name'], sorted((o.get('interactions') or {}).keys())) for o in json.load(open('/opt/IBM/ace-13.0.2.2/server/nodejs_all/node_modules/@ibm-app-connect/loopback-connector-slack/overrides/objects.json'))]"
```

| `businessObject` | displayName | Actions | Slack scope needed |
|---|---|---|---|
| `message` | Messages | `CREATE` (Send message), `RETRIEVEALL` (Retrieve messages) | `chat:write` / `search:read` |
| `files` | Files | `CREATE`, `DELETEALL`, `RETRIEVE`, `RETRIEVEALL` | `files:*` |
| `channel` | Channels | `CREATE`, `UPDATE`, `RETRIEVE`, `RETRIEVEALL` (Retrieve **public** channels) | `channels:read` / `channels:write` |
| `group` | Private channels | `CREATE`, `UPDATE`, `RETRIEVE`, `RETRIEVEALL` (Retrieve **private** channels) | `groups:read` / `groups:write` |
| `im` | Direct messages | `RETRIEVEALL` | `im:read` |
| `mpim` | Multi person direct messages | `RETRIEVEALL` | `mpim:read` |
| `user` | Users | `RETRIEVE`, `RETRIEVEALL` | `users:read` |
| `usergroup` | User groups | `CREATE`, `UPDATEALL`, `RETRIEVEALL` | `usergroups:*` |
| `RawMessage` | Raw messages | `CREATED` (New slash command message) | — (input node) |

> **`channel` means public, `group` means private.** This is Slack's legacy
> naming and it is the single easiest thing to get wrong. A bot in a public
> channel will never appear in a `group` query and vice versa.

Per-object request/response contracts live in
`loopback-connector-slack/lib/models/<object>.json` — read `interactions` for
`requestProperties.mandatory` and `responseProperties.included`, and
`filterSupport` for what a retrieve needs.

---

## Authentication

### The policy

`policyType="slack"` is **not in `Policy.xsd`** — connector policy types are
validated from the connector descriptors instead, so don't expect to find it
there. The Toolkit-generated form:

```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<policies>
  <policy policyName="Slack1" policyTemplate="online_v1_basic_oauth" policyType="slack">
    <applicationType>online</applicationType>
    <applicationVersion>v1</applicationVersion>
    <authenticationMethod>BASIC_OAUTH</authenticationMethod>
    <credentialName>SLACK_DEMO_POLICIES_Slack1</credentialName>
  </policy>
</policies>
```

`authenticationMethod` maps to a field in
`loopback-connector-slack/descriptors/slack.json`: `BASIC_OAUTH` →
`access_token_basic`, `OAUTH2_WEB` → `access_token`. **Both are just a static
access token** — neither takes a client id/secret or a refresh token.

### The vault credential

```bash
mqsicredentials --work-dir <workdir> --vault-key <key> --create \
  --credential-type slack --credential-name SLACK_DEMO_POLICIES_Slack1 \
  --access-token xoxb-...
```

- The credential name must equal the policy's `credentialName`.
- **`slack` takes exactly one property, `--access-token`.** There is no
  `--refresh-token` (compare `anaplan`, which has an oauth variant with four).
  So ACE cannot rotate a Slack token; a rotating token must be re-entered by
  hand.
- Missing credential = the flow will not start:
  ```
  BIP1374E: The credential 'SLACK_DEMO_POLICIES_Slack1' of type 'Slack' is required by the
            policy '{SLACK_DEMO_POLICIES}:Slack1' used by message flow node
            'Slack Request'/'slack_flow' but it is not defined.
  ```

### Getting a usable token (this wasted the most time)

Slack has two unrelated token systems and the wrong one is easier to find.

| Prefix | What it is | Where | Usable? |
|---|---|---|---|
| `xoxe.xoxp-` | App Configuration Token | api.slack.com/apps → **bottom of the apps list** | ❌ |
| `xoxe-` | its refresh partner | same panel | ❌ |
| `xoxb-` | Bot User OAuth Token | click into the app → **OAuth & Permissions** | ✅ |
| `xoxp-` (no dot) | User OAuth Token | same page | ✅ |

**If it starts `xoxe`, it is the wrong panel.** Those tokens carry
`identify, app_configurations:read, app_configurations:write`, expire in ~12
hours, and exist only to edit app manifests. Symptom:

```
{"ok":false,"error":"missing_scope",
 "needed":"channels:read,groups:read,mpim:read,im:read",
 "provided":"identify,app_configurations:read,app_configurations:write"}
```

Verify any token before wiring it — one call saves a deploy cycle:

```bash
curl -s -X POST https://slack.com/api/auth.test -D - -o /dev/null \
  -H "Authorization: Bearer $TOKEN" | grep -i x-oauth-scopes
```

**Adding scopes and reinstalling widens the existing token in place** — it does
*not* necessarily issue a new string. Observed: the same `xoxb-` token gained
`chat:write, groups:history` after a reinstall, so the vault needed no change.
Check the old token's scopes before hunting for a new one.

---

## Schema files

`gen/<schemaPrefix>.request.schema.json` / `.response.schema.json`. **Each must
contain at least `{}`** — an empty file passes `ibmint package` and
`mqsicreatebar -cleanBuild`, then fails at runtime with `BIP5753E ... The
document is empty`.

Which files are required is action-dependent, and this is not documented:

| Action | request schema | response schema |
|---|---|---|
| `RETRIEVEALL` | not required | required if `messageSetProperty` names it |
| `CREATE` | **required** | not required |

A missing request schema on a `CREATE` fails at **flow start**:

```
BIP9958E: The discovery connector node 'Slack'/'slack_send' in message flow
          'slack_send_flow' cannot find the deployed JSON schema file
          'gen/slack_send_flow.Slack_Request.request.schema.json' for the
          request schema based on schema base name 'gen/slack_send_flow.Slack_Request'.
```

---

## The `noData` terminal

Unlike the S3 and LDAP nodes documented earlier, the Slack request node has a
**fourth terminal, `OutTerminal.noData`**, and a zero-result `RETRIEVE*` routes
there. **Leave it unwired and an empty result set becomes an exception**, not an
empty response:

```
BIP2230E: Error detected whilst processing a message in node 'slack_flow.Slack Request'.
BIP9975E: No documents found.
```

Over HTTP that surfaces as a **404**, which looks nothing like "your query
matched zero rows". Wire it to a Compute that returns an empty result:

```xml
<connections xmi:type="eflow:FCMConnection" xmi:id="FCMConnection_4"
  targetNode="FCMComposite_1_5" sourceNode="FCMComposite_1_1"
  sourceTerminalName="OutTerminal.noData" targetTerminalName="InTerminal.in"/>
```

Treat this as mandatory on every connector retrieve, not just Slack's.

---

## Supplying a request

### `RETRIEVEALL` — filter only

```xml
<filter>
  <queryProperties limit="50" allowTruncation="true"/>
</filter>
```

**`limit` is required in practice.** With `allowTruncation` alone the retrieve
silently returns a single record — the same trap as S3.

### `CREATE` — body only, and `OBJECT_NAME` is mandatory

No `<filter>`. The body carries the request properties, built in ESQL:

```sql
SET OutputRoot.JSON.Data.OBJECT_NAME = 'ace-slack-test';   -- parent discriminator
SET OutputRoot.JSON.Data.OBJECT_ID   = 'C01234ABCDE';
SET OutputRoot.JSON.Data.text        = 'Hello from IBM ACE';
```

`OBJECT_NAME` identifies the parent object — for `message` that is the channel —
and the connector refuses the request without it, even though the model lists
both `OBJECT_NAME` and `OBJECT_ID` in the interaction's `basic` set:

```
BIP9937E: Unexpected failure invoking JavaScript function,
          'Error: Parent discriminator connector property OBJECT_NAME - not found in body or filter'
```

These go in the **body**, not in a `<connectorProperty/>` row the way S3's
`bucketName` does. A `CREATE` returns `ts`, Slack's message id, at
`InputRoot.JSON.Data.ts` in the next node.

---

## Reading messages: not possible with a bot token

`message`/`RETRIEVEALL` mandates a `query` filter:

```json
"filterSupport": {"queryableFields":["query"],"mandatory":["query"],
                  "supportedFilters":["TOKEN"],"actions":["RETRIEVEALL"]}
```

That maps to Slack **search**, not `conversations.history`, and search is closed
to bot tokens:

```
search.messages       → {"ok":false,"error":"not_allowed_token_type"}
conversations.history → ok:true   (works, but the connector never calls it)
```

So `groups:history` does not unlock it. Options:

- a **user** token (`xoxp-`) with `search:read` — but it searches the whole
  workspace, not one channel;
- for "last N messages in channel X", use an **HTTPRequest node** against
  `conversations.history` instead of this connector. That is the honest answer.

---

## Slack behaviours that look like ACE faults

- **Private channels are invisible to non-members, in both directions.** A bot
  with `groups:read` sees only private channels it has been *invited to*, so a
  correct flow legitimately returns `count: 0`. Conversely a channel created by
  the bot (`conversations.create`) has the bot as its only member and will not
  appear in your own Slack sidebar until you are invited
  (`conversations.invite`).
- **`/invite @name` depends on the app's display name**, not the value
  `auth.test` reports. The reliable path is channel name → **Integrations** →
  **Add an App**.
- **`chat:write` only allows posting to channels the bot belongs to.**

## BIP codes

| Code | Meaning |
|---|---|
| `BIP1374E` | vault credential named by the policy is missing |
| `BIP9958E` | required `gen/*.schema.json` not deployed (CREATE needs the request one) |
| `BIP5753E` | a `gen/*.schema.json` exists but is zero bytes — put `{}` in it |
| `BIP9975E` | zero-result retrieve with `noData` unwired |
| `BIP9937E` | connector-side JS error; read the quoted text (e.g. missing `OBJECT_NAME`) |
