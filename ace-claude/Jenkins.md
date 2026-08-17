## Jenkins node Requirements

1. Request nodes: `xmi:type="ComIbmApplicationConnectorRequest_jenkins.msgnode"`, `applicationConnectorType="jenkins"`
2. Set `schemaPrefix="gen/<FlowName>.Jenkins_Request"` — create `<FlowName>.Jenkins_Request.request.schema.json` and `<FlowName>.Jenkins_Request.response.schema.json` in the `gen/` subdirectory per the connector general rules in SKILL.md.
3. Set `policyUrl="{<PolicyProjectName>}:Jenkins1"` — follow Policy Project structure in PolicyProject.md.
4. Allowed `displayName`/`action`/`businessObject` combinations:
  -  displayName="Retrieve projects" action="RETRIEVEALL" businessObject="project"
  -  displayName="Delete project" action="DELETEALL" businessObject="project"
  -  displayName="Enable project" action="ENABLE_JOB" businessObject="project"
  -  displayName="Disable project" action="DISABLE_JOB" businessObject="project"
  -  displayName="Verify whether project exists" action="VERIFY_JOB" businessObject="project"
  -  displayName="Retrieve builds" action="RETRIEVEALL" businessObject="build"
  -  displayName="Delete build" action="DELETEALL" businessObject="build"
  -  displayName="Get queued build information" action="GET_QUEUE_INFO" businessObject="build"
  -  displayName="Start build" action="START_BUILD" businessObject="build"
  -  displayName="Stop build" action="STOP_BUILD" businessObject="build"
  -  displayName="Cancel queued build" action="CANCEL_QUEUE_BUILD" businessObject="build"
  -  displayName="Download console output" action="DOWNLOAD_CONSOLE_OUTPUT" businessObject="build"
  -  displayName="Download build artifacts" action="DOWNLOAD_BUILD_ARTIFACTS" businessObject="build"
  -  displayName="Get last build" action="GET_LAST_BUILD" businessObject="build"

## Policy XML example

Name the policy file `Jenkins1.policyxml` (or as chosen). Example:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<policies>
  <policy longDescription="" policyName="Jenkins1" policyTemplate="online_v1_basic" policyType="jenkins" shortDescription="" version="">
     <credentialName>JenkinsCredential</credentialName>
     <applicationVersion>v1</applicationVersion>
     <applicationType>online</applicationType>
     <authenticationMethod>BASIC</authenticationMethod>
     <endpointUrl>https://123.45.123.45</endpointUrl>
  </policy>
</policies>
```
Policy file structure MUST conform to the ACE Policy XML schema; optionally verify against `<ACE_INSTALL_DIR>/common/schemas/Policy/Policy.xsd`.
