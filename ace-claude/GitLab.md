## GitLab node Requirements

1. Request nodes: `xmi:type="ComIbmApplicationConnectorRequest_gitlab.msgnode"`, `applicationConnectorType="gitlab"`
2. Input nodes: `xmi:type="ComIbmApplicationConnectorInput_gitlab.msgnode"`, `applicationConnectorType="gitlab"`
3. Set `schemaPrefix="gen/<FlowName>.GitLab_Request"` — create `<FlowName>.GitLab_Request.request.schema.json` and `<FlowName>.GitLab_Request.response.schema.json` in the `gen/` subdirectory per the connector general rules in SKILL.md.
4. Set `policyUrl="{<PolicyProjectName>}:GitLab1"` — follow Policy Project structure in PolicyProject.md.
5. Allowed `displayName`/`action`/`businessObject` combinations:
  -  displayName="Create project" action="CREATE" businessObject="Project"
  -  displayName="Retrieve projects" action="RETRIEVEALL" businessObject="Project"
  -  displayName="Update project" action="UPDATEALL" businessObject="Project"
  -  displayName="Delete project" action="DELETEALL" businessObject="Project"
  -  displayName="Share project with group" action="SHAREPROJECTWITHGROUP" businessObject="Project"
  -  displayName="Create group" action="CREATE" businessObject="Group"
  -  displayName="Retrieve groups" action="RETRIEVEALL" businessObject="Group"
  -  displayName="Update group" action="UPDATEALL" businessObject="Group"
  -  displayName="Delete group" action="DELETEALL" businessObject="Group"
  -  displayName="Create issue" action="CREATE" businessObject="Issue"
  -  displayName="Retrieve issues" action="RETRIEVEALL" businessObject="Issue"
  -  displayName="Update issue" action="UPDATEALL" businessObject="Issue"
  -  displayName="Delete issue" action="DELETEALL" businessObject="Issue"
  -  displayName="Move issue" action="MOVEANISSUE" businessObject="Issue"
  -  displayName="Retrieve namespaces" action="RETRIEVEALL" businessObject="Namespace"
  -  displayName="Retrieve commits" action="RETRIEVEALL" businessObject="Commit"
  -  displayName="Revert a commit" action="REVERTACOMMIT" businessObject="Commit"
  -  displayName="Create branch" action="CREATE" businessObject="Branch"
  -  displayName="Retrieve branches" action="RETRIEVEALL" businessObject="Branch"
  -  displayName="Delete branch" action="DELETEALL" businessObject="Branch"
  -  displayName="Create merge request" action="CREATE" businessObject="MergeRequest"
  -  displayName="Retrieve merge requests" action="RETRIEVEALL" businessObject="MergeRequest"
  -  displayName="Update merge request" action="UPDATEALL" businessObject="MergeRequest"
  -  displayName="Delete merge request" action="DELETEALL" businessObject="MergeRequest"
  -  displayName="Accept merge request" action="ACCEPTMERGEREQUEST" businessObject="MergeRequest"
  -  displayName="Create milestone" action="CREATE" businessObject="Milestone"
  -  displayName="Retrieve milestones" action="RETRIEVEALL" businessObject="Milestone"
  -  displayName="Update milestone" action="UPDATEALL" businessObject="Milestone"
  -  displayName="Delete milestone" action="DELETEALL" businessObject="Milestone"
  -  displayName="Retrieve users" action="RETRIEVEALL" businessObject="User"
  -  displayName="Retrieve jobs" action="RETRIEVEALL" businessObject="Job"
  -  displayName="Retry job" action="RETRYAJOB" businessObject="Job"
  -  displayName="Cancel job" action="CANCELAJOB" businessObject="Job"
  -  displayName="Erase job" action="ERASEAJOB" businessObject="Job"
  -  displayName="Download job artifacts" action="DOWNLOADJOBARTIFACTS" businessObject="Job"
  -  displayName="Create label" action="CREATE" businessObject="Label"
  -  displayName="Retrieve labels" action="RETRIEVEALL" businessObject="Label"
  -  displayName="Update label" action="UPDATEALL" businessObject="Label"
  -  displayName="Delete label" action="DELETEALL" businessObject="Label"
  -  displayName="Create tag" action="CREATE" businessObject="Tag"
  -  displayName="Retrieve tags" action="RETRIEVEALL" businessObject="Tag"
  -  displayName="Delete tag" action="DELETEALL" businessObject="Tag"
  -  displayName="Create release" action="CREATE" businessObject="Release"
  -  displayName="Retrieve releases" action="RETRIEVEALL" businessObject="Release"
  -  displayName="Update release" action="UPDATEALL" businessObject="Release"
  -  displayName="Delete release" action="DELETEALL" businessObject="Release"
  -  displayName="Create pipeline" action="CREATE" businessObject="Pipeline"
  -  displayName="Retrieve pipelines" action="RETRIEVEALL" businessObject="Pipeline"
  -  displayName="Delete pipeline" action="DELETEALL" businessObject="Pipeline"
  -  displayName="Retry jobs in a pipeline" action="RETRYJOBSINAPIPELINE" businessObject="Pipeline"
  -  displayName="Cancel a pipeline jobs" action="CANCELAPIPELINEJOBS" businessObject="Pipeline"
  -  displayName="Retrieve members" action="RETRIEVEALL" businessObject="Member"
  -  displayName="Add member" action="ADDMEMBER" businessObject="Member"
  -  displayName="Edit member" action="EDITMEMBER" businessObject="Member"
  -  displayName="Remove member" action="REMOVEMEMBER" businessObject="Member"
  -  displayName="Create issue note" action="CREATE" businessObject="IssueNote"
  -  displayName="Retrieve issue notes" action="RETRIEVEALL" businessObject="IssueNote"
  -  displayName="Update issue note" action="UPDATEALL" businessObject="IssueNote"
  -  displayName="Delete issue note" action="DELETEALL" businessObject="IssueNote"
  -  displayName="Create merge request note" action="CREATE" businessObject="MergeRequestNote"
  -  displayName="Retrieve merge request notes" action="RETRIEVEALL" businessObject="MergeRequestNote"
  -  displayName="Update merge request note" action="UPDATEALL" businessObject="MergeRequestNote"
  -  displayName="Delete merge request note" action="DELETEALL" businessObject="MergeRequestNote"
  -  displayName="Create epic" action="CREATE" businessObject="Epic"
  -  displayName="Retrieve epics" action="RETRIEVEALL" businessObject="Epic"
  -  displayName="Update epic" action="UPDATEALL" businessObject="Epic"
  -  displayName="Delete epic" action="DELETEALL" businessObject="Epic"
  -  displayName="Create epic note" action="CREATE" businessObject="EpicNote"
  -  displayName="Retrieve epic notes" action="RETRIEVEALL" businessObject="EpicNote"
  -  displayName="Update epic note" action="UPDATEALL" businessObject="EpicNote"
  -  displayName="Delete epic note" action="DELETEALL" businessObject="EpicNote"

## Policy XML example

Name the policy file `GitLab1.policyxml` (or as chosen). Example:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<policies>
  <policy longDescription="" policyName="GitLab1" policyTemplate="online_v1_basic" policyType="gitlab" shortDescription="" version="">
     <credentialName>GitLabCredential</credentialName>
     <applicationVersion>v1</applicationVersion>
     <applicationType>online</applicationType>
     <authenticationMethod>BASIC</authenticationMethod>
     <endpointUrl/>
     <proxyId/>
  </policy>
</policies>
```
Policy file structure MUST conform to the ACE Policy XML schema; optionally verify against `<ACE_INSTALL_DIR>/common/schemas/Policy/Policy.xsd`.
