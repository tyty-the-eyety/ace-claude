# MQ publish / subscribe (runtime-proven)

Two publishers and one subscriber. Workspace proof: `demo-apps/MQ_PUBSUB_APP`,
driven against IBM MQ 9.4 and Toolkit-validated (0 problems).

| File | Role | How it reaches the topic |
|---|---|---|
| `MQ_PUB_OUTPUT_MF` | publisher | `MQOutput` → **QALIAS with `TARGTYPE(TOPIC)`** |
| `MQ_PUB_PUBLICATION_MF` | publisher | `ComIbmPublication`, topic from `Properties.Topic` |
| `MQ_SUB_MF` | subscriber | `MQInput` on the queue an **admin subscription** delivers to |

`mq-objects.mqsc` defines everything needed; apply with
`runmqsc <QMGR> < mq-objects.mqsc`.

## The key fact: pub/sub is configured in MQ, not on the nodes

Read from the node definitions themselves
(`tools/plugins/com.ibm.etools.mft.ibmnodes.definitions_<version>.jar`):

- **`MQOutput` has no topic attribute** — only `queueName`, `destinationMode`
  (default `fixed`), `transactionMode`/`persistenceMode` (default `automatic`).
- **`MQInput` has no subscription attribute** — `queueName` is mandatory and the
  only topic-ish property is `topicProperty`, a plain string. It cannot subscribe.
- **`ComIbmPublication` has no topic attribute either** — its properties are
  connection/SSL plus `subscriptionPoint`.

So all three routes go through MQ objects:

**Publishing with MQOutput** — point it at an alias queue whose target is a topic
object. Nothing in the flow mentions a topic:

```
DEFINE QALIAS(ACE.DEMO.PUB.ALIAS) TARGTYPE(TOPIC) TARGET(ACE.DEMO.TOPIC)
```
```xml
<nodes xmi:type="ComIbmMQOutput.msgnode:FCMComposite_1" ... queueName="ACE.DEMO.PUB.ALIAS"/>
```

**Publishing with the Publication node** — set the topic on the message:

```sql
SET OutputRoot.Properties.Topic = 'ace/demo/orders';
```

**Subscribing** — an administrative subscription delivers to a queue, and
`MQInput` reads that queue:

```
DEFINE SUB(ACE.DEMO.SUB) TOPICOBJ(ACE.DEMO.TOPIC) DEST(ACE.PUBSUB.SUBQ)
```
```xml
<nodes xmi:type="ComIbmMQInput.msgnode:FCMComposite_1" ... queueName="ACE.PUBSUB.SUBQ"/>
```

## Durability is not a choice here

**Administrative subscriptions are durable by definition.** Publications
accumulate on the destination queue while the subscriber flow is stopped. A
non-durable subscription can only be created by an application at runtime, and no
built-in ACE node does that — `MQInput` has no subscription properties at all.

If you need non-durable behaviour, delete and recreate the `SUB` around the test,
or use a connector input node (MQTT/Kafka) instead.

## Verify independently of the flows

Prove the MQ plumbing before blaming ACE:

```bash
echo "hello" | amqsput ACE.DEMO.PUB.ALIAS <QMGR>
echo "DISPLAY QLOCAL(ACE.PUBSUB.SUBQ) CURDEPTH" | runmqsc <QMGR>
amqsget ACE.PUBSUB.SUBQ <QMGR>
```

Then drive the flows and read the subscriber's output queue:

```bash
curl -X POST http://localhost:7800/mqpub/output -H 'Content-Type: application/json' \
  -d '{"orderId":"ORD-A1","amount":10.5}'
curl -X POST http://localhost:7800/mqpub/publication -H 'Content-Type: application/json' \
  -d '{"orderId":"ORD-B1","amount":20.5}'
amqsget ACE.PUBSUB.OUT <QMGR>
```

Both publishers land on the same subscription, so the subscriber output carries a
`publisher` field identifying which route produced it.

## Teardown

```
DELETE SUB(ACE.DEMO.SUB)
DELETE QALIAS(ACE.DEMO.PUB.ALIAS)
DELETE QLOCAL(ACE.PUBSUB.SUBQ)
DELETE QLOCAL(ACE.PUBSUB.OUT)
DELETE TOPIC(ACE.DEMO.TOPIC)
```
