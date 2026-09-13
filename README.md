# ace-claude

> Built on **[ace-bob](https://github.com/ot4i/ace-bob)** by Ben Thompson (IBM) —
> see [Credits & origin](#credits--origin).

A [Claude Code](https://claude.com/claude-code) skill for authoring **IBM App
Connect Enterprise (ACE)** artifacts — message flows, ESQL, JavaCompute, DFDL,
policies, and database integrations — entirely from the command line, with no
ACE Toolkit required.

Every capability in the table below was built and **verified at runtime** against
a real ACE 13.0.2.2 server, local IBM MQ, and a live PostgreSQL database —
deployed to a headless integration server, driven with real messages, and the
output observed. `demo-apps/` contains the actual deployable projects used as
proof; all of them build clean with `ibmint package`.

> **Note on authenticity:** the skill authors ACE's XML/ESQL/Java artifacts
> directly. Much of what it knows — especially the undocumented msgflow encodings
> for Collector, Aggregation, Resequence, and DatabaseRetrieve nodes — was
> reverse-engineered from the product's own bytecode and schemas, then proven by
> deploying and driving real messages through the flows.

---

## What's in here

```
ace-claude/            The skill itself — copy this into your Claude Code skills dir.
├── SKILL.md           Authoritative node/ESQL reference (read first).
├── CLAUDE.md          Optional autonomous build-and-verify loop (a template — set your paths).
├── ODBC.md JDBC.md    Step-by-step database enablement runbooks.
├── DFDL.md            DFDL domain runbook.
├── PolicyProject.md   Policy project scaffolding + attachment.
├── IntegrationServer.md  Starting / testing / stopping a headless server.
├── LEARNINGS.md       Log of non-obvious gotchas discovered while building.
├── examples/          Copy-ready, runtime-verified snippets per capability.
└── <Connector>.md     Per-connector notes (S3, Salesforce, Jira, ServiceNow, ...).

demo-apps/             Full deployable ACE projects, one per proven capability.
```

## Installing the skill

Copy the `ace-claude/` directory into your Claude Code skills location:

```bash
# project-level (this repo/workspace only)
cp -r ace-claude .claude/skills/ace-claude

# or global (all your projects)
cp -r ace-claude ~/.claude/skills/ace-claude
```

Then, in a project where you do ACE work, point Claude Code at it — the skill's
`SKILL.md` and `examples/` are self-describing. If you want the autonomous
build-and-verify loop, copy `ace-claude/CLAUDE.md` into your project root and
edit the **Environment** block to match your install (ACE path, workspace,
dev-server work dir, vault key, queue manager, DSN).

## What it can do (all runtime-verified)

| Capability | Proof app | Docs |
|---|---|---|
| MQ flows: input/output, compute, routing, subflows, error handling | `MQ_*_APP` | `SKILL.md` |
| HTTP flows: WSInput / WSRequest / WSReply, JSON transform | `HTTP_JSON_APP`, `HTTP_MQ_BRIDGE_APP` | `SKILL.md` |
| HTTP passthrough: body relayed unchanged (`messageDomainProperty="BLOB"`) | `HTTP_PASSTHRU_APP` | `SKILL.md` |
| XML → JSON mapping: renames, type casts, repeating → JSON array | `XML_JSON_MAPPING_APP` | `SKILL.md` |
| File I/O: FileInput → transform → FileOutput, with TryCatch error path | `FILE_IO_APP` | `SKILL.md` |
| ESQL ↔ ODBC (PASSTHRU **and** direct `SELECT`) | `DB_PG_ODBC_APP`, `DB_ENRICH_APP` | `ODBC.md` |
| JavaCompute + JDBC (JDBCProviders policy) | `PG_JDBC_APP` + `PG_JDBC_JAVA` | `JDBC.md` |
| Built-in DatabaseRetrieve node | `DB_NODE_APP` | `examples/dbnode/` |
| DFDL parse + serialize | `DFDL_DEMO_APP` | `DFDL.md` |
| EDA nodes: Aggregation, Collector, Resequence | `MQ_AGGREGATION_APP`, `MQ_COLLECTOR_APP`, `MQ_RESEQUENCE_APP` | `examples/eda/` |
| Policy projects: MQEndpoint, HTTPRequest, Timer, WLM, TCPIP, Security/vault, ... | `POLICY_DEMO_APP` + `POLICY_DEMO_POLICIES` | `PolicyProject.md` |
| Headless flow unit testing (Test Project + NodeSpy) | `HTTP_JSON_APP_Test` | `examples/unittest/` |
| Amazon S3 connector: create, upsert, download, copy, list and delete objects | `S3_CONNECTOR_APP` + `S3_CONNECTOR_POLICIES` | `AmazonS3.md`, `examples/s3/` |
| Kafka: producer, consumer and read nodes, PLAINTEXT and SASL | `KAFKA_DEMO_APP` + `KAFKA_DEMO_POLICIES` | `examples/kafka/` |
| LDAP connector: search, create, update, delete entries | `LDAP_DEMO_APP` + `LDAP_DEMO_POLICIES` | `examples/ldap/` |
| MQ publish/subscribe: MQOutput-via-alias and Publication node, admin subscription | `MQ_PUBSUB_APP` | `examples/mqpubsub/` |
| MQTT publish/subscribe against Mosquitto | `MQTT_DEMO_APP` | `examples/mqtt/` |
| Timer nodes: Scheduler (interval + cron), TimeoutNotification (automatic + controlled), TimeoutControl set/cancel, Timer policy | `TIMER_DEMO_APP` + `TIMER_DEMO_POLICIES` | `Timer.md`, `examples/timer/` |
| Routing nodes: Route, Filter, RouteToLabel + Label, FlowOrder | `ROUTING_DEMO_APP` | `examples/routing/` |
| Slack connector: list private channels, send message | `SLACK_DEMO_APP` + `SLACK_DEMO_POLICIES` | `Slack.md`, `examples/slack/` |

> **Connector scope.** All six Amazon S3 actions in the table are
> runtime-verified against a real bucket. Filter-driven actions (list, delete)
> need a `<filter>` child element on the request node — without it, list silently
> returns a single record and delete fails outright. That encoding is written by
> the ACE Toolkit and appears in no product schema; it is documented in
> `ace-claude/AmazonS3.md`.
>
> `FILE_IO_APP` reads and writes under `/tmp/ace-file-io/`. Create the
> directories before starting the server —
> `mkdir -p /tmp/ace-file-io/{in/archive,out,error}` — ACE File nodes require
> absolute paths and will not create the input directory themselves.
> `HTTP_PASSTHRU_APP` expects a backend on `http://localhost:8080`.

## Prerequisites for running the demo apps

- IBM App Connect Enterprise 13.x (developed against 13.0.2.2)
- IBM MQ (for the MQ-backed apps) — a local queue manager
- PostgreSQL (for the ODBC/JDBC/DatabaseRetrieve apps) — or adapt the DSN/policy
  to your database
- The `ibmint` CLI (ships with ACE) — used to package and deploy; no Toolkit needed

The database examples use placeholder connection details (`db.example.com`,
`mydb`, `dbuser`, `<DB_PASSWORD>`). See `ace-claude/ODBC.md` and
`ace-claude/JDBC.md` for the exact setup steps, including credential storage in
the server vault.

## Honest limitations

This gets you most of the way, not all of the way. Treat generated artifacts as
a strong first draft that a developer verifies — **the onus is always on the
developer to check the output.** That is true of any code generation, and saying
so plainly is more useful than pretending otherwise.

Two things worth knowing specifically:

- **`ibmint package` is not a validator.** It compiles against the *server*
  classpath and does not run the Eclipse/Toolkit validators, so a project can
  package and deploy cleanly while still carrying Toolkit build-path or node
  property problems. To check that too:
  `mqsicreatebar -data <workspace> -a <app> -cleanBuild` reproduces the
  Toolkit's Problems view headlessly. Use both.
- **Known issue — `DB_NODE_APP`:** the ACE Toolkit reports three errors on the
  `LookupTier` DatabaseRetrieve node ("Mandatory table name property is is not
  valid…", plus unset `Value` / `Value Type`). The app **packages, deploys and
  runs correctly**, and the same file validates clean via `mqsicreatebar`. The
  DatabaseRetrieve msgflow grid is undocumented for hand-authored XML (see
  `ace-claude/examples/dbnode/README.md`), and the Toolkit's editor validator
  does not treat `operator="ASC"` SELECT-column rows the way the runtime does.
  Logged for a future fixpack review; not a functional defect.

## Credits & origin

This project stands on the shoulders of **[ace-bob](https://github.com/ot4i/ace-bob)**,
the original ACE skill published by Open Technologies for Integration. It was
introduced by **Ben Thompson** (Chief Architect, IBM App Connect Enterprise) and
**Sanjay Nagchowdhury** (Technical Lead, IBM App Connect Enterprise) in the blog
post
**["Using IBM Bob in the ACE Toolkit"](https://community.ibm.com/community/user/blogs/ben-thompson1/2026/05/21/using-ibm-bob-in-the-ace-toolkit)**
— with thanks to them and to the ACE development team for making it openly
available.

`ace-claude` began as an adaptation of ace-bob for [Claude Code](https://claude.com/claude-code),
then was extended substantially with runtime-verified database (ODBC/JDBC),
EDA-node, DatabaseRetrieve, and headless flow-testing capabilities.

## License

- New work in this repository is licensed under the **Apache License 2.0** —
  see [`LICENSE`](LICENSE) and [`NOTICE`](NOTICE).
- Portions derived from ace-bob remain under their original **MIT License**
  (Copyright © 2026 Open Technologies for Integration), retained in full in
  [`LICENSE-ace-bob`](LICENSE-ace-bob) as that license requires.

Contributions welcome. The `ace-claude/LEARNINGS.md` file is the running record
of hard-won, non-obvious findings — new discoveries belong there.
