# S3_CONNECTOR_APP — Amazon S3 connector

Six HTTP-fronted flows exercising the `ComIbmApplicationConnectorRequest_amazons3`
node. **All six are runtime-verified** against a real bucket.
Read `ace-claude/AmazonS3.md` before changing anything here.

| Flow | Endpoint | Action | Status |
|---|---|---|---|
| `S3_CREATE_MF` | `POST /s3/create` | `CREATE` / `object` | **Works.** Runtime-verified |
| `S3_GET_MF` | `POST /s3/get` | `DOWNLOAD_OBJECT` / `object` | **Works.** Runtime-verified |
| `S3_COPY_MF` | `POST /s3/copy` | `COPY_OBJECT` / `object` | **Works.** Runtime-verified |
| `S3_DELETE_MF` | `POST /s3/delete` | `DELETEALL` / `object` | **Works.** Runtime-verified — needs a where-clause `<filter>` (see below) |
| `S3_UPSERT_MF` | `POST /s3/upsert` | `UPSERTWITHWHERE` / `object` | **Works.** Needs a body **and** a where-clause `<filter>` |
| `S3_LIST_MF` | `POST /s3/list` | `RETRIEVEALL` / `objectcollection` | **Works.** Runtime-verified — needs the `<filter>` element (see below) |

## The `<filter>` element — why `S3_LIST_MF` works and how

```xml
<filter>
  <queryProperties limit="15" allowTruncation="true"/>
</filter>
```

A child element of the request node. **Remove it and the flow returns exactly one
object**, however many are in the bucket: the connector defaults its page size to
1 when no filter object is present. This is the single least guessable thing in
the app — `queryProperties` is in no schema; it was recovered by setting a Filter
in the ACE Toolkit UI and reading what it wrote.

## The where-clause filter — how `S3_DELETE_MF` works

`DELETEALL` does not take the object key from the message body. It reads a where
clause from the node's `<filter>` element:

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

`[[$Environment/deletePlaceHolder]]` is a message-tree reference, so the ESQL
upstream sets the value:

```sql
SET Environment.deletePlaceHolder = InputRoot.JSON.Data.key;
```

Despite the name, `DELETEALL` is a **single-object** delete scoped by that Key —
verified: it removed only the named object and left the rest of the bucket alone.

## Setup

1. **Bucket.** Create one and put its name everywhere this app says `my-bucket`
   (the ESQL files and `S3_CONNECTOR_POLICIES/AmazonS31.policyxml`). Set the
   region in the policy too.

2. **IAM.** Least privilege for the three working flows:

   ```json
   { "Effect": "Allow",
     "Action": ["s3:ListBucket","s3:GetBucketLocation"],
     "Resource": "arn:aws:s3:::my-bucket" },
   { "Effect": "Allow",
     "Action": ["s3:GetObject","s3:PutObject","s3:DeleteObject"],
     "Resource": "arn:aws:s3:::my-bucket/*" }
   ```

   `S3_LIST_MF` additionally needs account-wide `s3:ListAllMyBuckets` on `*` —
   `objectcollection` spans every bucket in the account and cannot be scoped.

3. **Credential** in the server vault:

   ```bash
   mqsicredentials --work-dir <work-dir> --create --vault-key <key> \
     --credential-type amazons3 --credential-name AmazonS3Credential \
     --access-key-id AKIA... --secret-access-key ...
   ```

4. **Build and deploy:**

   ```bash
   ibmint package --input-path <workspace> \
     --output-bar-file bars/S3_CONNECTOR_APP.bar \
     --project S3_CONNECTOR_APP --project S3_CONNECTOR_POLICIES
   ibmint deploy --input-bar-file bars/S3_CONNECTOR_APP.bar \
     --output-work-directory <work-dir>
   IntegrationServer --work-dir <work-dir> --vault-key <key>
   ```

## Driving it

```bash
curl -X POST http://localhost:7800/s3/create -H 'Content-Type: application/json' \
  -d '{"key":"hello.txt","content":"written by an ACE flow"}'

curl -X POST http://localhost:7800/s3/get -H 'Content-Type: application/json' \
  -d '{"key":"hello.txt"}'

curl -X POST http://localhost:7800/s3/copy -H 'Content-Type: application/json' \
  -d '{"key":"hello.txt","sourceBucket":"my-bucket"}'

curl -X POST http://localhost:7800/s3/list -H 'Content-Type: application/json' -d '{}'

curl -X POST http://localhost:7800/s3/delete -H 'Content-Type: application/json' \
  -d '{"key":"hello.txt"}'
```

Verify with the AWS CLI rather than trusting the flow response:

```bash
aws s3 ls s3://my-bucket/
aws s3api head-object --bucket my-bucket --key hello.txt
```

## Two traps worth knowing

- `ContentType` is validated against a **fixed 16-value MIME enum**. An arbitrary
  type fails with `is not one of enum values: ...`. `text/plain`, `text/rtf` and
  `binary/octet-stream` are among the allowed ones.
- The `gen/*.schema.json` files **must contain `{}`**. Empty files package and
  validate cleanly, then fail at runtime with `BIP5753E` and the app never starts.
