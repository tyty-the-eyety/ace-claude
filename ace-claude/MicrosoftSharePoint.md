## Microsoft SharePoint node Requirements

1. Request nodes: `xmi:type="ComIbmApplicationConnectorRequest_mssharepoint.msgnode"`, `applicationConnectorType="mssharepoint"`
2. Input nodes: `xmi:type="ComIbmApplicationConnectorInput_mssharepoint.msgnode"`, `applicationConnectorType="mssharepoint"`
3. Set `schemaPrefix="gen/<FlowName>.MicrosoftSharePoint_Request"` — create `<FlowName>.MicrosoftSharePoint_Request.request.schema.json` and `<FlowName>.MicrosoftSharePoint_Request.response.schema.json` in the `gen/` subdirectory per the connector general rules in SKILL.md.
4. Set `policyUrl="{<PolicyProjectName>}:MicrosoftSharePoint1"` — follow Policy Project structure in PolicyProject.md.
5. Allowed `displayName`/`action`/`businessObject` combinations:
  -  displayName="Retrieve all folders" action="RETRIEVEALL" businessObject="FolderCollection"
  -  displayName="Retrieve all files" action="RETRIEVEALL" businessObject="FileCollection"
  -  displayName="Retrieve all sites" action="RETRIEVEALL" businessObject="SiteCollection"
  -  displayName="Retrieve all lists" action="RETRIEVEALL" businessObject="ListCollection"
  -  displayName="Retrieve all list items" action="RETRIEVEALL" businessObject="ListItemCollection"
  -  displayName="Retrieve all list item attachments" action="RETRIEVEALL" businessObject="ListItemAttachmentCollection"
  -  displayName="Create file" action="CREATE" businessObject="File"
  -  displayName="Retrieve files" action="RETRIEVEALL" businessObject="File"
  -  displayName="Delete file" action="DELETEALL" businessObject="File"
  -  displayName="Update file" action="UPDATEALL" businessObject="File"
  -  displayName="Download file" action="DOWNLOADFILE" businessObject="File"
  -  displayName="Rename file" action="RENAMEFILE" businessObject="File"
  -  displayName="Share file" action="SHAREFILE" businessObject="File"
  -  displayName="Create folder" action="CREATE" businessObject="Folder"
  -  displayName="Retrieve folders" action="RETRIEVEALL" businessObject="Folder"
  -  displayName="Delete folder" action="DELETEALL" businessObject="Folder"
  -  displayName="Update folder" action="UPDATEALL" businessObject="Folder"
  -  displayName="Create site" action="CREATE" businessObject="Site"
  -  displayName="Retrieve sites" action="RETRIEVEALL" businessObject="Site"
  -  displayName="Delete site" action="DELETEALL" businessObject="Site"
  -  displayName="Update site" action="UPDATEALL" businessObject="Site"
  -  displayName="Create list" action="CREATE" businessObject="List"
  -  displayName="Retrieve lists" action="RETRIEVEALL" businessObject="List"
  -  displayName="Delete list" action="DELETEALL" businessObject="List"
  -  displayName="Update list" action="UPDATEALL" businessObject="List"
  -  displayName="Create list item" action="CREATE" businessObject="ListItem"
  -  displayName="Retrieve list items" action="RETRIEVEALL" businessObject="ListItem"
  -  displayName="Delete list item" action="DELETEALL" businessObject="ListItem"
  -  displayName="Update list item" action="UPDATEALL" businessObject="ListItem"
  -  displayName="Create list item attachment" action="CREATE" businessObject="ListItemAttachment"
  -  displayName="Retrieve all the list item attachments" action="RETRIEVEALL" businessObject="ListItemAttachment"
  -  displayName="Delete all the list item attachments" action="DELETEALL" businessObject="ListItemAttachment"
  -  displayName="Update list item attachment" action="UPDATEALL" businessObject="ListItemAttachment"
  -  displayName="Download list item attachment" action="DOWNLOADLISTITEMATTACHMENT" businessObject="ListItemAttachment"
  -  displayName="Retrieve all the folder items" action="RETRIEVEALL" businessObject="FolderItem"
  -  displayName="Retrieve users" action="RETRIEVEALL" businessObject="User"
  -  displayName="Retrieve Acl" action="RETRIEVEALL" businessObject="Acl"
  -  displayName="Retrieve shared links" action="RETRIEVEALL" businessObject="SharedLinks"
  -  displayName="Search files" action="RETRIEVEALL" businessObject="SearchFiles"

## Policy XML example

Name the policy file `MicrosoftSharePoint1.policyxml` (or as chosen). Example:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<policies>
  <policy longDescription="" policyName="MicrosoftSharePoint1" policyTemplate="online_v1_basic_oauth" policyType="mssharepoint" shortDescription="" version="">
     <credentialName>MicrosoftSharePointCredential</credentialName>
     <applicationVersion>2019</applicationVersion>
     <applicationType>online</applicationType>
     <authenticationMethod>BASIC_OAUTH</authenticationMethod>
     <endpointUrl>https://YourSharePointHost:8443</endpointUrl>
     <siteCollectionUrl/>
     <domainName/>
     <workstationName/>
     <proxyId/>
  </policy>
</policies>
```
Policy file structure MUST conform to the ACE Policy XML schema; optionally verify against `<ACE_INSTALL_DIR>/common/schemas/Policy/Policy.xsd`.
