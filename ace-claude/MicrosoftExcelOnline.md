## Microsoft Excel Online node Requirements

1. Request nodes: `xmi:type="ComIbmApplicationConnectorRequest_msexcel.msgnode"`, `applicationConnectorType="msexcel"`
2. Input nodes: `xmi:type="ComIbmApplicationConnectorInput_msexcel.msgnode"`, `applicationConnectorType="msexcel"`
3. Set `schemaPrefix="gen/<FlowName>.MicrosoftExcelOnline_Request"` — create `<FlowName>.MicrosoftExcelOnline_Request.request.schema.json` and `<FlowName>.MicrosoftExcelOnline_Request.response.schema.json` in the `gen/` subdirectory per the connector general rules in SKILL.md.
4. Set `policyUrl="{<PolicyProjectName>}:MicrosoftExcelOnline1"` — follow Policy Project structure in PolicyProject.md.
5. Allowed `displayName`/`action`/`businessObject` combinations:
  -  displayName="Insert row" action="CREATE" businessObject="Row"
  -  displayName="Retrieve rows" action="RETRIEVEALL" businessObject="Row"
  -  displayName="Delete row" action="DELETEALL" businessObject="Row"
  -  displayName="Update row" action="UPDATEALL" businessObject="Row"
  -  displayName="Append row" action="APPENDROW" businessObject="Row"
  -  displayName="New row appended" action="CREATED" businessObject="Row"
  -  displayName="Retrieve drives" action="RETRIEVEALL" businessObject="Drive"
  -  displayName="Create workbook" action="CREATE" businessObject="Workbook"
  -  displayName="Retrieve workbooks" action="RETRIEVEALL" businessObject="Workbook"
  -  displayName="Delete workbook" action="DELETEALL" businessObject="Workbook"
  -  displayName="Rename workbook" action="UPDATEALL" businessObject="Workbook"
  -  displayName="Download workbook" action="DOWNLOADWORKBOOK" businessObject="Workbook"
  -  displayName="Upload workbook" action="UPLOADWORKBOOK" businessObject="Workbook"
  -  displayName="Create worksheet" action="CREATE" businessObject="Worksheet"
  -  displayName="Retrieve worksheets" action="RETRIEVEALL" businessObject="Worksheet"
  -  displayName="Delete worksheet" action="DELETEALL" businessObject="Worksheet"
  -  displayName="Rename worksheet" action="UPDATEALL" businessObject="Worksheet"
  -  displayName="Create table" action="CREATE" businessObject="Table"
  -  displayName="Retrieve tables" action="RETRIEVEALL" businessObject="Table"
  -  displayName="Delete table" action="DELETEALL" businessObject="Table"
  -  displayName="Update table" action="UPDATEALL" businessObject="Table"
  -  displayName="Retrieve column data" action="RETRIEVEALL" businessObject="Column"
  -  displayName="Append table row" action="CREATE" businessObject="Tablerow"
  -  displayName="Retrieve table rows" action="RETRIEVEALL" businessObject="Tablerow"
  -  displayName="Delete table row" action="DELETEALL" businessObject="Tablerow"
  -  displayName="Update table row" action="UPDATEALL" businessObject="Tablerow"
  -  displayName="New table row appended" action="CREATED" businessObject="Tablerow"
  -  displayName="Retrieve cell ranges" action="RETRIEVEALL" businessObject="Range"

## Policy XML example

Name the policy file `MicrosoftExcelOnline1.policyxml` (or as chosen). Example:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<policies>
  <policy longDescription="" policyName="MicrosoftExcelOnline1" policyTemplate="online_v1_basic_oauth" policyType="msexcel" shortDescription="" version="">
     <credentialName>MicrosoftExcelOnlineCredential</credentialName>
     <applicationVersion>v1</applicationVersion>
     <applicationType>online</applicationType>
     <authenticationMethod>BASIC_OAUTH</authenticationMethod>
  </policy>
</policies>
```
Policy file structure MUST conform to the ACE Policy XML schema; optionally verify against `<ACE_INSTALL_DIR>/common/schemas/Policy/Policy.xsd`.
