# Timer and Scheduler nodes (ACE 13.0.2.2)

Runtime-proven on 2026-09-11 against a standalone IntegrationServer (work dir
`/path/to/timer-test`, MQ 9.4.0.5 queue manager `TEST_QM`), except
where a section says otherwise. Worked example: `examples/timer/`, proof app
`TIMER_DEMO_APP`.

ACE has three timer-family nodes, all implemented by `server/lil/imbtimer.lil`
and all gated by the runtime component `Timer`:

| Node | `xmi:type` | Kind | Introduced |
|---|---|---|---|
| Timeout Notification | `ComIbmTimeoutNotification.msgnode` | input | 6.0.0.0 |
| Timeout Control | `ComIbmTimeoutControl.msgnode` | mid-flow | 6.0.0.0 |
| Scheduler | `ComIbmScheduler.msgnode` | input | 12.0.9.0 |

## Which one to use

| You want | Use | Needs MQ |
|---|---|---|
| "Run this flow every N seconds/minutes" | Scheduler, `scheduleType="interval"` | no |
| "Run this flow at 09:30 every day" | Scheduler, `scheduleType="calendar"` | no |
| "…every *weekday*" | Scheduler calendar **+ a day filter in the flow** — see `days` below | no |
| "Run this flow every N seconds", pre-ACE-12 style | TimeoutNotification, `operationMode="automatic"` | no |
| "Hold THIS message and replay it later" | TimeoutControl + TimeoutNotification `operationMode="controlled"` | **yes** |

The Scheduler node supersedes automatic-mode TimeoutNotification for plain
heartbeats — it needs no unique identifier, it carries the tick times in the
message, and its calendar mode covers times of day. Controlled mode has no
Scheduler equivalent: storing and replaying a real message is still
TimeoutControl's job.

---

## MQ prerequisite (controlled mode only)

`MessageFlow.xsd` annotates all three nodes `requiresMQ="false"`. That is true
for the Scheduler and for **automatic**-mode TimeoutNotification, and **false
for controlled mode** — the timeout store is an MQ queue. Without a queue
manager both flows refuse to start:

```
BIP2685E: The node of type 'TimeoutControl' named 'set_timeout' requires
          a queue manager to be specified on the integration node.
```

Fix — in the work dir's `server.conf.yaml`:

```yaml
defaultQueueManager: 'TEST_QM'
```

…and make sure `SYSTEM.BROKER.TIMEOUT.QUEUE` exists on that queue manager.

**You have to create it yourself here.** The `SYSTEM.BROKER.*` queues are
provisioned when an *integration node* is created against a queue manager
(the `mqsicreatebroker` lineage). A **standalone integration server** provisions
nothing — it just attaches to whatever queue manager you point it at — so on a
hand-created queue manager those queues simply do not exist. That is exactly
what the shipped script is for:

```bash
runmqsc <QM> < /opt/IBM/ace-13.0.2.2/server/sample/wmq/iib_queues_create.mqsc
# DEFINE QLOCAL('SYSTEM.BROKER.TIMEOUT.QUEUE') DEFPSIST(NO) MAXDEPTH(100000) MAXMSGL(104857600)
```

Corroborated here: `TEST_QM` was created by hand, and
`SYSTEM.BROKER.TIMEOUT.QUEUE` carries `CRDATE(2026-07-14)` — the day that script
was run for the EDA work, not the day the queue manager was made.

The one exception is a **prefixed** timeout queue, which the runtime does create
on demand — see [section 4](#4-the-timer-policy).

---

## 1. TimeoutNotification — automatic mode

Fires on its own clock, no MQ, no partner node.

```xml
<nodes xmi:type="ComIbmTimeoutNotification.msgnode:FCMComposite_1"
       xmi:id="FCMComposite_1_1" location="100,140"
       uniqueIdentifier="AUTOTICK" operationMode="automatic"
       timeoutInterval="5" transactionMode="no">
  <translation xmi:type="utility:ConstantString" string="every_5_seconds"/>
</nodes>
```

| Property | Values | Notes |
|---|---|---|
| `uniqueIdentifier` | ≤ 12 chars, **required** | also the Timer-policy attach point |
| `operationMode` | `automatic` \| `controlled` | default `automatic` |
| `timeoutInterval` | seconds, default `1` | ignored when a Timer policy overrides it |
| `transactionMode` | `yes` \| `no` \| `automatic` | default `yes` |

Terminals: `OutTerminal.out`, `OutTerminal.catch`, `OutTerminal.failure`.
There is **no input terminal** — it is an input node in both modes.

**What arrives on `out`:** an empty message. `Root` has only `Properties` — no
body parser at all, so build the whole payload yourself in the next node. The
tick metadata is in `LocalEnvironment`:

```
LocalEnvironment.TimeoutRequest.Action         = 'SET'
LocalEnvironment.TimeoutRequest.Identifier     = 'AUTOTICK'   -- the uniqueIdentifier
LocalEnvironment.TimeoutRequest.StartDate      = '2026-09-11'
LocalEnvironment.TimeoutRequest.StartTime      = '08:20:39.866465'
LocalEnvironment.TimeoutRequest.Count          = 1
LocalEnvironment.TimeoutRequest.Interval       = 5             -- the timeoutInterval
LocalEnvironment.TimeoutRequest.IgnoreMissed   = TRUE
LocalEnvironment.TimeoutRequest.AllowOverwrite = TRUE
```

### Timer policy override

`uniqueIdentifier` doubles as the policy attach point — see
[section 4](#4-the-timer-policy).

---

## 2. TimeoutControl + controlled TimeoutNotification

Two flows. Flow A receives a message and hands TimeoutControl a *timeout
request* saying when to replay it; flow B's controlled TimeoutNotification wakes
up at that moment and propagates the stored message.

The two nodes are paired by **`uniqueIdentifier` — it must be identical on
both**.

### TimeoutControl properties

| Property | Default | Notes |
|---|---|---|
| `uniqueIdentifier` | — **required** | must match the notification node |
| `requestLocation` | `InputLocalEnvironment.TimeoutRequest` | where the request is read from |
| `requestPersistence` | `automatic` | `automatic` \| `yes` \| `no` |
| `storedMessageLocation` | *(blank = whole message)* | **leave blank**, see below |
| `messageDomain` / `messageSet` / `messageType` / `messageFormat` | — | parse hints for the replayed message |
| `queuePrefix` | — | see [section 4](#4-the-timer-policy) — normally set on the policy, not the node |

Terminals: `InTerminal.in`, `OutTerminal.out`, `OutTerminal.failure`.

### The timeout request

Written into `LocalEnvironment.TimeoutRequest`. The eight fields are fixed —
the authoritative list is `common/schemas/MessageAssembly/LocalEnvironment.schema.json`:

| Field | Type | Meaning |
|---|---|---|
| `Action` | `'SET'` \| `'CANCEL'` | |
| `Identifier` | string | names *this request* (not the node) |
| `StartDate` | `'yyyy-MM-dd'` or `'TODAY'` | **absolute** date of the first fire |
| `StartTime` | `'HH:mm:ss'` or `'NOW'` | **absolute** time of the first fire |
| `Interval` | integer seconds | gap between fires when `Count > 1` |
| `Count` | integer, `-1` = forever | number of fires |
| `IgnoreMissed` | `'TRUE'`/`'FALSE'` | skip fires missed while the server was down |
| `AllowOverwrite` | `'TRUE'`/`'FALSE'` | allow replacing an existing request with this `Identifier` |

> **`Interval` is not a delay.** `StartTime='NOW'` fires *immediately*, whatever
> `Interval` says — `Interval` is only the repeat gap. To delay by N seconds you
> compute the wall-clock instant yourself.

### The three things that actually bite

**(a) The Compute node feeding TimeoutControl needs `computeMode="destinationAndMessage"`.**
The LocalEnvironment compute mode is spelled `destination`, not
`localEnvironment` — the full enum is `message`, `destination`,
`destinationAndMessage`, `exception`, `exceptionAndMessage`,
`exceptionAndDestination`, `all`. An invalid value is accepted silently by
`ibmint package` and by the runtime, which then falls back to message-only
propagation, so the request never reaches the node:

```
BIP4601E: The Timeout Control Node 'X.set_timeout' failed to navigate to the
          message location specified. The location specified was:
          'InputLocalEnvironment.TimeoutRequest'.
```

**(b) The stored message MUST carry an `MQMD`.** The timeout store is an MQ
queue and the notification node parses what it reads as an MQ message. If the
tree entering TimeoutControl has no `OutputRoot.MQMD`, nothing is stored, the
timeout still fires on time, and the notification flow dies on an empty
bitstream:

```
BIP4621E: Exception condition detected on input node: 'X.delayed_message'.
BIP5902W: An error occurred in parser 'Root' while parsing the field named
          'MQMD' ... The data being parsed was 'Null Buffer'.
BIP6105E: The remaining bitstream is too small to contain an 'MQMD' structure.
```

Three lines of ESQL fix it:

```sql
SET OutputRoot.MQMD.Version        = 2;
SET OutputRoot.MQMD.Format         = 'MQSTR';
SET OutputRoot.MQMD.CodedCharSetId = 1208;
```

With the MQMD present the message round-trips with its domain intact — a JSON
body comes back as `Root.JSON.Data.*` even with `messageDomain` left blank.

**(c) Leave `storedMessageLocation` blank.** Blank stores the whole message and
replays it unchanged. Setting it to `InputRoot` wraps the body one level deeper
(`Root.JSON.Data.Data.*`). It is a **Field-Reference**, not an XPath — `$Body`
is rejected at flow start:

```
BIP2211E: Invalid configuration attribute value '$Body', is not valid for
          target attribute 'storedMessageLocation'; valid values are
          'Field-Reference'.
```

### Worked example

```sql
CREATE COMPUTE MODULE TIMER_CONTROL_MF_SetRequest
	CREATE FUNCTION Main() RETURNS BOOLEAN
	BEGIN
		DECLARE delaySecs INTEGER COALESCE(InputRoot.JSON.Data.delaySeconds, 10);
		DECLARE reqId     CHARACTER COALESCE(InputRoot.JSON.Data.requestId, 'req-default');
		DECLARE fireAt    TIMESTAMP CURRENT_TIMESTAMP + CAST(delaySecs AS INTERVAL SECOND);

		SET OutputLocalEnvironment.TimeoutRequest.Action         = 'SET';
		SET OutputLocalEnvironment.TimeoutRequest.Identifier     = reqId;
		SET OutputLocalEnvironment.TimeoutRequest.StartDate      = CAST(fireAt AS CHARACTER FORMAT 'yyyy-MM-dd');
		SET OutputLocalEnvironment.TimeoutRequest.StartTime      = CAST(fireAt AS CHARACTER FORMAT 'HH:mm:ss');
		SET OutputLocalEnvironment.TimeoutRequest.Interval       = delaySecs;
		SET OutputLocalEnvironment.TimeoutRequest.Count          = 1;
		SET OutputLocalEnvironment.TimeoutRequest.IgnoreMissed   = 'TRUE';
		SET OutputLocalEnvironment.TimeoutRequest.AllowOverwrite = 'TRUE';

		SET OutputRoot.Properties          = InputRoot.Properties;
		SET OutputRoot.MQMD.Version        = 2;
		SET OutputRoot.MQMD.Format         = 'MQSTR';
		SET OutputRoot.MQMD.CodedCharSetId = 1208;
		SET OutputRoot.JSON.Data.requestId = reqId;
		SET OutputRoot.JSON.Data.payload   = InputRoot.JSON.Data.payload;
		RETURN TRUE;
	END;
END MODULE;
```

Cancelling needs only two fields, on a TimeoutControl node with the same
`uniqueIdentifier` as the one that set it:

```sql
SET OutputLocalEnvironment.TimeoutRequest.Action     = 'CANCEL';
SET OutputLocalEnvironment.TimeoutRequest.Identifier = reqId;
```

If the flow that replies to the caller sits downstream of TimeoutControl,
rebuild the body in a second Compute — otherwise the MQMD you added is
serialized into the HTTP response as binary.

---

## 3. Scheduler node

An input node with two modes. No MQ, no unique identifier, no partner node.

```xml
<nodes xmi:type="ComIbmScheduler.msgnode:FCMComposite_1"
       xmi:id="FCMComposite_1_1" location="100,140"
       scheduleIdentifier="IntervalTick" scheduleType="interval"
       interval="10" unit="second" timeZone="UTC" runOnceOnCheck="true">
  <translation xmi:type="utility:ConstantString" string="every_10_seconds"/>
</nodes>
```

| Property | Default | Values |
|---|---|---|
| `scheduleIdentifier` | `''` | free text, appears in the output message |
| `scheduleType` | `interval` | **`interval`** \| `calendar` |
| `interval` | `1` | integer, `interval` mode only |
| `unit` | `minute` | `second` \| `minute` \| `hour` |
| `cronExp` | `00 * * * *` | `calendar` mode only, see grammar below |
| `days` | `MON,TUE,WED,THU,FRI,SAT,SUN` | day filter for `calendar` mode |
| `timeZone` | `UTC` | |
| `runOnceOnCheck` | `false` | fire once immediately when the flow starts |
| `messageType` | `scheduler` | `scheduler` \| `timeout` \| `recordedMessage` |
| `messageAssembly` | `''` | message assembly file, for `recordedMessage` |
| `retryMechanism` | `failure` | `failure` \| `shortRetry` \| `shortAndLongRetry` |
| `retryThreshold` / `shortRetryInterval` / `longRetryInterval` | `0` / `0` / `300` | |
| `componentLevel` | `flow` | `flow` \| `node` |
| `additionalInstances` | `0` | |

Terminals: `OutTerminal.out`, `OutTerminal.catch`, `OutTerminal.failure`. No
input terminal.

> **`scheduleType` is `interval`, not `repeatInterval`.** The Toolkit node
> definition (`ComIbmScheduler.msgnode` in
> `tools/plugins/com.ibm.etools.mft.ibmnodes.definitions_13.0.2.2.jar`) declares
> the enum literal as `repeatInterval`, and its `.properties` file labels it
> "Repeat Interval". The **runtime rejects that value**:
>
> ```
> BIP5064E: The Scheduler node 'x' was unable to process the invalid
>           schedule type 'repeatinterval'.
> ```
>
> `MessageFlow.xsd` is the one that is right here (`scheduleTypeType` =
> `interval` | `calendar`). This is the reverse of the usual order of trust for
> node properties, so check the runtime rather than the `.msgnode` for this one.

### Cron grammar (`scheduleType="calendar"`)

ACE's cron is **not** Unix cron and **not** Quartz. It is five fields:

```
<minute> <hour> <day-of-month> <month> <day-of-week>
```

with a much narrower grammar than either:

| Field | Accepts | Rejects |
|---|---|---|
| minute | a literal `0`–`59` only | `*`, lists, ranges, steps |
| hour | a literal `0`–`23`, or `*` | lists, ranges, steps |
| day-of-month | a literal `1`–`31`, or `*` | lists, ranges, steps |
| month | `*` **only** | any literal or name |
| day-of-week | `*` **only** | any literal or name — use the `days` property |

Probed on 13.0.2.2, one flow per expression — a bad expression fails at flow
start with `BIP5063E: ... unable to parse the cron expression '...'`:

| Expression | | Expression | |
|---|---|---|---|
| `00 * * * *` | ✅ every hour on the hour | `* * * * *` | ❌ `*` in minute |
| `0 * * * *` | ✅ single-digit minute | `*/5 * * * *` | ❌ step |
| `00 12 * * *` | ✅ 12:00 daily | `00,30 * * * *` | ❌ list |
| `00 00 * * *` | ✅ midnight daily | `00-05 * * * *` | ❌ range |
| `5 5 * * *` | ✅ 05:05 daily | `00 */2 * * *` | ❌ step in hour |
| `59 23 * * *` | ✅ 23:59 daily | `00 * * * MON` | ❌ day-of-week name |
| `00 12 1 * *` | ✅ 12:00 on the 1st | `00 * * * 1` | ❌ day-of-week number |
| `00 12 31 * *` | ✅ 12:00 on the 31st | `00 12 * 6 *` | ❌ month literal |
| | | `00 12 * JUN *` | ❌ month name |
| | | `60 * * * *` | ❌ out of range |
| | | `00 24 * * *` | ❌ out of range |
| | | `00 12 32 * *` | ❌ out of range |
| | | `0 * * * * ?` | ❌ Quartz 6-field |
| | | `00 ? * * *` | ❌ Quartz `?` |
| | | `30`, `00 * * *` | ❌ wrong field count |

Consequences worth knowing before you design a schedule:

- **The finest calendar granularity is hourly.** Anything sub-hourly has to use
  `scheduleType="interval"` with `unit="second"` or `unit="minute"`.
- **Month selection is not expressible.** Filter in the flow if you need it.

### `days` is honoured in `interval` mode and ignored in `calendar` mode

This is the one that will catch you out, because it is the opposite of what the
property looks like it is for.

| Mode | `days="SAT,SUN"`, run on a Friday |
|---|---|
| `interval` | silent — no ticks for the 2.5 minutes observed |
| `calendar` | **fired anyway** |

Four calendar flows with `days` of `FRI`, `SAT,SUN`, `SUN` and all-seven were
deployed together and all four fired at the same scheduled minute on a Friday.
So a calendar schedule **cannot** be restricted to weekdays: not by the
expression (day-of-week must be `*`) and not by `days`. If you need
"09:30 on weekdays", fire hourly-or-daily and filter on
`EXTRACT(DAYOFWEEK FROM CURRENT_DATE)` in the flow.

### Two calendar Schedulers on the same minute fire each other twice

A calendar Scheduler that owns its fire minute is exact — one propagation,
`currentEventTime` = `HH:MM:00.000`:

```
ALONEC 08:58:00.001175  current='2026-09-11T08:58:00.000+01:00'
```

Add a second calendar Scheduler on the *same* minute in the same integration
server and at least one of the pair gets an extra propagation, stamped 1 ms
before the boundary:

```
SHAREA 08:59:00.001031  current='2026-09-11T08:59:00.000+01:00'     <- 1 event
SHAREB 08:59:00.000495  current='2026-09-11T08:58:59.999+01:00'  \
SHAREB 08:59:00.001720  current='2026-09-11T08:59:00.000+01:00'  /  <- 2 events
```

With four calendar flows sharing a minute the counts ran 2, 2, 4, 4 and varied
between runs. **Give each calendar Scheduler its own minute**, or make the
downstream flow idempotent. Interval mode showed no such effect.

### What arrives on `out` (`messageType="scheduler"`)

Both a JSON body and a LocalEnvironment folder, with the same three fields:

```
Root.JSON.Data.lastEventTime         = '1970-01-01T00:00:00.000+01:00'
Root.JSON.Data.currentEventTime      = '2026-09-11T08:22:53.796+01:00'
Root.JSON.Data.scheduleIdentifier    = 'IntervalTick'

LocalEnvironment.Scheduler.lastEventTime      = ...
LocalEnvironment.Scheduler.currentEventTime   = ...
LocalEnvironment.Scheduler.scheduleIdentifier = ...
```

`lastEventTime` is the epoch on the first fire after a flow start, then the
previous `currentEventTime`. `Properties.CodedCharSetId` is 1208 and
`Properties.ReplyProtocol` is `FILE`.

**Not verified here:** `messageType="timeout"` and `messageType="recordedMessage"`
(with `messageAssembly`), and the whole retry group (`retryMechanism`,
`retryThreshold`, `shortRetryInterval`, `longRetryInterval`). Only
`messageType="scheduler"` was exercised. The `timeout` value is presumably the
TimeoutNotification shape — an empty body plus `LocalEnvironment.TimeoutRequest`
— and would be the migration path for a flow that used to start with an
automatic TimeoutNotification node, but confirm it before relying on it.

---

## 4. The Timer policy

The one operational policy in this family. It has exactly two properties, and it
attaches to **TimeoutControl and TimeoutNotification only** — the Scheduler node
has no `<links operationalPolicy=...>` in its XSD type and takes no policy at
all.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<policies>
  <policy policyType="Timer" policyName="TickPrefixed" policyTemplate="Timer">
    <queuePrefix>test</queuePrefix>
    <timeoutInterval>3</timeoutInterval>
  </policy>
</policies>
```

Attach it by putting `{POLICY_PROJECT}:PolicyName` in the node's
`uniqueIdentifier` — on **either** mode, and on both halves of a controlled
pair:

```xml
uniqueIdentifier="{TIMER_DEMO_POLICIES}:TickPrefixed"
```

A controlled pair still pairs correctly through the policy reference: both nodes
just have to name the same policy.

| Property | Effect |
|---|---|
| `timeoutInterval` | overrides the node's own `timeoutInterval` (automatic mode). Proven: node `30`, policy `3` → 3s ticks; node `30`, policy `5` → 5s ticks |
| `queuePrefix` | selects the timeout queue — see below |

Both are `iib:dynamic="false"` in `Policy.xsd`, so a change needs a restart.

### `queuePrefix` is an infix, and ACE creates the queue for you

Despite the name, the value is inserted **before** `.QUEUE`, not prepended:

```
<queuePrefix/>            →  SYSTEM.BROKER.TIMEOUT.QUEUE
<queuePrefix>test</...>   →  SYSTEM.BROKER.TIMEOUT.test.QUEUE
```

It is **case-sensitive** — the queue really is created with a lowercase `test`
in the middle, so `runmqsc` needs the name in quotes to find it
(`DISPLAY QLOCAL('SYSTEM.BROKER.TIMEOUT.test.QUEUE')`; unquoted, runmqsc
uppercases it and reports AMQ8147E).

**A prefixed queue does not have to exist — the runtime defines it at flow
start.** This is the one place where ACE provisions a `SYSTEM.BROKER.*` queue
for you, and it makes sense: nothing could have pre-created a queue whose name
you invent in a policy. Two fresh prefixes were tried on a standalone server and
in both cases the queue's `CRDATE`/`CRTIME` matched the flow-start instant to
the second.

That it is a deliberate `DEFINE` and not MQ default inheritance is clear from
the attributes:

| | `DEFPSIST` | `MAXDEPTH` | `MAXMSGL` |
|---|---|---|---|
| `SYSTEM.DEFAULT.LOCAL.QUEUE` (what a bare create would inherit) | NO | 5000 | 4194304 |
| auto-created `SYSTEM.BROKER.TIMEOUT.test.QUEUE` | **YES** | **100000** | **104857600** |
| shipped `iib_queues_create.mqsc` | NO | 100000 | 104857600 |

Depth and max-message-length match ACE's own script exactly; only `DEFPSIST`
differs (the auto-created one is persistent by default).

**This does not extend to the un-prefixed `SYSTEM.BROKER.TIMEOUT.QUEUE`** — see
[the MQ prerequisite](#mq-prerequisite-controlled-mode-only). On a standalone
integration server you create that one yourself from the shipped script. It was
not re-tested by deletion here because other applications on this queue manager
depend on it.

Use a prefix to give one application its own timeout store instead of sharing
`SYSTEM.BROKER.TIMEOUT.QUEUE` with every other flow on the queue manager.

### The policy does not change the MQ requirement

`Policy.xsd` annotates `ComIbmTimerPolicyType` with `requiresMQ="true"`, which
reads as though attaching a Timer policy drags MQ in. It does not —
**`operationMode` alone decides.** With `defaultQueueManager` commented out:

| Flow | Policy | Result |
|---|---|---|
| automatic | `queuePrefix` empty | started, ticked |
| automatic | `queuePrefix="test"` | started, ticked |
| automatic | `queuePrefix="probe2"` | started, ticked |
| TimeoutControl | `queuePrefix="test"` | `BIP2685E` |
| controlled TimeoutNotification | `queuePrefix="test"` | `BIP2685E` |

So an automatic-mode node with a prefixed policy will *use and create* the
prefixed queue when a queue manager is configured, but does not need one.

### `policyTemplate`

The Toolkit writes `policyTemplate="Timer"` — the type name. Hand-written
policies in this skill have used `policyTemplate=""` and shipped IBM templates
omit the attribute; all three deploy and run. Prefer the Toolkit form for new
files.

---

## BIP codes

| Code | Meaning |
|---|---|
| `BIP2685E` | timeout node in controlled mode, no `defaultQueueManager` |
| `BIP4601E` | TimeoutControl could not navigate to `requestLocation` — usually a wrong `computeMode` upstream |
| `BIP6105E` + `BIP5902W` "Null Buffer" | stored message had no `MQMD` |
| `BIP2211E` | `storedMessageLocation` is not a Field-Reference |
| `BIP5063E` | Scheduler cron expression rejected |
| `BIP5064E` | Scheduler `scheduleType` invalid (`repeatInterval`) |
