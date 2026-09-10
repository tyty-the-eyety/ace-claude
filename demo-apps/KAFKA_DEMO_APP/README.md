# KAFKA_DEMO_APP — Kafka producer, consumer and read

Three flows exercising ACE's Kafka nodes. **All runtime-verified** against Apache
Kafka 4.3.1, and **Toolkit-validated** (`mqsicreatebar -cleanBuild`, 0 markers).
Full reference: `ace-claude/examples/kafka/`.

These use the **connector-family** node types (`connectorName="Kafka"`) — what the
ACE Toolkit palette provides. ACE also has a legacy family
(`ComIbmKafkaProducer`/`Consumer`/`Read`) which runs but **does not build in the
Toolkit**; do not use it.

| Flow | Endpoint / trigger | Node type | Connection |
|---|---|---|---|
| `KAFKA_PRODUCE_MF` | `POST /kafka/publish` | `com_ibm_connector_kafka_ComIbmOutput` | policy `{KAFKA_DEMO_POLICIES}:LocalKafka` |
| `KAFKA_CONSUME_MF` | topic `ace.demo` | `com_ibm_connector_kafka_ComIbmEventInput` | inline on the node |
| `KAFKA_READ_MF` | `POST /kafka/read` | `com_ibm_connector_kafka_ComIbmRequest` | inline, partition 0 offset 0 |

`KAFKA_DEMO_POLICIES` ships two policies: `LocalKafka` (PLAINTEXT) and
`LocalKafkaSasl` (SASL_PLAINTEXT + vault credential). Both runtime-proven.

## Setup

```bash
docker run -d --name kafka -p 9092:9092 apache/kafka:latest
docker exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 \
  --create --topic ace.demo --partitions 1 --replication-factor 1

mkdir -p /tmp/ace-kafka/out      # the consumer flow writes one file per message

ibmint package --input-path <workspace> --output-bar-file bars/KAFKA_DEMO_APP.bar \
  --project KAFKA_DEMO_APP --project KAFKA_DEMO_POLICIES
ibmint deploy --input-bar-file bars/KAFKA_DEMO_APP.bar --output-work-directory <work-dir>
```

For SASL, add the credential and point the producer at `LocalKafkaSasl`:

```bash
mqsicredentials --work-dir <work-dir> --create --vault-key <key> \
  --credential-type kafka --credential-name KafkaSasl --username <u> --password <p>
```

## Driving it

```bash
curl -X POST http://localhost:7800/kafka/publish -H 'Content-Type: application/json' \
  -d '{"orderId":"ORD-1001","amount":42.5}'

ls /tmp/ace-kafka/out/          # kafka-p<partition>-o<offset>.json, one per message

curl -X POST http://localhost:7800/kafka/read -H 'Content-Type: application/json' -d '{}'
```

Verify independently rather than trusting the flow's own reply:

```bash
docker run --rm --network host apache/kafka:latest \
  /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 \
  --topic ace.demo --from-beginning --max-messages 10 --group verify-1
```

## Expect this on a fresh broker

`initialOffset` (i.e. `auto.offset.reset`) defaults to `latest`. These flows set
`earliest`, but with the default the **first** message published right after server
start is often not delivered — the consumer group is still rebalancing. It looks
like a broken flow and isn't.

See `ace-claude/examples/kafka/README.md` for the node-level gotchas: the two node
families, the mandatory `notFoundAction` (one of whose valid values is `no match`,
with a space), the per-node metadata subtrees, and the `computeMode` the file sink
requires.
