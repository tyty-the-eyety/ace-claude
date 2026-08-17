## IBM Cloudant node Requirements

1. Request nodes: `xmi:type="ComIbmApplicationConnectorRequest_cloudantdb.msgnode"`, `applicationConnectorType="cloudantdb"`
2. Set `schemaPrefix="gen/<FlowName>.IBMCloudant_Request"` — create `<FlowName>.IBMCloudant_Request.request.schema.json` and `<FlowName>.IBMCloudant_Request.response.schema.json` in the `gen/` subdirectory per the connector general rules in SKILL.md.
3. Set `policyUrl="{<PolicyProjectName>}:IBMCloudant1"` — follow Policy Project structure in PolicyProject.md.
4. Allowed `displayName`/`action`/`businessObject` combinations:
  -  displayName="Create database" action="CREATE" businessObject="database"
  -  displayName="Retrieve databases" action="RETRIEVEALL" businessObject="database"
  -  displayName="Delete database" action="DELETEALL" businessObject="database"
  -  displayName="Create document" action="CREATE" businessObject="document"
  -  displayName="Retrieve documents" action="RETRIEVEALL" businessObject="document"
  -  displayName="Retrieve document" action="RETRIEVE" businessObject="document"
  -  displayName="Update document" action="UPDATEALL" businessObject="document"
  -  displayName="Update document" action="UPDATE" businessObject="document"
  -  displayName="Update or create document" action="UPSERTWITHWHERE" businessObject="document"
  -  displayName="Delete document" action="DELETEALL" businessObject="document"
  -  displayName="Create attachment" action="CREATE" businessObject="attachment"
  -  displayName="Retrieve attachments" action="RETRIEVEALL" businessObject="attachment"
  -  displayName="Retrieve attachment" action="RETRIEVE" businessObject="attachment"
  -  displayName="Update attachment" action="UPDATEALL" businessObject="attachment"
  -  displayName="Update attachment" action="UPDATE" businessObject="attachment"
  -  displayName="Update or create attachment" action="UPSERTWITHWHERE" businessObject="attachment"
  -  displayName="Delete attachment" action="DELETEALL" businessObject="attachment"
  -  displayName="Retrieve design documents" action="RETRIEVEALL" businessObject="design_document"
  -  displayName="Retrieve views" action="RETRIEVEALL" businessObject="view"
  -  displayName="Apply view" action="APPLYVIEW" businessObject="view"

## Policy XML example

Name the policy file `IBMCloudant1.policyxml` (or as chosen). Example:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<policies>
  <policy longDescription="" policyName="IBMCloudant1" policyTemplate="online_v1_basic" policyType="cloudantdb" shortDescription="" version="">
     <credentialName>IBMCloudantCredential</credentialName>
     <applicationVersion>v1</applicationVersion>
     <applicationType>online</applicationType>
     <authenticationMethod>BASIC</authenticationMethod>
     <hostname/>
  </policy>
</policies>
```
Policy file structure MUST conform to the ACE Policy XML schema; optionally verify against `<ACE_INSTALL_DIR>/common/schemas/Policy/Policy.xsd`.
