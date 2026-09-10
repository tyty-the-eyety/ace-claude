## Amazon S3 node Requirements

> **Status: partially runtime-verified (ACE 13.0.2.2, eu-west-2, 2026-09-06).**
> All six actions below were deployed and driven against a real bucket and the
> results observed in S3. Filter-driven actions (delete, list) additionally
> require a `<filter>` child element on the request node — see *Supplying a
> filter*; without it they fail or silently return one record.
> Everything on this page that is not marked proven is still unverified.

1. Request nodes: `xmi:type="ComIbmApplicationConnectorRequest_amazons3.msgnode"`, `applicationConnectorType="amazons3"`
2. Set `schemaPrefix="gen/<FlowName>.AmazonS3_Request"` — create `<FlowName>.AmazonS3_Request.request.schema.json` and `<FlowName>.AmazonS3_Request.response.schema.json` in the `gen/` subdirectory per the connector general rules in SKILL.md. **These files must contain at least `{}` — an empty file fails at runtime with BIP5753E and the application will not start.**
3. Set `policyUrl="{<PolicyProjectName>}:AmazonS31"` — follow Policy Project structure in PolicyProject.md.
4. Required node attributes (`lowerBound="1"` in the msgnode definition):
   `applicationConnectorType`, `displayName`, `action`, `businessObject`,
   `schemaPrefix`, `policyUrl`, `dataLocation` (default `$Body`),
   `outputDataLocation` (default `$OutputRoot`), `resultDataLocation` (default `$ResultRoot`).
   Terminals are `InTerminal.in`, `OutTerminal.out`, `OutTerminal.failure`.
5. Allowed `displayName`/`action`/`businessObject` combinations:
  -  displayName="Retrieve all buckets" action="RETRIEVEALL" businessObject="bucketcollection"
  -  displayName="Retrieve all objects" action="RETRIEVEALL" businessObject="objectcollection"
  -  displayName="Update or create object" action="UPSERTWITHWHERE" businessObject="object"
  -  displayName="Delete object" action="DELETEALL" businessObject="object"
  -  displayName="Retrieve object metadata" action="RETRIEVEALL" businessObject="object"
  -  displayName="Retrieve object content" action="DOWNLOAD_OBJECT" businessObject="object"
  -  displayName="Copy object" action="COPY_OBJECT" businessObject="object"
  -  displayName="Create object" action="CREATE" businessObject="object"
  -  displayName="Create bucket" action="CREATE" businessObject="bucket"
  -  displayName="Retrieve buckets" action="RETRIEVEALL" businessObject="bucket"
  -  displayName="Delete bucket" action="DELETEALL" businessObject="bucket"
  -  displayName="Retrieve bucket CORS configuration" action="RETRIEVEALL" businessObject="cors"
  -  displayName="Delete bucket CORS configuration" action="DELETEALL" businessObject="cors"
  -  displayName="Update CORS configuration of bucket" action="UPSERT_BUCKET_CORS" businessObject="cors"
  -  displayName="Retrieve bucket ACL" action="RETRIEVEALL" businessObject="bucketacl"
  -  displayName="Update bucket ACL" action="UPDATEALL" businessObject="bucketacl"
  -  displayName="Retrieve object ACL" action="RETRIEVEALL" businessObject="objectacl"
  -  displayName="Update object ACL" action="UPDATEALL" businessObject="objectacl"
  -  displayName="Retrieve object tags" action="RETRIEVEALL" businessObject="objecttags"
  -  displayName="Update object tags" action="UPSERT_OBJECT_TAGS" businessObject="objecttags"
  -  displayName="Delete object tags" action="DELETEALL" businessObject="objecttags"
  -  displayName="Retrieve bucket tags" action="RETRIEVEALL" businessObject="buckettags"
  -  displayName="Update bucket tags" action="UPSERT_BUCKET_TAGS" businessObject="buckettags"
  -  displayName="Delete bucket tags" action="DELETEALL" businessObject="buckettags"
  -  displayName="Retrieve bucket website hosting configuration" action="RETRIEVEALL" businessObject="bucketwebsite"
  -  displayName="Update bucket website hosting configuration" action="UPSERT_BUCKET_WEBSITE" businessObject="bucketwebsite"
  -  displayName="Delete bucket website hosting configuration" action="DELETEALL" businessObject="bucketwebsite"
  -  displayName="Retrieve bucket requester payment configuration" action="RETRIEVEALL" businessObject="bucketrequestpayment"
  -  displayName="Update bucket requester payment configuration" action="UPDATEALL" businessObject="bucketrequestpayment"
  -  displayName="Retrieve bucket default encryption configuration" action="RETRIEVEALL" businessObject="bucketencryption"
  -  displayName="Update bucket default encryption configuration" action="UPSERT_BUCKET_ENCRYPTION" businessObject="bucketencryption"
  -  displayName="Delete bucket default encryption configuration" action="DELETEALL" businessObject="bucketencryption"
  -  displayName="Retrieve bucket lifecycle configuration" action="RETRIEVEALL" businessObject="bucketlifecycleconfiguration"
  -  displayName="Update bucket lifecycle configuration" action="UPSERT_BUCKET_LIFECYCLE_CONFIGURATION" businessObject="bucketlifecycleconfiguration"
  -  displayName="Delete bucket lifecycle configuration" action="DELETEALL" businessObject="bucketlifecycleconfiguration"
  -  displayName="Retrieve bucket policy" action="RETRIEVEALL" businessObject="bucketpolicy"
  -  displayName="Update bucket policy" action="UPSERT_BUCKET_POLICY" businessObject="bucketpolicy"
  -  displayName="Delete bucket policy" action="DELETEALL" businessObject="bucketpolicy"
  -  displayName="Retrieve bucket inventory configuration" action="RETRIEVEALL" businessObject="bucketinventoryconfiguration"
  -  displayName="Update or create bucket inventory configuration" action="UPSERTWITHWHERE" businessObject="bucketinventoryconfiguration"
  -  displayName="Delete bucket inventory configuration" action="DELETEALL" businessObject="bucketinventoryconfiguration"
  -  displayName="Retrieve bucket metrics configuration" action="RETRIEVEALL" businessObject="bucketmetricsconfiguration"
  -  displayName="Update or create bucket metrics configuration" action="UPSERTWITHWHERE" businessObject="bucketmetricsconfiguration"
  -  displayName="Delete bucket metrics configuration" action="DELETEALL" businessObject="bucketmetricsconfiguration"
  -  displayName="Retrieve bucket analytics configuration" action="RETRIEVEALL" businessObject="bucketanalyticsconfiguration"
  -  displayName="Update or create analytics configuration for bucket" action="UPSERTWITHWHERE" businessObject="bucketanalyticsconfiguration"
  -  displayName="Delete bucket analytics configuration" action="DELETEALL" businessObject="bucketanalyticsconfiguration"
  -  displayName="Retrieve bucket replication configuration" action="RETRIEVEALL" businessObject="bucketreplication"
  -  displayName="Update bucket replication configuration" action="UPSERT_BUCKET_REPLICATION" businessObject="bucketreplication"
  -  displayName="Delete bucket replication configuration" action="DELETEALL" businessObject="bucketreplication"
  -  displayName="Retrieve bucket transfer acceleration configuration" action="RETRIEVEALL" businessObject="bucketaccelerateconfiguration"
  -  displayName="Update bucket transfer acceleration configuration" action="UPDATEALL" businessObject="bucketaccelerateconfiguration"
  -  displayName="Retrieve bucket server access logging" action="RETRIEVEALL" businessObject="bucketlogging"
  -  displayName="Update bucket server access logging" action="UPDATEALL" businessObject="bucketlogging"
  -  displayName="Retrieve bucket event configuration" action="RETRIEVEALL" businessObject="bucketnotificationconfiguration"
  -  displayName="Update bucket event configuration" action="UPDATEALL" businessObject="bucketnotificationconfiguration"
  -  displayName="Retrieve object torrent" action="RETRIEVEALL" businessObject="objecttorrent"
  -  displayName="Retrieve object versions" action="RETRIEVEALL" businessObject="objectversioning"
  -  displayName="Update bucket versioning status" action="UPDATEALL" businessObject="bucketversioning"
  -  displayName="Retrieve bucket versioning status" action="RETRIEVEALL" businessObject="bucketversioning"
  -  displayName="Retrieve bucket location" action="RETRIEVEALL" businessObject="bucketlocation"

## Which actions actually work (read this before choosing one)

Connector actions split into two classes. The model JSON tells you which, before
you write any code: `<ACE_INSTALL>/server/nodejs_all/node_modules/@ibm-app-connect/loopback-connector-amazons3/lib/models/<businessObject>.json`,
look at the interaction you want.

| Class | Model signature | Where values go | Hand-authorable? |
|---|---|---|---|
| **Body-driven** | interaction has `requestProperties` | message body, via ESQL, with `dataLocation="$Body"` | **Yes** |
| **Filter-driven** | interaction has only `filterSupport` | a where clause / limit, in a `<filter>` child element | **Yes**, but only via the `<filter>` element — not the body |

Verified against six actions, with no exceptions:

| Action / businessObject | Class | Status |
|---|---|---|
| `CREATE` / `object` | body | **Proven.** Object appeared in the bucket with the right content and ContentType |
| `DOWNLOAD_OBJECT` / `object` | body | **Proven.** Content returned in the response |
| `COPY_OBJECT` / `object` | body | **Proven.** Same-bucket copy with `MetadataDirective: REPLACE` changed ContentType and LastModified |
| `DELETEALL` / `object` | filter | **Works** with a where-clause `<filter>` — see *Supplying a filter*. Deletes exactly the named object |
| `UPSERTWITHWHERE` / `object` | body **and** filter | **Works.** Needs a body (`Key`, `Body`, `ContentType`) *and* a where-clause `<filter>`. Uses `Body`, not `content` |
| `RETRIEVEALL` / `objectcollection` | filter | **Works** once a `<filter>` element is present — see *Supplying a filter*. Without one it returns a single record |

`UPSERTWITHWHERE` has **both** `requestProperties` and `filterSupport`, and
**needs both** — a body *and* a where-clause `<filter>`. Runtime-verified: it
created a new object, then updated the same key in place.

**`DELETEALL` is not a bulk wipe.** It is this connector's action id for
"Delete object", a single-object delete scoped by a `Key` filter. There is no
separate single-delete action. Without a key it errors rather than deleting.

## Request encoding

**Body-driven actions.** Build the request in a Compute node before the request
node. `bucketName` must be in the body even though it is also in the policy:

```sql
-- CREATE: mandatory Key. Note it uses 'content', NOT 'Body'
-- (UPSERTWITHWHERE uses 'Body' for the same concept).
SET OutputRoot.JSON.Data.bucketName  = 'my-bucket';
SET OutputRoot.JSON.Data.Key         = 'file.txt';
SET OutputRoot.JSON.Data.content     = 'hello';
SET OutputRoot.JSON.Data.ContentType = 'text/plain';
```

**`ContentType` is a fixed enum**, not free text. The connector validates it
against a 16-value MIME list and rejects anything else with
`validateCustomActionv0 ... is not one of enum values: ...`.
`text/plain`, `text/rtf` and `binary/octet-stream` are among the allowed values;
check `lib/models/object.json` for the full list.

**Parent connector properties.** Values the connector treats as parent
properties (e.g. `bucketName` for filter-driven actions) are supplied by an
undocumented child element of the request node — an unbounded table, the same
family of encoding as the DatabaseRetrieve grid:

```xml
<nodes xmi:type="ComIbmApplicationConnectorRequest_amazons3.msgnode:FCMComposite_1" ... >
  <translation xmi:type="utility:ConstantString" string="s3_request"/>
  <connectorProperty propertyName="bucketName" displayName="Bucket name"
                     propertyValue="my-bucket" type="string" displayValue="my-bucket"/>
</nodes>
```

Row fields are `propertyName`, `displayName`, `propertyValue`, `type`, `displayValue`.
These arrive at the connector as its `query` object (confirmed in service trace).

**Supplying a filter — `<filter>` is a child ELEMENT, not the `filter=""` attribute.**
This is the encoding, and it is not guessable — `queryProperties` appears nowhere
in the msgnode definition or in any product schema. It is written by the Toolkit
and parsed by the runtime:

```xml
<nodes xmi:type="ComIbmApplicationConnectorRequest_amazons3.msgnode:FCMComposite_1" ... >
  <filter>
    <queryProperties limit="15" allowTruncation="true"/>
  </filter>
  <translation xmi:type="utility:ConstantString" string="s3_request"/>
</nodes>
```

**Without a `<filter>` element, RETRIEVEALL returns exactly ONE record.** That is
not a bug — the connector defaults the page size to 1 when no filter object is
present at all:

```js
// loopback-connector-provider-embedded/lib/utils.js
c = e?.query?.filter
s = c ? (s = c.limit, delete c.limit) : s = 1
```

The connector fetches everything from S3 and then truncates: a trace shows
`Total Objects obtained for bucket are: 4` followed by
`Total number of items being sent to caller 1`. Set `limit` to the page size you
want. Runtime-verified: with `limit="15"` a 4-object bucket returns all 4.

The empty `filter=""` **attribute** does nothing — it is not this encoding.

**The where-clause form of `<filter>`** (needed by `DELETEALL` and any action with
`filterSupport` mandatory fields) is a nested structure, also Toolkit-written:

```xml
<filter>
  <filterElementObject type="where">
    <filterElementArray type="and">
      <connectorPropertyRef propertyName="bucketName" compareAction=""/>
      <filterProperty propertyName="Key" displayName="Object name"
                      propertyValue="[[$Environment/deletePlaceHolder]]" compareAction=""/>
    </filterElementArray>
  </filterElementObject>
</filter>
```

- `<connectorPropertyRef/>` links the where clause to a `<connectorProperty/>`
  row on the same node (here, `bucketName`).
- `<filterProperty/>` carries the actual condition.
- **`propertyValue="[[$Environment/...]]"` is a message-tree reference**, in
  double square brackets. A literal value can be used instead; the reference form
  is what makes the flow dynamic.

With a message reference, ESQL upstream must populate that location:

```sql
-- the request node's filter reads [[$Environment/deletePlaceHolder]]
SET Environment.deletePlaceHolder = InputRoot.JSON.Data.key;
```

Note the message body is **not** used to carry the key for a filter-driven
action — the Environment tree is. Runtime-verified: deletes exactly the named
object and leaves the rest of the bucket untouched.

### Trap: `<requestMap>` templates are LITERALS, not message references

If the Toolkit is used to build the request graphically it sets
`mappingMode="jsonmap"` and adds a `<requestMap map="..."/>` row holding a JSON
mapping. **In that mapping, `"template"` is a literal string.** It is NOT the
`[[$Environment/...]]` message-reference syntax used by `<filterProperty/>`:

```json
{"mappings":[{"Key":{"template":"$Environment/myKey"}}]}
```

That does not read the Environment tree — it creates an object whose key is the
eleven-character string `$Environment/myKey`. Observed: the flow returned
`{"status":"created","s3":{"Key":"$Environment/deletePlaceHolder"}}` and S3
gained an object under a `$Environment/` prefix. HTTP 200, plausible response,
wrong result.

Two different value syntaxes therefore coexist on the same node:

| Element | Value syntax | Meaning |
|---|---|---|
| `<filterProperty propertyValue="...">` | `[[$Environment/x]]` | message-tree reference |
| `<requestMap map="...">` `"template"` | plain text | literal |

The demo flows deliberately build requests in **ESQL** rather than `jsonmap`, so
`mappingMode` and `<requestMap>` are absent. Keep it that way unless you have
verified the mapping syntax at runtime.

### Parent properties go in exactly ONE place

`bucketName` is a *parent* connector property. Where it belongs depends on
whether the node has a `<connectorProperty/>` row for it:

| Node shape | Where `bucketName` goes |
|---|---|
| No `<connectorProperty/>` (e.g. `CREATE`, `DOWNLOAD_OBJECT`) | **in the message body**, set in ESQL |
| Has `<connectorProperty/>` (e.g. `DELETEALL`, `UPSERTWITHWHERE`) | **only in that row** — putting it in the body as well is rejected with `["is not allowed to have the additional property \"bucketName\""]` |

The runtime strips parent properties from the request body (the service trace
shows a `removeParents` step), so a duplicate is a hard error rather than a
harmless repeat.

## IAM permissions

Least privilege for the body-driven actions is `s3:ListBucket` +
`s3:GetBucketLocation` on the bucket ARN, and `s3:GetObject` / `s3:PutObject` /
`s3:DeleteObject` on the `bucket/*` ARN.

**`RETRIEVEALL` / `objectcollection` additionally requires account-wide
`s3:ListAllMyBuckets` on `*`** — it retrieves objects from *every* bucket in the
account and cannot be scoped to one bucket. Without it the connector fails with a
403 naming the missing action.

## Policy

Verified against `<ACE_INSTALL>/common/schemas/Connectors/PolicyConnectors.xsd`
(`ComIbmAmazons3PolicyType`) and `aceDesignerAuthTypeDefinitions.json`.

Name the policy file `AmazonS31.policyxml` (or as chosen):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<policies>
  <policy policyType="amazons3" policyName="AmazonS31" policyTemplate="">
    <credentialName>AmazonS3Credential</credentialName>
    <applicationVersion>v1</applicationVersion>
    <applicationType>online</applicationType>
    <authenticationMethod>BASIC</authenticationMethod>
    <region>eu-west-2</region>
    <bucketName>my-bucket</bucketName>
  </policy>
</policies>
```

- `authenticationMethod` is an enum with **exactly one legal value: `BASIC`**.
  (Earlier revisions of this file said `AWS_BASIC_PKI`, which does not exist.)
- `applicationVersion` must be `v1`; `applicationType` must be `online`.
- `region` and `bucketName` are optional but are used — they show up in
  `LocalEnvironment.WrittenDestination.amazons3` at runtime. `bucketName` here
  does **not** remove the need to supply it per request.
- `policyTemplate=""` works. There is no `online_v1_aws_basic_pki` template
  anywhere in the product.
- There are **no** `roleArn`, `oidcServerUrl`, `profileArn`, `trustAnchorArn` or
  `hostname` fields. The first four are IAM Roles Anywhere properties and are
  irrelevant to access-key authentication.

Policy file structure MUST conform to the ACE Policy XML schema; optionally verify against `<ACE_INSTALL_DIR>/common/schemas/Policy/Policy.xsd`.

## Credentials

The credential type is `amazons3`, taking an access key id and secret:

```bash
mqsicredentials --work-dir <work-dir> --create --vault-key <key> \
  --credential-type amazons3 --credential-name AmazonS3Credential \
  --access-key-id AKIA... --secret-access-key ...
```

`mqsicredentials --help` prints an authoritative table of the credential type and
properties for **every** connector — consult it rather than guessing.

## Debugging a connector failure

Connector errors surface as `BIP4000E` / `BIP9937E` and are raised inside the
connector's Node.js layer, so only a **service trace** explains them. See the
trace runbook in `IntegrationServer.md`.
