# MQTT_DEMO_APP — MQTT publish and subscribe

Two flows, **runtime-verified** against Eclipse Mosquitto 2 and
**Toolkit-validated** (`mqsicreatebar -cleanBuild`, 0 problems for this project).
Full reference: `ace-claude/examples/mqtt/`.

| Flow | Endpoint / trigger | Node |
|---|---|---|
| `MQTT_PUB_MF` | `POST /mqtt/publish` | `com_ibm_connector_mqtt_ComIbmOutput` |
| `MQTT_SUB_MF` | topic `ace/mqtt/orders` | `com_ibm_connector_mqtt_ComIbmEventInput` → FileOutput `/tmp/ace-mqtt/out` |

Topic, host, port and QoS are **node properties** — no broker-side objects and no
policy required. (Contrast `MQ_PUBSUB_APP`, where none of the nodes has a topic
property at all.)

## Setup

```bash
mkdir -p /tmp/mosq/config && cp mosquitto.conf /tmp/mosq/config/   # from examples/mqtt/
docker run -d --name mosquitto -p 1883:1883 \
  -v /tmp/mosq/config:/mosquitto/config:ro eclipse-mosquitto:2

mkdir -p /tmp/ace-mqtt/out

ibmint package --input-path <workspace> --output-bar-file bars/MQTT_DEMO_APP.bar \
  --project MQTT_DEMO_APP
ibmint deploy --input-bar-file bars/MQTT_DEMO_APP.bar --output-work-directory <work-dir>
```

Change `hostName`/`port`/`topicName` on the nodes if your broker differs.

## Driving it

```bash
# ACE publishes; watch it arrive with an independent subscriber
docker exec -d mosquitto sh -c 'mosquitto_sub -h localhost -t "ace/mqtt/orders" -C 1 > /tmp/got.txt'
curl -X POST http://localhost:7800/mqtt/publish -H 'Content-Type: application/json' \
  -d '{"orderId":"MQTT-A1","amount":12.34}'
docker exec mosquitto cat /tmp/got.txt

# publish externally; the subscriber flow writes a file
docker exec mosquitto mosquitto_pub -h localhost -t "ace/mqtt/orders" \
  -m '{"orderId":"MQTT-B1","amount":56.78}'
ls /tmp/ace-mqtt/out/
```

Each written file contains the payload plus `LocalEnvironment.MQTT.Input`
(`Topic`, `QualityOfService`, `Duplicate`, `Retained`).
