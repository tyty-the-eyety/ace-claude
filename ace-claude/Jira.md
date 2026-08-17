## Jira node Requirements

1. Request nodes: `xmi:type="ComIbmApplicationConnectorRequest_jira.msgnode"`, `applicationConnectorType="jira"`
2. Input nodes: `xmi:type="ComIbmApplicationConnectorInput_jira.msgnode"`, `applicationConnectorType="jira"`
3. Set `schemaPrefix="gen/<FlowName>.Jira_Request"` — create `<FlowName>.Jira_Request.request.schema.json` and `<FlowName>.Jira_Request.response.schema.json` in the `gen/` subdirectory per the connector general rules in SKILL.md.
4. Set `policyUrl="{<PolicyProjectName>}:Jira1"` — follow Policy Project structure in PolicyProject.md.
5. Allowed `displayName`/`action`/`businessObject` combinations:
  -  displayName="Create issue (deprecated)" action="CREATE" businessObject="Issue"
  -  displayName="Retrieve issues (deprecated)" action="RETRIEVEALL" businessObject="Issue"
  -  displayName="Delete issue (deprecated)" action="DELETEALL" businessObject="Issue"
  -  displayName="Update issue (deprecated)" action="UPDATEALL" businessObject="Issue"
  -  displayName="Add subtask to an issue (deprecated)" action="CREATESUBTASK" businessObject="Issue"
  -  displayName="Create issue" action="CREATE" businessObject="IssueV2"
  -  displayName="Retrieve issues" action="RETRIEVEALL" businessObject="IssueV2"
  -  displayName="Delete issue" action="DELETEALL" businessObject="IssueV2"
  -  displayName="Update issue" action="UPDATEALL" businessObject="IssueV2"
  -  displayName="Add subtask to an issue" action="CREATESUBTASK" businessObject="IssueV2"
  -  displayName="Retrieve priorities" action="RETRIEVEALL" businessObject="Priority"
  -  displayName="Retrieve project types" action="RETRIEVEALL" businessObject="ProjectType"
  -  displayName="Retrieve permissions" action="RETRIEVEALL" businessObject="Permissions"
  -  displayName="Retrieve my permissions" action="LISTMYPERMISSIONS" businessObject="Permissions"
  -  displayName="Create project" action="CREATE" businessObject="Project"
  -  displayName="Retrieve projects" action="RETRIEVEALL" businessObject="Project"
  -  displayName="Update project" action="UPDATEALL" businessObject="Project"
  -  displayName="Delete project" action="DELETEALL" businessObject="Project"
  -  displayName="Create filter" action="CREATE" businessObject="Filter"
  -  displayName="Retrieve filters" action="RETRIEVEALL" businessObject="Filter"
  -  displayName="Update filter" action="UPDATEALL" businessObject="Filter"
  -  displayName="Delete filter" action="DELETEALL" businessObject="Filter"
  -  displayName="Retrieve issue links" action="RETRIEVEALL" businessObject="IssueLink"
  -  displayName="Delete issue link" action="DELETEALL" businessObject="IssueLink"
  -  displayName="Retrieve issue link types" action="RETRIEVEALL" businessObject="IssueLinkType"
  -  displayName="Delete issue link type" action="DELETEALL" businessObject="IssueLinkType"
  -  displayName="Update issue link type" action="UPDATEALL" businessObject="IssueLinkType"
  -  displayName="Create issue link type" action="CREATE" businessObject="IssueLinkType"
  -  displayName="Create issue type" action="CREATE" businessObject="IssueType"
  -  displayName="Retrieve issue types" action="RETRIEVEALL" businessObject="IssueType"
  -  displayName="Update issue type" action="UPDATEALL" businessObject="IssueType"
  -  displayName="Delete issue type" action="DELETEALL" businessObject="IssueType"
  -  displayName="Retrieve issue votes" action="RETRIEVEALL" businessObject="IssueVote"
  -  displayName="Add vote to issue" action="ADDISSUEVOTE" businessObject="IssueVote"
  -  displayName="Remove vote from issue" action="REMOVEISSUEVOTE" businessObject="IssueVote"
  -  displayName="Retrieve issue comments" action="RETRIEVEALL" businessObject="IssueComment"
  -  displayName="Delete issue comment" action="DELETEALL" businessObject="IssueComment"
  -  displayName="Update issue comment" action="UPDATEALL" businessObject="IssueComment"
  -  displayName="Create issue comment" action="CREATE" businessObject="IssueComment"
  -  displayName="Create issue attachment" action="CREATE" businessObject="IssueAttachment"
  -  displayName="Retrieve issue attachments" action="RETRIEVEALL" businessObject="IssueAttachment"
  -  displayName="Delete issue  attachment" action="DELETEALL" businessObject="IssueAttachment"
  -  displayName="Download attachment content" action="DOWNLOADATTACHMENTCONTENT" businessObject="IssueAttachment"
  -  displayName="Create issue worklog" action="CREATE" businessObject="IssueWorklog"
  -  displayName="Retrieve issue worklogs" action="RETRIEVEALL" businessObject="IssueWorklog"
  -  displayName="Update issue worklog" action="UPDATEALL" businessObject="IssueWorklog"
  -  displayName="Delete issue worklog" action="DELETEALL" businessObject="IssueWorklog"
  -  displayName="Retrieve project versions" action="RETRIEVEALL" businessObject="ProjectVersion"
  -  displayName="Delete project version" action="DELETEALL" businessObject="ProjectVersion"
  -  displayName="Update project version" action="UPDATEALL" businessObject="ProjectVersion"
  -  displayName="Create project version" action="CREATE" businessObject="ProjectVersion"
  -  displayName="Retrieve project components" action="RETRIEVEALL" businessObject="ProjectComponent"
  -  displayName="Delete project component" action="DELETEALL" businessObject="ProjectComponent"
  -  displayName="Create project component" action="CREATE" businessObject="ProjectComponent"
  -  displayName="Update project component" action="UPDATEALL" businessObject="ProjectComponent"
  -  displayName="Retrieve notification schemes" action="RETRIEVEALL" businessObject="NotificationScheme"
  -  displayName="Retrieve resolutions" action="RETRIEVEALL" businessObject="Resolution"
  -  displayName="Retrieve statuses" action="RETRIEVEALL" businessObject="Status"
  -  displayName="Retrieve project roles" action="RETRIEVEALL" businessObject="ProjectRole"
  -  displayName="Delete project role" action="DELETEALL" businessObject="ProjectRole"
  -  displayName="Update project role" action="UPDATEALL" businessObject="ProjectRole"
  -  displayName="Create project role" action="CREATE" businessObject="ProjectRole"
  -  displayName="Retrieve groups" action="RETRIEVEALL" businessObject="Group"
  -  displayName="Delete group" action="DELETEALL" businessObject="Group"
  -  displayName="Create group" action="CREATE" businessObject="Group"
  -  displayName="Retrieve users" action="RETRIEVEALL" businessObject="User"
  -  displayName="Retrieve users assignable for issue" action="USERASSIGNABLEFORISSUE" businessObject="User"
  -  displayName="Retrieve users assignable for project" action="USERASSIGNABLEFORPROJECT" businessObject="User"
  -  displayName="Find users with permissions" action="USERSWITHPERMISSIONS" businessObject="User"
  -  displayName="Retrieve groups" action="RETRIEVEALL" businessObject="GroupCollection"
  -  displayName="Retrieve issues" action="RETRIEVEALL" businessObject="IssueCollection"
  -  displayName="Retrieve all issues" action="RETRIEVEALL" businessObject="Issue collections"
  -  displayName="Retrieve issue watchers" action="RETRIEVEALL" businessObject="IssueWatcher"
  -  displayName="Add me as watcher to issue" action="ADDISSUEWATCHER" businessObject="IssueWatcher"
  -  displayName="Remove from issue watcher list" action="REMOVEISSUEWATCHER" businessObject="IssueWatcher"
  -  displayName="Retrieve issue comments" action="RETRIEVEALL" businessObject="IssueCommentCollection"

## Policy XML example

Name the policy file `Jira1.policyxml` (or as chosen). Example:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<policies>
  <policy longDescription="" policyName="Jira1" policyTemplate="online_v1_basic" policyType="jira" shortDescription="" version="">
     <credentialName>JiraCredential</credentialName>
     <applicationVersion>v1</applicationVersion>
     <applicationType>online</applicationType>
     <authenticationMethod>BASIC</authenticationMethod>
     <endpointUrl>https://myjirahost:port</endpointUrl>
     <proxyId/>
  </policy>
</policies>
```
Policy file structure MUST conform to the ACE Policy XML schema; optionally verify against `<ACE_INSTALL_DIR>/common/schemas/Policy/Policy.xsd`.
