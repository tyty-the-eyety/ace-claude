---
name: ace-integration-server
description: >
  Start, run, test, and stop standalone (independent) IBM ACE integration
  servers with the IntegrationServer command — NOT integration nodes/brokers.
  Use when a task involves spinning up an integration server, running a deployed
  BAR live, smoke-testing or unit-testing a message flow, creating or pointing at
  a server work directory, choosing IntegrationServer parameters (ports, vault
  key, test project, stop-after-duration, log format), or running flows
  non-interactively for autonomous verification in a homelab/LXC. Triggers:
  "spin up a server", "run the BAR", "start the integration server",
  "test my flow live", "run the test project", "stop the server".
---

# ACE Integration Server (standalone / independent)

This skill covers running an **independent integration server** via the
`IntegrationServer` command. An independent server is self-contained: it has its
own work directory and is started directly, with no integration node (broker)
managing it. Use this — not `mqsicreatebroker`/node administration — for homelab,
container, and autonomous build-verify work.

## When to use vs an integration node
- **Independent server (this skill):** one server, one work dir, started by
  `IntegrationServer --work-dir ...`. Lightweight, container-friendly, ideal for
  dev/test and CI-style verification.
- **Integration node (broker):** a node process administering multiple servers,
  managed with the `mqsi*` node commands. Heavier; only reach for it if a task
  explicitly needs node-managed servers. Default to the independent server.

## Core concepts
- **Work directory:** the server's home. Everything deployed (BARs, overrides,
  config, vault) lives under it. The server *is* its work dir.
- **server.conf.yaml:** the server's config file inside the work dir (ports,
  logging, resource managers). Edit this for persistent config rather than
  passing everything as flags.
- **Independent resources:** anything deployed that is not inside an application
  or library. If you deploy these, you must set a default application on first
  start (see `--default-application-name`).

## Lifecycle

Always source the profile first so the commands are on PATH:
```bash
. <ACE_INSTALL>/server/bin/mqsiprofile
```

### 1. Create the work directory (once)
```bash
mqsicreateworkdir <WORKDIR>
```

### 2. Deploy a BAR into it
Deploy headlessly into the work dir **before** starting the server:
```bash
ibmint deploy --input-bar-file <APP>.bar --output-work-directory <WORKDIR>
```
The server reads its work dir on startup, so deploying first means everything is
available the moment the server initialises — no second step needed. This is the
correct approach for the build loop.

Do **not** use `POST /apiv2/deploy` (the admin REST API) in the build loop.
That endpoint requires the server to already be running and is reserved for
pushing updates to a long-lived server without restarting it (see
[Deploying to a running server](#deploying-to-a-running-server) below).

### 3. Start the server
```bash
IntegrationServer \
  --work-dir <WORKDIR> \
  --name <SERVER_NAME> \
  --admin-rest-api <ADMIN_PORT> \
  --http-port-number <HTTP_PORT> \
  --console-log --log-output-format ibmjson
```
Enter it on one line in practice; line breaks above are only for readability.

### 4. Confirm it's ready
`IntegrationServer` runs in the **foreground** and does not return — so detect
readiness out-of-band. Prefer polling the admin REST API (deterministic, no
reliance on a specific BIP code):
```bash
until curl -sk https://localhost:<ADMIN_PORT>/apiv2/ >/dev/null; do sleep 2; done
```
Note: the admin REST API is **HTTPS** (BIP3132I confirms this). Using `http://`
silently fails even when the server is fully ready. Always use `-sk` to skip
certificate verification against the self-signed cert.
Fallback: watch the event log for BIP1991I ("Integration server has finished
initialization").

### 5. Smoke test
Hit an input node and check the response, e.g. an HTTP flow:
```bash
curl -s -m 10 http://localhost:<HTTP_PORT>/<endpoint> -d '<test payload>'
```

### 6. Stop the server
Preferred: POST to the shutdown endpoint — clean, no signal arithmetic needed:
```bash
curl -sk -X POST https://localhost:<ADMIN_PORT>/apiv2/shutdown
```
This returns immediately (empty 200); the process exits a few seconds later.
Confirm before proceeding:
```bash
until ! kill -0 <PID> 2>/dev/null; do sleep 1; done
```
Alternative: send SIGTERM directly to the captured PID. Or use
`--stop-after-duration` so the server stops itself.

## Autonomous verification patterns (the important part)

Because the command is a foreground process, pick one of these for hands-off use:

**A. Ephemeral smoke-test run (self-cleaning).** Bound the server's lifetime so
no orphan process is left behind:
```bash
# deploy first, then start — the server reads the work dir on init
ibmint deploy --input-bar-file <APP>.bar --output-work-directory <WORKDIR>
IntegrationServer --work-dir <WORKDIR> --name <SERVER_NAME> \
  --admin-rest-api <ADMIN_PORT> --http-port-number <HTTP_PORT> \
  --stop-after-duration 90 --console-log --log-output-format ibmjson &
# poll: until curl -sk https://localhost:<ADMIN_PORT>/apiv2/ >/dev/null; do sleep 2; done
# run smoke test inside the window, server stops itself
```

**B. Built-in unit-test run (preferred for flow correctness).** The server can
run a test project and exit when done — this is the cleanest "agent checks its
own work" path:
```bash
IntegrationServer --work-dir <WORKDIR> \
  --test-project <TEST_PROJECT> \
  --start-msgflows false \
  --no-nodejs
```
- `--start-msgflows false` initialises flows in a stopped state so they don't take
  live traffic while the tests run.
- `--no-nodejs` skips Node.js init for a faster start. Note it also **disables the
  admin REST API**, so don't combine it with the REST-API readiness check in
  pattern A — here you don't need to, the run exits on its own.

**C. Long-lived dev server.** For a persistent target you just deploy into,
run it under systemd in the dev LXC and let the build loop deploy BARs into its
work dir.

Use `--console-log --log-output-format ibmjson` whenever the agent must parse
output — JSON-formatted BIP messages are far more reliable to analyse than text.

## Parameter reference (curated, reworded)
- `--work-dir <path>` — **required**. The server's work directory.
- `--name <name>` — server name. If omitted, defaults to the last folder of the
  work-dir path.
- `--admin-rest-api <port>` — enables the admin REST API on this port. The
  listener is **HTTPS** (BIP3132I). Always access it with `https://` and `-sk`.
- `--http-port-number <port>` — reserves the HTTP port in config, but the
  listener only binds when a deployed HTTP Input node needs it. An empty server
  will refuse connections on this port — that is expected, not an error.
- `--default-application-name <name>` — **required the first time** you start a
  server that has independent (non-app/library) resources deployed; creates a
  default app and moves those resources into it. Skipping this is a common
  first-start failure.
- `--console-log` — send event log (BIP messages) to stdout.
- `--event-log <file>` — send the event log to a file instead of stdout.
- `--log-output-format text|ibmjson` — human text (default) or machine-readable
  JSON. Use `ibmjson` for autonomous parsing.
- `--start-msgflows true|false` — whether flows accept input on init (default
  true). Set false to init flows stopped so tests can run without live traffic.
- `--test-project <name>` — run that test project's tests, then stop.
- `--no-nodejs` — skip Node.js init (faster start). Disables JS-dependent nodes
  (e.g. SalesforceRequest, LoopBackRequest) **and the admin REST API**.
- `--stop-after-duration <seconds>` — auto-stop after N seconds. Ideal for
  ephemeral autonomous runs.
- `--mq-queue-manager-name <qmgr>` — associate a local default queue manager.
- `--overrides-directory <dir>` — extra config overrides for the server.
- `--vault-key <key>` / `--vaultrc-location <path>` / `--ext-vault-key <key>` —
  supply the vault key when the work dir contains a vault. `--ext-vault-key` is
  for the **External Directory Vault**. These can also come from the
  `MQSI_VAULT_KEY` / `MQSI_VAULTRC_LOCATION` env vars, or a `.mqsivaultrc` file in
  HOME. A server with a vault will not start without the correct key.
- `--diagnostic-trace` / `--service-trace` / `--service-trace-size <size>` —
  tracing; heavy, only for deep debugging.

## Gotchas
- **Foreground process** — it does not daemonise. Background it, bound it with
  `--stop-after-duration`, or run it under systemd. Never assume the command
  returns on its own (except `--test-project` and `--stop-after-duration` runs).
- **First start with independent resources** needs `--default-application-name`.
- **Vault present?** Startup fails without the matching key.
- **Port conflicts** in a shared LXC — set `--http-port-number` and
  `--admin-rest-api` explicitly and confirm they're free.
- **`--no-nodejs` kills the admin REST API** — don't rely on REST readiness when
  it's set.
- **One line** — always issue the command on a single line.
- **Admin REST API is HTTPS, not HTTP** (confirmed ACE 13.0.2.2). BIP3132I
  says "RestAdmin **https**". Poll with `curl -sk https://localhost:<ADMIN_PORT>/apiv2/`
  — using `http://` will silently fail even when the server is fully up.
- **HTTP port only opens when a flow needs it.** `--http-port-number` reserves
  the port in config but the listener does not bind until an HTTP Input node is
  deployed and started. An empty server will refuse connections on that port.
- **Graceful shutdown via REST is async.** `POST /apiv2/shutdown` returns
  immediately (empty 200); the process takes a few more seconds to exit. Confirm
  with `kill -0 <PID>` before declaring the server down.

## Deploying to a running server

Use this only when you need to push an update to a long-lived server **without
restarting it** (Pattern C). Not for the build loop.

```bash
curl -sk -X POST https://localhost:<ADMIN_PORT>/apiv2/deploy \
  -H "Content-Type: application/octet-stream" \
  --data-binary @<APP>.bar
```

The response is a JSON `responseLog` array. A successful deploy returns two BIP
entries: `BIP9332I` (application created/updated) and `BIP9326I` (BAR deployed).
Any `severityCode: "E"` entry means the deploy failed — read the `text` field.

| Method | When to use |
|---|---|
| `ibmint deploy --output-work-directory` | Build loop: deploy before starting the server |
| `POST /apiv2/deploy` | Live update: push to an already-running long-lived server |

## Integration with the build loop
This skill is the run-and-verify half of the CLAUDE.md build loop:
`ibmint package` → `ibmint deploy --output-work-directory <WORKDIR>` →
`IntegrationServer --work-dir <WORKDIR> ...` → smoke/test → stop. When a new node
or artifact type runs clean here for the first time, save a minimal working
example and note the gotcha per the project's self-extension rule.

## Tracing (runtime-verified 2026-09-06)

Needed when a failure happens *inside* a node's own runtime — connector nodes in
particular raise `BIP4000E` / `BIP9937E` from their Node.js layer, and only a
service trace explains them.

**Scope the trace to ONE app.** A whole-server trace on a work dir with a dozen
apps deployed produced a 229MB file in which the flow under investigation was a
few dozen lines. Deploy the app on its own into a throwaway work dir first:

```bash
mqsicreateworkdir /tmp/trace-server
ibmint deploy --input-bar-file <APP>.bar --output-work-directory /tmp/trace-server
IntegrationServer --work-dir /tmp/trace-server --vault-key <key> \
  --service-trace --user-trace
```

**Two things that will waste your time:**

- `trace:` and `userTrace:` in `server.conf.yaml` produce **no trace files at
  all** for an independent integration server. Use the command-line flags
  `--service-trace` / `--user-trace`.
- Output lands in `<work-dir>/config/common/log/`, **not** `<work-dir>/log/`:
  - `integration_server.<name>.userTrace.0.txt` — flow/node level, ~1MB
  - `integration_server.<name>.trace.0.txt` — service level, ~200MB+
  Both are already plain text; no `mqsireadlog`/`mqsiformatlog` needed.

`mqsichangetrace` requires `--integration-node` and therefore does **not** apply
to an independent integration server.

Start with user trace alone; add service trace only when you need the connector
layer. In a service trace the useful lines are tagged `<JS>`:

```bash
grep "<JS>" integration_server.<name>.trace.0.txt \
  | grep -v "Statistics.js\|WebSocket\|resolved value"
```

**Gotcha:** ACE redacts any field literally named `Key` from traces as though it
were a secret — `MESSAGE WAS REDACTED AS IT CONTAINED SECRETS (Key)`. The value
is not recoverable from the trace.

## Vaults: work-dir vs external directory (runtime-verified)

Two separate stores, two separate key flags. Credentials referenced by a
connector policy can live in **either** — the policy only matches on credential
*name* and *type*, not location.

| | work-dir vault | external directory vault |
|---|---|---|
| lives in | `<work-dir>/config/...` | any directory you choose |
| create | `mqsivault --work-dir <wd> --create --vault-key <k>` | `mqsivault --ext-vault-dir <d> --create --ext-vault-key <k>` |
| address it with | `--work-dir <wd>` | `--ext-vault-dir <d>` |
| key flag | `--vault-key` | `--ext-vault-key` |
| server start | `--vault-key <k>` | `--ext-vault-key <k>` |
| server also needs | — | `Credentials.ExternalDirectoryVault.directory` in `server.conf.yaml` |

The external one is for sharing credentials across several servers/nodes. Point a
server at it in `server.conf.yaml`:

```yaml
Credentials:
  ExternalDirectoryVault:
    directory: '/path/to/extdirvault'
```

Writing a credential is the same command either way, only the connectionSpec and
key flag change:

```bash
mqsicredentials --work-dir <wd>       --vault-key <k>     --create --credential-type slack ...
mqsicredentials --ext-vault-dir <d>   --ext-vault-key <k> --create --credential-type slack ...
```

Useful commands: `--report` (lists names/types, no secrets), `--set-as-default`
per credential type, `--export`/`--import` to move credentials between vaults as
an encrypted zip (`--archive-location` + `--archive-key`), and
`mqsivault --verify-key` to test a key without changing anything.

### A forgotten vault key is unrecoverable

`store.yaml` holds an `aes_256_cbc` key derived from the password. There is no
recovery path, and a keyless read is refused outright:

```
BIP15158E: To administer credentials in an integration server vault or integration
           node vault, you must specify a vault access key ... To administer
           credentials in an external directory vault, you must supply an external
           directory vault access key.
```

The only options are to remember it or `mqsivault --destroy` and recreate,
losing every credential in that vault. **Store the key in a `.mqsivaultrc`** so
this cannot happen:

```bash
mqsivault --vaultrc-store-ext-key --ext-vault-dir <d> --ext-vault-key <k>   # external
mqsivault --work-dir <wd> --vaultrc-store-key --vault-key <k>               # work-dir
mqsivault --vaultrc-store-default-key --vault-key <k>                       # default for all
```

`--vaultrc-location <dir>` chooses where the file goes; commands then find the
key without `--vault-key` on the command line. Note that any credential in the
old vault stays stranded — a `.mqsivaultrc` written later cannot open a vault
whose key is already lost.

### Recovering from a lost key without touching the old vault

A stranded vault does not have to block work: give the test server its own
work-dir vault and recreate just the credentials the flow needs. Proven on
2026-09-13 when an external vault's password was forgotten — a fresh
`mqsivault --work-dir ... --create` plus one `mqsicredentials --create` had the
Slack flow running, with the old external vault left untouched for later.

## Source
Built from the IBM App Connect Enterprise 13.0.x *IntegrationServer command*
reference. Most parameters apply to ACE 12 as well; confirm newer ones
(e.g. `--ext-vault-key`) against your 12 install.
https://www.ibm.com/docs/en/app-connect/13.0.x?topic=commands-integrationserver-command
