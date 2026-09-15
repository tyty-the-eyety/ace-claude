# Learnings

Non-obvious gotchas discovered while getting new artifact types green.
One short entry each; newest at the bottom.

## Policy projects (first clean build+deploy+runtime proof)

- **`endpointURL`, not `endpointUrl`.** The IBM docs table for the HTTPRequest
  policy lists the property as `endpointUrl`; the Policy.xsd and the runtime use
  `endpointURL`. Trust `/opt/IBM/ace-13.0.2.2/common/schemas/Policy/Policy.xsd`
  over the docs' property-name column.
- **Node attachment attribute is `policyUrl`.** In msgflow XML, the "Policy"
  property of an HTTPRequest (WSRequest) node is
  `policyUrl="{PolicyProject}:PolicyName"` — braces are literal.
- **Admin REST path for a policy** is
  `/apiv2/policies/<project>/policy/<policyName>` — the middle segment is the
  literal word `policy`, not the policy type. `/apiv2/policy-projects` does not
  exist.
- **`ibmint package` takes repeated `--project` flags** — app + policy project
  can go into one BAR; the deploy log confirms with
  `BIP9332I: PolicyProject '<name>' has been reloaded successfully`.
- **Ephemeral smoke tests must run in ONE shell invocation.** With
  `--stop-after-duration`, the countdown starts at launch; agent tool-call
  latency between "start server" and "curl" can eat the whole window. Start the
  server in the background, poll readiness, smoke-test, and shut down inside a
  single command block instead (skip `--stop-after-duration`, use
  `POST /apiv2/shutdown`).
- **A work dir containing only `run/` is not a valid work dir.** If
  `server.conf.yaml` is missing, run `mqsicreateworkdir <dir>` first — it is
  non-destructive to an existing `run/` tree.
- **Local reference material beats the website:** policy type list and exact
  property names live in `common/schemas/Policy/Policy.xsd`; shipped example
  policies live in `server/adminservices/PolicyTemplates/` (filename pattern
  `<Name>.<Type>.policyxml` there is template-only — in a policy project the
  filename is just `<policyName>.policyxml`).

## Timer / WLM / ActivityLog / HTTPReply / MQEndpoint / JDBCProviders policies

- **Never keep a deployed work dir inside the package input path.**
  `ibmint package --input-path <dir>` scans every subdirectory; a deployed
  policy project under `dev-server/run/` (recognised by its `policy.descriptor`,
  no `.project` needed) collides with its own source project:
  `duplicate entry: <proj>/policy.descriptor` → BIP8081E. Same trap applies to
  example scaffolds saved inside the workspace — the template in
  `examples/policy/` must not carry a real project name in `.project`.
  Dev server work dir is now `/path/to/dev-server` (outside).
- **A malformed policy fails at FLOW START, not at package or deploy time**, and
  the BIP codes are cryptic: a bad enum value or unrecognised property element
  in an HTTPReply policy gives `BIP9320E` + `BIP2328E "SQL datatype 'UNKNOWN'
  ... 'CHARACTER' expected"` on every flow that has a reply node; a bad
  WorkloadManagement value names itself better (`BIP7987E`). When flows
  mysteriously fail to start after adding a policy, diff the policy against a
  shipped template first.
- **Two confirmed IBM docs errors:** WLM `processingTimeoutAction` must be
  lowercase `none` (docs say `None`); the HTTPReply filter element is
  `userAgentCompressionBlockFilter` as in the shipped template — Policy.xsd's
  `userAgentBlockCompressionFilter` (different word order) breaks flow start.
  Order of trust: shipped `PolicyTemplates/*.policyxml` > Policy.xsd > docs.
- **HTTPReply compressionType valid values** are `allMimeTypes`,
  `compressibleMimeTypes`, `noCompression` (enum lives in
  `common/schemas/MessageFlow/MessageFlow.xsd`, not Policy.xsd) — not "gzip".
  However: on ACE 13.0.2.2's embedded native HTTP listener, no
  `Content-Encoding` was ever produced — neither via the
  `Defaults.Policies.HTTPReply` server default nor via a node-level
  `<flow>#reply.compressionType` BAR override. Treat reply compression as
  unverifiable on an independent server at this fix level.
- **WLM policy attaches via BAR override `<flowName>#wlmPolicy`** — NOT
  `workloadManagementPolicy`. `ibmint apply overrides` happily writes unknown
  property names into broker.xml (BIP1140I, no validation) and the runtime
  silently ignores them. The authoritative list of overridable flow/node
  properties is the deployed `run/<APP>/META-INF/broker.xml` — read it before
  guessing override keys. Proof of throttle: 6 HTTP requests at
  `maximumRateMsgsPerSec=2` took ~2.5–3.0s vs ~0.09s without.
- **Timer policy attaches through the timeout node's `uniqueIdentifier`**
  msgflow attribute set to `{proj}:policy`; policy `timeoutInterval` overrides
  the node's own value (node said 1s, policy said 3s → ticks every 3s).
  Works WITHOUT MQ on an independent server despite the docs' queue talk.
- **ActivityLog policies are active as soon as they are deployed** — no
  attachment step. `fileName` in the policy gets entries for every resource
  interaction (RM=File etc.); CSV-ish format, `formatEntries=true` adds the
  human-readable message column.
- **FileOutput keeps appending records in `<outputDirectory>/mqsitransit/`**
  until a Finish File criterion moves the file — look there during tests.
- **Non-dynamic policy types (JDBCProviders etc.) deploy fine to a STOPPED
  server's work dir** — the restart-all-applications constraint only bites on
  live redeploys.

## MQEndpoint runtime proof (local MQ 9.4.0.5, TEST_QM)

- **MQ authorizes by `mqm` group, root included** — `strmqm` as root gives
  AMQ7077E if root is not in `mqm`. Fix: `usermod -aG mqm <user>` then either
  re-login or wrap single commands in `sg mqm -c "..."`.
- **Never start IntegrationServer under `sg`/`newgrp`** — they are setgid
  binaries, so glibc strips `LD_LIBRARY_PATH` (secure-execution) and the server
  dies with `libbipmain.so: cannot open shared object file`. It is also
  unnecessary: for `connection=SERVER` local bindings, the queue manager looks
  up the connecting *user's* groups fresh from the OS database — the server
  process itself does not need `mqm` in its supplementary groups.
- **MQEndpoint policy for a local queue manager:** `connection=SERVER` +
  `destinationQueueManagerName` only; hostname/port/channel are CLIENT-mode
  properties. Proof pattern: MQOutput node with NO queue manager configured and
  no server default queue manager — the put can only succeed via the
  `policyUrl` policy.
- **An HTTP→MQ bridge writes the HTTPInputHeader into the MQ message body**
  unless you strip it (the queue message began `POST /policy-mq HTTP/1.1...`).
  In real flows delete `OutputRoot.HTTPInputHeader` (or copy only the body)
  before an MQOutput node.
- MQ sample binaries for queue verification live in `/opt/mqm/samp/bin/`
  (`amqsput`/`amqsget <QUEUE> <QMGR>`); `runmqsc` one-liners pipe fine:
  `echo "DEFINE QLOCAL(X) REPLACE" | runmqsc TEST_QM`.

## SecurityProfiles + vault credentials (both directions runtime-proven)

- **Inbound basic auth without LDAP is possible:** SecurityProfiles policy with
  `authentication=Local` and `authenticationConfig=<name of a vault credential
  of type `local`>`. Attach on the HTTP Input node with BOTH
  `securityProfileName="{proj}:policy"` AND `identityType="usernameAndPassword"`
  (the identityType tells the node to extract the Authorization header).
  Result: no/wrong credentials → 401 issued by the runtime before the flow
  runs; correct credentials → flow executes. Worked first try.
- **Outbound credential injection:** HTTPRequest policy `credentialName=<name
  of a vault credential of type `http`, auth-type basic>` — the WSRequest node
  adds `Authorization: Basic` itself; the flow never sees the secret.
- **Credential *type* must match the consumer**: `local` for Local-auth
  security profiles, `http` for HTTPRequest/RESTRequest credentialName, plus
  `jdbc`/`odbc`/`mq`/`kafka` etc. — full table in `mqsicredentials --help`.
- **A work dir with a vault refuses to start without the key** — every
  IntegrationServer invocation for that work dir now needs
  `--vault-key <KEY>` (or `MQSI_VAULT_KEY` / `.mqsivaultrc`).
- `mqsicredentials --report` works offline against a stopped server's work dir
  and shows names/usernames but never secrets.

## TCPIPClient / TCPIPServer policies (runtime-proven, first try)

- **TCPIP policy property names are PascalCase** (`Hostname`, `Port`,
  `MaximumConnections`) — unlike every other policy type's camelCase. They are
  legacy configurable-service style; copy them from Policy.xsd exactly.
- **Attachment is the `connectionDetails` node attribute** on all six TCPIP
  nodes, set to `{proj}:policy`. The same attribute accepts a literal
  `host:port`, so seeing a raw port there is the smell that a policy should
  replace it.
- Working proof pattern: client `closeConnection="afterData"` + server
  `recordDetection="wholeFile"` = one record per connection, no delimiter
  configuration needed. `messageDomainProperty="BLOB"` on the server input.
- **BIP5731W is logged at WARNING level but is informational** — "TCPIP
  Server Connection manager ... is being started/stopped" appears on normal
  startup/shutdown and usefully echoes the policy reference in use. Do not
  treat it as a failure signal when grepping logs.
- The TCPIPServer policy's listener binds as soon as the flow starts —
  `ss -tln | grep <port>` is a cheap first verification before sending data.

## HTTPProxy policy (deployable at 13.0.2.2, but not attachable)

- **The IBM docs describe the LATEST fixpack, not yours.** The HTTPProxy policy
  docs (13.0.8) list HTTPRequest/HTTPInput as consumers via a connector-policy
  `proxyName={proj}:policy` property — but `proxyName` does not exist anywhere
  in the 13.0.2.2 Policy.xsd. Before designing around a docs feature, grep the
  local Policy.xsd for the property and check `versionIntroduced`.
- **`httpProxyLocation` on HTTPRequest (WSRequest) nodes is literal-only** at
  13.0.2.2: a `{proj}:policy` value fails flow start with
  `BIP2211E: ... valid values are '[http://]host[:port][/path]'`. BIP2211E is
  the "this node attribute doesn't take policy references" signal.
- The proxy mechanism itself is fine and was runtime-proven with a literal
  `httpProxyLocation="localhost:8899"`: target host `proxied.invalid`
  (unresolvable) still returned 200 because the proxy carried the traffic, and
  the proxy log showed the absolute-form request URI. A ~50-line Python
  `ThreadingHTTPServer` forward proxy is enough for this proof.
- The HTTPProxy policy body (`proxyUrl`, `credentialName` of type `httpproxy`)
  validates, deploys, and REST-dumps cleanly — keep it in the template for
  the day the runtime is on a fixpack with `proxyName`.

## DFDL domain (parse + write runtime-proven)

- **Do not author DFDL schemas from scratch** — the install ships a full
  worked example at `<ACE_INSTALL>/server/sample/dfdl/` (`company.xsd` +
  matching CRLF `company.txt` + expected `company.xml`) and pre-canned base
  formats + starter templates inside
  `tools/plugins/com.ibm.dfdl.precanned.formats_*.jar`. Copy
  `examples/dfdl/DFDL_APP_TEMPLATE/` and mutate.
- **Input node config**: `messageDomainProperty="DFDL"` +
  `messageTypeProperty="{}:RootElement"` (empty braces = no-namespace
  schema). Toolkit labels: `messageSetProperty` = "Message model" (leave
  unset for app-deployed schemas), `messageTypeProperty` = "Message".
- **Serialization needs no MessageType**: build `OutputRoot.DFDL.<Root>...`
  and the reply/output node serializes it — the root element name alone
  resolves the deployed schema (verified by removing
  `SET OutputRoot.Properties.MessageType` → byte-identical output).
- **DFDL parsing is lazy**: a malformed payload does NOT fail at the input
  node; it fails at the first ESQL navigation into `InputRoot.DFDL`, as
  BIP2230E at the Compute node wrapping BIP5803E/BIP5807E. The BIP5807E
  carries the DFDL parser's `CTDP` message with element path + byte offset
  (e.g. CTDP3042E terminator not found = LF vs CRLF mismatch). Node
  attribute `validateTiming="immediate"` moves failure to the input node.
- **JSON array output gotcha (corrects earlier belief)**:
  `OutputRoot.JSON.Data.x.items[i].f` alone produces repeated `"items"`
  keys — invalid JSON. Must
  `CREATE FIELD ... .items IDENTITY (JSON.Array)items;` then set
  `... .items.Item[i].f`. The early XML_JSON_MAPPING_APP note claiming `[i]`
  alone makes an array was wrong.
- `dfdl:textNumberPattern="#0.###"` drops trailing zeros when writing
  (`50000.00` → `50000`); use `"#0.00"` for fixed two-decimal wire formats.
- Nillable round trip works: `nillable="true"` + `nilValue="%ES;"` — empty
  `dept=` parses to SQL NULL and lands as JSON `null`.

## EDA nodes: Aggregation / Collector / Resequence (all three runtime-proven)

Working examples: `examples/eda/` (copy-ready flows); workspace proofs
`MQ_AGGREGATION_APP`, `MQ_COLLECTOR_APP`, `MQ_RESEQUENCE_APP`.

- **Queue manager prerequisites (both required):** a server-wide
  `defaultQueueManager` in `server.conf.yaml`/overrides (per-node MQEndpoint
  policies are NOT enough — BIP2685E "requires a queue manager to be specified
  on the integration node"), AND the EDA system queues from
  `<ACE_INSTALL>/server/sample/wmq/iib_queues_create.mqsc`
  (`SYSTEM.BROKER.AGGR.*`, `SEQ.*`, `EDA.*`, `TIMEOUT.QUEUE`) — a manually
  created QM has none of them.
- **Resequence `startOfSequence`/`endOfSequence` are mode-encoded**: first
  char is a discriminator (0=literal, 1=predicate, 2=automatic), remainder is
  the value. Literal 1→3 is `startOfSequence="01" endOfSequence="03"`, NOT
  `"1"`/`"3"`. Plain `"1"` = predicate mode with an empty XPath →
  `boolean()` wrong-arg crash (BIP4390W via BIP4703) on EVERY message;
  omitting them entirely = NumberFormatException at flow start. The XSD
  defaults `"00"`/`"2"` are these encodings, not plain numbers.
  `sequencePath` root is `$Body` (`$Root` is not a valid alias in msgflow
  xpath properties).
- **AggregateRequest goes AFTER the MQOutput node**, not before — it reads
  the request's MsgId from `LocalEnvironment.WrittenDestination` to register
  the pending reply (BIP4428E "Missing aggregate request information" when
  wired before). Set `newMsgId="true"` on each leg's MQOutput so legs get
  distinct correlation ids; the backend must reply with
  `MQMD.CorrelId = request MQMD.MsgId` to the `replyToQ`.
- **AggregateControl/Reply cannot be short-circuited in one transaction** —
  wiring AggregateRequest straight into AggregateReply.in gives BIP4435E
  "blank reply ID". Replies must arrive via separate MQInput node(s) feeding
  `AggregateReply.in`.
- **AggregateReply output is a `ComIbmAggregateReplyBody` tree with one child
  folder per `folderName`**, each holding the full reply message tree
  (`...ComIbmAggregateReplyBody.LegA.XMLNSC...`). It has no wire format —
  serializing it directly yields a 0-byte message; always map it in a Compute
  first.
- **Aggregation policy attaches through `aggregateName` itself**:
  `aggregateName="{proj}:policy"` on both AggregateControl and AggregateReply
  (runtime-proven; policy supplies timeoutInterval etc.).
- **Collector dynamic input terminals** are declared as
  `<inTerminals terminalNodeID="InTerminal.legA" dynamic="true" label="legA"/>`
  child elements, with connections targeting `targetTerminalName="InTerminal.legA"`.
- **msgflow "table" properties serialize as REPEATED elements named after the
  table, row values as attributes** — same convention as the Route node's
  `<filterTable .../>` rows. Collector:
  `<eventHandlerPropertyTable terminal="legA" quantity="1" timeout="0"/>` per
  terminal. The XSD's nested `<eventHandlerProperty>` row-element form parses
  to an EMPTY table at runtime → NPE deep in `CollectorNode.addToCollections`
  on every message. The `terminal` column is the bare label (no prefix).
- **Collector output tree is `InputRoot.Collection`** with a `CollectionName`
  child plus one folder per terminal label
  (`InputRoot.Collection.legA.XMLNSC...`); Collector policy attaches via
  `configurableService="{proj}:policy"` (runtime-proven).
- Debug technique that cracked this: the EDA node classes are plain Java in
  `<ACE_INSTALL>/server/lil/mqsieda.par` — `unzip` + `javap -p -c` shows the
  exact attribute names and lookup keys the runtime reads
  (`getUserDefinedAttribute("eventHandlerPropertyTable")`,
  `MbInputTerminal.getName()` returning bare names like `control`).

## Older MQ_* app smoke tests (all five pass; two latent bugs found)

All queues for these apps now exist on TEST_QM (INPUT/OUTPUT.QUEUE,
HIGH/MEDIUM/LOW.PRIORITY.QUEUE, ERROR/DEADLETTER.QUEUE,
INPUT.XML/OUTPUT.JSON.QUEUE). Apps sharing INPUT.QUEUE (COMPUTE/ROUTE/
SUBFLOW/ERRHANDLING) must be deployed ONE at a time — concurrent MQInput
nodes steal each other's messages; remove `run/<APP>` between rounds.

- **`InputRoot.*[<]` must be UNQUOTED** — `InputRoot.*['<']` fails at flow
  start with BIP2434E "field index expression does not evaluate to INTEGER;
  ''<'' evaluates to CHARACTER". The quoted form was in SKILL.md itself (now
  fixed in 4 places) and had propagated into MQ_COMPUTE_APP and
  MQ_SUBFLOW_APP (both fixed + runtime-proven).
- XML_JSON_MAPPING_APP had the known `messageDomain=` (vs
  `messageDomainProperty=`) bug from before that rule existed — body parsed
  as BLOB so every XMLNSC path silently resolved empty: output was
  structurally valid JSON with all values missing. A silent-empty-output
  symptom on an MQ flow should trigger checking this attribute first.
- MQMD.Priority-based routing is easy to smoke test without custom put code:
  `ALTER QLOCAL(...) DEFPRTY(n)` before each `amqsput` (amqsput inherits the
  queue default priority).
- Verified runtime outputs: COMPUTE passthrough byte-identical; ROUTE
  priorities 8/5/0 → HIGH/MEDIUM/LOW; SUBFLOW passthrough; ERRHANDLING happy
  → OUTPUT.QUEUE and 'FAIL...' → ERROR.QUEUE with full ErrorMessage envelope
  (Code 2951 + ExceptionList); FIRST_MAPPING XML→JSON with typed array.

## ODBC from ESQL (PostgreSQL, runtime-proven first try)

Working example: `examples/odbc/` (flow + ESQL + odbc.ini + README).
Workspace proof: `DB_PG_ODBC_APP` → PostgreSQL 17 on the homelab LXC.

- ACE ships DataDirect wire-protocol ODBC drivers (`server/ODBC/drivers/lib/`,
  Postgres = `UKpsql95.so`) — no OS driver packages needed; mqsiprofile already
  puts the dir on LD_LIBRARY_PATH.
- Three required pieces: odbc.ini (DSN stanza), `ODBCINI=<path>` env var at
  EVERY server start, and a vault credential whose **name equals the DSN**:
  `mqsicredentials --credential-type odbc --credential-name <DSN> ...`
  (the `mqsisetdbparms -n odbc::DSN` form in older docs is the node-based
  path; on a standalone work-dir server use mqsicredentials + vault key).
- Compute node: `dataSource="<DSN>"` attribute + `TO Database.<DSN>` in
  PASSTHRU. Postgres returns lowercase column names; ESQL field refs are
  case-sensitive.
- `?` markers + `VALUES(...)` and `INSERT ... RETURNING id` both work through
  the DataDirect driver (RETURNING behaves like a SELECT result set).
- `CAST(inet AS VARCHAR)` includes the netmask (`/32`) — use `host(ip)` to
  strip; JSON serializer escapes `/` as `\/` by default (strict escapeMode).

## JDBC via JDBCProviders policy + JavaCompute (runtime-proven first try)

Runbook: `JDBC.md`. Example: `examples/jdbc/`. Workspace proof: `PG_JDBC_APP`
+ `PG_JDBC_JAVA` → PostgreSQL 17. This also upgrades JDBCProviders from
"deploy+REST only" to fully runtime-proven.

- Don't use ACE's shipped `ACpostgresql.jar` (shaded App Connect driver,
  undocumented URL scheme) — download the official `org.postgresql` jar to a
  stable path and point `jarsURL` at the directory.
- No PostgreSQL JDBCProviders template ships; clone the MySQL template shape.
  `[user]`/`[password]` URL tokens are filled from the vault credential named
  by `securityIdentity` (`mqsicredentials --credential-type jdbc
  --credential-name <securityIdentity>`).
- `ibmint package` COMPILES Java projects itself (BIP8409I) — a JavaCompute
  app needs no Toolkit: JCN-nature .project + .classpath + src/, app .project
  <projects> reference, both on the package command; jar lands in the appzip.
- `getJDBCType4Connection("{proj}:policy", JDBC_TransactionType.MB_TRANSACTION_AUTO)`
  — the policy reference is the provider name; `JDBC_TransactionType` is a
  nested enum of MbNode (inherited, no import). Never close the Connection.
- Standalone javac check before ibmint saves cycles:
  `javac -cp server/classes/jplugin2.jar:server/classes/javacompute.jar ...`

## Direct ESQL SELECT over ODBC (no PASSTHRU) runtime-proven

Workspace proof: `DB_ENRICH_APP` redone against live Postgres — JSON in on
INPUT.QUEUE, tier looked up from `customers`, routed to PREMIUM/STANDARD/
UNKNOWN queues; all four test cases (two tiers, unrecognised tier, missing
row) routed correctly first try.

- Direct-SELECT table qualification is `Database.<DSN>.<schema>.<table>`
  (`Database.PGDB.public.customers`) — schema is mandatory in this form,
  unlike PASSTHRU where the schema lives inside the SQL string.
- `THE(SELECT ITEM T.col ...)` returns a scalar; NULL when no row matches —
  a plain `IF x = '...' / ELSEIF / ELSE` handles found/unknown/missing in one
  block (NULL comparisons are not TRUE, so ELSE catches them).
- Works with `computeMode="exception"` routing (PROPAGATE TO TERMINAL +
  RETURN FALSE, no OutputRoot copy) — the two patterns compose cleanly.
- Same PGDB DSN/vault credential/ODBCINI setup as PASSTHRU (see ODBC.md);
  nothing extra needed for the direct form.

## DatabaseRetrieve node (built-in JDBC lookup, runtime-proven)

Full rules in `examples/dbnode/README.md`; workspace proof `DB_NODE_APP`.
Headlines (all reverse-engineered from jdbcnodes.jar bytecode): the node
executes its `sqlQuery` attribute VERBATIM (Toolkit generates it — hand-author
the full prepared statement or get "parameter index out of range");
queryElements rows split into SELECT-column rows (`operator="ASC"/"DESC"`,
≥1 mandatory) and WHERE-predicate rows (`valueType="Element"` + XPath value,
bound to `?` in row order); dataElements `columnName` must be the qualified
`customers.tier` form (result hashtable key) with `$OutputRoot/...` as the
write path; `copyMessage="true"` = enrichment mode; wire `keyNotFound`
(primary terminal, zero-rows case). `dataSourceName` takes the
`{proj}:policy` JDBCProviders reference.

## Flow unit testing (Test Project, headless, 3/3 pass)

Full walkthrough: `examples/unittest/README.md`; workspace proof
`HTTP_JSON_APP_Test` (NodeSpy on HTTP_JSON_MF's Compute node).
- Test project = Java project + `testProjectNature` + **`testproject.descriptor`**
  (the marker `ibmint package` requires — without it: BIP15214E "No valid
  resources"; descriptor XML recovered from libCentralCommandLib.so strings)
  + `src/main/java` layout. Packaged as a `.testzip` inside the BAR.
- Run: `IntegrationServer --test-project <T> --start-msgflows false
  --no-nodejs --vault-key <K>` — prints TEST RESULTS/TOTALS, exit 0/1.
- `propagatedJSONFromTerminalOnCall(expectedJSON, terminal, call)` — JSON
  argument FIRST (name misleads; wrong order = 'Output terminal "{json}"
  does not exist').
- `messagePath()` reads FAIL on propagated/received assemblies (BIP2331 for
  every path form even when the tree provably contains the element) — assert
  via the JSON matcher or `getJSONMessageBodyAsString()`.

## File nodes (FileInput / FileOutput) — three ways to fail silently

Workspace proof: `FILE_IO_APP`. All three were found by driving a real file
through the flow; every one of them packages and deploys clean, so `ibmint
package` success says nothing about whether a File flow works.

- **Directories must be ABSOLUTE.** A relative `inputDirectory="data/in"` is
  rejected at startup with `BIP3333E: cannot resolve the relative file path`,
  and the input node never starts (`BIP3332E`). There is no project-relative or
  work-dir-relative form — the flow runs from the server work dir with nothing
  to anchor against. Pick a writable absolute path.
- **`computeMode="destinationAndMessage"` is mandatory when the ESQL names the
  output file.** Setting `OutputLocalEnvironment.Destination.File.Name` under
  the default `computeMode="message"` silently discards LocalEnvironment;
  FileOutput then receives an empty filename and fails with
  `BIP3325E: ... cannot use the directory '<dir>' for file name ''`. The input
  is retried to the limit and backed out, so **nothing is ever written and the
  input file disappears** — the failure looks like the flow never ran. This is
  the standing `computeMode` rule biting in its most invisible form.
- **The original filename is `InputLocalEnvironment.File.Name`.**
  `InputLocalEnvironment.ComIbmFileInput.Response.FileName` looks plausible and
  parses fine, but resolves to NULL — so a `CASE WHEN origName IS NOT NULL`
  fallback always takes the ELSE branch and every output silently gets the same
  fixed name. Symptom: correct content, wrong (constant) filename.
- **`BIP3316W` at initialisation is expected, not a bug.** "File node '<label>'
  has no valid filename specified as property ''" simply means no filename node
  property is set — correct when the name comes from LocalEnvironment. Use it as
  a diagnostic pairing: `BIP3316W` at init + `BIP3325E` at runtime = the
  LocalEnvironment name isn't arriving (see `computeMode` above); `BIP3316W`
  alone with output appearing = working as designed.
- **Don't put paths in node labels.** BIP messages quote the label, so a node
  labelled `/out` still reports `File node '/out'` after the directory has
  changed, which actively misleads diagnosis. Label by role
  (`write_processed`, `write_error`).

## The Toolkit validates things `ibmint package` never checks

`ibmint package` compiles against the *server* classpath and does not run the
Eclipse validators at all. `mqsicreatebar -data <workspace> -a <app> -cleanBuild`
does — it reproduces the Toolkit's Problems view headlessly, with no GUI. Four
defects were found this way in projects that had always packaged clean:

- **DatabaseRetrieve query-element rows must have `valueType` and `value` *set*,
  or the attributes absent entirely — never present-but-empty.** A SELECT-column
  row (`operator="ASC"`) binds no value, so `valueType="" value=""` looks
  harmless and is accepted at runtime, but the Toolkit validator rejects it with
  THREE errors from one cause, including a misleading
  `Mandatory table name property is is not valid ... for row '1'` (sic — the
  doubled "is" is IBM's typo). The table name is not the problem. Deleting the
  two empty attributes clears all three; supplying a dummy value also works but
  lies about the row's meaning.
- **JavaCompute projects: `com.ibm.etools.mft.jcn.JCN_CONTAINER` may not
  resolve.** Symptom: `JDBC_TransactionType cannot be resolved to a variable`
  (it is a nested enum of `MbJavaComputeNode`, inherited and referenced
  unqualified). Use explicit variable entries instead —
  `JCN_HOME/javacompute.jar`, `JCN_HOME/jplugin2.jar`,
  `COMMON_CLASSES_HOME/IntegrationAPI.jar`.
- **Test projects: `com.ibm.etools.mft.unittest.integrationTestDependencies`
  may resolve to nothing**, failing `org.junit`, `com.ibm.integration` AND
  `org.hamcrest` together. Use `org.eclipse.jdt.junit.JUNIT_CONTAINER/5` plus
  `COMMON_CLASSES_HOME/IntegrationTest.jar` and
  `COMMON_CLASSES_HOME/hamcrest-2.2.jar`.
- **`HTTPReply` is valid in `Policy.xsd` but rejected by the Toolkit policy
  editor** ("contains an invalid policy type HTTPReply"). Runtime-valid,
  Toolkit-invalid. Because `mqsicreatebar` refuses to build a workspace with any
  error, one such policy blocks Toolkit BAR builds for every project beside it.
- **Committed build output masks all of this.** A stale `bin/` or `.jar` lets
  Eclipse skip compilation, so a broken `.classpath` validates clean. Delete
  build output before trusting a validation run — and note those paths are
  usually gitignored, so a fresh clone has no mask and fails immediately.

## Runtime success does not imply the standing rules were followed

`HTTP_PASSTHRU_APP` (WSInput → WSRequest → WSReply) returns a correct 200 with
the body round-tripped while its WSRequest node carries none of `httpVersion`,
`protocol`, or `messageDomainProperty` — the attributes the standing rules
require on **every** WSRequest. The runtime does not enforce them, so a green
smoke test is not evidence of conformance; only reading the msgflow is. Audit
node attributes separately from behaviour.


## Amazon S3 connector — the ApplicationConnector Request node

First connector taken from documented-only to runtime-proven (six actions:
CREATE, UPSERTWITHWHERE, DOWNLOAD_OBJECT, COPY_OBJECT, RETRIEVEALL, DELETEALL).
Working example: `examples/s3/`; workspace proof `S3_CONNECTOR_APP`.

- **Two classes of connector action, and the model JSON tells you which.**
  In `<ACE>/server/nodejs_all/node_modules/@ibm-app-connect/loopback-connector-<name>/lib/models/<object>.json`,
  an interaction with `requestProperties` is **body-driven** (values set in ESQL,
  `dataLocation="$Body"`); one with only `filterSupport` is **filter-driven**
  (values come from a `<filter>` element, and the body is ignored). An
  interaction declaring *both* needs both. Check before writing the flow — a
  filter-driven action built as body-driven builds, deploys and starts cleanly,
  then fails or silently returns one record.
- **`<filter>` is a child ELEMENT, not the `filter=""` attribute.** The attribute
  exists on the node and is inert; every syntax tried behaved identically to
  supplying nothing. Two forms, both written by the Toolkit and parsed by the
  runtime, and **neither appears in any msgnode definition or product schema**:
  ```xml
  <filter><queryProperties limit="15" allowTruncation="true"/></filter>
  ```
  ```xml
  <filter><filterElementObject type="where"><filterElementArray type="and">
    <connectorPropertyRef propertyName="bucketName" compareAction=""/>
    <filterProperty propertyName="Key" displayName="Object name"
                    propertyValue="[[$Environment/myKey]]" compareAction=""/>
  </filterElementArray></filterElementObject></filter>
  ```
- **Omit `<filter>` on a RETRIEVEALL and you get exactly ONE record.** Not a bug:
  `s = c ? (s = c.limit, delete c.limit) : s = 1` in
  `loopback-connector-provider-embedded/lib/utils.js`. The connector fetches
  everything then truncates — trace showed `Total Objects obtained for bucket
  are: 4` followed by `Total number of items being sent to caller 1`. HTTP 200,
  well-formed JSON, silently incomplete.
- **`[[$Environment/x]]` in `filterProperty/@propertyValue` is a message-tree
  reference**; set it upstream with `SET Environment.x = ...`. But
  **`"template"` inside a `<requestMap>` is a LITERAL** — putting a path there
  creates an object literally named `$Environment/x`. Two syntaxes, same-looking
  values, opposite meaning.
- **Parent properties go in exactly one place.** If the node has a
  `<connectorProperty propertyName="bucketName" .../>` row, `bucketName` must NOT
  also be in the body — the runtime strips parents from the body and rejects the
  duplicate (`is not allowed to have the additional property "bucketName"`). With
  no such row, it belongs in the body.
- **`gen/*.schema.json` must contain at least `{}`.** Empty files pass
  `ibmint package` AND `mqsicreatebar -cleanBuild`, then fail at runtime with
  `BIP5753E: ... The document is empty` and the app never starts.
- **Property names differ between interactions for the same concept.** `CREATE`
  takes `content`; `UPSERTWITHWHERE` takes `Body`. `ContentType` is a fixed
  16-value MIME enum — an arbitrary type fails `validateCustomActionv0`.
- **`DELETEALL` is a single-object delete** scoped by a `Key` filter, not a bulk
  wipe. There is no separate single-delete action.
- **The credential type table is authoritative.** `mqsicredentials --help` lists
  the required properties for every connector (`amazons3: --secret-access-key
  --access-key-id`). The policy needs `authenticationMethod` from
  `common/schemas/Connectors/PolicyConnectors.xsd` — for amazons3 the enum has
  exactly one value, `BASIC`.
- **Every Toolkit round-trip sets `displayName="undefined"`** on the request
  node. Check that attribute after opening a connector flow in the Toolkit.
- **Connector failures are raised in the connector's Node.js layer**, so only a
  service trace explains them; the useful lines are tagged `<JS>`. Trace one app
  in its own work dir — see the tracing section of `IntegrationServer.md`.


## Kafka nodes (Producer, Consumer, Read — all runtime-proven)

Working examples: `examples/kafka/`; workspace proof `KAFKA_DEMO_APP` against
Apache Kafka 4.3.1 in Docker.

- **There are TWO Kafka node families and only one is usable.** The Toolkit
  palette gives connector-framework nodes —
  `com_ibm_connector_kafka_ComIbmOutput` / `...ComIbmEventInput` /
  `...ComIbmRequest`, each with `connectorName="Kafka"`. There is also a legacy
  family (`ComIbmKafkaProducer` / `ComIbmKafkaConsumer` / `ComIbmKafkaRead`).
  Both are registered in `AdminServices.bir` and **both run correctly**, but the
  legacy nodes **do not build in the ACE Toolkit**, so a flow using them cannot be
  opened and maintained in the IDE. Always author the connector family.
  `mqsicreatebar -cleanBuild` on the connector version returns 0 markers.
  *(Header-only detail: an earlier revision of this file recommended the legacy
  names. They were runtime-proven but Toolkit-hostile; corrected.)*
- Both families share `server/connectors/kafka/connectorkafka.jar`, so **property
  names and LocalEnvironment paths are identical either way** — useful, because the
  jar bytecode is the only authoritative source for them (built-in node
  definitions are not shipped as files).
- **Connector-node namespace URIs are PATHS, not the prefix repeated.** The single
  thing that made these flows resolve in the Toolkit:
  `xmlns:com_ibm_connector_kafka_ComIbmEventInput.msgnode="com/ibm/connector/kafka/ComIbmEventInput.msgnode"`
  — underscores in the prefix, **slashes in the URI**. Built-in nodes repeat the
  name identically (`xmlns:ComIbmCompute.msgnode="ComIbmCompute.msgnode"`), and
  extrapolating that rule to connector nodes produces a flow that packages,
  deploys and RUNS correctly while the Toolkit reports
  `Message node "..." cannot be located` and a terminal error per connection.
- **`bootstrapServers` is a mandatory NODE property even when a Kafka policy
  supplies it** — `Unset mandatory property "Bootstrap servers"` in the Toolkit,
  though the flow runs. Set it on the node and attach the policy anyway.
- **`mqsicreatebar` reads its verdict from the Problem list, not the log tail.**
  `Checking the workspace for markers counter:N` is a progress/retry counter, NOT
  a marker count — I misread it as "0 markers" and wrongly reported a clean
  validation. Also, **no BAR is written if ANY project in the workspace has
  errors**, so a missing BAR does not mean the project you asked about failed.
  Grep for `^\s*Problem [0-9]+.*<PROJECT>` to get the real answer.
- **`notFoundAction` on the Request node is mandatory and has no usable default.**
  Omit it and the flow will not start: `BIP3882E: The value 'NULL' supplied for
  property 'notFoundAction' to the Kafka connector is invalid`. Valid values, from
  `KafkaRequestConnector.setNotFoundAction` bytecode: `latest`, `earliest`,
  `exception`, and **`no match`** — with a space. The connector's own trace calls
  `no match` "the default value" while still rejecting NULL. A miss propagates on
  `OutTerminal.noMatch`.
- **`initialOffset` on the EventInput node is `auto.offset.reset`.** It defaults to
  `latest`, so a message published while the consumer group is still rebalancing
  just after server start is never delivered. It looks exactly like a broken flow.
  Use `initialOffset="earliest"` when testing.
- **Metadata lands in a different LocalEnvironment subtree per node:** EventInput
  -> `LocalEnvironment.Kafka.Input`; Request -> `LocalEnvironment.Kafka.Read`.
  Both carry `topicName`, `partition`, `offset`, **all CHARACTER, not INTEGER**.
- **Connection works inline on the node OR from a Kafka policy** attached with
  `policyUrl="{Project}:Name"`; both proven, including SASL_PLAINTEXT with
  `securityIdentity` and a `mqsicredentials --credential-type kafka` credential.
- Writing `OutputLocalEnvironment.Destination.File.Name` for a downstream
  FileOutput needs **`computeMode="destinationAndMessage"`** — same trap already
  recorded for FILE_IO_APP; `localEnvironmentAndMessage` silently discards it and
  FileOutput fails with `BIP3325E ... for file name ''`. FileOutput also has **no
  `fileName` attribute**; setting one is ignored.


## LDAP connector (ApplicationConnector family — search/create/update/delete proven)

Working examples: `examples/ldap/`; workspace proof `LDAP_DEMO_APP` against
OpenLDAP, Toolkit-validated (BAR built cleanly in the IDE).

- **Same node family as Amazon S3** (`ComIbmApplicationConnectorRequest_<type>`,
  `applicationConnectorType="ldap"`), and the namespace URI here IS the prefix
  repeated — the simple rule. Only the *connector* family (`com_ibm_connector_*`,
  e.g. Kafka) uses a slash path. Everything learned about `<filter>`,
  `<connectorProperty/>` and the body/filter classes transferred unchanged, and
  update/delete worked first try because of it.
- **`businessObject` is the LDAP object class, not `entry`.** LDAP uses
  `dynamicObjects` and ships no `objects.json`; `entry.json` is only a template.
  `businessObject="entry"` fails with `The class definition for the object or its
  parent object is either missing or invalid`. Consequently the class's own schema
  attributes are first-class body properties, and template-only fields are
  rejected — a where clause on `searchCriteria`/`scope` gives `is not allowed to
  have the additional property`, and `ldapObjectClass` is refused on create
  despite the template marking it mandatory.
- **The connector builds the DN itself**, in
  `@ibm-app-connect/ldap-api-utils/lib/util/requestUtil.js` → `formCreateReqObject`:
  `dn = "cn="+cn [+",l="] [+",st="] [+",o="] [+",c="] [+",street="] [+",uid="+uid] + "," + (ou || r)`,
  then `delete entry.ou`. So **`ou` carries the full container DN** and never
  reaches the directory as an attribute; **`uid`/`l`/`st`/`o`/`c`/`street` become
  DN COMPONENTS, not attributes**; and `baseDN` in the body is passed through as an
  attribute (`ldapErrCode 17 baseDN: attribute type undefined`) even though it is
  the correct `<connectorProperty/>` for RETRIEVEALL. Sending `uid` on create
  yields `ldapErrCode 32 No Such Object` quoting the deepest *matched* ancestor —
  an existing DN — which makes the error look nonsensical.
- **Multi-valued attributes must be JSON arrays** even for one value
  (`is not of a type(s) array`): `sn`, `mail` are arrays; `cn`, `uid` scalars.
- `lib/constants.json` in a connector is worth reading before anything else — it
  holds `createMandatoryFields` per class (inetOrgPerson → `cn`, the RDN),
  `updateDeleteMandatoryFilter`, `customActionMandatoryFields`, valid scopes and
  page limits.
- **Connector payloads are redacted in service trace** (`"CUSTOMER-DATA-REDACTED"`).
  A trace gives the call sequence but not the data — for payload problems read the
  `*-api-utils` package source instead. That is what solved this one; the trace did not.

### ApplicationConnector INPUT nodes need a separate `*event` package

`ComIbmApplicationConnectorInput_ldap` packages, deploys and reports
`BIP2269I ... started successfully`, then fails in a retry loop about once a
second, emitting nothing:

```
BIP9937E: TypeError: i.getModel(...).subscribe is not a function
BIP5073E: Failed to establish connection to 'LDAP'
BIP9953E: An error occurred while trying to receive an event from the first application.
```

ACE ships event/input support as separate packages
`loopback-connector-<type>event` (17 installed: gmailevent, asanaevent,
googlepubsubevent, ...). There is no `loopback-connector-ldapevent`, and base
connectors implement no `subscribe`/`unsubscribe` (amazons3 and salesforce do not
either). **Before authoring any `ComIbmApplicationConnectorInput_*` node, check
that `loopback-connector-<type>event` exists in
`<ACE>/server/nodejs_all/node_modules/@ibm-app-connect/`.** The "Has Input node"
column in the SKILL.md connector table is design-time only and does not imply
runtime support. This failure mode is worse than a wrong result: the flow claims
to have started and then burns CPU indefinitely.


## MQ publish/subscribe — and where built-in node definitions actually live

Working examples: `examples/mqpubsub/`; workspace proof `MQ_PUBSUB_APP` against
IBM MQ 9.4, Toolkit-validated (0 problems).

- **ALL 139 built-in node definitions ship in a Toolkit plugin jar:**
  `tools/plugins/com.ibm.etools.mft.ibmnodes.definitions_<version>.jar`. Unzip it
  and read `.msgnode` files exactly as for connector nodes — mandatory attributes
  (`lowerBound="1"`), defaults, terminals, the lot. *(An earlier entry claimed
  built-in node definitions were not on disk and fell back to bytecode plus
  Toolkit round-trips. That was wrong — this jar is the authoritative source.)*
- **Pub/sub is configured in MQ, not on the nodes.** From those definitions:
  `ComIbmMQOutput` has NO topic attribute (only `queueName`, `destinationMode`
  default `fixed`, `transactionMode`/`persistenceMode` default `automatic`);
  `ComIbmMQInput` has NO subscription attribute (`queueName` mandatory, and
  `topicProperty` is just a string — it cannot subscribe); `ComIbmPublication`
  has no topic attribute either, only connection/SSL properties plus
  `subscriptionPoint`.
- **Publishing with MQOutput** means pointing it at a `QALIAS` whose
  `TARGTYPE(TOPIC)` targets a topic object. The flow contains nothing
  topic-specific — a reader of the msgflow alone cannot tell it publishes.
- **Publishing with the Publication node** takes the topic from the message:
  `SET OutputRoot.Properties.Topic = 'ace/demo/orders';`
- **Subscribing** requires an administrative subscription
  (`DEFINE SUB ... DEST(queue)`) with `MQInput` reading that queue.
- **Subscription durability is not a design choice.** Admin subscriptions are
  durable by definition, so publications accumulate while the subscriber flow is
  stopped. Non-durable subscriptions can only be created by an application at
  runtime, which no built-in node does. Do not promise non-durable behaviour with
  MQInput.
- Verify the MQ plumbing independently before blaming ACE: `amqsput` to the alias,
  check `CURDEPTH` on the subscription queue, `amqsget` it back.


## MQTT pub/sub — and the namespace-URI rule explained

Working examples: `examples/mqtt/`; workspace proof `MQTT_DEMO_APP` against
Eclipse Mosquitto 2, Toolkit-validated (0 problems).

- **The connector xmlns URI is the node's path inside its Toolkit plugin jar.**
  This supersedes the earlier "connector nodes use a slash path" note, which
  described the symptom rather than the rule. `MQTTNodes_<v>.jar` stores its nodes
  under `com/ibm/connector/mqtt/`, the definitions jar stores Kafka's under
  `com/ibm/connector/kafka/` and MQInput/Compute at the top level — which is
  exactly why the first two need slashes and the third does not. **To find any
  node's URI, locate its `.msgnode` in `<ACE>/tools/plugins/*.jar` and use the
  archive-internal path.**
- **MQTT node types are the connector family**:
  `com_ibm_connector_mqtt_ComIbmOutput` and `com_ibm_connector_mqtt_ComIbmEventInput`,
  both `connectorName="MQTT"`. The runtime registry also lists
  `ComIbmMQTTPublishNodeType`/`ComIbmMQTTSubscribeNodeType` but **no matching
  `.msgnode` exists anywhere in the product** — registry names are not proof that
  a node type is usable.
- **MQTT is entirely node-configured**: `clientId`, `topicName`, `hostName`,
  `port` (1883), `qos`, `useSSL` are all mandatory node properties. No broker
  objects, no policy needed. This is the exact opposite of MQ pub/sub, where none
  of MQOutput/MQInput/Publication has a topic property and everything is done with
  MQ objects — worth showing side by side when explaining ACE messaging.
- Subscriber metadata: `LocalEnvironment.MQTT.Input` with `Topic`,
  `QualityOfService`, `Duplicate`, `Retained`.
- Mosquitto 2.x listens only on localhost inside its container unless given a
  config with `listener 1883 0.0.0.0` and `allow_anonymous true` — without it the
  broker looks up but nothing can connect.

## Timer family: Scheduler, TimeoutNotification, TimeoutControl

Full runbook in `Timer.md`, copy-ready flows in `examples/timer/`, proof app
`TIMER_DEMO_APP`. All three nodes come from `server/lil/imbtimer.lil`.

- **`requiresMQ="false"` in `MessageFlow.xsd` is only half true.** It holds for
  the Scheduler and for **automatic**-mode TimeoutNotification, both of which run
  on a bare server. **Controlled** mode does need a queue manager — the timeout
  store is `SYSTEM.BROKER.TIMEOUT.QUEUE` — and without `defaultQueueManager` in
  `server.conf.yaml` both the TimeoutControl flow and the controlled
  TimeoutNotification flow refuse to start with `BIP2685E`. Same trap, same BIP
  code, as the EDA nodes.
- **Scheduler `scheduleType` is `interval`, NOT `repeatInterval` — and here the
  `.msgnode` is the wrong source.** `ComIbmScheduler.msgnode` declares the enum
  literal `repeatInterval` and its `.properties` labels it "Repeat Interval", but
  the runtime rejects it: `BIP5064E ... invalid schedule type 'repeatinterval'`.
  `MessageFlow.xsd` (`scheduleTypeType` = `interval` | `calendar`) is right. This
  reverses the usual order of trust for node properties, so for Scheduler enums
  check the XSD, not the Toolkit definition.
- **ACE's cron is neither Unix cron nor Quartz, and it is literals-only.** Five
  fields `<minute> <hour> <day-of-month> <month> <day-of-week>`; minute must be a
  literal 0-59 (`*` is rejected), hour and day-of-month take a literal or `*`,
  and **month and day-of-week must both be `*`**. No lists, ranges, steps,
  names, `?`, or 6-field forms — every one fails at flow start with `BIP5063E`.
  Proven by deploying one flow per expression and reading the startup log; the
  matrix is in `Timer.md`. Two design consequences: **the finest calendar
  granularity is hourly** (sub-hourly needs `scheduleType="interval"`), and
  month selection is simply not expressible.
- **`days` works in `interval` mode and is ignored in `calendar` mode.** With
  `interval`, `days="SAT,SUN"` on a Friday stayed silent for 2.5 minutes as
  intended. With `calendar`, flows with `days="FRI"`, `days="SAT,SUN"`,
  `days="SUN"` and all-seven all fired at the same scheduled minute on a Friday.
  So a cron schedule cannot be restricted to weekdays by either the expression
  (day-of-week must be `*`) or the property — filter in the flow.
- **Two calendar Scheduler nodes on the same minute make each other fire
  twice.** A calendar flow that owns its fire minute is exact: one propagation,
  `currentEventTime` = `HH:MM:00.000`. Put a second calendar Scheduler on the
  *same* minute in the same integration server and at least one of them gets an
  extra propagation stamped 1 ms early, `HH:MM-1:59.999` — with four such flows
  the counts ran 2, 2, 4, 4 and varied between runs. Give each calendar Scheduler
  its own minute, or make the flow idempotent. Interval mode was exact throughout
  (10s ticks, one each).
- **The Compute that writes a timeout request needs
  `computeMode="destinationAndMessage"`.** The LocalEnvironment compute mode is
  spelled `destination`; the full enum is `message`, `destination`,
  `destinationAndMessage`, `exception`, `exceptionAndMessage`,
  `exceptionAndDestination`, `all`. There is no `localEnvironment*` literal, and
  an invalid value is accepted silently by `ibmint package` **and** by the
  runtime, which quietly falls back to message-only propagation. The symptom is
  at the wrong node: `BIP4601E ... failed to navigate to the message location
  specified ... 'InputLocalEnvironment.TimeoutRequest'`.
- **The message handed to TimeoutControl must carry an `MQMD`, or nothing is
  stored.** The timeout still fires exactly on time, and the controlled
  TimeoutNotification then dies on an empty bitstream: `BIP4621E` + `BIP5902W`
  ("The data being parsed was 'Null Buffer'") + `BIP6105E` ("remaining bitstream
  is too small to contain an 'MQMD' structure"). Three lines of ESQL
  (`OutputRoot.MQMD.Version`/`.Format`/`.CodedCharSetId`) fix it, and with the
  MQMD present the body round-trips with its domain intact even when the node's
  `messageDomain` is blank.
- **Leave `storedMessageLocation` blank.** Blank stores and replays the whole
  message unchanged; setting it to `InputRoot` nests the body one level deeper
  (`Root.JSON.Data.Data.*`). It is a **Field-Reference**, so `$Body` fails at
  flow start: `BIP2211E ... valid values are 'Field-Reference'`.
- **`StartDate`/`StartTime` are absolute and `Interval` is not a delay.**
  `'TODAY'`/`'NOW'` fires immediately whatever `Interval` says — `Interval` is
  only the gap between fires when `Count > 1`. For "in N seconds", compute
  `CURRENT_TIMESTAMP + CAST(n AS INTERVAL SECOND)` and format it
  `'yyyy-MM-dd'` / `'HH:mm:ss'`. Proven: request 08:35:30 → fired 08:35:45.
  `Action='CANCEL'` + `Identifier` on a TimeoutControl node with the same
  `uniqueIdentifier` removes a pending request (proven: a 30s timeout cancelled
  immediately never fired).
- **Automatic-mode TimeoutNotification propagates an empty message** — `Root`
  has only `Properties`, no body parser. The tick metadata is in
  `LocalEnvironment.TimeoutRequest`, where `Identifier` is the node's
  `uniqueIdentifier` and `Interval` its `timeoutInterval`. The Scheduler instead
  gives you a real body: `Root.JSON.Data` **and** `LocalEnvironment.Scheduler`,
  each with `lastEventTime`, `currentEventTime`, `scheduleIdentifier`.
- **Trace node `pattern` takes ESQL field references with dots.** An XPath-style
  `${LocalEnvironment/Scheduler/currentEventTime}` stops the flow starting with
  `BIP2432E: The correlation name 'Scheduler' is not valid`. A Trace node with
  `destination="file"` is a better tick logger than FileOutput for this kind of
  proof — it appends immediately instead of buffering in `mqsitransit/`.

### Timer policy (prompted by a Toolkit-generated `timer.policyxml`)

- **`queuePrefix` is an INFIX, not a prefix.** `<queuePrefix>test</queuePrefix>`
  gives `SYSTEM.BROKER.TIMEOUT.test.QUEUE` — the value lands before `.QUEUE`. It
  is case-sensitive, so `runmqsc` needs the name quoted
  (`DISPLAY QLOCAL('SYSTEM.BROKER.TIMEOUT.test.QUEUE')`); unquoted, runmqsc
  uppercases it and says AMQ8147E not found while `DISPLAY QLOCAL(*)` clearly
  lists it.
- **ACE defines the PREFIXED queue itself at flow start, and only that one.**
  Two fresh prefixes both produced a queue whose `CRDATE`/`CRTIME` matched the
  flow-start instant, with `DEFPSIST(YES) MAXDEPTH(100000) MAXMSGL(104857600)` —
  versus `NO / 5000 / 4194304` inherited from `SYSTEM.DEFAULT.LOCAL.QUEUE`, so
  it is a deliberate `DEFINE` and not default inheritance. It makes sense as the
  exception: nothing could pre-create a queue whose name you invent in a policy.
  **It does not generalise.** The `SYSTEM.BROKER.*` set is provisioned by
  creating an *integration node* against a queue manager; a **standalone
  integration server provisions nothing**, which is what
  `server/sample/wmq/iib_queues_create.mqsc` is for. Corroborated locally: on a
  hand-created queue manager, `SYSTEM.BROKER.TIMEOUT.QUEUE`'s `CRDATE` matched
  the day that script was run for the EDA work, **not** the day the queue
  manager itself was created. A prefix is how you give one application its own
  timeout store.
- **The Timer policy does NOT change the MQ requirement**, even though
  `Policy.xsd` annotates `ComIbmTimerPolicyType` `requiresMQ="true"`.
  `operationMode` alone decides: with `defaultQueueManager` commented out, all
  three automatic-mode flows started and ticked — including two carrying a
  `queuePrefix` policy — while both controlled-mode flows failed `BIP2685E`.
- **The policy attaches to controlled mode too**, not just automatic, and a
  controlled pair still pairs through the policy reference as long as both nodes
  name the same policy in `uniqueIdentifier`. `timeoutInterval` overrides the
  node's (node 30 + policy 3 → 3s ticks; node 30 + policy 5 → 5s ticks).
- **The Scheduler node takes no policy at all** — `ComIbmSchedulerNodeType` has
  no `<links operationalPolicy=...>` in MessageFlow.xsd, unlike both timeout
  node types. Everything about a Scheduler is node properties or a BAR override.
- **The Timer policy has exactly two properties**, `queuePrefix` and
  `timeoutInterval`, both `iib:dynamic="false"` — a change needs a restart.
- **`policyTemplate="Timer"` is what the Toolkit writes** (settles a question
  open since): the attribute carries the policy *type* name. The
  hand-written `policyTemplate=""` in this skill's templates and the shipped
  IBM templates' omission of the attribute both also deploy and run.

## Routing nodes: Filter, RouteToLabel/Label, FlowOrder

Copy-ready flows in `examples/routing/`, proof app `ROUTING_DEMO_APP`. The Route
node was already proven (`MQ_ROUTE_APP`); these are the rest of the
`noderouting` family, minus DatabaseRoute. **None of them needs MQ, a database
or any external service** — five HTTP flows on a bare standalone server, which
makes this the cheapest capability folder to re-verify.

- **A Filter node's ESQL is a `CREATE FILTER MODULE`, not a COMPUTE MODULE**, and
  inside it **`InputRoot` does not exist — use `Root`.** A Filter has no output
  message, so none of the `Input*`/`Output*` correlation names are in scope; only
  `Root`, `Body`, `Properties`, `Environment`, `LocalEnvironment`,
  `ExceptionList`, `DestinationList` are. This one is nastier than the usual
  msgflow mistakes because it fails at **deploy** and takes the **whole
  application** down rather than the single flow:
  `BIP9318E: Request to 'PreSetupValidate' resource '<APP>' ... failed` +
  `BIP2432E: The correlation name 'InputRoot.JSON.Data.amount' is not valid.`
  Contrast the timer nodes, where a bad node property gives a per-flow
  `BIP9320E` and the rest of the app still starts.
- **The Filter node's `unknown` terminal is ESQL three-valued logic, not an error
  path.** A comparison against a missing field yields UNKNOWN, so it routes to
  `unknown`, not `false`. Proven: `{"amount":150}`→true, `{"amount":50}`→false,
  `{}`→unknown. Either wire the terminal or `COALESCE` the field.
- **RouteToLabel has no `out` terminal (only `in` + `failure`) and Label has no
  `in` terminal (only `out`).** They are never wired together — the runtime jumps
  from one to the other by name, which means a correct flow looks disconnected in
  the Toolkit.
- **The label list is `LocalEnvironment.Destination.RouterList.DestinationData[n].labelName`,
  a structure in NO product schema** — `LocalEnvironment.schema.json` types
  `RouterList` as a bare `{"type":"object"}`. Same class of undocumented
  convention as the Collector table rows and the DatabaseRetrieve grid. The
  Compute that writes it needs `computeMode="destinationAndMessage"`.
- **`mode` takes exactly one entry from that list**: `routeToFirst` → entry 1,
  `routeToLast` → entry 2 of a two-entry list (proven side by side with identical
  lists). An **empty or absent list throws** rather than falling through:
  `BIP4256E: The RouteToLabel node '<name>' was unable to locate a 'labelName'
  element in the local environment.`
- **FlowOrder has no properties at all** and genuinely serialises the branches:
  two chained Trace nodes on `first` plus one on `second`, all appending to one
  file, came out `FIRST-step1 → FIRST-step2 → SECOND`. Both branches get the same
  message, so put a reply node on one branch only.
- **Route node rule order is first-match-in-table-order** with
  `distributionMode="first"`: a score of 95 satisfied both `> 80` (gold) and
  `> 50` (silver) and took gold, the rule listed first. List the narrowest rule
  first. A missing field matches nothing and falls to `default` with no
  exception. Also confirmed the node works on JSON (`$Root/JSON/Data/score`), not
  just MQMD as in the older MQ example.

## Slack connector (RETRIEVEALL + CREATE runtime-proven)

Runbook in `Slack.md`, copy-ready flows in `examples/slack/`, proof app
`workspace2/test_app` + `workspace2/SLACK_POL`. Both `.msgnode` files sit at the
**root** of `com.ibm.etools.mft.connectornodes.definitions_13.0.2.2.jar`, so the
`xmlns` URI is the bare filename (no slash path, unlike Kafka).

- **The request node has a FOURTH terminal, `OutTerminal.noData`, and leaving it
  unwired turns an empty result set into an exception.** A zero-result
  `RETRIEVE*` routes there; unconnected it gives
  `BIP2230E` + `BIP9975E: No documents found`, which over HTTP surfaces as a
  **404** — nothing about that says "your query matched zero rows". Wire it to a
  Compute returning an empty collection. **Assume this applies to every
  connector retrieve, not just Slack's** — the S3 and LDAP pages predate the
  finding and don't mention it.
- **A `CREATE` action requires its `gen/<prefix>.request.schema.json` to exist;
  a `RETRIEVEALL` does not.** Missing it fails at FLOW START with `BIP9958E`
  naming the exact path. Content can be `{}` — but not zero bytes, which is the
  separate `BIP5753E` already recorded for S3.
- **`OBJECT_NAME` is the "parent discriminator" and is mandatory on `CREATE`**,
  even though the connector's own model lists both `OBJECT_NAME` and `OBJECT_ID`
  in the interaction's `basic` set. Without it:
  `BIP9937E ... 'Error: Parent discriminator connector property OBJECT_NAME -
  not found in body or filter'`. For `message` it is the channel. Both go in the
  request **body**, not a `<connectorProperty/>` row — consistent with the
  earlier S3 note that a property belongs in the body when no such row exists.
- **`queryProperties` needs `limit`, not just `allowTruncation`** — same
  silently-returns-one-record trap as S3.
- **`policyType="slack"` is not in `Policy.xsd`.** Connector policy types are
  validated from the connector descriptors, so don't go looking for them in the
  policy schema. `authenticationMethod` maps to a field in
  `loopback-connector-slack/descriptors/slack.json`: `BASIC_OAUTH` →
  `access_token_basic`, `OAUTH2_WEB` → `access_token`; **both are just a static
  access token**, neither takes a client id/secret or refresh token.
- **The `slack` vault credential type has exactly one property,
  `--access-token`** — no `--refresh-token` (compare `anaplan`, which has a
  four-property oauth variant). ACE therefore cannot rotate a Slack token: a
  rotating one must be re-entered by hand.
- **The authoritative object/action list is the connector's own model, not the
  docs:** `server/nodejs_all/node_modules/@ibm-app-connect/loopback-connector-<name>/overrides/objects.json`
  for objects and interactions, `lib/models/<object>.json` for each
  interaction's `requestProperties.mandatory` / `responseProperties.included` and
  its `filterSupport`. This is the connector equivalent of reading node
  definitions out of the Toolkit jar, and it answered every "what does this
  action accept" question in this session.
- **Read `filterSupport` before building a retrieve flow.** Slack's
  `message`/`RETRIEVEALL` declares `mandatory: ["query"]`, which maps to Slack
  *search* rather than `conversations.history` — and search rejects bot tokens
  (`not_allowed_token_type`), so that action is unreachable with `xoxb-`
  regardless of `groups:history`. Checking the model first avoided building a
  flow that could never work.

### Slack-side facts that present as ACE faults

- **Slack tokens: `xoxe.xoxp-`/`xoxe-` are App Configuration Tokens** from the
  panel at the bottom of the apps list — scoped
  `identify, app_configurations:read/write`, ~12h expiry, useless for workspace
  data. The usable ones are `xoxb-`/`xoxp-` from the app's **OAuth &
  Permissions** page. Prefix test: **if it starts `xoxe`, wrong panel.** Always
  check scopes before wiring a token in:
  `curl -s -X POST https://slack.com/api/auth.test -D - -o /dev/null -H "Authorization: Bearer $T" | grep -i x-oauth-scopes`
- **Adding scopes and reinstalling widens the existing token in place** — it does
  not necessarily issue a new string. Observed: the same `xoxb-` gained
  `chat:write, groups:history` after a reinstall and the vault needed no change.
  Check the old token's scopes before going to fetch a new one.
- **`channel` = public channels, `group` = private channels** in Slack's object
  model. A bot in a public channel never appears in a `group` query.
- **Private channels are invisible to non-members in both directions.** A bot
  with `groups:read` sees only ones it was *invited* to, so `count: 0` is often
  the correct answer rather than a bug; and a channel created by the bot via
  `conversations.create` has the bot as its only member, so it does not appear in
  your own Slack until `conversations.invite` adds you.

### `mqsicreatebar` silently produces an empty BAR for connector-node apps

- **Connector nodes cannot be compiled to CMF, so `mqsicreatebar` needs
  `-deployAsSource`.** Without it the build reports **zero** `Problem N:` lines —
  the marker count we normally read as the verdict — exits non-zero, and writes a
  BAR containing only `META-INF`. The cause is logged separately, not as a
  problem marker:
  ```
  The message flow contains a ComIbmApplicationConnectorRequest_slack.msgnode
  which cannot be added as CMF. You cannot use the compile and in-line resource
  option for this message flow.
  [ERROR] { Error adding to BAR Model. fileToAdd: L/test_app/slack_flow.msgflow }
          - com.ibm.etools.mft.bar.model.BrokerArchiveException
  ```
  With `-deployAsSource` the same workspace builds clean (exit 0, `test_app.appzip`
  present, 0 markers). **Always check the BAR actually contains `<APP>.appzip`**
  rather than trusting a zero-marker result — this is the one case found so far
  where "no problems" and "nothing built" look identical.

## REST request nodes (3 of 4 runtime-proven vs the Swagger Petstore)

Runbook in `REST.md`, copy-ready flows in `examples/rest/`, proof app
`REST_DEMO_APP` (workspace2). Driven against
`https://petstore.swagger.io/v2/swagger.json` — no MQ, no DB, no credentials,
only outbound HTTPS, so this is a cheap family to re-verify.
`AppConnectRESTRequest` is **not** proven: it targets connectors hosted on IBM
App Connect, so it needs an App Connect instance rather than any REST endpoint.

- **These nodes are spec-driven, not URL-driven — that is why a hand-authored one
  will not build.** `definitionFile` and `operationName` are mandatory with no
  defaults, and `definitionFile` is a **plain filename relative to the
  application root** (spec beside the msgflow; `ibmint package` ships it
  automatically). `operationName` is an `operationId` from the spec. With only
  those set the node called `https://petstore.swagger.io/v2/store/inventory` —
  scheme, host, `basePath` and the HTTP verb all came from the document, and **no
  URL appears anywhere in the flow**. `baseURL` overrides the host.
- **`definitionType` is `swagger_20` or `openapi_3`** — note `openapi_3`, NOT
  `openapi_30`. The two provider ids come from
  `com.ibm.etools.mft.restapi.ui`: `com/ibm/broker/rest/{swagger_20,openapi_3}/ApiProviderImpl`.
  It is a plain `xsd:string` (XSD default `swagger_20`), so a wrong value is not
  caught by schema validation. Mandatory only on `AppConnectRESTRequest`.
- **The msgnode's mandatory-looking `name` property is a red herring.** It is
  `lowerBound="1"` with no default, but all four proven flows package, deploy and
  run without it. Don't chase it.
- **`iib:parameters` is a child ELEMENT and rows use the repeated form**, not the
  XSD's nested `<ParametersTableRow>` — the same convention as the Collector's
  `eventHandlerPropertyTable`, and another case where the XSD's row shape is not
  what the runtime reads:
  ```xml
  <parameters name="status" type="query" expression="$Body/Data/status"/>
  ```
  `name` must match the spec's parameter, `type` is `query`/`path`/`header`
  (required), `expression` is `iib:valueType="xpath"`. `$Body/Data/status` reaches
  a field in a JSON request body — for the JSON domain `$Body` is the `JSON`
  parser element. Proven: the node built `…/pet/findByStatus?status=available`.
- **Sync and async put response metadata in different places.** `RESTRequest`
  populates `LocalEnvironment.WrittenDestination.REST` (`URL`, `Method`,
  `StatusCode`, `TotalRequestTime`, header/body sizes, Compression subtrees);
  `RESTAsyncResponse` populates `LocalEnvironment.REST.Response` (`StatusCode`,
  `CorrelationID`, sizes). **`WrittenDestination.REST` is not available in the
  async response flow** — it belongs to the flow that made the call.
- **`error` and `failure` are separate terminals** on the request nodes. `error`
  carries HTTP error-status responses (body at `errorDataLocation`, default
  `$OutputRoot`); `failure` is node exceptions. `RESTAsyncRequest` has no `error`
  terminal — the response node owns it.
- **The async pair is joined by a correlator, not a wire.**
  `asyncResponseCorrelator` on the request must equal `asyncRequestCorrelator` on
  the response. `RESTAsyncResponse` is an **input node**
  (`nodeType="response,asynchronous,input"`) with no `in` terminal, so a correct
  pair looks like two disconnected flows — same visual oddity as
  RouteToLabel/Label. **The HTTP reply identifier survives the hop**: flow A
  receives the request and ends at the async node, flow B replies to the original
  caller, and flow A's thread is not held during the call.
- **The `-deployAsSource` requirement is about CONNECTOR nodes, not Java.** REST
  nodes are `requiresJava="true"` too, yet `REST_DEMO_APP` builds clean under a
  plain `mqsicreatebar -cleanBuild` (exit 0, `REST_DEMO_APP.appzip` present).
  Only `ComIbmApplicationConnector*` / `com_ibm_connector_*` nodes cannot be
  compiled to CMF.
- `securityIdentity` on these nodes uses a **`rest`** vault credential, and the
  in-field help says to give the name **without** the `rest::` prefix. Auth types:
  `basic` (username+password), `basicApiKey` (both + api-key), `apiKey` (api-key).

## Flow unit testing part 2: NodeStub, and a correction

Extends the earlier entry. Proof project `REST_DEMO_TEST_APP` (workspace2),
**19/19 green in 0.4s with no network access**; examples in
`examples/unittest/` (`GET_INVENTORY`, `FIND_BY_STATUS`, `STUB_BEHAVIOUR`,
`ASSERTION_STYLES`).

- **CORRECTION to the earlier entry: `messagePath()` is NOT broken.** That
  entry said reads "fail with BIP2331 for every path form" on a propagated
  assembly. They fail for the *slash* form. The API has **two different path
  syntaxes** and the earlier attempt used the wrong one:
  | Use | Syntax |
  |---|---|
  | `hasMessageTreeElement()`, `messagePath()` read, **and `localEnvironmentPath()` when building** | dotted from Root, no `$`, no slashes — `JSON.Data.inventory.available`, `WrittenDestination.REST.URL` |
  | `ignorePath()` only | slash-delimited, **leading slash** — `/JSON/Data/pending` |
  A `$` path on a **read** fails loudly as `Field '$' within field 'Root' does
  not exist` ($ taken as a literal child name); a *folder* path raises BIP2111,
  so `messagePath()` reads leaves only.
- **A `$.` prefix when BUILDING an assembly fails SILENTLY**, which is far worse.
  `localEnvironmentPath("$.WrittenDestination.REST.URL").setValue(...)` succeeds,
  serializes into the assembly as a literal `<iib:element iib:name="$">` wrapper,
  and the flow never sees the value — no error anywhere. Use the dotted form for
  writes too.
- **`NodeStub` replaces a node, so a flow calling REST/DB/a connector can be
  tested hermetically** — but only if it is driven correctly, and the wrong way
  gives NO warning:
  - ✅ `new NodeSpy(inputNode).propagate(assembly, "out")` — the stub intercepts.
  - ❌ `stub.evaluate(assembly, false, "in")` — **the REAL node executes** (the
    HTTP call actually went out, `statusCode:200`) and the stub propagates an
    **EMPTY** message. `isTrainedForCall()` returns `true` either way, so the
    stub looks configured while being bypassed.
- **`NodeSpy.evaluate(assembly, boolean, terminal)`'s boolean means "evaluate in
  ISOLATION", not "propagate"** — `true` stops at that node, `false` lets the
  flow continue downstream. Proven by downstream call counts. The older
  `TransformTest` passes `true` and is correct, but only because it tests a
  single node.
- **Stop the flow on the DOWNSTREAM node's INPUT terminal**
  (`setStopAtInputTerminal("in")` on the HTTPReply node). Using
  `setStopAtOutputTerminal("out")` on the node you are asserting about drives its
  propagate count to **0**, which reads as "the node never ran".
- **A stub DOES carry LocalEnvironment** when the dotted path form is used, so
  ESQL reading `LocalEnvironment.WrittenDestination.*` IS coverable. (An earlier
  draft of this entry claimed body-only; that was the silent `$.` trap above, not
  a stub limitation — caught by re-testing with the corrected syntax.)
- **`onCall()` cannot express per-call behaviour at all — a hard API limit.**
  Proven from bytecode, not just observation: `NodeStub` caches one
  `iDefaultTraining`, so `onCall()` returns the same `TrainedBehaviour` every
  time, and **no native signature carries a call index**
  (`_onCallPropagatesMessage(handle, inTerminal, outTerminal, assembly)`).
  Training twice replays call 1. `isTrainedForCall(s)` takes the **input terminal
  name** ("in" → true; "out"/"1"/"bogus" → false), which is the last thing that
  could have carried a call number. Drive the flow once per scenario instead.
- **A node can only be mocked ONCE per test**: a second `NodeSpy` on the same
  node raises `Node already mocked: <flow>/<node>. You can only mock a Data Flow
  Node once`. Hoist the spy and reuse it.
- **`propagatesInputMessage(in, out)` is a pass-through stub** — replaces the
  node but forwards whatever arrived.
- **`getMessageTreeSerializedForm()` is the fastest way to find a path** — it
  dumps the logical tree as XML with `iib:valueType` on every element. Same for
  `getLocalEnvironmentTreeSerializedForm()` / `Environment` / `ExceptionList`.
- **Tree matchers assert logical TYPE as well as value**:
  `hasMessageTreeElement(p).isInteger().equals(42)`, with `.ignoreTypes()` when
  only the text matters. Much better failure messages than a whole-body compare.
- Compute nodes report `outputTerminalNames() = failure,out,out1,out2,out3,out4`
  whether or not the extra terminals are wired.
- **Async pairs (`RESTAsyncRequest`/`RESTAsyncResponse`) need both techniques.**
  The response half is an input node, so `propagate()` straight into it tests it
  hermetically — note it reads `LocalEnvironment.REST.Response`, not
  `WrittenDestination.REST`. For the correlation end to end, use
  `whenPropagateCountIs(1)` on the response flow's node and wait on the
  `CompletableFuture` after driving the request flow.
- **`--start-msgflows false` silently breaks any cross-flow test.** With flows
  stopped the async response flow never runs and the wait times out at propagate
  count 0. Run async-pair tests WITHOUT that flag; guard them with JUnit's
  `assumeTrue(...)` so they report ABORTED rather than FAILED elsewhere (26/26
  with flows started, 25 + 1 skipped without, exit 0 either way).
- **A test CAN deploy its own application**: `TestSetup.setBarFileSearchPath(dir)`
  + `deployBarFile("X.bar")` (or an absolute path) returns true, and the flow is
  immediately spy-able and drivable — proven with an app deliberately absent from
  the work dir. **But it is a REAL deployment**: the app is unpacked into
  `<work-dir>/run/` and persists after the run, and the suite slowed from ~2s to
  ~12s. Isolate it in its own class; it is not side-effect free.
- **Test-project classpath: all five jars ship with ACE** —
  `common/classes/IntegrationTest.jar`, `common/classes/hamcrest-2.2.jar`,
  `tools/plugins/junit-jupiter-api_5.10.2.jar`,
  `tools/plugins/junit-platform-commons_1.10.2.jar`, and
  `tools/plugins/org.opentest4j_1.3.0.jar` (the last ONLY for `assumeTrue()`,
  whose absence fails as `cannot access TestAbortedException`).
- **Never use `org.eclipse.jdt.USER_LIBRARY/...` in a test project's
  `.classpath`** — a user library is defined **per Eclipse workspace**, so the
  project builds in one workspace and shows unresolved imports in another, and it
  does not resolve headlessly. Use the
  `com.ibm.etools.mft.unittest.integrationTestDependencies` container (portable)
  or explicit `kind="lib"` jar paths (absolute, always resolves).
- **After hand-editing `.classpath`, the Toolkit needs Refresh (F5) + Project →
  Clean before the jars even appear in Project Properties → Java Build Path** (and
  they may still need ticking). Until then the editor shows unresolved imports
  while `ibmint package` compiles the same source cleanly — so **Toolkit red
  markers are not evidence the tests are broken**. Confirm against the headless
  build first. This bites systematically because this skill hand-authors project
  files outside the Toolkit.
- Still unexplored: `SpyObjectReference.subflowNode()` and the exception matchers
  `hasMessageNumber` / `containsMessageText` / `causedBy` (the latter need a flow
  with error handling wired, which none of these have).
