## REST request nodes

> **Status: three of four runtime-proven (ACE 13.0.2.2, 2026-09-14)** against the
> public Swagger Petstore (`https://petstore.swagger.io/v2/swagger.json`).
> `RESTRequest` (with and without parameters) and the
> `RESTAsyncRequest`/`RESTAsyncResponse` pair were deployed and driven, and the
> responses observed. Copy-ready flows in `examples/rest/`, proof app
> `REST_DEMO_APP`. **`AppConnectRESTRequest` is NOT verified** — it needs an IBM
> App Connect instance; its section below is read from the node model and IBM's
> own tooltip.

Four nodes, all at the **root** of
`tools/plugins/com.ibm.etools.mft.ibmnodes.definitions_13.0.2.2.jar`, so every
`xmlns` URI is the bare filename:

| Node | `xmi:type` | Introduced | `requiresJava` | Terminals |
|---|---|---|---|---|
| REST Request | `ComIbmRESTRequest.msgnode` | 10.0.0.5 | yes | `in` → `out`, `error`, `failure` |
| REST Async Request | `ComIbmRESTAsyncRequest.msgnode` | 10.0.0.6 | yes | `in` → `out`, `failure` |
| REST Async Response | `ComIbmRESTAsyncResponse.msgnode` | 10.0.0.6 | no | **no `in`** → `out`, `error`, `failure`, `catch` |
| App Connect REST Request | `ComIbmAppConnectRESTRequest.msgnode` | 10.0.0.11 | yes | `in` → `out`, `error`, `failure` |

All four need the `REST` runtime component (`requiresComponents="REST"`), none
needs MQ.

IBM's tooltips draw the line between the two request nodes:

```
ComIbmRESTRequest           → "interact with REST APIs hosted on external HTTP servers"
ComIbmAppConnectRESTRequest → "interact with connectors hosted on App Connect using REST"
```

---

## These are spec-driven, not URL-driven

This is the whole point of the family, and the reason a hand-authored node fails
to build. Unlike `ComIbmWSRequest`, which takes a URL, a REST node takes an
**OpenAPI/Swagger document plus an operation id** and derives everything else.

Three mandatory properties (`lowerBound="1"`, no default):

| Property | Meaning |
|---|---|
| `definitionFile` | the spec, **as a plain filename relative to the application root** |
| `operationName` | an `operationId` from that spec |
| `definitionType` | `swagger_20` or `openapi_3` — mandatory only on `AppConnectRESTRequest`, defaults to `swagger_20` elsewhere |

```xml
<nodes xmi:type="ComIbmRESTRequest.msgnode:FCMComposite_1" xmi:id="FCMComposite_1_2"
       location="340,160" definitionType="swagger_20"
       definitionFile="petstore.json" operationName="getInventory">
  <translation xmi:type="utility:ConstantString" string="get_inventory"/>
</nodes>
```

Put the spec beside the msgflow at the app root; `ibmint package` ships it
automatically (`BIP1859I: Successfully added file 'petstore.json'`).

**No URL appears anywhere in the flow.** With only the three properties above,
the node called `https://petstore.swagger.io/v2/store/inventory` — scheme, host,
`basePath` and the HTTP verb all came from the spec's `schemes`, `host`,
`basePath` and the operation's method. `baseURL` overrides the host if you need
to point a production spec at a test server.

### `definitionType` values

A plain `xsd:string`, so a wrong value is not caught by schema validation. The
two provider ids come from `com.ibm.etools.mft.restapi.ui`:

```
swagger_20  → com/ibm/broker/rest/swagger_20/ApiProviderImpl
openapi_3   → com/ibm/broker/rest/openapi_3/ApiProviderImpl
```

Note **`openapi_3`**, not `openapi_30`.

### `name` is a red herring

The msgnode marks a `name` property `lowerBound="1"` with no default, which reads
as mandatory. It is not — all four proven flows package, deploy and run without
it. Don't chase it.

---

## Parameters

`iib:parameters` is a child **element** of the node, not an attribute. Rows
serialize as **repeated elements named after the table** — the XSD's nested
`<ParametersTableRow>` form is not what the runtime reads, exactly as with the
Collector's `eventHandlerPropertyTable`:

```xml
<nodes xmi:type="ComIbmRESTRequest.msgnode:FCMComposite_1" ... operationName="findPetsByStatus">
  <parameters name="status" type="query" expression="$Body/Data/status"/>
  <translation xmi:type="utility:ConstantString" string="find_by_status"/>
</nodes>
```

| Attribute | Notes |
|---|---|
| `name` | **required** — must match a parameter name in the spec |
| `type` | **required** — `query`, `path` or `header` |
| `expression` | `iib:valueType="xpath"` — an XPath over the message assembly |
| `description` | optional, transient (Toolkit display only) |

`$Body/Data/status` reaches a field in a JSON request body: for the JSON domain
`$Body` is the `JSON` parser element, so the body's `status` field is
`$Body/Data/status`. Proven — the node built
`…/pet/findByStatus?status=available` from a `{"status":"available"}` request.

---

## Response metadata differs between sync and async

Sync (`RESTRequest`) populates `LocalEnvironment.WrittenDestination.REST`:

```sql
SET OutputRoot.JSON.Data.calledUrl  = InputLocalEnvironment.WrittenDestination.REST.URL;
SET OutputRoot.JSON.Data.method     = InputLocalEnvironment.WrittenDestination.REST.Method;
SET OutputRoot.JSON.Data.statusCode = InputLocalEnvironment.WrittenDestination.REST.StatusCode;
SET OutputRoot.JSON.Data.elapsedMs  = InputLocalEnvironment.WrittenDestination.REST.TotalRequestTime;
```

Also available there: `RequestHeadersSize`, `RequestBodySize`,
`ResponseHeadersSize`, `ResponseBodySize`, `CorrelationID`, and `Compression` /
`Decompression` subtrees.

Async (`RESTAsyncResponse`) instead populates `LocalEnvironment.REST.Response`
with `StatusCode`, `CorrelationID`, `ResponseHeadersSize`, `ResponseBodySize`,
`Decompression`. **`WrittenDestination.REST` is not available in the response
flow** — it belongs to the flow that made the call.

The response body itself arrives as the message: `outputDataLocation` defaults to
`$OutputRoot`, so the parsed body is `InputRoot.JSON.Data` in the next node. A
JSON array response comes through as repeated `Item` children, so it needs the
`CARDINALITY` + `(JSON.Array)` treatment already documented for connectors.

### `error` vs `failure`

The request nodes have **both**. `error` carries HTTP error-status responses
(where `errorDataLocation`, default `$OutputRoot`, holds the body); `failure` is
for node exceptions. Wire `error` if the API signals business outcomes with 4xx.

---

## The async pair

Two flows joined by a correlator, not by a wire:

```
flow A:  HTTPInput → RESTAsyncRequest   (asyncResponseCorrelator="petstoreAsync")
flow B:  RESTAsyncResponse (asyncRequestCorrelator="petstoreAsync") → Compute → HTTPReply
```

- `asyncResponseCorrelator` on the request and `asyncRequestCorrelator` on the
  response **must be identical**. The Toolkit generates a UUID; any matching
  string works.
- `RESTAsyncResponse` is an **input node** (`nodeType="response,asynchronous,input"`)
  with no `in` terminal — it starts its own flow. A correct async pair therefore
  looks like two disconnected flows, the same visual oddity as RouteToLabel/Label.
- **The HTTP reply identifier survives the hop.** Flow A receives the request and
  ends at the async node; flow B replies to the original caller. Proven:
  `POST /rest/async {"status":"pending"}` → `{"count":75,"firstName":"doggie"}`.
  That is the value of the node — flow A's thread is not held during the call.
- `RESTAsyncRequest` has no `error` terminal (the response node owns that) and
  adds `AddRequestToGroup` / `GroupRequestTimeout` for batching several requests
  before waiting.

---

## Other `RESTRequest` properties

Defaults worth knowing: `timeoutForServer=120`, `protocol=TLS`, `accept=*/*`,
`contentType=$iib:determineFromInputMessage`, `dataLocation=$Body`,
`resultDataLocation=$ResultRoot`, `outputDataLocation=$OutputRoot`,
`errorDataLocation=$OutputRoot`, `followRedirection=false`,
`enableKeepAlive=true`, `requestCompressionType=none`,
`acceptCompressedResponses=false`, `hostnameChecking=true`.

- `protocol`: `TLS`, `TLSv1`, `TLSv1.1`, `TLSv1.2`, `TLSv1.3`, `SSL_TLS`, `SSL_TLSv2`
- `requestCompressionType`: `none`, `gzip`, `zlib-deflate`, `raw-deflate`
- `securityIdentity` uses a `rest` vault credential; the in-field help says to
  give the name **without** the `rest::` prefix. Three auth types:
  ```
  rest: --auth-type basic        --username <arg> --password <arg>
  rest: --auth-type basicApiKey  --username <arg> --password <arg> --api-key <arg>
  rest: --auth-type apiKey       --api-key <arg>
  ```
- `httpProxyLocation` for an outbound proxy (see `LEARNINGS.md` on the HTTPProxy
  policy — at 13.0.2.2 only the literal node property works).

---

## App Connect REST Request (NOT runtime-verified)

For calling **connectors hosted on IBM App Connect** rather than an arbitrary
REST server — i.e. letting App Connect run the connector and hold its
credentials, instead of running an `ApplicationConnector*` node locally.

Same three mandatory properties and the same `<parameters>` element, but stripped
right down: **14 properties against ~40**. It has `securityIdentity`, `baseURL`,
`timeoutForServer`, `contentType`, `dataLocation` and `parameters`, and **lacks**
every SSL/`protocol` property, `followRedirection`, `enableKeepAlive`,
`requestCompressionType`, `accept`, `httpProxyLocation`,
`resultDataLocation`/`outputDataLocation`/`errorDataLocation`, and all response
parsing and validation properties. No response-parsing config is the tell: the
endpoint is always App Connect over HTTPS with a known response shape.

Differences from the other nodes:

- `definitionType` is **mandatory** here (optional elsewhere), and its label is
  "Definitions language" rather than "Specification version".
- `securityIdentity` is the expected way to authenticate — a `rest` credential,
  most likely `--auth-type apiKey`.

Untested route, for when an App Connect instance is available: export the
connector's OpenAPI document from App Connect, drop it in the app as
`definitionFile`, store the API key as a `rest` credential, and set
`securityIdentity` to that name without the `rest::` prefix.

---

## BIP codes

| Code | Meaning |
|---|---|
| `BIP9958E` | a REST node's schema/definition file is not deployed — check `definitionFile` is at the app root and in the BAR |
| `BIP3120E` / `BIP2230E` | exception on the input node / at the REST node — read the nested message |
