## Google Cloud Storage node Requirements

1. Request nodes: `xmi:type="ComIbmApplicationConnectorRequest_googlecloudstorage.msgnode"`, `applicationConnectorType="googlecloudstorage"`
2. Set `schemaPrefix="gen/<FlowName>.GoogleCloudStorage_Request"` — create `<FlowName>.GoogleCloudStorage_Request.request.schema.json` and `<FlowName>.GoogleCloudStorage_Request.response.schema.json` in the `gen/` subdirectory per the connector general rules in SKILL.md.
3. Set `policyUrl="{<PolicyProjectName>}:GoogleCloudStorage1"` — follow Policy Project structure in PolicyProject.md.
4. Allowed `displayName`/`action`/`businessObject` combinations:
  -  displayName="Create bucket" action="CREATE" businessObject="Bucket"
  -  displayName="Retrieve buckets" action="RETRIEVEALL" businessObject="Bucket"
  -  displayName="Delete bucket" action="DELETEALL" businessObject="Bucket"
  -  displayName="Set bucket default storage class" action="SETBUCKETDEFAULTSTORAGECLASS" businessObject="Bucket"
  -  displayName="Get bucket default storage class" action="GETBUCKETDEFAULTSTORAGECLASS" businessObject="Bucket"
  -  displayName="Retrieve bucket ACLs" action="RETRIEVEALL" businessObject="BucketACL"
  -  displayName="Update bucket ACL" action="UPDATEBUCKETACL" businessObject="BucketACL"
  -  displayName="Get bucket tags" action="GETBUCKETTAGS" businessObject="BucketTags"
  -  displayName="Set bucket tags" action="SETBUCKETTAGS" businessObject="BucketTags"
  -  displayName="Retrieve bucket lifecycle configurations" action="RETRIEVEALL" businessObject="BucketLifecycleConfiguration"
  -  displayName="Update bucket lifecycle configuration" action="UPDATEBUCKETLIFECYCLECONFIG" businessObject="BucketLifecycleConfiguration"
  -  displayName="Get bucket versioning" action="GETBUCKETVERSIONING" businessObject="BucketVersioning"
  -  displayName="Set bucket versioning" action="SETBUCKETVERSIONING" businessObject="BucketVersioning"
  -  displayName="Retrieve bucket CORS configurations" action="RETRIEVEALL" businessObject="BucketCORSConfiguration"
  -  displayName="Update bucket CORS configuration" action="UPDATEBUCKETCORSCONFIG" businessObject="BucketCORSConfiguration"
  -  displayName="Retrieve bucket logging configurations" action="RETRIEVEALL" businessObject="BucketLoggingConfiguration"
  -  displayName="Update bucket logging configuration" action="UPDATEBUCKETLOGGINGCONFIGURATION" businessObject="BucketLoggingConfiguration"
  -  displayName="Retrieve bucket website" action="RETRIEVEALL" businessObject="BucketWebsite"
  -  displayName="Update bucket website" action="UPDATEBUCKETWEBSITE" businessObject="BucketWebsite"
  -  displayName="Retrieve bucket location" action="RETRIEVEALL" businessObject="BucketLocation"
  -  displayName="Retrieve objects" action="RETRIEVEALL" businessObject="Object"
  -  displayName="Update or create object" action="UPSERTWITHWHERE" businessObject="Object"
  -  displayName="Delete object" action="DELETEALL" businessObject="Object"
  -  displayName="Copy object" action="COPYOBJECT" businessObject="Object"
  -  displayName="Download object content" action="DOWNLOADOBJECTCONTENT" businessObject="Object"
  -  displayName="Retrieve object ACL" action="RETRIEVEALL" businessObject="ObjectACL"
  -  displayName="Update object ACL" action="UPDATEOBJECTACL" businessObject="ObjectACL"
  -  displayName="Retrieve object versioning" action="RETRIEVEALL" businessObject="ObjectVersioning"

## Policy XML example

Name the policy file `GoogleCloudStorage1.policyxml` (or as chosen). Example:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<policies>
  <policy longDescription="" policyName="GoogleCloudStorage1" policyTemplate="online_v1_basic" policyType="googlecloudstorage" shortDescription="" version="">
     <credentialName>GoogleCloudStorageCredential</credentialName>
     <applicationVersion>v1</applicationVersion>
     <applicationType>online</applicationType>
     <authenticationMethod>BASIC</authenticationMethod>
     <bucketName/>
     <proxyId/>
  </policy>
</policies>
```
Policy file structure MUST conform to the ACE Policy XML schema; optionally verify against `<ACE_INSTALL_DIR>/common/schemas/Policy/Policy.xsd`.
