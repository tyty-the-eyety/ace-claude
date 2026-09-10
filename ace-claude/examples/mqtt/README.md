# MQTT publish / subscribe (runtime-proven)

Two flows. Workspace proof: `demo-apps/MQTT_DEMO_APP`, driven against Eclipse
Mosquitto 2 and Toolkit-validated (0 problems).

| File | Node | Role |
|---|---|---|
| `MQTT_PUB_MF` | `com_ibm_connector_mqtt_ComIbmOutput` | `WSInput /mqtt/publish → Compute → MQTT Output → WSReply` |
| `MQTT_SUB_MF` | `com_ibm_connector_mqtt_ComIbmEventInput` | `MQTT EventInput → Compute → FileOutput` |

Both carry `connectorName="MQTT"`.

## Everything is a node property — unlike MQ pub/sub

This is the clean contrast with `examples/mqpubsub/`. MQTT needs **no broker-side
objects at all**: topic, host, port and QoS live on the node.

```xml
<nodes xmi:type="com_ibm_connector_mqtt_ComIbmOutput.msgnode:FCMComposite_1" ...
       connectorName="MQTT" clientId="ace-mqtt-pub" topicName="ace/mqtt/orders"
       hostName="localhost" port="1883" qos="0" useSSL="false"/>

<nodes xmi:type="com_ibm_connector_mqtt_ComIbmEventInput.msgnode:FCMComposite_1" ...
       connectorName="MQTT" clientId="ace-mqtt-sub" topicName="ace/mqtt/orders"
       hostName="localhost" port="1883" qos="0" useSSL="false"
       messageDomainProperty="JSON"/>
```

Mandatory (`lowerBound="1"`): `clientId`, `topicName`, `hostName`, `port`
(default 1883), `qos`, `useSSL` (default false). `securityIdentity` is optional.

By comparison, MQ pub/sub has **no** topic property on any node — publishing needs
a `QALIAS` with `TARGTYPE(TOPIC)` and subscribing needs an administrative
`DEFINE SUB`. See `examples/mqpubsub/README.md`.

A `ComIbmMQTTPublishPolicyType` / `ComIbmMQTTSubscribePolicyType` exists in
`Policy.xsd` (same fields, plus `securityIdentity`) with credential type
`mqtt: --username --password`, if you would rather externalise the connection.
Not required for a plaintext broker.

## The namespace URI

```xml
xmlns:com_ibm_connector_mqtt_ComIbmOutput.msgnode="com/ibm/connector/mqtt/ComIbmOutput.msgnode"
```

The URI is **the node's path inside its Toolkit plugin jar** —
`tools/plugins/MQTTNodes_<version>.jar` stores these under
`com/ibm/connector/mqtt/`. Nodes stored at the top level of a jar (MQInput,
Compute, …) use just the filename as the URI. Get it wrong and the flow packages,
deploys and RUNS while the Toolkit reports `Message node "..." cannot be located`.

## Metadata

The subscriber receives `LocalEnvironment.MQTT.Input`:

```json
{"Topic":"ace/mqtt/orders","QualityOfService":0,"Duplicate":false,"Retained":false}
```

## Broker for testing

Mosquitto 2.x binds only to localhost inside the container unless configured, so
mount `mosquitto.conf` (supplied here):

```bash
docker run -d --name mosquitto -p 1883:1883 \
  -v $PWD:/mosquitto/config:ro eclipse-mosquitto:2
```

Verify independently of the flows, in both directions:

```bash
# ACE publishes -> this should print the message
docker exec mosquitto mosquitto_sub -h localhost -t "ace/mqtt/orders" -C 1

# publish externally -> the subscriber flow should write a file
docker exec mosquitto mosquitto_pub -h localhost -t "ace/mqtt/orders" \
  -m '{"orderId":"MQTT-B1","amount":56.78}'
ls /tmp/ace-mqtt/out/
```

## Note on the node names

The runtime registry (`server/adminservices/schemas/AdminServices.bir`) lists
`ComIbmMQTTPublishNodeType` and `ComIbmMQTTSubscribeNodeType`, but **no such
`.msgnode` definitions exist anywhere in the product** — those names are not
usable node types. The `com_ibm_connector_mqtt_*` nodes above are the real ones.

The FileOutput sink needs `computeMode="destinationAndMessage"` on the Compute
that sets `OutputLocalEnvironment.Destination.File.Name`.
