# Flow unit test examples — worked test classes (runtime-proven, 29/29 pass)

Copy-ready JUnit 5 test classes for the ACE test harness. **Read `../../Testing.md`
first** — it carries the harness model, the setup, and every rule these files
depend on. This page only says what each file demonstrates.

Two proof projects: `HTTP_JSON_APP_Test` (NodeSpy on a single node) and
`REST_DEMO_TEST_APP` (everything else).

| File | Demonstrates | Against |
|---|---|---|
| `TransformTest.java` | `NodeSpy` on one Compute node — the simplest possible shape | `HTTP_JSON_MF` |
| `GET_INVENTORY.java` | `NodeStub` — a flow tested with the REST call replaced, no network | `REST_INVENTORY_MF` |
| `FIND_BY_STATUS.java` | a JSON **array** response: `CARDINALITY` + `Item[]`, empty/single/many | `REST_FINDBYSTATUS_MF` |
| `ASYNC_PAIR.java` | an async pair — the response half in isolation, then the correlation end to end via `whenPropagateCountIs()` | `REST_ASYNC_MF` + `REST_ASYNC_RESP_MF` |
| `ASSERTION_STYLES.java` | all four ways to assert on a propagated message, side by side | `REST_INVENTORY_MF` |
| `STUB_BEHAVIOUR.java` | executable documentation of how `NodeStub` actually behaves | `REST_INVENTORY_MF` |
| `SELF_DEPLOY.java` | a test deploying its **own** BAR via `TestSetup.deployBarFile()` | `DEPLOY_PROBE_APP` |

Scaffolding to copy: `testproject.descriptor`, `test-project.project`,
`test-project.classpath`.

## Which one to start from

- **Testing one node's logic** (a Compute's mapping, a Filter's condition) →
  `TransformTest.java`. Nothing else is needed.
- **The flow calls something you cannot reach in a test** (REST, database,
  connector) → `GET_INVENTORY.java`. This is the common case.
- **The response is a JSON array** → `FIND_BY_STATUS.java`.
- **Two flows joined by a correlator** → `ASYNC_PAIR.java`.
- **Unsure how to assert something** → `ASSERTION_STYLES.java`.

## Two files that are documentation, not application tests

`STUB_BEHAVIOUR.java` and `SELF_DEPLOY.java` assert **framework** behaviour
rather than flow correctness — they pin down traps that each cost a debugging
cycle to find, so they are worth keeping as a reference but do not belong in a
real project's suite. `SELF_DEPLOY` additionally has a **side effect**: it really
deploys an app into `<work-dir>/run/` and leaves it there, and it is what takes
the suite from ~2s to ~12s.

## The three traps most likely to bite you

Full detail in `../../Testing.md`; this is the short version.

1. **Drive a stubbed flow from its INPUT node with `propagate()`.** Calling
   `evaluate()` on the stub silently runs the **real** node — the call goes out —
   while the stub propagates an empty message, and `isTrainedForCall()` still
   reports `true`.
2. **Paths are dotted from Root** (`JSON.Data.inventory.available`) for both
   reads and writes; only `ignorePath` uses a leading slash. A `$.` prefix fails
   **silently** when building an assembly.
3. **Red markers in the Toolkit do not mean the tests are broken.** After editing
   `.classpath` by hand: Refresh (F5) → Project → Clean → tick the jars. `ibmint
   package` compiles the same source cleanly throughout.
