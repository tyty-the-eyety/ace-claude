## Amazon S3 node Requirements

1. Request nodes: `xmi:type="ComIbmApplicationConnectorRequest_amazons3.msgnode"`, `applicationConnectorType="amazons3"`
2. Set `schemaPrefix="gen/<FlowName>.AmazonS3_Request"` — create `<FlowName>.AmazonS3_Request.request.schema.json` and `<FlowName>.AmazonS3_Request.response.schema.json` in the `gen/` subdirectory per the connector general rules in SKILL.md.
3. Set `policyUrl="{<PolicyProjectName>}:AmazonS31"` — follow Policy Project structure in PolicyProject.md.
4. Allowed `displayName`/`action`/`businessObject` combinations:
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

## Policy XML example

Name the policy file `AmazonS31.policyxml` (or as chosen). Example:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<policies>
  <policy longDescription="" policyName="AmazonS31" policyTemplate="online_v1_aws_basic_pki" policyType="amazons3" shortDescription="" version="">
     <credentialName>AmazonS3Credential</credentialName>
     <applicationVersion>v1</applicationVersion>
     <applicationType>online</applicationType>
     <authenticationMethod>AWS_BASIC_PKI</authenticationMethod>
     <roleArn/>
     <region/>
     <hostname/>
     <bucketName/>
     <oidcServerUrl/>
     <profileArn/>
     <trustAnchorArn/>
     <proxyId/>
  </policy>
</policies>
```
Policy file structure MUST conform to the ACE Policy XML schema; optionally verify against `<ACE_INSTALL_DIR>/common/schemas/Policy/Policy.xsd`.
