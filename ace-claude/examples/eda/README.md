# EDA node examples — Aggregation, Collector, Resequence (all runtime-proven, ACE 13.0.2.2 + MQ 9.4.0.5)

Copy-ready flows for the three MQ-backed event-driven-architecture node families.
Read the matching `LEARNINGS.md` section ("EDA nodes") before mutating these —
each file encodes at least one non-obvious convention that is NOT in the XSD or
IBM docs, and getting them wrong produces deep Java/XPath errors, not clear
config messages.

## Environment prerequisites (both mandatory, one-time)

1. `defaultQueueManager: '<QM>'` in the server's `server.conf.yaml` (or
   `overrides/`). Per-node MQEndpoint policies alone are NOT sufficient for
   these nodes — they store state "on the queue manager specified on the
   integration node" (BIP2685E without it).
2. The EDA system queues on that QM. Run the shipped script:
   `runmqsc <QM> < <ACE_INSTALL>/server/sample/wmq/iib_queues_create.mqsc`
   (or at minimum the `SYSTEM.BROKER.AGGR.*`, `SYSTEM.BROKER.SEQ.*`,
   `SYSTEM.BROKER.EDA.*`, `SYSTEM.BROKER.TIMEOUT.QUEUE` definitions).
   A manually created queue manager has none of these.

## MQ_RESEQUENCE_MF.msgflow
`MQInput → ReSequence → MQOutput`. Reorders messages by `$Body/Msg/SeqNum`.
- `startOfSequence="01"` / `endOfSequence="03"` — **mode-encoded**: first char
  0=literal, 1=predicate, 2=automatic; remainder is the value. `"1"` alone is
  predicate-mode-with-empty-XPath and crashes every message (BIP4390W).
- Smoke test: put SeqNum 3,1,2 → output queue holds 1,2,3.

## MQ_AGGREGATION_MF.msgflow + MQ_AGGREGATION_BACKEND_MF.msgflow (one app)
Classic async scatter/gather:
```
MQInput → AggregateControl ─out→ Tag → MQOutput(leg request, replyToQ, newMsgId=true) → AggregateRequest
                           ─out→ (second leg, same shape)
                           ─control→ AggregateReply.control
MQInput(leg reply queue) ×2 → AggregateReply.in → BuildCombined (Compute) → MQOutput
```
- **AggregateRequest sits AFTER the MQOutput** — it registers the pending reply
  from `LocalEnvironment.WrittenDestination` (BIP4428E if wired before).
- Replies must arrive via separate MQInput nodes; the backend must set
  `MQMD.CorrelId = request MQMD.MsgId` (see the BACKEND flow, which simulates
  two such responders).
- `AggregateReply` output is a `ComIbmAggregateReplyBody` tree (one folder per
  `folderName`, each a full message tree) with **no wire format** — always map
  it in a Compute (`BuildCombined`) before an output node.
- Aggregation policy attaches via `aggregateName="{proj}:policy"` on BOTH
  AggregateControl and AggregateReply.
- Smoke test: put `<Msg><OrderId>1001</OrderId></Msg>` → one combined message
  with both legs' results.

## MQ_COLLECTOR_MF.msgflow
`MQInput ×2 → Collector(dynamic terminals legA/legB, quantity 1 each) → Compute → MQOutput`.
- Dynamic input terminals:
  `<inTerminals terminalNodeID="InTerminal.legA" dynamic="true" label="legA"/>`,
  connections target `InTerminal.legA`.
- Event handler table rows are **repeated elements named after the table**
  (same convention as Route's `filterTable`):
  `<eventHandlerPropertyTable terminal="legA" quantity="1" timeout="0"/>`.
  The nested row-element form from the XSD parses to an empty table → NPE per
  message.
- Output tree is `InputRoot.Collection` (children: `CollectionName` + one
  folder per terminal label) — map it in a Compute before the output node.
- Collector policy attaches via `configurableService="{proj}:policy"`.
- Smoke test: put one message on each input queue → one combined message.
