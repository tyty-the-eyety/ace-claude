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
