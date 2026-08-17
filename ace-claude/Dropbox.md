## Dropbox node Requirements

1. Request nodes: `xmi:type="ComIbmApplicationConnectorRequest_dropbox.msgnode"`, `applicationConnectorType="dropbox"`
2. Set `schemaPrefix="gen/<FlowName>.Dropbox_Request"` — create `<FlowName>.Dropbox_Request.request.schema.json` and `<FlowName>.Dropbox_Request.response.schema.json` in the `gen/` subdirectory per the connector general rules in SKILL.md.
3. Set `policyUrl="{<PolicyProjectName>}:Dropbox1"` — follow Policy Project structure in PolicyProject.md.
4. Allowed `displayName`/`action`/`businessObject` combinations:
  -  displayName="Create Team Group" action="CREATE" businessObject="teamgroups"
  -  displayName="Retrieve all team groups" action="RETRIEVEALL" businessObject="teamgroups"
  -  displayName="Updates a group's name and/or external ID." action="UPDATEALL" businessObject="teamgroups"
  -  displayName="Delete Teams group" action="DELETEALL" businessObject="teamgroups"
  -  displayName="Add Team Group Member" action="ADDGROUPMEMBERS" businessObject="teamgroups"
  -  displayName="Retrieve all team group members" action="LISTGROUPMEMBERS" businessObject="teamgroups"
  -  displayName="Delete Teams group member" action="REMOVEGROUPMEMBERS" businessObject="teamgroups"
  -  displayName="Add Team Member" action="CREATE" businessObject="teammembers"
  -  displayName="Retrieve team members" action="RETRIEVEALL" businessObject="teammembers"
  -  displayName="Delete Teams Member" action="DELETEALL" businessObject="teammembers"
  -  displayName="Namespaces List" action="RETRIEVEALL" businessObject="namespaces"
  -  displayName="Retrieve team folders" action="RETRIEVEALL" businessObject="teamfolders"

## Policy XML example

Name the policy file `Dropbox1.policyxml` (or as chosen). Example:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<policies>
  <policy longDescription="" policyName="Dropbox1" policyTemplate="online_v1_basic_oauth" policyType="dropbox" shortDescription="" version="">
     <credentialName>DropboxCredential</credentialName>
     <applicationVersion>v1</applicationVersion>
     <applicationType>online</applicationType>
     <authenticationMethod>BASIC_OAUTH</authenticationMethod>
     <proxyId/>
  </policy>
</policies>
```
Policy file structure MUST conform to the ACE Policy XML schema; optionally verify against `<ACE_INSTALL_DIR>/common/schemas/Policy/Policy.xsd`.
