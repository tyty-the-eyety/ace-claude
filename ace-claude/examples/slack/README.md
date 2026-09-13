# Slack connector — ApplicationConnector Request node (runtime-proven, ACE 13.0.2.2)

Copy-ready flows for `ComIbmApplicationConnectorRequest_slack`. Both were driven
against a real Slack workspace and the results observed in Slack. Full
reference: `../../Slack.md`. Proof app: `demo-apps/SLACK_DEMO_APP` +
`demo-apps/SLACK_DEMO_POLICIES`.

| File | Action | Object | Request shape | Endpoint |
|---|---|---|---|---|
| `slack_flow` | `RETRIEVEALL` | `group` (private channels) | **filter only** — `<queryProperties limit=…/>` | `POST /slack/channels` |
| `slack_send_flow` | `CREATE` | `message` | **body only** — `OBJECT_NAME` + `text` | `POST /slack/send` |

`SLACK_DEMO_POLICIESICY_TEMPLATE/` is the policy project. `gen/` holds the schema files —
rename all of them if you rename the flows, because `schemaPrefix` derives from
the flow name.

Observed results:

```
POST /slack/channels → {"channels":[{"id":"C01234ABCDE","name":"ace-slack-test","isArchived":false}],"count":1}
POST /slack/send     → {"status":"sent","ts":"1700000000.000100"}
```

## Read this before adapting them

**1. `channel` means PUBLIC channels, `group` means PRIVATE.** Slack's legacy
naming, and the easiest thing here to get wrong. `group`/`RETRIEVEALL` needs
`groups:read`; `channel`/`RETRIEVEALL` needs `channels:read`. A bot sitting in a
public channel will never show up in a `group` query.

**2. `OutTerminal.noData` must be wired on any retrieve.** This node has four
terminals — `out`, `noData`, `failure`, plus `in`. A zero-result `RETRIEVE*`
routes to `noData`, and if it is unwired the empty result becomes an exception:

```
BIP2230E: Error detected whilst processing a message in node 'slack_flow.Slack Request'.
BIP9975E: No documents found.
```

Over HTTP that is a **404**, which looks nothing like "nothing matched".
`slack_flow` wires it to a `no_channels` Compute that returns
`{"channels":[],"count":0}` with a clean 200.

**3. `limit` on `queryProperties`, not just `allowTruncation`.** Without it the
retrieve silently returns one record — the same trap as S3.

**4. A `CREATE` needs its `request` schema file to exist**; a `RETRIEVEALL` does
not. Missing one fails at *flow start*:

```
BIP9958E: ... cannot find the deployed JSON schema file
          'gen/slack_send_flow.Slack_Request.request.schema.json'
```

The content can be `{}`, but the file must not be zero bytes (`BIP5753E`).

**5. `OBJECT_NAME` is the parent discriminator and is mandatory on `CREATE`.**
For `message` it is the channel. `OBJECT_ID` alone is not enough even though the
connector's model lists both in the interaction's `basic` set:

```
BIP9937E: ... 'Error: Parent discriminator connector property OBJECT_NAME - not found in body or filter'
```

Both go in the request **body** via ESQL — not in a `<connectorProperty/>` row
the way S3's `bucketName` does.

**6. The `xmlns` URI is the bare filename.** Both Slack `.msgnode` files sit at
the root of `com.ibm.etools.mft.connectornodes.definitions_13.0.2.2.jar`, so
unlike Kafka there is no slash path:

```xml
xmlns:ComIbmApplicationConnectorRequest_slack.msgnode="ComIbmApplicationConnectorRequest_slack.msgnode"
```

## Setting it up

1. Rename `SLACK_DEMO_POLICIESICY_TEMPLATE` (directory, `.project` `<name>`) and point
   `policyUrl="{<YourPolicyProject>}:Slack1"` at it.
2. Get a **`xoxb-`** (or `xoxp-`) token from the app's **OAuth & Permissions**
   page — *not* the App Configuration Token panel at the bottom of the apps
   list, which yields unusable `xoxe.xoxp-` tokens. See `../../Slack.md`.
3. Store it under the name the policy expects:
   ```bash
   mqsicredentials --work-dir <workdir> --vault-key <key> --create \
     --credential-type slack --credential-name SLACK_DEMO_POLICIES_Slack1 --access-token xoxb-...
   ```
   `slack` accepts only `--access-token`. Without the credential the flow will
   not start (`BIP1374E`).
4. Scopes: `groups:read` for `slack_flow`, `chat:write` for `slack_send_flow`.
   Verify before deploying — one call beats a deploy cycle:
   ```bash
   curl -s -X POST https://slack.com/api/auth.test -D - -o /dev/null \
     -H "Authorization: Bearer $TOKEN" | grep -i x-oauth-scopes
   ```

## Not possible with a bot token

`message`/`RETRIEVEALL` mandates a `query` filter, which maps to Slack **search**
rather than `conversations.history`, and search rejects bot tokens
(`not_allowed_token_type`). `groups:history` does not help. For "last N messages
in a channel", use an HTTPRequest node against `conversations.history`. Details
in `../../Slack.md`.

## Slack behaviours that look like ACE faults

- A bot sees only private channels it has been **invited to**, so `count: 0` can
  be the correct answer. Channel name → **Integrations** → **Add an App** is the
  reliable way to add it; `/invite @name` depends on the app's display name.
- A channel created via `conversations.create` has the caller as its **only**
  member — if a bot created it, you cannot see it in Slack until you are invited.
- `chat:write` only permits posting to channels the bot belongs to.
