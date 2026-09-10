# IBM ACE Development — Claude Instructions

## Before any ACE task
Read `ace-claude/SKILL.md` in full before creating or modifying any msgflow, ESQL, Java, or Map artifact.

**Then copy from a runtime-verified artifact rather than assembling node XML from prose.** The projects in `demo-apps/` and the per-capability folders under `ace-claude/examples/` were deployed to a real ACE **13.0.2.2** server, driven with real messages, and validated in the ACE Toolkit (`mqsicreatebar -cleanBuild`, zero problems). Where SKILL.md and one of those artifacts disagree, the artifact is right — fix SKILL.md.

**Always read `ace-claude/LEARNINGS.md`** for the node family you are about to use. It records the gotchas that only surface at runtime or in the Toolkit: undocumented XML encodings, mandatory properties with no usable default, and cases where a flow packages, deploys and runs while still being wrong.

Do not trust arbitrary older workspace flows — but the `demo-apps/` and `examples/` artifacts named here are verified and should be copied, including their `xmlns:` declarations.

Read the relevant example file from `ace-claude/examples/` before creating artifacts of that type:
- `ace-claude/examples/mq_nodes.msgflow` — MQ Input/Output nodes
- `ace-claude/examples/http_nodes.msgflow` — HTTP Input, WSRequest, WSReply
- `ace-claude/examples/subflow_terminals.subflow` — subflow boundary terminal xmi:ids
- `ace-claude/examples/esql_patterns.esql` — DECLARE order, loops, datetime, error handlers, namespace mapping
- `ace-claude/examples/policy/` — 13 verified policies (`POLICY_PROJECT_TEMPLATE/`), the matching attachment flows (`flows/`), and WLM-override/proxy tools (`tools/`); read `README.md` + `flows/README.md`, and `ace-claude/PolicyProject.md`
- `ace-claude/examples/dfdl/` — verified DFDL app (`DFDL_APP_TEMPLATE/`: schema + parse flow + write flow + test payload); read `README.md` there and `ace-claude/DFDL.md`
- `ace-claude/examples/eda/` — runtime-proven Aggregation/Collector/Resequence flows (mode-encoded Resequence start/end, MQOutput→AggregateRequest order, Collector dynamic terminals + repeated-element table rows); read `README.md` there and the LEARNINGS.md "EDA nodes" section BEFORE using these node types
- `ace-claude/examples/odbc/` — runtime-proven ESQL→ODBC PostgreSQL app (PASSTHRU SELECT/INSERT+RETURNING); read `ace-claude/ODBC.md` (step-by-step enablement runbook) + the example `README.md` before any ESQL database work
- `ace-claude/examples/jdbc/` — runtime-proven JavaCompute→JDBC PostgreSQL app (JDBCProviders policy, vault jdbc credential, ibmint-compiled Java project); read `ace-claude/JDBC.md` (runbook) before any JavaCompute or DB-node work
- `ace-claude/examples/dbnode/` — runtime-proven DatabaseRetrieve (built-in JDBC lookup) app; its msgflow grid encoding is entirely undocumented — read `examples/dbnode/README.md` rules 1-6 BEFORE authoring any Database* node
- `ace-claude/examples/unittest/` — runtime-proven headless flow unit testing (Test Project + NodeSpy + ibmint testzip + IntegrationServer --test-project); read its `README.md` before writing flow tests
- `ace-claude/examples/s3/` — runtime-proven Amazon S3 connector (6 actions; both `<filter>` forms — `queryProperties limit=` for retrieve, `filterElementObject type="where"` + `[[$Environment/x]]` for delete/upsert); read its `README.md` and `ace-claude/AmazonS3.md` before any connector work
- `ace-claude/examples/kafka/` — runtime-proven Kafka producer/consumer/read (connector-family nodes, PLAINTEXT and SASL); read its `README.md` before any Kafka work

Read `ace-claude/IntegrationServer.md` when a task involves starting, testing, or stopping an integration server (spinning up a server, running a BAR live, smoke-testing a flow, ephemeral test runs, vault keys, port selection).

## Standing rules
- Do not add error handling (TryCatch, catch terminals, failure paths) unless explicitly requested
- Do not set `computeMode` on a Compute node unless the ESQL touches `OutputLocalEnvironment` or `OutputDestination`
- Use `messageDomainProperty=` on MQ Input nodes — never `messageDomain=`
- Every WSRequest node must have `httpVersion="1.1"`, `protocol="TLS"`, and `messageDomainProperty=` — the runtime does not enforce these, so verify by reading the msgflow, never by smoke-testing alone
- File Input / File Output directories must be ABSOLUTE paths — relative paths fail at startup with BIP3333E
- A Compute node that sets `OutputLocalEnvironment.Destination.File.Name` must have `computeMode="destinationAndMessage"`, and must read the source filename from `InputLocalEnvironment.File.Name` (never `ComIbmFileInput.Response.FileName` — it silently resolves to NULL)
- Connector nodes (`com_ibm_connector_*`, `ComIbmApplicationConnector*`) must have BOTH the right `xmi:type` and the right `xmlns:` URI — the URI is NOT always the prefix repeated (Kafka uses a slash path, e.g. `com/ibm/connector/kafka/ComIbmOutput.msgnode`). A wrong URI packages, deploys and RUNS while the Toolkit reports "Message node ... cannot be located". Copy both lines from `examples/`
- Validate with `mqsicreatebar -data <ws> -a <APP> -cleanBuild` as well as `ibmint package`; read the verdict from the `Problem N: Resource - /PROJECT/...` lines, not the log tail, and note no BAR is written if ANY project in the workspace has errors
- Never use `out` or `in` as ESQL variable or parameter names — they are reserved words
- Datetime format strings must use `HH` (24-hour) — never `hh` (12-hour)
- `DECLARE systemEnv EXTERNAL` (or any EXTERNAL) is app-scoped — one declaration per application is sufficient
- OutputRoot REFERENCE declarations must come after the output path is created (via SET or CREATE LASTCHILD)
- InputRoot references and simple variables (CHARACTER, INTEGER etc.) declare at the top before any SET

## Code reviews
When asked to review an ACE application, produce a `REVIEW.md` in the application folder. Report per flow in point form. Suggest fixes — do not apply them unless asked.


## Autonomous ACE BAR build & verification loop
 
You build and verify ACE artifacts yourself. Do not stop to ask the human to
check a flow — package it, deploy it, and read the result. The human QAs only at
the end, for optimisation, not correctness.
 
### Definition of done (state this to yourself before starting any task)
A task is DONE only when:
1. The BAR packages with a clean exit code (no `BIP....E` errors), AND
2. It deploys to the dev integration server without error, AND
3. The smoke test for the flow returns the expected output.
If you cannot reach all three after the attempt cap below, STOP and write a
failure report. Do not keep editing blindly.
 
### Environment

> **TEMPLATE — set these for your own environment before using this file.**
> The values below are the placeholders/examples this skill was developed
> against. Replace the paths, vault key, queue-manager name, DSN, and version
> numbers to match your install, then delete this note. Everything else in this
> file (the loop, the reading rules, the guardrails) is environment-independent.

- ACE install / profile:  `. <ACE_INSTALL>/server/bin/mqsiprofile`  (example: `/opt/IBM/ace-13.0.2.2/...`)
- Source workspace:       `/path/to/workspace`
- BAR output dir:         `/path/to/workspace/bars`
- Dev server name:        `dev`
- Dev server work dir:    `/path/to/dev-server`  — keep it OUTSIDE the source workspace: `ibmint package --input-path` scans every directory under the input path, and a deployed policy project in `run/` (identified by its `policy.descriptor`) collides with its own source project (`duplicate entry` → BIP8081E)
- Dev server vault key:   `--vault-key <VAULT_KEY>`  (example: `devVaultKey123`) — if the work dir contains a vault, the server will NOT start without this flag
- ODBC (DB flows):        `export ODBCINI=/path/to/dev-server/odbc.ini` before starting the server — DSN `<DSN>` → your database (vault credential of the same name); see `ODBC.md`
- Local MQ:               queue manager `<QUEUE_MANAGER>` (example: `TEST_QM`; MQ at `/opt/mqm`, not on PATH; manual start: `sg mqm -c "/opt/mqm/bin/strmqm <QUEUE_MANAGER>"`)
- (optional) admin host:  `https://localhost:7600`  — admin REST API is **HTTPS** (`curl -sk`)
Operate ONLY inside your own workspace and dev integration server.
Never touch other servers or anything outside `/path/to/workspace`.
 
### The loop
 
```bash
# 0. Source the profile once per session (commands are not on PATH otherwise)
. /opt/IBM/ace-13.0.2.2/server/bin/mqsiprofile
 
# 1. Package — this COMPILES and VALIDATES the flow + ESQL.
#    A malformed msgflow or broken ESQL fails here. Capture all output.
ibmint package \
  --input-path /path/to/workspace \
  --output-bar-file /path/to/workspace/bars/<APP>.bar \
  --project <APP> \
  2>&1 | tee /path/to/workspace/bars/build-<APP>.log
echo "exit=${PIPESTATUS[0]}"
 
# 2. Deploy into the dev server work dir (headless, toolkit-independent)
#    Always use ibmint deploy --output-work-directory, NOT the admin REST API.
#    The server reads its work dir on startup, so deploy-then-start means
#    everything is available immediately with no extra step.
#    Reserve POST /apiv2/deploy for live updates to a running long-lived server.
ibmint deploy \
  --input-bar-file /path/to/workspace/bars/<APP>.bar \
  --output-work-directory /path/to/dev-server \
  2>&1 | tee -a /path/to/workspace/bars/build-<APP>.log
 
# 3. Smoke test — e.g. HTTP input node:
curl -s -m 10 http://localhost:<PORT>/<endpoint> -d '<test payload>'
# or MQ-driven: put a canonical message, read the reply queue, diff vs expected.
```
 
> Confirm exact `ibmint` flags against your own ACE 12 vs 13 installs — syntax
> drifts slightly between versions. The loop structure does not.
 
### Reading the output (this is the part you must get right)
- **Exit code first.** Non-zero on the package step = build failed, full stop.
- **Grep the log for BIP codes.** `BIP....E` = error, must fix. `BIP....W` =
  warning, note it in the QA trail but do not block on it.
- **No matching BIP error but non-zero exit?** Re-read the raw stderr — it's
  usually a missing dependency project, an unresolved schema, or a bad path.
- Keep the full `build-<APP>.log`; never summarise away the original error text.
### Attempt cap (do not burn the session in a loop)
Max **5** fix attempts per task. After the 5th failed attempt, STOP and write
`<WORKSPACE_PATH>/FAILURE-<APP>.md` containing: the final command, the exit code,
the relevant BIP lines, what you changed each attempt, and your best hypothesis
for the human. Then end the task.
 
### QA trail you must leave (so human review is fast)
On every successful build, maintain these in the workspace root:
- `BUILD-LOG.md` — one line per build: timestamp, app, result, BIP warnings.
- `CHANGES.md` — what you edited and why, grouped by task.
- `OPTIMISATION-CANDIDATES.md` — anything you did purely to get it green that
  you suspect is not the clean/idiomatic way (hardcoded values, broad
  exception handling, default DFDL settings, copied boilerplate). This is the
  human's QA worklist — be honest and specific here.
### Self-extension
When you get a NEW artifact type building and deploying clean for the first time
(a policy, a DFDL schema, an S3 request node, etc.):
1. Save a minimal working reproduction into `examples/<type>/`.
2. Append the non-obvious gotchas to `LEARNINGS.md` (one short entry).
Prefer copying and mutating these examples over authoring artifact XML from
scratch — it is far more reliable.
### Guardrails
- Dev server only. Never deploy to any other integration node.
- Never delete source projects; build into `bars/`, deploy into the work dir.
- If the same BIP error recurs across 3 different tasks, add it to `LEARNINGS.md`
  with the fix so you stop rediscovering it.
