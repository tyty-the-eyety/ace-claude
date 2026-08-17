## Microsoft Azure Blob Storage node Requirements

1. Request nodes: `xmi:type="ComIbmApplicationConnectorRequest_azureblobstorage.msgnode"`, `applicationConnectorType="azureblobstorage"`
2. Set `schemaPrefix="gen/<FlowName>.MicrosoftAzureBlobstorage_Request"` — create `<FlowName>.MicrosoftAzureBlobstorage_Request.request.schema.json` and `<FlowName>.MicrosoftAzureBlobstorage_Request.response.schema.json` in the `gen/` subdirectory per the connector general rules in SKILL.md.
3. Set `policyUrl="{<PolicyProjectName>}:MicrosoftAzureBlobstorage1"` — follow Policy Project structure in PolicyProject.md.
4. Allowed `displayName`/`action`/`businessObject` combinations:
  -  displayName="Create container" action="CREATE" businessObject="container"
  -  displayName="Retrieve containers" action="RETRIEVEALL" businessObject="container"
  -  displayName="Delete container" action="DELETEALL" businessObject="container"
  -  displayName="Set container metadata" action="SETCONTAINERMETADATA" businessObject="container"
  -  displayName="Check container exists" action="EXISTS" businessObject="container"
  -  displayName="Set container ACL" action="SETCONTAINERACL" businessObject="container"
  -  displayName="Get container ACL" action="GETCONTAINERACL" businessObject="container"
  -  displayName="Retrieve blobs" action="RETRIEVEALL" businessObject="blob"
  -  displayName="Update or create blob" action="UPSERTWITHWHERE" businessObject="blob"
  -  displayName="Delete blob" action="DELETEALL" businessObject="blob"
  -  displayName="Copy blob" action="COPYBLOB" businessObject="blob"
  -  displayName="Set blob metadata" action="SETBLOBMETADATA" businessObject="blob"
  -  displayName="Check blob exists" action="BLOBEXISTS" businessObject="blob"
  -  displayName="Download blob content" action="DOWNLOADBLOBCONTENT" businessObject="blob"
  -  displayName="Abort copy blob" action="ABORTCOPYBLOB" businessObject="blob"
  -  displayName="Create blob snapshot" action="CREATE" businessObject="snapshot"
  -  displayName="Retrieve blob snapshots" action="RETRIEVEALL" businessObject="snapshot"
  -  displayName="Delete blob snapshot" action="DELETEALL" businessObject="snapshot"
  -  displayName="Retrieve blob versions" action="RETRIEVEALL" businessObject="version"
  -  displayName="Delete blob version" action="DELETEALL" businessObject="version"
  -  displayName="Update or create page blob" action="UPSERTWITHWHERE" businessObject="pageBlob"
  -  displayName="Add page" action="APPENDPAGES" businessObject="pageBlob"
  -  displayName="Update or create append blob" action="UPSERTWITHWHERE" businessObject="appendBlob"
  -  displayName="Append block" action="APPENDBLOCK" businessObject="appendBlob"
  -  displayName="Set blob service properties" action="SETBLOBSERVICEPROPERTIES" businessObject="blobService"
  -  displayName="Get blob service properties" action="GETBLOBSERVICEPROPERTIES" businessObject="blobService"

## Policy XML example

Name the policy file `MicrosoftAzureBlobstorage1.policyxml` (or as chosen). Example:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<policies>
  <policy longDescription="" policyName="MicrosoftAzureBlobstorage1" policyTemplate="online_v1_basic" policyType="azureblobstorage" shortDescription="" version="">
     <credentialName>MicrosoftAzureBlobstorageCredential</credentialName>
     <applicationVersion>v1</applicationVersion>
     <applicationType>online</applicationType>
     <authenticationMethod>BASIC</authenticationMethod>
     <accountName>YourStorageAccount</accountName>
     <tenantId/>
     <proxyId/>
  </policy>
</policies>
```
Policy file structure MUST conform to the ACE Policy XML schema; optionally verify against `<ACE_INSTALL_DIR>/common/schemas/Policy/Policy.xsd`.
