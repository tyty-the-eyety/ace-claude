# Timer-family examples — Scheduler, TimeoutNotification, TimeoutControl (all runtime-proven, ACE 13.0.2.2 + MQ 9.4.0.5)

Copy-ready flows for the three timer nodes. Read `../../Timer.md` and the
`LEARNINGS.md` section "Timer / Scheduler nodes" before mutating these — the
controlled-mode pair in particular has three requirements that no product schema
states, and getting them wrong gives you a flow that deploys, starts, fires on
time, and silently loses the message.

Proof app: `TIMER_DEMO_APP`.

## Environment prerequisites

Only the **controlled-mode** pair (`TIMER_CONTROL_MF` + `TIMER_FIRE_MF` +
`TIMER_CANCEL_MF`) needs anything beyond a plain server:

1. `defaultQueueManager: '<QM>'` in `server.conf.yaml` — without it both flows
   refuse to start with `BIP2685E`, despite `MessageFlow.xsd` annotating the
   nodes `requiresMQ="false"`.
2. `SYSTEM.BROKER.TIMEOUT.QUEUE` on that queue manager
   (`runmqsc <QM> < <ACE_INSTALL>/server/sample/wmq/iib_queues_create.mqsc`).
   **A standalone integration server provisions nothing** — the `SYSTEM.BROKER.*`
   queues come from creating an *integration node* against a queue manager, so on
   a hand-created queue manager you must run that script yourself. (The one
   exception is a *prefixed* timeout queue — see `TIMER_POLICY_TEMPLATE/` below.)

`TIMER_SCHED_INTERVAL_MF`, `TIMER_SCHED_CRON_MF` and `TIMER_AUTO_MF` need
neither.

---

## TIMER_SCHED_INTERVAL_MF.msgflow — Scheduler, fixed interval

`Scheduler → Trace(file)`. Ticks every 10 seconds.

- **`scheduleType="interval"`.** The Toolkit `.msgnode` declares the enum
  literal as `repeatInterval` and the runtime rejects that value
  (`BIP5064E ... invalid schedule type 'repeatinterval'`). `MessageFlow.xsd` is
  the one that is right here.
- `unit` is `second` | `minute` | `hour`; `interval` is the count.
- `runOnceOnCheck="true"` fires once immediately at flow start — useful for a
  proof, usually wrong in production.
- Output: `Root.JSON.Data` **and** `LocalEnvironment.Scheduler`, both holding
  `lastEventTime`, `currentEventTime`, `scheduleIdentifier`. On the first fire
  after a start, `lastEventTime` is the epoch.

Observed: ticks at 08:22:53, :03, :13, :23 — exactly 10s apart.

## TIMER_SCHED_CRON_MF.msgflow — Scheduler, calendar

Same shape with `scheduleType="calendar"` and `cronExp`.

**ACE's cron is neither Unix cron nor Quartz.** Five fields,
`<minute> <hour> <day-of-month> <month> <day-of-week>`, and only literals:

| Field | Accepts |
|---|---|
| minute | a literal `0`–`59` **only** — `*` is rejected |
| hour | a literal `0`–`23`, or `*` |
| day-of-month | a literal `1`–`31`, or `*` |
| month | `*` only |
| day-of-week | `*` only — use the `days` property |

No lists (`00,30`), no ranges (`00-05`), no steps (`*/5`), no names (`MON`,
`JUN`), no 6-field Quartz form, no `?`. A bad expression fails at flow start
with `BIP5063E`. Full probe matrix in `../../Timer.md`.

**The finest calendar granularity is hourly** — anything sub-hourly needs
`scheduleType="interval"`.

**`days` does not work in calendar mode.** It is honoured in `interval` mode
(`days="SAT,SUN"` stayed silent through a Friday) and ignored in `calendar`
mode — four calendar flows with `days` of `FRI`, `SAT,SUN`, `SUN` and all-seven
all fired at the same minute on a Friday. Since day-of-week must also be `*` in
the expression, a calendar schedule cannot be restricted to weekdays at all:
filter on `EXTRACT(DAYOFWEEK FROM CURRENT_DATE)` in the flow.

**Give each calendar Scheduler its own minute.** One calendar flow owning its
minute fires exactly once at `HH:MM:00.000`. Two sharing a minute make at least
one of them fire twice, the extra stamped `HH:MM-1:59.999`; with four sharing a
minute the counts ran 2, 2, 4, 4 and varied between runs.

Observed: `cronExp="40 * * * *"` fired at 08:40:00.000.

## TIMER_AUTO_MF.msgflow — TimeoutNotification, automatic mode

`TimeoutNotification → Trace(file)`. Ticks every `timeoutInterval` seconds with
no MQ and no partner node — the pre-ACE-12 way of doing what the Scheduler's
interval mode now does.

- `uniqueIdentifier` is required (≤ 12 chars) even though nothing pairs with it.
  Set it to `{POLICY_PROJECT}:PolicyName` to attach a Timer policy, whose
  `timeoutInterval` then overrides the node's.
- The node has **no input terminal** in either mode.
- Output: an **empty message** — `Root` has only `Properties`, no body parser.
  The tick metadata is in `LocalEnvironment.TimeoutRequest` (`Identifier` is the
  node's `uniqueIdentifier`, `Interval` is its `timeoutInterval`).

## TIMER_CONTROL_MF.msgflow + .esql — TimeoutControl, SET

`HTTPInput(/timer/delay) → Compute(build request) → TimeoutControl → Compute(ack) → HTTPReply`.

POST `{"requestId":"ORDER-42","delaySeconds":15,"payload":"release held order"}`
and the message is stored and replayed 15 seconds later by `TIMER_FIRE_MF`.

Three things this file encodes that nothing documents:

1. **`computeMode="destinationAndMessage"` on the Compute that writes the
   request.** The LocalEnvironment compute mode is spelled `destination`; there
   is no `localEnvironment*` literal. An invalid value is accepted silently by
   `ibmint package` **and** by the runtime, which falls back to message-only
   propagation — the symptom is `BIP4601E ... failed to navigate to the message
   location specified ... 'InputLocalEnvironment.TimeoutRequest'`.

2. **The stored message must carry an `MQMD`.** The timeout store is an MQ
   queue and the notification node parses what it reads as an MQ message. With
   no `OutputRoot.MQMD`, the timeout still fires on time but the notification
   flow dies on an empty bitstream (`BIP6105E` + `BIP5902W "Null Buffer"`).
   Three lines fix it:
   ```sql
   SET OutputRoot.MQMD.Version        = 2;
   SET OutputRoot.MQMD.Format         = 'MQSTR';
   SET OutputRoot.MQMD.CodedCharSetId = 1208;
   ```

3. **`StartDate`/`StartTime` are absolute, and `Interval` is not a delay.**
   `'TODAY'`/`'NOW'` fires *immediately* whatever `Interval` says. To delay by N
   seconds, compute the instant:
   ```sql
   DECLARE fireAt TIMESTAMP CURRENT_TIMESTAMP + CAST(delaySecs AS INTERVAL SECOND);
   SET OutputLocalEnvironment.TimeoutRequest.StartDate = CAST(fireAt AS CHARACTER FORMAT 'yyyy-MM-dd');
   SET OutputLocalEnvironment.TimeoutRequest.StartTime = CAST(fireAt AS CHARACTER FORMAT 'HH:mm:ss');
   ```

Also note `storedMessageLocation` is left **blank** on the node: blank stores
and replays the whole message unchanged, while `InputRoot` nests the body one
level deeper (`Root.JSON.Data.Data.*`). It is a Field-Reference, so `$Body` is
rejected at flow start with `BIP2211E`.

The second Compute (`build_ack`) exists because the reply is downstream of
TimeoutControl — without it the MQMD you just added is serialized into the HTTP
response as binary.

Observed: request 08:35:30 → fired 08:35:45, payload intact.

## TIMER_FIRE_MF.msgflow — TimeoutNotification, controlled mode

`TimeoutNotification(controlled) → Trace(file)`. Paired to TimeoutControl by
**`uniqueIdentifier` — identical on both nodes** (`DELAYDEMO` here). The
`Identifier` inside the timeout request is a different thing: it names one
pending request within that pair.

## TIMER_CANCEL_MF.msgflow + .esql — TimeoutControl, CANCEL

Same shape as the SET flow. A cancel needs only two fields, on a TimeoutControl
node carrying the same `uniqueIdentifier` as the one that set it:

```sql
SET OutputLocalEnvironment.TimeoutRequest.Action     = 'CANCEL';
SET OutputLocalEnvironment.TimeoutRequest.Identifier = reqId;
```

## TIMER_POLICY_MF.msgflow — attaching the Timer policy

`TimeoutNotification(automatic) → Trace(file)`, identical to `TIMER_AUTO_MF`
except that `uniqueIdentifier` names a policy instead of a plain string:

```xml
uniqueIdentifier="{TIMER_POLICY_TEMPLATE}:TickPrefixed"
```

The node still declares `timeoutInterval="30"`, and the policy's value wins —
observed ticking every 3s, not 30s. That is the whole point of the file: the node
property becomes a fallback once a policy is attached.

## TIMER_POLICY_TEMPLATE/ — the Timer policy

A copy-ready policy project with the two variants that matter. The Timer policy
attaches to **TimeoutControl and TimeoutNotification only** — the Scheduler node
has no policy link at all.

| File | `queuePrefix` | `timeoutInterval` |
|---|---|---|
| `TickPlain.policyxml` | empty → `SYSTEM.BROKER.TIMEOUT.QUEUE` | 3 |
| `TickPrefixed.policyxml` | `test` → `SYSTEM.BROKER.TIMEOUT.test.QUEUE` | 3 |

Attach by putting the policy reference in `uniqueIdentifier`, on either mode and
on both halves of a controlled pair:

```xml
uniqueIdentifier="{TIMER_POLICY_TEMPLATE}:TickPrefixed"
```

- `timeoutInterval` **overrides the node's own** — node `30` + policy `3` gave
  3s ticks.
- `queuePrefix` is an **infix**, not a prefix: the value lands before `.QUEUE`,
  and it is case-sensitive (`runmqsc` needs the name quoted or it uppercases it).
- **ACE creates the prefixed queue itself at flow start** —
  `DEFPSIST(YES) MAXDEPTH(100000) MAXMSGL(104857600)`, versus the
  `NO / 5000 / 4194304` a bare create would inherit from
  `SYSTEM.DEFAULT.LOCAL.QUEUE`, so it is a deliberate `DEFINE`. This is the one
  `SYSTEM.BROKER.*` queue a standalone server provisions for you — reasonably,
  since nothing could pre-create a queue whose name you invent in a policy. The
  un-prefixed `SYSTEM.BROKER.TIMEOUT.QUEUE` still has to come from the shipped
  script. Use a prefix to give one application its own timeout store instead of
  sharing the default queue.
- The policy does **not** change the MQ requirement, despite
  `requiresMQ="true"` on the policy type in `Policy.xsd`: with no
  `defaultQueueManager`, all three automatic-mode flows still started and ticked
  (prefixed ones included) while both controlled flows failed with `BIP2685E`.
  `operationMode` alone decides.
- `policyTemplate="Timer"` is what the Toolkit writes; `""` and omitting it also
  work.

Rename the project directory and its `.project` `<name>` before use — the
reference in `uniqueIdentifier` must match the real project name.

## Trace nodes in these examples

The examples log with `ComIbmTrace` (`destination="file"`, `filePath`,
`pattern`) rather than FileOutput, because a Trace node appends immediately
while FileOutput buffers records in `<outputDirectory>/mqsitransit/` until a
Finish File criterion fires.

`pattern` takes **ESQL field references with dots** — `${LocalEnvironment.Scheduler.currentEventTime}`.
An XPath-style `${LocalEnvironment/Scheduler/currentEventTime}` stops the flow
starting with `BIP2432E: The correlation name 'Scheduler' is not valid`.
