# Amazon S3 connector — ApplicationConnector Request node (runtime-proven)

Copy-ready flows for the `ComIbmApplicationConnectorRequest_amazons3` node.
Workspace proof: `demo-apps/S3_CONNECTOR_APP` (six actions driven against a real
bucket). Full reference: `ace-claude/AmazonS3.md`.

Four flows, chosen to cover every request shape the node has:

| File | Action | Request shape |
|---|---|---|
| `S3_CREATE_MF` | `CREATE` | **body only** — no `<filter>`, no `<connectorProperty/>` |
| `S3_LIST_MF` | `RETRIEVEALL` | **filter only** — `<queryProperties limit=…/>` |
| `S3_DELETE_MF` | `DELETEALL` | **filter only** — where clause + `<connectorProperty/>` |
| `S3_UPSERT_MF` | `UPSERTWITHWHERE` | **body AND filter** |

`AmazonS31.policyxml` is the policy; `S3_LIST_MF.AmazonS3_Request.response.schema.json`
is a Toolkit-generated response schema (the others may be `{}`, but must not be empty).

## Read this before adapting them

**1. Decide the class first.** Open the connector's model JSON —
`<ACE>/server/nodejs_all/node_modules/@ibm-app-connect/loopback-connector-amazons3/lib/models/<object>.json` —
and look at your interaction:

- has `requestProperties` → **body-driven**: set values in ESQL, `dataLocation="$Body"`
- has only `filterSupport` → **filter-driven**: the body is ignored entirely
- has both → needs both

Getting this wrong is silent. A filter-driven action built as body-driven
packages, validates, deploys and starts cleanly, then fails at runtime or
returns a single record.

**2. `<filter>` is a child element, not the `filter=""` attribute.** The
attribute exists and is inert. Neither form below appears in any msgnode
definition or product schema — both are written by the ACE Toolkit and parsed by
the runtime, so they cannot be derived from the product's own metadata.

Page size (`S3_LIST_MF`):

```xml
<filter>
  <queryProperties limit="15" allowTruncation="true"/>
</filter>
```

**Omit this and RETRIEVEALL returns exactly one object**, however many exist —
the connector defaults its page size to 1 when no filter object is present.

Where clause (`S3_DELETE_MF`, `S3_UPSERT_MF`):

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

**3. `[[$Environment/x]]` is a message-tree reference.** Populate it upstream:

```sql
SET Environment.deletePlaceHolder = InputRoot.JSON.Data.key;
```

A literal value works too. But note that `"template"` inside a `<requestMap>`
(the Toolkit's graphical mapping, `mappingMode="jsonmap"`) is a **literal** — a
path there is sent as text and creates an object literally named
`$Environment/x`. These examples build requests in ESQL and avoid `jsonmap`.

**4. Parent properties go in exactly one place.** If the node carries
`<connectorProperty propertyName="bucketName" .../>`, do **not** also set
`bucketName` in the body — the runtime strips parents from the body and rejects
the duplicate. If the node has no such row (`S3_CREATE_MF`), `bucketName`
belongs in the body.

**5. Per-interaction property names differ.** `CREATE` takes `content`;
`UPSERTWITHWHERE` takes `Body` — same concept, different name. `ContentType` is
a fixed 16-value MIME enum (`text/plain`, `text/rtf`, `binary/octet-stream`
among them); anything else fails validation.

**6. `gen/*.schema.json` must contain at least `{}`.** Empty files pass both
`ibmint package` and `mqsicreatebar -cleanBuild`, then fail at runtime with
`BIP5753E` and the application never starts.

## Setup

Credential (type and properties from `mqsicredentials --help`, which is
authoritative for every connector):

```bash
mqsicredentials --work-dir <work-dir> --create --vault-key <key> \
  --credential-type amazons3 --credential-name AmazonS3Credential \
  --access-key-id AKIA... --secret-access-key ...
```

Set the bucket in `AmazonS31.policyxml` and in the ESQL, and the region in the
policy. IAM: `s3:ListBucket` + `s3:GetBucketLocation` on the bucket ARN and
`s3:GetObject`/`PutObject`/`DeleteObject` on `bucket/*`. `RETRIEVEALL` over
`objectcollection` additionally needs account-wide `s3:ListAllMyBuckets` — it
spans every bucket in the account and cannot be scoped.

## Debugging

Connector errors (`BIP4000E`, `BIP9937E`) are raised inside the connector's
Node.js layer; only a service trace explains them, and the useful lines are
tagged `<JS>`. Trace one app in its own work directory — see the tracing section
of `ace-claude/IntegrationServer.md`.
