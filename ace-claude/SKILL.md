---
name: ace-claude
description: Create IBM App Connect Enterprise message flows and associated configuration files such as ESQL, Java and Maps
---

# ACE Toolkit Message Flow Generator
You are an expert at developing integration artifacts of various types for IBM App Connect Enterprise (ACE). Specifically you are trained for:
- Creating message flows (files with filename "*.msgflow)
- Creating ESQL for message flow Compute nodes (files with filename "*.esql")
- Creating Java for message flow JavaCompute nodes (files with filename "*.java")
- Creating Maps for message flow Map nodes (files with filename "*.map")
- Providing a summary for each task you complete.

## Reference Examples
The `examples/` subfolder alongside this skill contains annotated reference files. Read the relevant file before creating artifacts of that type:

| File | What it demonstrates |
|---|---|
| `examples/mq_nodes.msgflow` | Correct MQ Input / MQ Output node XML, `messageDomainProperty`, terminal names |
| `examples/http_nodes.msgflow` | Correct HTTP Input, WSRequest (`httpVersion`, `protocol`, `messageDomainProperty`), WSReply |
| `examples/subflow_terminals.subflow` | Correct `InTerminal.Input` / `OutTerminal.Output` xmi:ids, `subflowImplFile` warning |
| `examples/esql_patterns.esql` | DECLARE placement, repeating elements, FOR loop, datetime `HH`, error handlers, namespace mapping |

#️ **CRITICAL: Pre-Creation Validation**
Before creating **ANY** artifacts, you **MUST**:
- Read the **ENTIRE** relevant section for the node/project type
- Verify the **EXACT** xmi:type namespace prefix from this document. DO NOT copy them from example .msgflow files as they may contain outdated or incorrect values.
- Verify the **EXACT** project natures required
- DO **NOT** copy from example files - they may be outdated 

# Workflow
1. **Create an ACE Toolkit Application Project**
   - If the user did not mention an existing Application project, you should create one. 
   - The ACE Toolkit Application Project provides a location where output files such as *.msgflow, *.esql, *.msp will be created.
   - When generating .msgflow files or .esql files they should always be created inside an ACE Toolkit Application project.
   - If you have not been given a name for the ACE Toolkit Application project, use the same name as the message flow (without the .msgflow file extension)
   - An ACE Toolkit Application project is a specialized form of an Eclipse project

2. **Create the .project file**
   - If you created a new ACE Toolkit Application Project it **MUST** contain a .project file.
   - To be recognised as an Eclipse project, the file system directory representing the project **MUST** contain a .project file
   - ACE Toolkit Application projects **MUST** contain the following project natures section in the .project file:
     ```
	 <natures>
		<nature>com.ibm.etools.msgbroker.tooling.applicationNature</nature>
		<nature>com.ibm.etools.msgbroker.tooling.messageBrokerProjectNature</nature>
	 </natures>
	 ```
   - ACE Toolkit Application projects **MUST** contain the following buildSpec section in the .project file:
     ```
     <buildSpec>
		<buildCommand>
			<name>com.ibm.etools.mft.applib.applibbuilder</name>
			<arguments>
			</arguments>
		</buildCommand>
		<buildCommand>
			<name>com.ibm.etools.mft.applib.applibresourcevalidator</name>
			<arguments>
			</arguments>
		</buildCommand>
		<buildCommand>
			<name>com.ibm.etools.mft.connector.policy.ui.PolicyBuilder</name>
			<arguments>
			</arguments>
		</buildCommand>
		<buildCommand>
			<name>com.ibm.etools.mft.applib.mbprojectbuilder</name>
			<arguments>
			</arguments>
		</buildCommand>
		<buildCommand>
			<name>com.ibm.etools.msg.validation.dfdl.mlibdfdlbuilder</name>
			<arguments>
			</arguments>
		</buildCommand>
		<buildCommand>
			<name>com.ibm.etools.mft.flow.adapters.adapterbuilder</name>
			<arguments>
			</arguments>
		</buildCommand>
		<buildCommand>
			<name>com.ibm.etools.mft.flow.sca.scabuilder</name>
			<arguments>
			</arguments>
		</buildCommand>
		<buildCommand>
			<name>com.ibm.etools.msg.validation.dfdl.mbprojectresourcesbuilder</name>
			<arguments>
			</arguments>
		</buildCommand>
		<buildCommand>
			<name>com.ibm.etools.mft.esql.lang.esqllangbuilder</name>
			<arguments>
			</arguments>
		</buildCommand>
		<buildCommand>
			<name>com.ibm.etools.mft.map.builder.mslmappingbuilder</name>
			<arguments>
			</arguments>
		</buildCommand>
		<buildCommand>
			<name>com.ibm.etools.mft.flow.msgflowxsltbuilder</name>
			<arguments>
			</arguments>
		</buildCommand>
		<buildCommand>
			<name>com.ibm.etools.mft.flow.msgflowbuilder</name>
			<arguments>
			</arguments>
		</buildCommand>
		<buildCommand>
			<name>com.ibm.etools.mft.decision.service.ui.decisionservicerulebuilder</name>
			<arguments>
			</arguments>
		</buildCommand>
		<buildCommand>
			<name>com.ibm.etools.mft.pattern.capture.PatternBuilder</name>
			<arguments>
			</arguments>
		</buildCommand>
		<buildCommand>
			<name>com.ibm.etools.mft.json.builder.JSONBuilder</name>
			<arguments>
			</arguments>
		</buildCommand>
		<buildCommand>
			<name>com.ibm.etools.mft.restapi.ui.restApiDefinitionsBuilder</name>
			<arguments>
			</arguments>
		</buildCommand>
		<buildCommand>
			<name>com.ibm.etools.mft.policy.ui.policybuilder</name>
			<arguments>
			</arguments>
		</buildCommand>
		<buildCommand>
			<name>com.ibm.etools.mft.msg.assembly.messageAssemblyBuilder</name>
			<arguments>
			</arguments>
		</buildCommand>
		<buildCommand>
			<name>com.ibm.etools.msg.validation.dfdl.dfdlqnamevalidator</name>
			<arguments>
			</arguments>
		</buildCommand>
		<buildCommand>
			<name>com.ibm.etools.mft.bar.ext.barbuilder</name>
			<arguments>
			</arguments>
		</buildCommand>
		<buildCommand>
			<name>com.ibm.etools.mft.unittest.ui.TestCaseBuilder</name>
			<arguments>
			</arguments>
		</buildCommand>
	 </buildSpec>
	 ```   
3. **Create the .settings subdirectory in the project**
   - If you created a new ACE Toolkit Application Project it **MUST** contain a .settings subdirectory
   - The .settings folder **MUST** contain a file named org.eclipse.core.resources.prefs which should contain the following:
     ```
     eclipse.preferences.version=1
     encoding/<project>=UTF-8
     ```

4. **Create the application.descriptor file in the project**
   - If you created a new ACE Toolkit Application Project it **MUST** contain a `.settings/org.eclipse.core.resources.prefs` file with at minimum:
     ```
     eclipse.preferences.version=1
     encoding/<project>=UTF-8
     ```
     The literal string `encoding/<project>=UTF-8` (not replaced with the project name) sets the default project encoding and suppresses the "no explicit encoding" warning in ACE Toolkit. Add additional `encoding/<filename>=UTF-8` lines for each file in the project.
   - If you created a new ACE Toolkit Application Project it **MUST** contain an application.descriptor file in the root of the project directory
   - The Application descriptor file has a fixed name application.descriptor. The content of the application.descriptor looks like this:
     ```
     <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
     <ns2:appDescriptor xmlns="http://com.ibm.etools.mft.descriptor.base" xmlns:ns2="http://com.ibm.etools.mft.descriptor.app">
       <references/> 
     </ns2:appDescriptor>
     ```
   - If you created a new ACE Toolkit Application Project, then at the very end of your task (after you have completed the rest of the steps defined below) you **MUST** always print the following message back to the user (which helps guide them on next steps) at the very end of your processing: "If you are working in the ACE Toolkit then to see the results and do further work with the generated project in the Application Development view, use the menu option to File > Import > Existing Projects into Workspace."	 
	 
5. **Create a message flow** 
   - When asked to create a message flow, you will create a *.msgflow file which will be understood by ACE Toolkit (do not get confused with creating a yaml file for ACE Designer)
   - The *.msgflow file **MUST** use the following exact root structure and namespace URIs. Replace `<FlowName>` with the actual message flow name (without the .msgflow extension) and replace `<ProjectName>` with the ACE Toolkit Application project name:
     ```xml
     <?xml version="1.0" encoding="UTF-8"?>
     <ecore:EPackage xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore" xmlns:eflow="http://www.ibm.com/wbi/2005/eflow" xmlns:utility="http://www.ibm.com/wbi/2005/eflow_utility" nsURI="<FlowName>.msgflow" nsPrefix="<FlowName>.msgflow">
       <eClassifiers xmi:type="eflow:FCMComposite" name="FCMComposite_1" nodeLayoutStyle="RECTANGLE">
         <eSuperTypes href="http://www.ibm.com/wbi/2005/eflow#//FCMBlock"/>
         <translation xmi:type="utility:ConstantString" string="<FlowName>"/>
         <colorGraphic16 xmi:type="utility:GIFFileGraphic" resourceName="platform:/plugin/<ProjectName>/icons/full/obj16/<FlowName>.gif"/>
         <colorGraphic32 xmi:type="utility:GIFFileGraphic" resourceName="platform:/plugin/<ProjectName>/icons/full/obj30/<FlowName>.gif"/>
         <composition>
           <!-- nodes and connections go here -->
         </composition>
         <propertyOrganizer/>
         <stickyBoard/>
       </eClassifiers>
     </ecore:EPackage>
     ```
   - For each node type used (e.g. ComIbmMQInput.msgnode, ComIbmMQOutput.msgnode), you **MUST** add a corresponding `xmlns:<NodeNamespace>="<NodeNamespace>"` attribute to the `<ecore:EPackage>` root element. For example, if using MQ Input and MQ Output nodes:
     ```xml
     <ecore:EPackage ... xmlns:ComIbmMQInput.msgnode="ComIbmMQInput.msgnode" xmlns:ComIbmMQOutput.msgnode="ComIbmMQOutput.msgnode" ...>
     ```
   - Node `<translation>` elements inside `<composition>` **MUST** use `xmi:type="utility:ConstantString"` with a `string=` attribute (not `key=` or `value=`)
   - **NEVER** use the namespace URIs `http://www.ibm.com/etools/mft/hpai81/flow/logical/...` or `http://www.ibm.com/etools/mft/hpai81/flow/utility/...` — these are invalid and will cause parsing errors in ACE Toolkit
   - Before adding any node, you **MUST** verify its xmi:type namespace prefix in the list below
   - When adding a node to a message flow, the xml element called nodes inside the message flow file **MUST** have an xmi:type attribute
   - Node xmi:type prefixes **MUST** come from the authoritative tables below. Do NOT guess or invent prefixes. If ACE is installed locally, you may optionally verify against `<ACE_INSTALL_DIR>/common/schemas/MessageFlow/MessageFlow.xsd`, but the tables take precedence.

### Node label conventions
   - MQ Input, MQ Output, MQ Get and other MQ-type node labels **MUST** be the queue name they are configured with (e.g. `string="INPUT.QUEUE"`).
   - HTTP Reply node label **MUST** be `reply`.
   - HTTP Request (outbound call) node label **MUST** be the remote URI from the `URLSpecifier` attribute (e.g. `string="http://localhost:8080"`).
   - Route node labels **MUST** follow the pattern `route_{description}` (e.g. `route_by_priority`).
   - Compute node labels **MUST** be a short descriptive snake_case name (e.g. `copy_props_mqmd_body`, `req_json_2_xml`). Do NOT use generic names like `Compute`.
   - Subflow Input label: `in`. Subflow Output label: `out`.

### Standard node type reference table

| Node | xmi:type prefix | Notes |
|---|---|---|
| **Subflow** | | |
| Subflow Input | `eflow:FCMSource` | Single output: `OutTerminal.out` |
| Subflow Output | `eflow:FCMSink` | Single input: `InTerminal.in` |
| **MQ** | | |
| MQ Input | `ComIbmMQInput.msgnode` | `queueName` required; ask user if not given |
| MQ Output | `ComIbmMQOutput.msgnode` | |
| MQ Reply | `ComIbmMQReply.msgnode` | |
| MQ Get | `ComIbmMQGet.msgnode` | |
| MQ Header | `ComIbmMQHeader.msgnode` | |
| Publication | `ComIbmPublication.msgnode` | |
| **HTTP / WS** | | |
| HTTP Input | `ComIbmWSInput.msgnode` | `URLSpecifier`=path suffix (required); see extended rules |
| HTTP Reply | `ComIbmWSReply.msgnode` | |
| HTTP Request | `ComIbmWSRequest.msgnode` | `URLSpecifier`=full URL (required); see extended rules |
| HTTP Header | `ComIbmHTTPHeader.msgnode` | |
| HTTP Async Request | `ComIbmHTTPAsyncRequest.msgnode` | |
| HTTP Async Response | `ComIbmHTTPAsyncResponse.msgnode` | |
| **REST** | | |
| REST Request | `ComIbmRESTRequest.msgnode` | |
| REST Async Request | `ComIbmRESTAsyncRequest.msgnode` | |
| REST Async Response | `ComIbmRESTAsyncResponse.msgnode` | |
| App Connect REST Request | `ComIbmAppConnectRESTRequest.msgnode` | |
| **SOAP** | | |
| SOAP Input | `ComIbmSOAPInput.msgnode` | |
| SOAP Reply | `ComIbmSOAPReply.msgnode` | |
| SOAP Request | `ComIbmSOAPRequest.msgnode` | |
| SOAP Async Request | `ComIbmSOAPAsyncRequest.msgnode` | |
| SOAP Async Response | `ComIbmSOAPAsyncResponse.msgnode` | |
| SOAP Envelope | `ComIbmSOAPEnvelope.msgnode` | |
| SOAP Extract | `ComIbmSOAPExtract.msgnode` | |
| **JMS** | | |
| JMS Input | `ComIbmJMSClientInput.msgnode` | |
| JMS Output | `ComIbmJMSClientOutput.msgnode` | |
| JMS Reply | `ComIbmJMSClientReply.msgnode` | |
| JMS Receive | `ComIbmJMSClientReceive.msgnode` | |
| JMS Header | `ComIbmJMSHeader.msgnode` | |
| **Kafka** | | |
| Kafka Consumer | `ComIbmKafkaMsgConsumer.msgnode` | |
| Kafka Producer | `ComIbmKafkaMsgProducer.msgnode` | |
| Kafka Read | `ComIbmKafkaMsgRead.msgnode` | |
| **Compute / Transform** | | |
| Compute | `ComIbmCompute.msgnode` | `computeExpression` + `computeMode` required; see extended rules |
| Java Compute | `ComIbmJavaCompute.msgnode` | |
| .NET Compute | `ComIbmDotNetCompute.msgnode` | |
| .NET Input | `ComIbmDotNetInput.msgnode` | |
| Mapping | `ComIbmMSLMapping.msgnode` | |
| XSL Transform | `ComIbmXslMqsi.msgnode` | |
| JSONata Mapping | `ComIbmJSONataMapping.msgnode` | |
| Reset Content Descriptor | `ComIbmResetContentDescriptor.msgnode` | |
| **Routing / Control** | | |
| Route | `ComIbmRoute.msgnode` | `distributionMode` + `outTerminals` + `filterTable` required; see extended rules |
| Filter | `ComIbmFilter.msgnode` | |
| Flow Order | `ComIbmFlowOrder.msgnode` | |
| Label | `ComIbmLabel.msgnode` | |
| Route To Label | `ComIbmRouteToLabel.msgnode` | |
| Pass Through | `ComIbmPassthru.msgnode` | |
| **Error / Diagnostics** | | |
| Throw | `ComIbmThrow.msgnode` | |
| Try Catch | `ComIbmTryCatch.msgnode` | |
| Trace | `ComIbmTrace.msgnode` | |
| Validate | `ComIbmValidate.msgnode` | |
| Log | `ComIbmLog.msgnode` | |
| **Callable Flow** | | |
| Callable Flow Input | `ComIbmCallableFlowInput.msgnode` | |
| Callable Flow Reply | `ComIbmCallableFlowReply.msgnode` | |
| Callable Flow Invoke | `ComIbmCallableFlowInvoke.msgnode` | |
| Callable Flow Async Invoke | `ComIbmCallableFlowAsyncInvoke.msgnode` | |
| Callable Flow Async Response | `ComIbmCallableFlowAsyncResponse.msgnode` | |
| **Aggregation / Sequence** | | |
| Aggregate Control | `ComIbmAggregateControl.msgnode` | |
| Aggregate Reply | `ComIbmAggregateReply.msgnode` | |
| Aggregate Request | `ComIbmAggregateRequest.msgnode` | |
| Collector | `ComIbmCollector.msgnode` | |
| Group Scatter | `ComIbmGroupScatter.msgnode` | |
| Group Gather | `ComIbmGroupGather.msgnode` | |
| Group Complete | `ComIbmGroupComplete.msgnode` | |
| Resequence | `ComIbmReSequence.msgnode` | |
| Sequence | `ComIbmSequence.msgnode` | |
| **Timer / Scheduler** | | |
| Timeout Control | `ComIbmTimeoutControl.msgnode` | |
| Timeout Notification | `ComIbmTimeoutNotification.msgnode` | |
| Scheduler | `ComIbmScheduler.msgnode` | |
| **File** | | |
| File Input | `ComIbmFileInput.msgnode` | `inputDirectory` must be ABSOLUTE; see extended rules |
| File Output | `ComIbmFileOutput.msgnode` | `outputDirectory` must be ABSOLUTE; filename via LocalEnvironment needs `computeMode`; see extended rules |
| File Read | `ComIbmFileRead.msgnode` | |
| File Exists | `ComIbmFileExists.msgnode` | |
| File Iterator | `ComIbmFileIterator.msgnode` | |
| **Database** | | |
| Database Input | `ComIbmDatabaseInput.msgnode` | |
| Database | `ComIbmDatabase.msgnode` | |
| Database Retrieve | `ComIbmDatabaseRetrieve.msgnode` | |
| Database Route | `ComIbmDatabaseRoute.msgnode` | |
| Change Data Capture | `ComIbmChangeDataCapture.msgnode` | |
| **Email** | | |
| Email Input | `ComIbmEmailInput.msgnode` | |
| Email Output | `ComIbmEmailOutput.msgnode` | |
| **FTE / CD** | | |
| FTE Input | `ComIbmFTEInput.msgnode` | |
| FTE Output | `ComIbmFTEOutput.msgnode` | |
| CD Input | `ComIbmCDInput.msgnode` | |
| CD Output | `ComIbmCDOutput.msgnode` | |
| **TCP** | | |
| TCPIP Client Input | `ComIbmTCPIPClientInput.msgnode` | |
| TCPIP Client Output | `ComIbmTCPIPClientOutput.msgnode` | |
| TCPIP Client Receive | `ComIbmTCPIPClientReceive.msgnode` | |
| TCPIP Server Input | `ComIbmTCPIPServerInput.msgnode` | |
| TCPIP Server Output | `ComIbmTCPIPServerOutput.msgnode` | |
| TCPIP Server Receive | `ComIbmTCPIPServerReceive.msgnode` | |
| **SAP / ERP** | | |
| SAP Input | `ComIbmSAPInput.msgnode` | |
| SAP Request | `ComIbmSAPRequest.msgnode` | |
| SAP Reply | `ComIbmSAPReply.msgnode` | |
| JDEdwards Input | `ComIbmJDEdwardsInput.msgnode` | |
| JDEdwards Request | `ComIbmJDEdwardsRequest.msgnode` | |
| PeopleSoft Input | `ComIbmPeopleSoftInput.msgnode` | |
| PeopleSoft Request | `ComIbmPeopleSoftRequest.msgnode` | |
| Siebel Input | `ComIbmSiebelInput.msgnode` | |
| Siebel Request | `ComIbmSiebelRequest.msgnode` | |
| **Miscellaneous** | | |
| CICS Request | `ComIbmCICSIPICRequest.msgnode` | |
| CORBA Request | `ComIbmCORBARequest.msgnode` | |
| IMS Request | `ComIbmIMSRequest.msgnode` | |
| ODM Rules | `ComIbmODMRules.msgnode` | |
| Security PEP | `ComIbmSecurityPEP.msgnode` | |
| Registry Lookup | `SRRetrieveEntity.msgnode` | |
| Endpoint Lookup | `SRRetrieveITService.msgnode` | |
| **Special connector nodes** | | |
| MQTT Subscribe | `com_ibm_connector_mqtt_ComIbmEventInput.msgnode` | `connectorName="MQTT"` |
| MQTT Publish | `com_ibm_connector_mqtt_ComIbmOutput.msgnode` | `connectorName="MQTT"` |
| Loop Back Request | `com_ibm_connector_loopback_ComIbmRequest.msgnode` | `connectorName="iib-loopback-connector"` |
| Salesforce Request (no discovery) | `com_ibm_connector_salesforce_ComIbmRequest.msgnode` | `connectorName="iib-salesforce-connector"` |

### ApplicationConnector node reference table

**General rule:** All `ComIbmApplicationConnectorRequest_*` nodes use `xmi:type="ComIbmApplicationConnectorRequest_<suffix>.msgnode"` with `applicationConnectorType="<suffix>"`. All `ComIbmApplicationConnectorInput_*` nodes use `xmi:type="ComIbmApplicationConnectorInput_<suffix>.msgnode"` with `applicationConnectorType="<suffix>"`.

| Connector | Suffix | Has Input node |
|---|---|---|
| Amazon CloudWatch | `amazoncloudwatch` | |
| Amazon DynamoDB | `amazondynamodb` | |
| Amazon EC2 | `amazonec2` | |
| Amazon EventBridge | `amazoneventbridge` | ✓ |
| Amazon Kinesis | `amazonkinesis` | |
| Amazon Lambda | `amazonlambda` | |
| Amazon RDS | `amazonrds` | |
| Amazon S3 | `amazons3` | |
| Amazon SES | `amazonses` | |
| Amazon SNS | `amazonsns` | |
| Amazon SQS | `amazonsqs` | ✓ |
| Anaplan | `anaplan` | |
| Apache Pulsar | `apachepulsar` | ✓ |
| Asana | `asana` | ✓ |
| Astra DB | `astradb` | ✓ |
| BambooHR | `bamboohr` | |
| Box | `box` | |
| Businessmap | `businessmap` | ✓ |
| Calendly | `calendly` | |
| ClickSend | `clicksend` | ✓ |
| CMIS | `cmis` | ✓ |
| Confluence | `confluence` | |
| Couchbase | `couchbase` | |
| Coupa | `coupa` | ✓ |
| Crystal Ball | `crystalball` | |
| Databricks | `databricks` | ✓ (Input only) |
| DocuSign | `docusign` | |
| Dropbox | `dropbox` | |
| Eventbrite | `eventbrite` | ✓ |
| Expensify | `expensify` | |
| Factorial HR | `factorialhr` | |
| Freshservice | `freshservice` | ✓ |
| Front | `front` | ✓ |
| GitHub | `github` | ✓ |
| GitLab | `gitlab` | ✓ |
| Gmail | `gmail` | ✓ |
| Google Analytics | `googleanalytics` | |
| Google Analytics 4 | `googleanalytics4` | |
| Google BigQuery | `googlebigquery` | |
| Google Calendar | `googlecalendar` | ✓ |
| Google Chat | `googlechat` | |
| Google Cloud Storage | `googlecloudstorage` | |
| Google Contacts | `googlecontacts` | |
| Google Drive | `googledrive` | |
| Google Gemini | `googlegemini` | |
| Google Groups | `googlegroups` | |
| Google PubSub | `googlepubsub` | ✓ |
| Google Sheets | `googlesheet` | ✓ |
| Google Tasks | `googletasks` | |
| Google Translate | `googletranslate` | |
| Greenhouse | `greenhouse` | ✓ |
| HubSpot CRM | `hubspotcrm` | |
| HubSpot Marketing | `hubspotmarketing` | ✓ |
| Hunter | `hunter` | |
| IBM Aspera | `ibmaspera` | |
| IBM Cloud Object Storage S3 | `ibmcoss3` | |
| IBM Cloudant | `cloudantdb` | |
| IBM Engineering Workflow Mgmt | `ibmewm` | ✓ |
| IBM FileNet Content Manager | `filenet` | |
| IBM Food Trust | `ift` | |
| IBM Maximo | `maximo` | ✓ |
| IBM OpenPages | `ibmopenpages` | ✓ |
| IBM Planning Analytics | `planninganalytics` | |
| IBM Sterling Intelligent Promising | `ibmsterlingiv` | |
| IBM Targetprocess | `apptiotargetprocess` | ✓ |
| IBM Watson Discovery | `watsondiscovery` | |
| IBM watsonx.ai | `ibmwatsonxai` | |
| IBM zOS Connect | `zosconnect` | |
| Infobip | `infobip` | |
| Insightly | `insightly` | ✓ |
| Jenkins | `jenkins` | |
| Jira | `jira` | ✓ |
| LDAP | `ldap` | ✓ |
| Magento | `magento` | ✓ |
| Mailchimp | `mailchimp` | ✓ |
| Marketo | `marketo` | ✓ |
| Microsoft Active Directory | `msad` | ✓ |
| Microsoft Azure Blob Storage | `azureblobstorage` | |
| Microsoft Azure Cosmos DB | `azurecosmosdb` | |
| Microsoft Azure DevOps | `azuredevops` | ✓ |
| Microsoft Azure Event Hubs | `azureeventhub` | ✓ |
| Microsoft Azure OpenAI | `azureopenai` | |
| Microsoft Azure Service Bus | `azureservicebus` | ✓ |
| Microsoft Dynamics 365 Finance | `msdynamicsfando` | |
| Microsoft Dynamics 365 Sales | `msdynamicscrmrest` | |
| Microsoft Entra ID | `azuread` | ✓ |
| Microsoft Excel Online | `msexcel` | ✓ |
| Microsoft Exchange | `msexchange` | ✓ |
| Microsoft OneDrive for Business | `msonedrive` | |
| Microsoft OneNote | `msonenote` | |
| Microsoft Power BI | `mspowerbi` | |
| Microsoft SharePoint | `mssharepoint` | ✓ |
| Microsoft Teams | `msteams` | ✓ |
| Microsoft To Do | `mstodo` | |
| Microsoft Viva Engage | `yammer` | ✓ |
| Milvus | `milvus` | |
| monday.com | `mondaydotcom` | ✓ |
| Oracle E-Business Suite | `oracleebs` | |
| Oracle HCM | `oraclehcm` | ✓ |
| Pinecone Vector DB | `pineconedb` | |
| Redis | `rediscache` | |
| Salesforce | `salesforce` | ✓ |
| Salesforce Account Engagement | `salesforceae` | |
| Salesforce Commerce Cloud | `sfcommerceclouddata` | ✓ |
| Salesforce Marketing Cloud | `salesforcemc` | |
| SAP Ariba | `sapariba` | |
| SAP OData | `sapodata` | |
| SAP S/4 HANA | `saps4hana` | ✓ |
| SAP SuccessFactors | `sapsuccessfactors` | ✓ |
| ServiceNow | `servicenow` | ✓ |
| Shopify | `shopify` | ✓ |
| Slack | `slack` | ✓ |
| Snowflake | `snowflake` | |
| Splunk | `splunk` | |
| Square | `square` | |
| SurveyMonkey | `surveymonkey` | ✓ |
| The Weather Company | `ibmtwc` | |
| Toggl Track | `toggltrack` | ✓ |
| Trello | `trello` | |
| Twilio | `twilio` | |
| UKG | `kronos` | |
| Vespa | `vespa` | |
| WordPress | `wordpress` | |
| Workday | `workday` | |
| Wrike | `wrike` | ✓ |
| Wufoo | `wufoo` | ✓ |
| Yapily | `yapily` | |
| Zendesk Service | `zendeskservice` | ✓ |
| Zoho Books | `zohobooks` | ✓ |
| Zoho CRM | `zohocrm` | ✓ |
| Zoho Inventory | `zohoinventory` | |
| Zoho Recruit | `zohorecruit` | ✓ |

### Connector Request node general rules

For **every** `ComIbmApplicationConnectorRequest_*` node, you **MUST**:
1. Set `schemaPrefix="gen/<FlowName>.<ConnectorLabel>_Request"` where `<ConnectorLabel>` matches the connector `.md` filename without extension (e.g. `AmazonS3` for `AmazonS3.md`).
2. Create two empty JSON schema files in the `gen/` subdirectory of the Application project:
   - `<FlowName>.<ConnectorLabel>_Request.request.schema.json`
   - `<FlowName>.<ConnectorLabel>_Request.response.schema.json`
3. Set `policyUrl="{<PolicyProjectName>}:<PolicyName>"`. Read the connector's `.md` file for the policy XML example and the allowed `displayName`/`action`/`businessObject` combinations. Read `PolicyProject.md` for Policy Project scaffolding rules.

### Extended node rules

#### Subflow nodes
   - A subflow file follows the same root structure as any other msgflow file but contains `eflow:FCMSource` and `eflow:FCMSink` nodes. The subflow's `nsURI` and `nsPrefix` are set to its filename (e.g. `copy_msg_headers.msgflow`).
   - To reference a subflow from a parent flow:
     1. Add `xmlns:<subflowName>.msgflow="<subflowName>.msgflow"` to the parent's root `<ecore:EPackage>` element.
     2. Add a node with `xmi:type="<subflowName>.msgflow:FCMComposite_1"` and `subflowImplFile="<subflowName>.msgflow"`.
     3. Into the subflow: `targetTerminalName="InTerminal.Input"`. Out of the subflow: `sourceTerminalName="OutTerminal.Output"`. Note the capital I/O — these differ from regular node terminals.
     4. Label the subflow node with the subflow's name.
   - The ESQL module naming convention for a Compute node inside a subflow is `<subflowName>_Compute`, in a file named `<subflowName>.esql`.

#### HTTP Input node
   - `URLSpecifier` attribute sets the URL path suffix (e.g. `URLSpecifier="/simple/proxy"`). This is required — the XSD field label is "Path to URL suffix". Do NOT use `path=` for this.
   - To parse the incoming body as a specific domain, set `messageDomainProperty="JSON"` (or `"XMLNSC"`, `"BLOB"`, etc.). Without this attribute, the default domain is used.
   - A typical HTTP proxy flow is: HTTP Input → HTTP Request → HTTP Reply.
   - ESQL pattern for JSON input/output in a Compute node:
     ```sql
     -- Read from incoming JSON body
     DECLARE myField CHARACTER InputRoot.JSON.Data.fieldName;

     -- Set the HTTP response content type
     SET OutputRoot.Properties.ContentType = 'application/json';

     -- Build the JSON response tree
     SET OutputRoot.JSON.Data.responseField = myField;
     SET OutputRoot.JSON.Data.status        = 'success';
     ```
     ASBITSTREAM serialises a parsed message domain tree to raw bytes (BLOB). The second argument is the CCSID (1208 = UTF-8):
     ```sql
     DECLARE rawInput BLOB ASBITSTREAM(InputRoot.JSON, 1208);
     SET OutputRoot.JSON.Data.echo = CAST(rawInput AS CHARACTER CCSID 1208);
     ```

#### MQ Input node — message domain
   - To parse the incoming MQ message body as a specific domain, set `messageDomainProperty="XMLNSC"` (or `"JSON"`, `"BLOB"`, etc.) on the node. This maps to the **Input Message Parsing** tab in the toolkit.
   - **NEVER** use `messageDomain=` — that attribute maps to the Basic tab and does not set the parser.

#### HTTP Request node
   - `URLSpecifier` attribute **MUST** be set to the full target URL. Omitting it causes a "Unset mandatory property" error.
   - **MUST** include `httpVersion="1.1"` — HTTP 1.0 must never be used.
   - **MUST** include `protocol="TLS"` — SSL must never be used; TLS 1.2 minimum is required.
   - To parse the response body as a specific domain, set `messageDomainProperty="XMLNSC"` (or `"JSON"` etc.) — the same attribute used on input nodes. **NEVER** use `responseDomainProperty=`.
   - `protocol="TLS"` is safe on a plain `http://` target — it selects the SSL/TLS protocol *if* the connection is HTTPS and does not force HTTPS. Runtime-verified against an `http://` backend: request succeeds, no warning. So there is never a reason to omit it.
   - For a **passthrough** flow (WSInput → WSRequest → WSReply, body relayed unchanged), use `messageDomainProperty="BLOB"` — it relays the payload byte-for-byte instead of parsing and re-serialising it. Runtime proof: `HTTP_PASSTHRU_APP`.
   - ⚠️ **None of these three attributes are enforced by the runtime.** A WSRequest node missing all of `httpVersion`, `protocol`, and `messageDomainProperty` still returns a correct HTTP 200 with the body relayed. A green smoke test is therefore **not** evidence of conformance — the only way to verify is to read the msgflow XML. Audit node attributes separately from behaviour.
   - Minimal correct example:
     ```xml
     <nodes xmi:type="ComIbmWSRequest.msgnode:FCMComposite_1" xmi:id="FCMComposite_1_2"
         location="400,140"
         URLSpecifier="http://backend.example.com/api/endpoint"
         httpVersion="1.1"
         protocol="TLS"
         messageDomainProperty="XMLNSC">
       <translation xmi:type="utility:ConstantString" string="http://backend.example.com/api/endpoint"/>
     </nodes>
     ```

#### Compute node
   - **MUST** have a `computeExpression` attribute: `esql://routine/#<ModuleName>.Main`
   - **Only set `computeMode`** when the ESQL actually modifies `OutputLocalEnvironment` or `OutputDestination`. If the module only reads/writes `OutputRoot`, **omit `computeMode` entirely** — the default is correct.
     - `computeMode="destinationAndMessage"` — ESQL modifies both `OutputRoot` and `OutputLocalEnvironment`/`OutputDestination` (e.g. setting HTTP status code AND building the response body).
     - `computeMode="localEnvironment"` — ESQL modifies only `OutputLocalEnvironment`.
   - **NEVER** set `computeMode="destinationAndMessage"` on a module that only builds the message. This was a common past mistake.
   - ESQL module name convention: `<FlowName>_Compute`, file named `<FlowName>.esql`.
   - Standard ESQL pattern to copy Properties, MQMD, and body (domain-agnostic):
     ```sql
     CREATE COMPUTE MODULE <FlowName>_Compute
         CREATE FUNCTION Main() RETURNS BOOLEAN
         BEGIN
             SET OutputRoot.Properties = InputRoot.Properties;
             SET OutputRoot.MQMD       = InputRoot.MQMD;
             DECLARE bodyRef REFERENCE TO InputRoot.*[<];
             SET OutputRoot.{FIELDNAME(bodyRef)} = bodyRef;
             RETURN TRUE;
         END;
     END MODULE;
     ```
     `InputRoot.*[<]` copies whichever body domain is present (XMLNSC, BLOB, MRM, JSON, etc.) without needing to know the parser upfront. Do NOT use `InputRoot.*[LAST]` — deprecated in ACE ESQL and will produce a parser warning.

#### File Input / File Output nodes
   These fail *silently* more often than any other node type: all of the mistakes below package and deploy with a clean exit code, so `ibmint package` success proves nothing about a File flow. Runtime proof: `FILE_IO_APP`.
   - **Directories MUST be absolute.** `inputDirectory` / `outputDirectory` accept no relative form — a relative `"data/in"` is rejected at startup with `BIP3333E: ... cannot resolve the relative file path`, and the input node never starts (`BIP3332E`). There is no project-relative or work-dir-relative resolution; the flow runs from the server work dir with nothing to anchor against.
   - **Naming the output file from ESQL requires `computeMode="destinationAndMessage"`** on the Compute node that sets it. Under the default `computeMode="message"` the `OutputLocalEnvironment` tree is discarded, FileOutput receives an empty filename, and the flow fails with `BIP3325E: ... cannot use the directory '<dir>' for file name ''`. The input message is then retried to the limit and backed out — so **nothing is written and the input file disappears**, which looks exactly like the flow never triggered. This is the general `computeMode` rule (see Compute node) in its most invisible form.
   - **The original filename is `InputLocalEnvironment.File.Name`.** `InputLocalEnvironment.ComIbmFileInput.Response.FileName` is *not* a real path — it parses fine but always resolves to NULL, so a `CASE WHEN origName IS NOT NULL` guard silently takes its ELSE branch and every output gets the same constant name. Symptom: correct file content, wrong (unchanging) filename.
   - Set the output name via `SET OutputLocalEnvironment.Destination.File.Name = ...`.
   - **`BIP3316W` at initialisation is expected, not a fault.** "File node '<label>' has no valid filename specified as property ''" only means no filename *node property* is set — correct when the name comes from LocalEnvironment. Read the two codes together: `BIP3316W` at init **+** `BIP3325E` at runtime = the LocalEnvironment name isn't arriving (fix `computeMode`); `BIP3316W` alone with files appearing in the output directory = working as designed.
   - **Never put a path in a node label.** BIP messages quote the label, so a node labelled `/out` still reports `File node '/out'` after its directory has changed — actively misleading during diagnosis. Label by role: `read_input_files`, `write_processed`, `write_error`.

#### Route node
   - Requires `distributionMode`: `"first"` (mutually exclusive routing) or `"all"` (route to all matches).
   - One `<outTerminals terminalNodeID="<name>" dynamic="true" label="<name>"/>` per dynamic output terminal. The fixed `default` terminal does NOT need an `<outTerminals>` declaration.
   - One `<filterTable filterPattern="<xpath>" routingOutputTerminal="<terminalName>"/>` per routing rule. Use `$Root/...` for the message tree and `$LocalEnvironment/...` for local environment. Note: `>` must be XML-escaped as `&gt;`.
   - Connections FROM the Route node: dynamic terminal names used directly as `sourceTerminalName` (no `OutTerminal.` prefix). Default terminal: `sourceTerminalName="OutTerminal.default"`.
   - Example:
     ```xml
     <nodes xmi:type="ComIbmRoute.msgnode:FCMComposite_1" xmi:id="FCMComposite_1_2" location="400,200" distributionMode="first">
       <outTerminals terminalNodeID="highPriority" dynamic="true" label="highPriority"/>
       <outTerminals terminalNodeID="mediumPriority" dynamic="true" label="mediumPriority"/>
       <translation xmi:type="utility:ConstantString" string="route_by_priority"/>
       <filterTable filterPattern="$Root/MQMD/Priority &gt; 6" routingOutputTerminal="highPriority"/>
       <filterTable filterPattern="$Root/MQMD/Priority &gt; 3" routingOutputTerminal="mediumPriority"/>
     </nodes>
     ```
     ```xml
     <connections ... sourceNode="FCMComposite_1_2" sourceTerminalName="highPriority"       targetTerminalName="InTerminal.in"/>
     <connections ... sourceNode="FCMComposite_1_2" sourceTerminalName="mediumPriority"     targetTerminalName="InTerminal.in"/>
     <connections ... sourceNode="FCMComposite_1_2" sourceTerminalName="OutTerminal.default" targetTerminalName="InTerminal.in"/>
     ```

6. **Create an ESQL file**
   - If the message flow contains a Compute node then create an associated ESQL file in the Application project

   #### REFERENCE variables and tree navigation

   - Declare `REFERENCE` variables for any path accessed more than once. Navigation is O(n) per access; a reference is a constant-time pointer.
     ```sql
     DECLARE orderRef REFERENCE TO InputRoot.XMLNSC.Order;
     -- now use orderRef.LineItem, orderRef.Total, etc. — no re-navigation
     ```
   - `DECLARE ref REFERENCE TO someNode` without an initial target is valid — the reference starts unset and can be positioned with `MOVE`.

   #### MOVE statement — the preferred traversal mechanism

   Use `MOVE` to reposition a reference in the message tree without allocating a new variable. Always check success with `LASTMOVE()`.

   | Form | Effect |
   |---|---|
   | `MOVE ref FIRSTCHILD;` | Descend to first child |
   | `MOVE ref NEXTSIBLING;` | Advance to next sibling (any name/type) |
   | `MOVE ref NEXTSIBLING NAME 'Item';` | Advance to next sibling named `Item` |
   | `MOVE ref NEXTSIBLING REPEAT TYPE NAME;` | Advance to next sibling with same name and type |
   | `MOVE ref TO InputRoot.XMLNSC.Order;` | Jump to a specific path |
   | `MOVE ref PARENT;` | Move up to parent |

   **`LASTMOVE(ref)`** returns `TRUE` if the previous MOVE succeeded (the reference is valid). Use it as a loop guard and for existence checks — it is cheaper than `CARDINALITY`.

   #### Iterating over repeating elements — use MOVE NEXTSIBLING, not index notation

   Array subscript notation (`Item[i]`) forces ESQL to walk the tree from the root on each access. For N elements this causes O(N²) traversals. `MOVE NEXTSIBLING` is O(N) — **50–500× faster** on real payloads.

   **Avoid (O(N²) — index notation):**
   ```sql
   DECLARE i INTEGER 1;
   DECLARE size INTEGER CARDINALITY(InputRoot.XMLNSC.Order.Item[]);
   WHILE i <= size DO
       SET OutputRoot.XMLNSC.Result.Item[i].Price = InputRoot.XMLNSC.Order.Item[i].Price;
       SET i = i + 1;
   END WHILE;
   ```

   **Preferred (O(N) — REFERENCE + MOVE):**
   ```sql
   DECLARE inputRef  REFERENCE TO InputRoot.XMLNSC.Order.Item[1];
   WHILE LASTMOVE(inputRef) DO
       CREATE LASTCHILD OF OutputRoot.XMLNSC.Result TYPE Name NAME 'Item';
       DECLARE outputRef REFERENCE TO OutputRoot.XMLNSC.Result.Item[<];
       SET outputRef.Price = inputRef.Price;
       MOVE inputRef NEXTSIBLING REPEAT TYPE NAME;
   END WHILE;
   ```

   For a `FOR` loop over a repeating field the syntax is even simpler:
   ```sql
   FOR item AS InputRoot.XMLNSC.Order.Item[] DO
       -- item is an implicit REFERENCE, no MOVE needed
       SET OutputRoot.XMLNSC.Result.Total = OutputRoot.XMLNSC.Result.Total + item.Price;
   END FOR;
   ```

   #### CARDINALITY — specific rules

   - **Do NOT** use `CARDINALITY` as the loop limit when the loop body also uses index notation — double penalty.
   - **Do NOT** use `CARDINALITY` to test field existence — use `LASTMOVE` instead (no tree walk required).
   - **Do NOT** use `CARDINALITY` on large unparsed files — it may force a full parse and risk an abend.
   - **Correct** use: compute array size **once** before a loop, into an `INTEGER` variable.
     ```sql
     DECLARE arraySize INTEGER CARDINALITY(InputRoot.XMLNSC.Order.Item[]);
     DECLARE i INTEGER 1;
     WHILE i <= arraySize DO
         -- ... index access only when no alternative ...
         SET i = i + 1;
     END WHILE;
     ```

   #### Dynamic field inspection — FIELDNAME, FIELDTYPE, FIELDVALUE

   Use these functions when the field name or type is not known at coding time:
   - `FIELDNAME(ref)` — returns the name of the field the reference points to (e.g. `'XMLNSC'`, `'Item'`).
   - `FIELDTYPE(ref)` — returns the type (e.g. `Name`, `Value`, `NameValue`).
   - `FIELDVALUE(ref)` — returns the scalar value of the field as a generic value.
   - `{FIELDNAME(ref)}` syntax — embeds the dynamic name in a SET target:
     ```sql
     DECLARE bodyRef REFERENCE TO InputRoot.*[<];
     SET OutputRoot.{FIELDNAME(bodyRef)} = bodyRef;   -- copies body domain-agnostically
     ```

   #### String manipulation

   - `LENGTH`, `SUBSTRING`, `RTRIM`, `LTRIM`, and string concatenation (`||`) access individual bytes in the message tree and are CPU-intensive. Minimize their use.
   - Avoid repeating the same concatenation inside a loop. Store the intermediate result in a `CHARACTER` variable.
   - Prefer `REPLACE()` over building a new string by re-parsing — re-parsing forces a full serialise/parse cycle.
   - Prefer `CAST()` over string-building functions when converting between types.

   #### EVAL statement

   - `EVAL` is very expensive: the string is compiled and executed as a second ESQL statement at runtime. Avoid it in all but the most exceptional circumstances.

   #### PASSTHRU (database nodes)

   - Use host variables (`?`) for dynamic values so the database driver can reuse prepared statements:
     ```sql
     PASSTHRU('UPDATE PRICES SET price = ? WHERE company = ?',
              InputBody.Price, InputBody.Company);
     ```
   - Do NOT call stored procedures via `PASSTHRU` with `CALL` — use `CREATE PROCEDURE ... LANGUAGE DATABASE EXTERNAL NAME '...'` instead, which avoids a repeated PREPARE on each invocation.

   #### XML parser choice

   - `XMLNSC` is the most efficient XML parser in ACE/IIB. Prefer it over MRM or the older `XML` domain for XML messages.

   #### Branching and control flow

   - Avoid deeply nested `IF` statements. Use `ELSEIF` or `CASE WHEN` for multiple mutually exclusive conditions — the engine can exit early without evaluating remaining branches.
     ```sql
     -- Prefer CASE over chained IF-ELSEIF for clarity and performance
     SET outputType = CASE inputReport
         WHEN 'PDF' THEN 'P'
         WHEN 'DOC' THEN 'D'
         ELSE 'X'
     END;
     ```

   #### General efficiency rules

   - Combine `DECLARE` with initialization in a single statement — avoids an extra evaluation.
   - Make ESQL modules as concise as possible — fewer statements means less parser overhead.
   - Avoid multiple consecutive Compute nodes in a flow — each node boundary copies the full message tree. Combine logic into a single Compute node where possible.
   - Use `CREATE ... PARSE` in preference to serialising a copy of the logical tree when you need to re-parse a transformed body.
   - Ensure the proper use of `REFERENCE` vs `VALUE` in `SET` statements — setting a REFERENCE copies the pointer, not the subtree.

   #### ESQL reserved words — never use as variable or parameter names

   The words `out` and `in` are reserved in ESQL and will cause compile errors or unexpected behaviour if used as identifiers. Use descriptive names instead:

   | Avoid | Use instead |
   |---|---|
   | `out` | `outRef`, `outMsgRef`, `outOnlineRef`, `outWholesaleRef` |
   | `in` | `inRef`, `inMsg`, `inReq` |

   #### REFERENCE placeholder pattern — output tree

   You cannot declare a REFERENCE to an output node that does not exist yet. Declare the reference pointing to any valid existing node (e.g. `InputRoot`), create the output path with a `SET`, then `MOVE` the reference to the new node:

   ```sql
   DECLARE outRef REFERENCE TO InputRoot;              -- valid placeholder

   SET OutputRoot.JSON.Data.order.id = inRef.orderId;  -- creates the path
   MOVE outRef TO OutputRoot.JSON.Data.order;          -- reposition

   SET outRef.total = CAST(inRef.amount AS DECIMAL);   -- navigate from outRef
   ```

   #### Multi-source format detection with FIELDNAME

   When a single queue carries messages from multiple XML schemas, detect the source by inspecting the root element name:

   ```sql
   DECLARE rootName CHARACTER FIELDNAME(InputRoot.XMLNSC.*[1]);
   -- returns 'OnlineOrder' or 'WholesaleOrder' (local name without namespace)

   IF rootName = 'OnlineOrder' THEN
       CALL MapOnlineOrder(InputRoot.XMLNSC.ns_on:OnlineOrder, outRef);
   ELSEIF rootName = 'WholesaleOrder' THEN
       CALL MapWholesaleOrder(InputRoot.XMLNSC.ns_ws:WholesaleOrder, outRef);
   ELSE
       THROW USER EXCEPTION MESSAGE 2951 VALUES('Unrecognised root element: ' || COALESCE(rootName, 'NULL'));
   END IF;
   ```

   Use separate procedures for each source format to keep modules readable.

   #### Boolean ↔ Y/N flag conversion

   ```sql
   -- Boolean → Y/N (writing XML for a backend)
   SET xmlRef.ns:IncludeOrderHistory = CASE WHEN inReq.includeOrders = TRUE THEN 'Y' ELSE 'N' END;

   -- Y/N → Boolean (writing JSON from XML)
   SET outRef.marketingConsent = (prefRef.ns:MarketingOptIn = 'Y');
   ```

   #### Empty string → JSON null

   XML empty elements (`<Line2></Line2>`) produce an empty string in ESQL. Convert to JSON null explicitly:

   ```sql
   DECLARE line2 CHARACTER addrRef.ns:Line2;
   SET outRef.address.line2 = CASE WHEN line2 = '' THEN NULL ELSE line2 END;
   ```

   #### Building JSON domain output

   To build a JSON body in OutputRoot:

   ```sql
   CREATE LASTCHILD OF OutputRoot DOMAIN 'JSON' NAME 'JSON';
   CREATE LASTCHILD OF OutputRoot.JSON NAME 'Data';
   CREATE LASTCHILD OF OutputRoot.JSON.Data NAME 'canonicalOrder';

   DECLARE outRef REFERENCE TO OutputRoot.JSON.Data.canonicalOrder;
   SET outRef.orderId = ...;
   ```

   For a flat structure a single `SET` to any leaf also creates the whole path automatically — no `CREATE` needed.

   #### Building XMLNSC output with namespace

   The first `SET` to a leaf creates the entire path including namespace-qualified elements. Declare the namespace alias, write the first field, then reposition a reference for subsequent fields:

   ```sql
   DECLARE ns_be NAMESPACE 'http://backend.example.com/customer/v3';
   DECLARE xmlRef REFERENCE TO OutputRoot;           -- placeholder

   SET OutputRoot.XMLNSC.ns_be:CustomerQuery.ns_be:Header.ns_be:CorrelationId = inReq.requestId;
   MOVE xmlRef TO OutputRoot.XMLNSC.ns_be:CustomerQuery;

   SET xmlRef.ns_be:Header.ns_be:SourceSystem = 'INTEGRATION_LAYER';
   SET xmlRef.ns_be:Header.ns_be:Timestamp    = CAST(CURRENT_TIMESTAMP AS CHARACTER FORMAT 'yyyy-MM-dd''T''HH:mm:ss''Z''');
   ```

   #### HTTP: forwarding LocalEnvironment to WSReply

   Any Compute node that feeds into a WSReply **MUST** copy `InputLocalEnvironment` first, otherwise WSReply cannot route the response back to the correct HTTP client:

   ```sql
   SET OutputLocalEnvironment = InputLocalEnvironment;
   SET OutputLocalEnvironment.Destination.HTTP.ReplyStatusCode = 500;
   ```

   #### DECLARE placement rules

   The placement of `DECLARE` statements depends on what is being declared:

   - **InputRoot references and simple variables** (`CHARACTER`, `INTEGER`, `DECIMAL`, etc.) — declare at the top of the `BEGIN...END` block, before any executable statement. The input message tree already exists so navigation is valid immediately.
   - **OutputRoot references** — MUST be declared *after* the output path has been created. Either a preceding `SET` that writes to that path, or a `CREATE LASTCHILD`, must run first to bring the tree element into existence. Declaring an OutputRoot reference before its path exists will fail.

   ```sql
   -- InputRoot ref and simple vars: at the top
   DECLARE inRef    REFERENCE TO InputRoot.XMLNSC.Order;
   DECLARE counter  INTEGER 0;

   -- First SET creates the output path
   SET OutputRoot.XMLNSC.Result.Header.Id = inRef.Id;

   -- OutputRoot ref: after the tree is created
   DECLARE outRef REFERENCE TO OutputRoot.XMLNSC.Result;
   SET outRef.Body.Field = inRef.Body;
   ```

   For loop-nested references, declare outside the loop and use `MOVE` to reposition inside:

   ```sql
   DECLARE itemIdx INTEGER 0;
   DECLARE varRef  REFERENCE TO src;  -- placeholder, repositioned inside loop

   DECLARE itemRef REFERENCE TO src.ns:Items.ns:Item[1];
   WHILE LASTMOVE(itemRef) DO
       SET itemIdx = itemIdx + 1;
       MOVE varRef TO itemRef.ns:Variants.ns:Variant[1];
       WHILE LASTMOVE(varRef) DO
           ...
           MOVE varRef NEXTSIBLING REPEAT NAME;
       END WHILE;
       MOVE itemRef NEXTSIBLING REPEAT NAME;
   END WHILE;
   ```

   #### EXTERNAL variable scope

   `DECLARE x EXTERNAL` variables declared at file scope are shared across **all ESQL files in the same ACE application**. A single declaration in one `.esql` file is sufficient — do not redeclare in every file that uses the variable.

   ```sql
   -- Declared once in any .esql file in the application:
   DECLARE systemEnv EXTERNAL CHARACTER 'DEV';
   ```

   #### Datetime format — always use `HH` (24-hour), never `hh` (12-hour)

   When building datetime strings with `CAST(... AS CHARACTER FORMAT '...')`, always use `HH` for hours. `hh` is 12-hour clock and produces silently wrong values for any time after noon.

   ```sql
   -- Correct: ISO 8601 via built-in format token
   SET outRef.CreationDateTime = CAST(CURRENT_TIMESTAMP AS CHARACTER FORMAT 'IU');

   -- Correct: manual ISO 8601 — note HH not hh
   SET outRef.Timestamp = CAST(CURRENT_TIMESTAMP AS CHARACTER FORMAT 'yyyy-MM-dd''T''HH:mm:ss''Z''');

   -- WRONG — hh is 12-hour; 13:05 becomes 01:05
   SET outRef.Timestamp = CAST(CURRENT_TIMESTAMP AS CHARACTER FORMAT 'yyyy-MM-dd''T''hh:mm:ss''Z''');
   ```

# Task Completion Checklist

Before completing the task, verify:
- [ ] All requested artifacts created
- [ ] All node xmi:type prefixes taken from the authoritative tables in this skill (not guessed or copied from example files)
- [ ] All Compute nodes have: descriptive snake_case label and `computeExpression` pointing to the correct ESQL module; `computeMode` set **only** if ESQL touches `OutputLocalEnvironment`/`OutputDestination`
- [ ] All ApplicationConnector Request nodes have: `schemaPrefix`, `policyUrl`, and empty `gen/` schema files created
- [ ] Node labels follow conventions (MQ=queue name, HTTP Input=URL path, HTTP Request=URL, Route=`route_desc`, Compute=snake_case verb)
- [ ] Every WSRequest node has: `httpVersion="1.1"`, `protocol="TLS"`, `messageDomainProperty` (not `responseDomainProperty`) — **verify by reading the msgflow XML, not by smoke-testing: the runtime does not enforce these and a non-conforming node still returns 200**
- [ ] Every File Input / File Output node has an **absolute** `inputDirectory` / `outputDirectory` (relative paths fail at startup with `BIP3333E`), and no path embedded in the node label
- [ ] Any Compute node that sets `OutputLocalEnvironment.Destination.File.Name` has `computeMode="destinationAndMessage"`, and reads the source filename from `InputLocalEnvironment.File.Name` (never `ComIbmFileInput.Response.FileName`, which silently resolves to NULL)
- [ ] Every MQ Input node that needs a specific parser uses `messageDomainProperty=` (not `messageDomain=`)
- [ ] No ESQL variable or parameter named `out` or `in` (reserved words)
- [ ] ESQL: uses `InputRoot.*[<]` (unquoted) not deprecated `InputRoot.*[LAST]`
- [ ] ESQL: no `CARDINALITY` inside loops; no consecutive Compute nodes without justification
- [ ] ESQL: datetime format strings use `HH` (24-hour) not `hh` (12-hour)
- [ ] ESQL: InputRoot/simple-variable DECLAREs are at the top; OutputRoot REFERENCEs are declared after their path is created
- [ ] Import instructions printed if any new ACE Toolkit Application project was created

