# Kafka nodes — Producer, Consumer and Read (runtime-proven)

Copy-ready flows for ACE's built-in Kafka nodes. Workspace proof:
`demo-apps/KAFKA_DEMO_APP`, driven against Apache Kafka 4.3.1.

| File | Node type | Shape |
|---|---|---|
| `KAFKA_PRODUCE_MF` | `com_ibm_connector_kafka_ComIbmOutput` | `WSInput → Compute → Producer → Compute → WSReply`, connection from a **policy** |
| `KAFKA_CONSUME_MF` | `com_ibm_connector_kafka_ComIbmEventInput` | `Consumer → Compute → FileOutput`, connection **inline** on the node |
| `KAFKA_READ_MF` | `com_ibm_connector_kafka_ComIbmRequest` | `WSInput → Request → Compute → WSReply`, with the `noMatch` path wired |

All three carry `connectorName="Kafka"`.

`LocalKafka.policyxml` is PLAINTEXT; `LocalKafkaSasl.policyxml` is SASL_PLAINTEXT
with a vault credential. Both are runtime-proven.

## Use the connector family, not the legacy nodes

ACE has **two** Kafka node families:

| Use this (what the Toolkit palette gives you) | Avoid |
|---|---|
| `com_ibm_connector_kafka_ComIbmOutput` | `ComIbmKafkaProducer` |
| `com_ibm_connector_kafka_ComIbmEventInput` | `ComIbmKafkaConsumer` |
| `com_ibm_connector_kafka_ComIbmRequest` | `ComIbmKafkaRead` |

Both are registered in the runtime and **both actually run**. The difference is
that the legacy `ComIbmKafka*` nodes **do not build in the ACE Toolkit**, so a
flow using them can be deployed but not maintained by anyone who opens it in the
IDE. The connector versions here validate clean —
`mqsicreatebar -cleanBuild` returns 0 problem markers.

Both families share `connectorkafka.jar`, so property names and LocalEnvironment
paths are the same either way.

### The namespace URI is a path — this is the part that bites

```xml
xmlns:com_ibm_connector_kafka_ComIbmEventInput.msgnode="com/ibm/connector/kafka/ComIbmEventInput.msgnode"
```

Underscores in the prefix, **slashes in the URI** — because the URI is the node's
**path inside its Toolkit plugin jar**. Kafka's definitions live at
`com/ibm/connector/kafka/…` inside
`tools/plugins/com.ibm.etools.mft.ibmnodes.definitions_<v>.jar`, while MQInput and
Compute sit at the jar's top level and therefore use a bare filename as their URI.
To find any node's URI, locate its `.msgnode` in `<ACE>/tools/plugins/*.jar` and
use the archive-internal path.

The failure is silent where it matters: the flow packages, deploys and runs
perfectly, while the Toolkit shows `Message node "..." cannot be located` and
refuses to open it properly. Copy the declarations from these examples verbatim.

Also note `bootstrapServers` is a **mandatory node property even when a policy
supplies it** — omit it and the Toolkit flags `Unset mandatory property
"Bootstrap servers"` although the flow runs.

If you do use a name that isn't registered at all, the BAR still packages and
deploys and the flow fails only at startup:

```
BIP2241E: A Loadable Implementation Library (.lil, .jar, or .par) is not found
for message flow node type '<name>Node'
```

## `notFoundAction` on KafkaRead is mandatory

No usable default. Omit it and the flow will not start:

```
BIP3882E: The value 'NULL' supplied for property 'notFoundAction' to the Kafka connector is invalid
```

Valid values: `latest`, `earliest`, `exception`, and **`no match`** — with a
space in it. The connector's own trace calls `no match` the default while still
rejecting NULL. A miss is propagated to terminal `OutTerminal.noMatch`, which
`KAFKA_READ_MF` wires to a "not-found" reply.

## Metadata: different subtree per node, all CHARACTER

| Node | LocalEnvironment path |
|---|---|
| KafkaConsumer | `LocalEnvironment.Kafka.Input` |
| KafkaRead | `LocalEnvironment.Kafka.Read` |

Both carry `topicName`, `partition`, `offset`. All are **CHARACTER, not
INTEGER** — cast before arithmetic.

## Connection: inline or policy

Both work and both are proven. Inline is fine for a demo; a policy is what you
want in practice, since it moves environment-specific detail out of the flow:

```xml
policyUrl="{KAFKA_DEMO_POLICIES}:LocalKafka"
```

Policy attributes come from `common/schemas/Policy/Policy.xsd`
(`ComIbmKafkaPolicyType`): `bootstrapServers`, `securityProtocol` and
`sslProtocol` are required; `saslMechanism` and `securityIdentity` are optional.

For SASL, use `LocalKafkaSasl.policyxml` and store the credential:

```bash
mqsicredentials --work-dir <work-dir> --create --vault-key <key> \
  --credential-type kafka --credential-name KafkaSasl \
  --username <user> --password <pass>
```

`securityIdentity` in the policy is that credential **name**.

## Two traps that cost real time

**`initialOffset` is `auto.offset.reset`, and defaults to `latest`.** A message
published while the consumer group is still rebalancing just after server start is
never delivered. It looks exactly like a broken flow. Set
`initialOffset="earliest"` when testing, or publish again and wait.

**Writing the FileOutput filename needs `computeMode="destinationAndMessage"`.**
With `localEnvironmentAndMessage` the OutputLocalEnvironment is silently
discarded and FileOutput fails with `BIP3325E ... for file name ''`. FileOutput
has no `fileName` attribute either — setting one is ignored.

## Broker for testing

```bash
docker run -d --name kafka -p 9092:9092 apache/kafka:latest
docker exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 \
  --create --topic ace.demo --partitions 1 --replication-factor 1
```

Verify flows with an independent console consumer rather than trusting the
flow's own reply:

```bash
docker run --rm --network host apache/kafka:latest \
  /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 \
  --topic ace.demo --from-beginning --max-messages 10 --group verify-1
```
