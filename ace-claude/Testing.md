# Flow unit testing in ACE — the test harness

> **Status: runtime-proven (ACE 13.0.2.2).** 29 tests across two proof projects,
> running headless in ~2s with no network, database, queue manager or vault.
> Worked test classes: `examples/unittest/`.
> Proof projects: `HTTP_JSON_APP_Test` (NodeSpy) and `REST_DEMO_TEST_APP`
> (NodeStub, async pair, self-deploy).

**Read this before writing or debugging a test project.** It is not needed for
authoring flows — nothing here applies to msgflow or ESQL authoring.

---

## The mental model: a mock layer on a live server, not a sandbox

The harness does not run flows in isolation. It starts a real integration server
with your application deployed, then **attaches native mocks to individual
deployed nodes** — `NodeSpy` holds an `iNativeMockHandle`, the matchers are
`native`, and there is a `JNIException` type.

Almost every surprising behaviour follows from that:

- Mocks are **server-global state**, not per-test-object — hence the static
  `TestSetup.restoreAllMocks()`, and hence
  `Node already mocked: <flow>/<node>. You can only mock a Data Flow Node once`
  if you construct a second spy for the same node in one test. Hoist and reuse.
- `NodeStub extends NodeSpy` — a stub is a spy *plus* trained behaviour.
- Testing is **white-box**: nodes are addressed by their label, so tests are
  coupled to node names in the flow.

### The two injection verbs — the single most important distinction

| Call | Means | Use for |
|---|---|---|
| `evaluate(assembly, isolation, terminal)` | **invoke this node's implementation** | testing one node |
| `propagate(assembly, terminal)` | **emit from this node's output terminal** | driving a whole flow |

This explains the worst trap in the API: calling `evaluate()` on a **stubbed**
node runs the **real** node, because `evaluate` means "call the implementation"
while trained behaviour is consulted on *flow dispatch*. See *Stubbing* below.

It also explains `evaluate()`'s boolean, which means **"evaluate in isolation"**,
not "propagate": `true` stops at that node, `false` lets the flow continue
downstream.

### Flow dispatch vs node invocation — why `--start-msgflows false` matters

With flows stopped you can still `evaluate()` and `propagate()` on nodes, because
mocks attach to nodes, which exist regardless. But anything requiring the **flow**
to dispatch does not happen — which is why a cross-flow or async test times out
silently with propagate count 0. Two mechanisms, one flag governing only one.

### What it is good and bad at

**Good:** flow logic. Sub-second, hermetic, no infrastructure. `NodeStub` means a
flow calling REST, a database or a connector is testable with none of them
reachable.

**Bad:** transport. `HTTPReply` cannot reply without a real request, so you must
stop the flow before it — you never test the response as a client sees it.

---

## Test project scaffold (all four pieces required)

1. `.project` — `org.eclipse.jdt.core.javabuilder` +natures
   `org.eclipse.jdt.core.javanature` AND
   `com.ibm.etools.msgbroker.tooling.testProjectNature` (see
   `test-project.project`; rename to `.project`).
2. **`testproject.descriptor` in the project root — the critical marker.**
   Without it `ibmint package` says "No valid resources found to package":
   ```xml
   <?xml version="1.0" encoding="UTF-8" standalone="yes"?><ns2:testprojectDescriptor xmlns="http://com.ibm.etools.mft.descriptor.base" xmlns:ns2="http://com.ibm.etools.mft.descriptor.testproject"></ns2:testprojectDescriptor>
   ```
3. Source under `src/main/java/` (the layout the framework expects).
4. `.classpath` with the `src/main/java` source entry (JRE container etc.).

## Classpath — the jars all ship with ACE

Nothing to download. Five jars, verified by compiling the example sources with
`javac` outside Eclipse:

```
<ACE_INSTALL>/common/classes/IntegrationTest.jar               com.ibm.integration.test.v1.*
<ACE_INSTALL>/common/classes/hamcrest-2.2.jar                  org.hamcrest.MatcherAssert
<ACE_INSTALL>/tools/plugins/junit-jupiter-api_5.10.2.jar       org.junit.jupiter.api.*
<ACE_INSTALL>/tools/plugins/junit-platform-commons_1.10.2.jar  required by jupiter
<ACE_INSTALL>/tools/plugins/org.opentest4j_1.3.0.jar           ONLY if you use assumeTrue()
```

`org.opentest4j` is easy to miss: omit it and `assumeTrue(...)` fails to compile
with `cannot access TestAbortedException` — naming a class you never imported.

Two ways to reference them in `.classpath`:

| Form | Portable? | Notes |
|---|---|---|
| `<classpathentry kind="con" path="com.ibm.etools.mft.unittest.integrationTestDependencies"/>` | yes | the container form; what a working Toolkit project uses |
| `<classpathentry kind="lib" path="<ACE_INSTALL>/common/classes/IntegrationTest.jar"/>` ×5 | no — absolute paths | always resolves, including headless |

Avoid `org.eclipse.jdt.USER_LIBRARY/Integration Test Library`: a user library is
defined **per Eclipse workspace**, so a project that builds in one workspace
shows unresolved imports in another, and it does not resolve headlessly at all.

> **After hand-editing `.classpath`, the Toolkit will not pick it up on its own.**
> Right-click the project → **Refresh (F5)**, then **Project → Clean**, and only
> *then* do the jars appear under Project Properties → Java Build Path, where they
> may still need ticking. Until you do that the editor shows unresolved imports
> for both `org.junit` and `com.ibm.integration` — while `ibmint package`
> compiles the very same source cleanly (`BIP8409I`). **Red markers in the
> Toolkit do not mean the tests are broken**; check the headless build before
> chasing them.

## Build → deploy → run (fully headless, no Toolkit)

```bash
ibmint package --input-path <WORKSPACE> --output-bar-file bars/<T>.bar --project <T>
#   → compiles the Java (BIP8409I) and adds <T>.testzip to the BAR
ibmint deploy --input-bar-file bars/<T>.bar --output-work-directory <WORKDIR>
IntegrationServer --work-dir <WORKDIR> --test-project <T> \
  --start-msgflows false --no-nodejs --vault-key <KEY>
#   → prints TEST RESULTS + TOTALS, exits 0 on pass / 1 on fail (CI-friendly)
```

The application under test must already be deployed in the same work dir.
`--start-msgflows false` keeps flows from taking live traffic; `--no-nodejs`
speeds startup (admin REST is off, not needed — the run self-terminates).

## Writing tests (API = common/classes/IntegrationTest.jar, JUnit 5)

```java
SpyObjectReference ref = new SpyObjectReference()
    .application("HTTP_JSON_APP").messageFlow("HTTP_JSON_MF").node("build_json_resp");
NodeSpy spy = new NodeSpy(ref);                    // node() takes the node LABEL
TestMessageAssembly input = new TestMessageAssembly();
input.buildJSONMessage("{\"firstName\":\"Ada\", ...}");   // also buildXMLMessage/buildBLOBMessage
spy.evaluate(input, true, "in");                   // inject at the input terminal
assertThat(spy, terminalPropagateCountIs("out", 1));
assertThat(spy, propagatedJSONFromTerminalOnCall("{...expected json...}", "out", 1));
```

Standalone pre-compile check (catches API errors before ibmint):
`javac -cp common/classes/IntegrationTest.jar:server/classes/junit-platform-console-standalone-1.7.0.jar ...`

## Gotchas (observed)

- **`Matchers.propagatedJSONFromTerminalOnCall(expectedJSON, terminal, call)`
  takes the JSON FIRST** — despite the name reading as "from terminal".
  Passing the terminal first fails with `Output terminal "{...json...}" does
  not exist`.
- **`messagePath()` DOES work on a propagated assembly — with the right path
  syntax.** An earlier note here said every form failed with BIP2331; that was
  the *slash* form. The API uses **two different path syntaxes**, and mixing them
  up is the whole problem:

  | Use | Syntax | Example |
  |---|---|---|
  | `hasMessageTreeElement()`, `messagePath()`, **and `localEnvironmentPath()` when building** | dotted from Root, **no `$`, no slashes** | `JSON.Data.inventory.available`, `WrittenDestination.REST.URL` |
  | `ignorePath()` only | slash-delimited with a **leading** slash | `/JSON/Data/inventory/pending` |

  **Never use a `$.` prefix.** On a read it fails loudly; when *building* it
  fails **silently** — `localEnvironmentPath("$.WrittenDestination.REST.URL")`
  creates a literal `<iib:element iib:name="$">` wrapper, `setValue()` succeeds,
  the assembly serializes happily, and the flow never sees the value.

  A `$`-prefixed path on a read fails as `Field '$' within field 'Root' does not
  exist` — `$` is taken as a literal child name. `messagePath()` only reads a
  **leaf**: a folder path raises BIP2111.
- `TestSetup.restoreAllMocks()` in `@AfterEach`.
- The run needs the work dir's `--vault-key` like any other server start.

---

## Stubbing a node with `NodeStub` — testing a flow without the outside world

`NodeSpy` observes a node; **`NodeStub` replaces it**. That is what makes it
possible to unit-test a flow whose middle node calls a REST API, a database or a
connector, without any of them being reachable. `REST_DEMO_TEST_APP` stubs the
REST Request node in `REST_INVENTORY_MF`, so 19 tests run in 0.4s with no network.

```java
NodeStub restNode = new NodeStub(new SpyObjectReference()
        .application("REST_DEMO_APP").messageFlow("REST_INVENTORY_MF").node("get_inventory"));
TestMessageAssembly canned = new TestMessageAssembly();
canned.buildJSONMessage("{\"available\":42}");
restNode.onCall().propagatesMessage("in", "out", canned);   // (inTerminal, outTerminal, body)

NodeSpy buildReply = new NodeSpy(node("build_reply"));
new NodeSpy(node("reply")).setStopAtInputTerminal("in");    // HTTPReply cannot reply in a test

TestMessageAssembly trigger = new TestMessageAssembly();
trigger.buildJSONMessage("{}");
new NodeSpy(node("/rest/inventory")).propagate(trigger, "out");   // drive from the INPUT node
```

### The trap: `evaluate()` on a stubbed node runs the REAL node

This cost several debugging cycles and gives no warning.

| | |
|---|---|
| `inputNodeSpy.propagate(assembly, "out")` | ✅ flow runs, **stub intercepts** |
| `stub.evaluate(assembly, false, "in")` | ❌ the **real node executes** — the HTTP call goes out — and the stub propagates an **EMPTY** message |

`isTrainedForCall("in")` returns `true` in both cases, so the stub looks
correctly configured while being bypassed. Drive a stubbed flow from its **input
node** with `propagate()`, never by calling `evaluate()` on the stub.

### `evaluate()`'s boolean means "in ISOLATION", not "propagate"

Proven by counting downstream calls:

```
evaluate(a, true,  "in") → downstream node call count 0   node in ISOLATION
evaluate(a, false, "in") → downstream node call count 1   flow CONTINUES
```

`TransformTest` passes `true` and is right — but only because it tests one node.

### Stopping the flow before a node that cannot run

Put the stop on the **downstream node's INPUT** terminal:

```java
new NodeSpy(node("reply")).setStopAtInputTerminal("in");     // ✅
new NodeSpy(node("build_reply")).setStopAtOutputTerminal("out");  // ❌ suppresses that
                                                                  //    propagate entirely
```

`setStopAtOutputTerminal` on the node you are asserting about drives its
propagate count to **0**, which reads as "the node never ran".

### Other stub semantics (all verified in `STUB_BEHAVIOUR.java`)

- **`propagatesInputMessage(in, out)` is a pass-through stub** — replaces the node
  but forwards whatever arrived, no canned body needed.
- **A stub DOES carry LocalEnvironment** — so ESQL reading
  `LocalEnvironment.WrittenDestination.*` can be covered, which makes the REST
  example's `calledUrl`/`method`/`statusCode` fields testable:
  ```java
  canned.localEnvironmentPath("WrittenDestination.REST.URL").setValue("https://example.invalid/x");
  canned.localEnvironmentPath("WrittenDestination.REST.StatusCode").setValue(201);
  ```
  (This corrects an earlier note here that said the body only was delivered —
  that was the `$.`-prefix trap above, not a stub limitation.)
- **`onCall()` cannot express per-call behaviour at all** — this is a hard limit,
  not a usage mistake. `NodeStub` caches a single `iDefaultTraining` field, so
  `onCall()` returns the *same* `TrainedBehaviour` every time, and **no native
  signature carries a call index**:
  ```
  _onCallPropagatesMessage(handle, inTerminal, outTerminal, assembly)
  ```
  Training twice does not make call 2 differ (it replays call 1). `isTrainedForCall(s)`
  takes the **input terminal name**, not a call number — `"in"` → true,
  `"out"`/`"1"`/`"bogus"` → false. If you need call-dependent behaviour, drive the
  flow once per scenario instead.
- **A node can only be mocked ONCE per test.** Constructing a second `NodeSpy`
  for the same node raises `Node already mocked: <flow>/<node>. You can only mock
  a Data Flow Node once` — hoist the spy into a variable and reuse it.
- `TestSetup.restoreAllMocks()` genuinely untrains: a stub built afterwards
  reports `isTrainedForCall == false`.

## Assertion styles

`ASSERTION_STYLES.java` runs all four side by side:

| Style | Use when |
|---|---|
| `getJSONMessageBodyAsString()` + `assertEquals` | you want the whole body in the failure message |
| `hasMessageTreeElement(path).isInteger().equals(42)` | precise, and asserts the **logical type** |
| `...ignoreTypes().equals("42")` | the value matters, the type does not |
| `messagePath(path).getValueAsString()` | reading one leaf into a variable |
| `equalsMessage(built).ignorePath("/a/b", true)` | compare whole trees, tolerating known-variable fields |

`TestMessageAssembly.buildAssemblyFromJSONBody(...)` builds the expected side.

**When a path assertion fails, print the tree** — it shows every path and its
`iib:valueType`, which is the fastest way to find the right path:

```java
System.out.println(assembly.getMessageTreeSerializedForm());
```

There are equivalents for the other trees: `getLocalEnvironmentTreeSerializedForm()`,
`getEnvironmentTreeSerializedForm()`, `getExceptionListTreeSerializedForm()`.

## Testing an async pair (`RESTAsyncRequest` / `RESTAsyncResponse`)

`ASYNC_PAIR.java` covers both halves, which need different techniques because
the two flows are joined by a **correlator, not a wire**.

**The response half, in isolation** — `RESTAsyncResponse` is an input node, so
propagate straight into it. Hermetic, no network:

```java
NodeSpy asyncResponse = new NodeSpy(node("REST_ASYNC_RESP_MF", "async_response"));
TestMessageAssembly a = new TestMessageAssembly();
a.buildJSONMessage("[{\"id\":1,\"name\":\"doggie\"}]");
a.localEnvironmentPath("REST.Response.StatusCode").setValue(207);   // dotted, no "$."
asyncResponse.propagate(a, "out");
```

Note the response half reads `LocalEnvironment.REST.Response`, whereas the
synchronous `RESTRequest` node uses `LocalEnvironment.WrittenDestination.REST`.

**The correlation, end to end** — drive the request flow and wait on the
response flow:

```java
CompletableFuture<Void> responseArrived = buildReply.whenPropagateCountIs(1);
new NodeSpy(node("REST_ASYNC_MF", "/rest/async")).propagate(trigger, "out");
responseArrived.get(25, TimeUnit.SECONDS);
```

> **`--start-msgflows false` breaks this test.** With flows stopped the response
> flow never runs and the wait times out with propagate count 0. Async-pair tests
> must run **without** that flag. `ASYNC_PAIR` uses `assumeTrue(...)` so it
> reports ABORTED (skipped) rather than FAILED when flows are stopped or the
> network is unavailable — 26/26 pass with flows started, 25 pass + 1 skipped
> without, exit 0 either way.

## A test can deploy its own application

`TestSetup.deployBarFile()` works — `SELF_DEPLOY.java` proves it with an app that
is **not** in the work dir beforehand, then drives the freshly deployed flow:

```java
TestSetup.setBarFileSearchPath("/path/to/bars");
assertTrue(TestSetup.deployBarFile("MY_APP.bar"));      // bare name + search path
// or: TestSetup.deployBarFile("/abs/path/MY_APP.bar");  // absolute path, no search path needed
NodeSpy build = new NodeSpy(node("MY_APP", "MY_FLOW", "some_node"));   // immediately spy-able
```

So a test project can be self-contained rather than depending on a separate
deploy step having happened first.

> **It is a REAL deployment with a side effect.** The app is unpacked into
> `<work-dir>/run/` and stays there after the run — not in-memory, not rolled
> back. A suite using this is no longer side-effect free, so keep it in its own
> class. Note also that it is markedly slower: the suite goes from ~2s to ~12s
> because a genuine deploy happens.

## API surface worth knowing (`common/classes/IntegrationTest.jar`)

- `NodeSpy`: `callCount()`, `propagateCount(t)`, `receiveCount(t)`,
  `propagatedMessageAssembly(t, n)`, `receivedMessageAssembly(t, n)`,
  `inputTerminalNames()`, `outputTerminalNames()`, `propagate()`, `evaluate()`,
  `setStopAtInput/OutputTerminal()`, `clearStopAtTerminals()`, `restore()`,
  and `whenPropagateCountIs(n)` / `whenCompletedTimes(n)` returning
  `CompletableFuture` for asynchronous flows.
- `SpyObjectReference`: `application()`, `defaultApplication()`, `library()`,
  `messageFlow()`, `node()`, **`subflowNode()`**.
- `TestSetup`: `restoreAllMocks()`, `deployBarFile()`, `setBarFileSearchPath()`,
  `setPropertiesElementsUnset()`, `setResolveTransactionOnPropagate()`.
- `Matchers`: also has exception matchers — `hasMessageNumber(n)`,
  `hasMessageText(s)`, `containsMessageText(s)`, `causedBy(...)` — and
  `hasLocalEnvironmentTreeElement` / `hasEnvironmentTreeElement` /
  `hasLocalExceptionListTreeElement` alongside `hasMessageTreeElement`.
- `MessageAssemblyMatcher` also offers `.ignoreTimeStamps()` and `.ignoreDateTime()`.

A useful sanity check: `outputTerminalNames()` on a Compute node returns
`failure,out,out1,out2,out3,out4` — the four extra out terminals exist whether
you wire them or not.

---

## Error decoder

| Symptom | Cause |
|---|---|
| `No valid resources found to package` | missing `testproject.descriptor` |
| unresolved `org.junit` / `com.ibm.integration` in the Toolkit | `.classpath` edited outside Eclipse — Refresh (F5) + Project → Clean, then tick the jars. `ibmint` compiles fine meanwhile |
| `cannot access TestAbortedException` | `org.opentest4j` missing from the classpath (only needed for `assumeTrue`) |
| `Output terminal "{...json...}" does not exist` | `propagatedJSONFromTerminalOnCall` argument order — JSON goes FIRST |
| `Field '$' within field 'Root' does not exist` | `$.`-prefixed read path; use dotted-from-Root |
| value set but never arrives, no error | `$.`-prefixed **write** path — silently builds a `$` wrapper |
| BIP2111 on a `messagePath()` read | path points at a folder; it reads leaves only |
| `Node already mocked: <flow>/<node>` | second `NodeSpy` on the same node in one test |
| downstream node has propagate count 0 | either `evaluate(..., true, ...)` (isolation), or `setStopAtOutputTerminal` on the node you are asserting about |
| stub ignored, real call goes out | drove the flow with `evaluate()` on the stub instead of `propagate()` on the input node |
| async/cross-flow test times out at 0 | `--start-msgflows false` — omit it |

## See also

- `examples/unittest/` — six worked test classes, one per technique
- `IntegrationServer.md` — server lifecycle, work dirs, vault keys
