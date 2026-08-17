# Flow unit testing — headless ACE Test Project (runtime-proven, 3/3 pass)

JUnit 5 tests that inject a message assembly directly into a single node
(NodeSpy) and assert on what it propagates — no HTTP client, queue, or
listener involved. Workspace proof: `HTTP_JSON_APP_Test` testing the
`build_json_resp` Compute node of `HTTP_JSON_MF`. Files here:
`TransformTest.java`, `testproject.descriptor`, `test-project.project`.

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
- **`messagePath("JSON/Data/x")` does NOT work on a propagated/received
  assembly** at 13.0.2.2 — every path form fails with BIP2331 even though
  `getMessageTreeSerializedForm()` shows the element. Assert with the JSON
  matcher or `getJSONMessageBodyAsString()` instead. (ElementReference paths
  are for BUILDING input assemblies.)
- `TestSetup.restoreAllMocks()` in `@AfterEach`.
- The run needs the work dir's `--vault-key` like any other server start.
