# REST request node examples — RESTRequest and the Async pair (runtime-proven, ACE 13.0.2.2)

Copy-ready flows for the REST request family, driven against the public Swagger
Petstore (`https://petstore.swagger.io/v2/swagger.json`, shipped here as
`petstore.json`). **No MQ, no database, no credentials** — only outbound HTTPS,
which makes this folder cheap to re-verify. Full reference: `../../REST.md`.
Proof app: `REST_DEMO_APP`.

| File | Node | Shows | Endpoint |
|---|---|---|---|
| `REST_INVENTORY_MF` | RESTRequest | the minimum viable config — no parameters | `POST /rest/inventory` |
| `REST_FINDBYSTATUS_MF` | RESTRequest | a `query` parameter via `<parameters>` | `POST /rest/findbystatus` |
| `REST_ASYNC_MF` | RESTAsyncRequest | fire-and-continue half of the async pair | `POST /rest/async` |
| `REST_ASYNC_RESP_MF` | RESTAsyncResponse | the correlated half that replies | — (input node) |

Observed:

```
POST /rest/inventory    {}                       → {"calledUrl":"https://petstore.swagger.io/v2/store/inventory",
                                                    "method":"GET","statusCode":200,"elapsedMs":103,"inventory":{...}}
POST /rest/findbystatus {"status":"available"}    → {"calledUrl":".../pet/findByStatus?status=available",
                                                    "statusCode":200,"count":243,"names":[...]}
POST /rest/async        {"status":"pending"}      → {"via":"RESTAsyncRequest + RESTAsyncResponse",
                                                    "statusCode":200,"count":75,"firstName":"doggie"}
```

## Read this before adapting them

**1. These nodes are spec-driven, not URL-driven.** That is why a hand-authored
REST node fails to build: `definitionFile` and `operationName` are mandatory and
have no defaults. `definitionFile` is a **plain filename relative to the app
root** — `petstore.json` sits beside the msgflows, and `ibmint package` ships it
automatically. `operationName` is an `operationId` from the spec.

**No URL appears anywhere in these flows.** Host, scheme, `basePath` and the HTTP
verb all come from the spec. Use `baseURL` to override the host.

**2. `definitionType` is `swagger_20` or `openapi_3`** (note: not `openapi_30`).
It is a plain string, so a wrong value is not caught by validation. Mandatory
only on `AppConnectRESTRequest`; defaults to `swagger_20` on these.

**3. Parameters are a child element, in the repeated form.** The XSD says nested
`<ParametersTableRow>`; that is not what the runtime reads. Use repeated elements
named after the table, as with the Collector:

```xml
<parameters name="status" type="query" expression="$Body/Data/status"/>
```

`name` must match the spec's parameter, `type` is `query`/`path`/`header`, and
`expression` is an XPath over the message assembly — `$Body/Data/status` reaches
`status` in a JSON request body.

**4. The `name` property is a red herring.** The msgnode marks it
`lowerBound="1"` with no default, which looks mandatory. None of these flows set
it and all four run.

**5. Response metadata is in a different place for sync vs async:**

| | |
|---|---|
| `RESTRequest` | `LocalEnvironment.WrittenDestination.REST` — `URL`, `Method`, `StatusCode`, `TotalRequestTime`, header/body sizes |
| `RESTAsyncResponse` | `LocalEnvironment.REST.Response` — `StatusCode`, `CorrelationID`, sizes |

`WrittenDestination.REST` is **not** available in the async response flow; it
belongs to the flow that made the call.

The body itself arrives as the message (`outputDataLocation` defaults to
`$OutputRoot`), so it is `InputRoot.JSON.Data` in the next node. A JSON array
response comes through as repeated `Item` children — see the `CARDINALITY` +
`(JSON.Array)` loop in `REST_FINDBYSTATUS_MF.esql`.

**6. `error` and `failure` are different terminals.** `error` carries HTTP
error-status responses (body at `errorDataLocation`, default `$OutputRoot`);
`failure` is for node exceptions. Neither is wired in these examples — add `error`
if your API signals business outcomes with 4xx.

## The async pair

`asyncResponseCorrelator` on the request and `asyncRequestCorrelator` on the
response **must match** (`petstoreAsync` here; the Toolkit generates a UUID, any
matching string works). `RESTAsyncResponse` is an **input node** with no `in`
terminal, so the two flows are joined by the correlator rather than a wire and
look disconnected — the same visual oddity as RouteToLabel/Label.

The HTTP reply identifier survives the hop: `REST_ASYNC_MF` receives the request
and ends at the async node, and `REST_ASYNC_RESP_MF` replies to the original
caller. That is the point — the request flow's thread is not held during the
call.

## Not included

`ComIbmAppConnectRESTRequest` is documented in `../../REST.md` but **not proven**
— it targets connectors hosted on IBM App Connect, so it needs an App Connect
instance rather than an arbitrary REST endpoint. Treat that section as
model-derived, not verified.
