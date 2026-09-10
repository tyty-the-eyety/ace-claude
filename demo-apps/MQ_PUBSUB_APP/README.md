# MQ_PUBSUB_APP — MQ publish/subscribe, both publish routes

Three flows: two publishers reaching the same topic by different means, and one
subscriber. **All runtime-verified** against IBM MQ 9.4 and **Toolkit-validated**
(`mqsicreatebar -cleanBuild`, 0 problems for this project).

| Flow | Endpoint / trigger | Node | Route to the topic |
|---|---|---|---|
| `MQ_PUB_OUTPUT_MF` | `POST /mqpub/output` | `ComIbmMQOutput` | QALIAS `ACE.DEMO.PUB.ALIAS` with `TARGTYPE(TOPIC)` |
| `MQ_PUB_PUBLICATION_MF` | `POST /mqpub/publication` | `ComIbmPublication` | `Properties.Topic` set in ESQL |
| `MQ_SUB_MF` | queue `ACE.PUBSUB.SUBQ` | `ComIbmMQInput` → `ComIbmMQOutput` | admin `SUB` delivers here; output to `ACE.PUBSUB.OUT` |

## Setup

```bash
runmqsc <QMGR> < mq-objects.mqsc        # topic, queues, alias, subscription

ibmint package --input-path <workspace> --output-bar-file bars/MQ_PUBSUB_APP.bar \
  --project MQ_PUBSUB_APP
ibmint deploy --input-bar-file bars/MQ_PUBSUB_APP.bar --output-work-directory <work-dir>
```

The integration server needs a `defaultQueueManager` in `server.conf.yaml`.

## Driving it

```bash
curl -X POST http://localhost:7800/mqpub/output -H 'Content-Type: application/json' \
  -d '{"orderId":"ORD-A1","amount":10.5}'

curl -X POST http://localhost:7800/mqpub/publication -H 'Content-Type: application/json' \
  -d '{"orderId":"ORD-B1","amount":20.5}'

amqsget ACE.PUBSUB.OUT <QMGR>     # both should appear, tagged by publisher
```

## Worth knowing before you copy this

**None of the three nodes has a topic or subscription property.** MQOutput has
only `queueName`; MQInput requires `queueName` and cannot subscribe; the
Publication node takes its topic from the message. Pub/sub is therefore
configured in MQ, not in the flow — see `ace-claude/examples/mqpubsub/README.md`.

**The subscription is durable and cannot be otherwise.** Admin subscriptions are
durable by definition, so publications queue up on `ACE.PUBSUB.SUBQ` while the
subscriber is stopped.
