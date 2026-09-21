# ace-claude

A portable Claude Code knowledge pack for developing IBM App Connect Enterprise (ACE) integration artifacts. Drop this folder into any ACE Toolkit workspace and Claude Code immediately knows how to build correct, production-quality flows, ESQL, Java, and Maps.

## Contents

| File / Folder | Purpose |
|---|---|
| `CLAUDE.md` | Auto-loaded by Claude Code — standing rules and pointers to the skill and examples |
| `SKILL.md` | Full ACE knowledge base: node types, msgflow XML structure, ESQL patterns, ODBC/JDBC, error handling, mapping patterns |
| `examples/` | Annotated reference files Claude reads before creating artifacts (see below) |
| `examples/mq_nodes.msgflow` | Correct MQ Input/Output node XML, `messageDomainProperty`, terminal names |
| `examples/http_nodes.msgflow` | HTTP Input, WSRequest (`httpVersion`, `protocol`, `messageDomainProperty`), WSReply |
| `examples/subflow_terminals.subflow` | Correct `InTerminal.Input` / `OutTerminal.Output` xmi:ids, `subflowImplFile` warning |
| `examples/esql_patterns.esql` | 7 annotated patterns: DECLARE order, repeating elements, FOR loop, datetime `HH`, error handlers, namespace mapping |
| `examples/unittest/` | Seven worked test classes indexed by technique; the rules live in `Testing.md` |
| `examples/timer/` | Timer-family flows: Scheduler interval + cron, automatic TimeoutNotification, TimeoutControl set/cancel + controlled replay, plus `TIMER_POLICY_TEMPLATE/` |
| `examples/routing/` | Routing-family flows: Route (HTTP + MQ), Filter (`FILTER MODULE`, three terminals), RouteToLabel/Label (`RouterList`), FlowOrder |
| `examples/rest/` | REST flows against the Swagger Petstore: no-parameter call, query parameter, async request/response pair |
| `examples/slack/` | Slack connector: private-channel retrieve + send message, policy template, `gen/` schemas |
| `examples/policy/` | Verified policy project template (13 policies) + `flows/` with the matching attachment msgflows/ESQL + `tools/` (WLM override file, proxy-proof script) + READMEs with attachment matrix, vault commands, proof patterns |
| `Timer.md` | Timeout Control / Timeout Notification / Scheduler nodes: modes, the MQ prerequisite, the timeout-request format, the Scheduler cron grammar, the Timer policy |
| `Testing.md` | Flow unit testing: the harness model, NodeSpy/NodeStub, assertion styles, classpath and error decoder — read only for testing work |
| `REST.md` | REST Request / Async Request+Response / App Connect REST Request: spec-driven config, the `<parameters>` element, async correlation |
| `Slack.md` | Slack connector runbook: object/action matrix, token types and scopes, `noData`, CREATE schema + `OBJECT_NAME` rules |
| `AmazonS3.md`, `Salesforce.md`, … | Connector-specific node property and policy guidance (supplements `SKILL.md`) |
| `PolicyProject.md` | Policy project scaffolding + generic policy reference (types, .policyxml format, attachment, packaging, REST verification) |
| `IntegrationServer.md` | Running standalone integration servers: lifecycle, autonomous verification patterns, vault keys, gotchas |
| `LEARNINGS.md` | Running log of verified gotchas (docs errors, BIP decoder ring, vault/MQ traps) — grows as new artifact types get proven |
| `LICENSE` | Apache License 2.0 — free to use, copy, modify, distribute (see `NOTICE`) |

## Setup

### Per workspace (recommended)

Place this folder directly inside your ACE Toolkit Eclipse workspace:

```
your-workspace/
  ace-claude/       ← clone here
  MY_APP/
  MY_OTHER_APP/
```

Claude Code automatically loads `ace-claude/CLAUDE.md` at the start of every session in that workspace — no further configuration needed.

```bash
cd /path/to/your/ace/workspace
git clone <this-repo-url> ace-claude
```

### Optional: workspace-level CLAUDE.md

If you want to add workspace-specific context (project inventory, team conventions, environment notes), create a `CLAUDE.md` at the workspace root alongside the `ace-claude/` folder:

```
your-workspace/
  CLAUDE.md         ← workspace-specific context (optional)
  ace-claude/       ← this repo
  MY_APP/
```

### Global setup (available in every session)

```bash
git clone <this-repo-url> ~/.claude/ace-claude
```

## How it works

When you open a workspace containing `ace-claude/`, Claude Code:
1. Loads `ace-claude/CLAUDE.md` — picks up standing rules and is directed to the skill
2. Reads `ace-claude/SKILL.md` in full before creating any ACE artifact
3. Reads the relevant `ace-claude/examples/` file for the artifact type being built
4. Reads the relevant connector MD file when the task involves a specific connector

## What Claude will and won't do

**Will do automatically:**
- Use correct msgflow namespace URIs and node xmi:type prefixes
- Set `messageDomainProperty=` (never `messageDomain=`) on MQ Input nodes
- Set `httpVersion="1.1"`, `protocol="TLS"`, `messageDomainProperty=` on every WSRequest node
- Place InputRoot REFERENCE declarations before SETs; OutputRoot REFERENCE declarations after tree creation
- Use `HH` (24-hour) in all datetime format strings — never `hh`
- Scope EXTERNAL variables at application level — one declaration per app
- Forward `LocalEnvironment` through any Compute node feeding into WSReply

**Will NOT do unless explicitly asked:**
- Add TryCatch nodes, catch terminals, or failure path wiring
- Set `computeMode` on Compute nodes (only added when ESQL touches `OutputLocalEnvironment`/`OutputDestination`)

## To-Do / Future Improvements

- [x] Policy projects: scaffold, HTTPRequest/UserDefined/Timer/WLM/ActivityLog/MQEndpoint/JDBCProviders/HTTPReply examples, runtime-verified
- [x] SecurityProfiles + vault credentials: inbound Local auth and outbound credential injection, runtime-verified
- [x] TCPIP Client/Server policies: connectionDetails attachment, socket round trip, runtime-verified
- [x] HTTP Proxy policy: deploy-verified; attachment (`proxyName`) is a post-13.0.2.2 fixpack feature — proxy transit proven via literal `httpProxyLocation`
- [x] Aggregation / Collector / Resequence: policies + all three node families runtime-verified — `examples/eda/`
- [ ] Add guidance on subflow library division and dependent library projects
- [ ] Expand connector MD files with more real-world policy examples
- [x] JavaCompute patterns example (JDBC, MbMessage/MbJSON API) — `JDBC.md` + `examples/jdbc/`, runtime-verified vs PostgreSQL
- [x] Database node (DatabaseRetrieve) example — `examples/dbnode/` incl. the undocumented msgflow grid encoding, runtime-verified
- [x] DFDL domain: schema anatomy, node config, parse + write flows, error signatures — `DFDL.md` + `examples/dfdl/`, runtime-verified
- [x] Flow unit testing, fully headless (Test Project + NodeSpy + `--test-project`) — `examples/unittest/`, 3/3 pass
- [x] ODBC + direct-ESQL-SELECT database access — `ODBC.md` + `examples/odbc/`, runtime-verified vs PostgreSQL
