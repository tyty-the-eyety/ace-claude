# Routing node examples — Route, Filter, RouteToLabel/Label, FlowOrder (runtime-proven, ACE 13.0.2.2)

Copy-ready flows for the `noderouting` family. **None of these needs MQ, a
database, or any external service** — the five HTTP flows run on a bare
standalone server, which makes this the cheapest capability folder to re-verify.

Proof app: `ROUTING_DEMO_APP` (the MQ variant is `MQ_ROUTE_APP`).

| File | Node | Endpoint |
|---|---|---|
| `ROUTE_XPATH_MF` | Route | `POST /route/xpath` |
| `MQ_ROUTE_MF` | Route, MQ-driven | `INPUT.QUEUE` |
| `ROUTE_FILTER_MF` | Filter | `POST /route/filter` |
| `ROUTE_LABEL_FIRST_MF` | RouteToLabel `routeToFirst` + Label | `POST /route/label/first` |
| `ROUTE_LABEL_LAST_MF` | RouteToLabel `routeToLast` + Label | `POST /route/label/last` |
| `ROUTE_ORDER_MF` | FlowOrder | `POST /route/order` |

---

## ROUTE_XPATH_MF / MQ_ROUTE_MF — the Route node

XPath rules in a `filterTable`, one dynamic terminal each, plus a fixed
`default`. The two files are the same node against different transports, so use
whichever matches your flow.

```xml
<nodes xmi:type="ComIbmRoute.msgnode:FCMComposite_1" xmi:id="FCMComposite_1_2" location="360,200" distributionMode="first">
  <outTerminals terminalNodeID="gold" dynamic="true" label="gold"/>
  <outTerminals terminalNodeID="silver" dynamic="true" label="silver"/>
  <translation xmi:type="utility:ConstantString" string="route_by_tier"/>
  <filterTable filterPattern="$Root/JSON/Data/score &gt; 80" routingOutputTerminal="gold"/>
  <filterTable filterPattern="$Root/JSON/Data/score &gt; 50" routingOutputTerminal="silver"/>
</nodes>
```

- `distributionMode` = `first` (stop at the first match) or `all`.
- One `<outTerminals terminalNodeID="x" dynamic="true" label="x"/>` per dynamic
  terminal. The fixed `default` terminal needs **no** declaration.
- Connections from a dynamic terminal use the **bare name** as
  `sourceTerminalName` — no `OutTerminal.` prefix. The default terminal *does*
  use the prefix: `OutTerminal.default`.
- `>` must be XML-escaped as `&gt;` inside `filterPattern`.
- **Rules are evaluated in table order and the first match wins** (with
  `distributionMode="first"`). Directly observed: a score of 95 satisfies both
  rules and went to `gold`, the one listed first. It follows that you must list
  the narrowest rule first — reversed, everything above 50 would take `silver`.
- A missing field matches nothing and falls to `default` — no exception.

Observed: `score` 95 → `gold`, 60 → `silver`, 10 → `default`, `{}` → `default`.

## ROUTE_FILTER_MF — the Filter node

ESQL returning a boolean, with **three** routing terminals plus `failure`.

```xml
<nodes xmi:type="ComIbmFilter.msgnode:FCMComposite_1" xmi:id="FCMComposite_1_2" location="330,200" filterExpression="esql://routine/#ROUTE_FILTER_MF_Check.Main">
```

Two things no product schema tells you:

**1. It is a `FILTER MODULE`, not a `COMPUTE MODULE`.**

```sql
CREATE FILTER MODULE ROUTE_FILTER_MF_Check
	CREATE FUNCTION Main() RETURNS BOOLEAN
	BEGIN
		RETURN Root.JSON.Data.amount > 100;
	END;
END MODULE;
```

**2. `InputRoot` does not exist here — use `Root`.** A Filter node has no output
message, so none of the `Input*`/`Output*` correlation names are in scope. Only
`Root`, `Body`, `Properties`, `Environment`, `LocalEnvironment`, `ExceptionList`
and `DestinationList` are. Getting this wrong fails at **deploy** time, and it
takes the *whole application* down, not just the one flow:

```
BIP9318E: Request to 'PreSetupValidate' resource 'ROUTING_DEMO_APP' ... failed.
BIP2432E: (.ROUTE_FILTER_MF_Check.Main, 5.10) : The correlation name
          'InputRoot.JSON.Data.amount' is not valid. Those in scope are:
          Environment, LocalEnvironment, Root, Body, Properties,
          ExceptionList, DestinationList.
```

**The `unknown` terminal is ESQL three-valued logic, not an error path.** Any
comparison against NULL yields UNKNOWN, so a missing field routes to `unknown`
rather than `false` — which is usually what you want, and is why the example
deliberately omits `COALESCE`:

| Body | Terminal |
|---|---|
| `{"amount":150}` | `true` |
| `{"amount":50}` | `false` |
| `{}` | `unknown` |

Terminals: `InTerminal.in`, `OutTerminal.true`, `OutTerminal.false`,
`OutTerminal.unknown`, `OutTerminal.failure`.

## ROUTE_LABEL_FIRST_MF / ROUTE_LABEL_LAST_MF — RouteToLabel + Label

A RouteToLabel node has **`in` and `failure` terminals only — no `out`**. A Label
node has **`out` only — no `in`**. They are not wired to each other; the runtime
jumps from one to the other by name.

```xml
<nodes xmi:type="ComIbmRouteToLabel.msgnode:FCMComposite_1" xmi:id="FCMComposite_1_3" location="560,240" mode="routeToFirst">
<nodes xmi:type="ComIbmLabel.msgnode:FCMComposite_1" xmi:id="FCMComposite_1_4" location="330,80" labelName="alpha">
```

The target list goes in `LocalEnvironment.Destination.RouterList.DestinationData`.
**That structure is in no product schema** — `LocalEnvironment.schema.json` types
`RouterList` as a bare `{"type": "object"}` — so this is the runtime-proven shape:

```sql
SET OutputLocalEnvironment.Destination.RouterList.DestinationData[1].labelName = 'alpha';
SET OutputLocalEnvironment.Destination.RouterList.DestinationData[2].labelName = 'beta';
```

- The Compute that writes it needs **`computeMode="destinationAndMessage"`** —
  `RouterList` lives under `Destination`. There is no `localEnvironment*` enum
  literal, and a wrong value is silently ignored.
- **`mode` picks one entry from the list, and only one.** Proven with the same
  two-entry list in both flows:

| `mode` | handled by |
|---|---|
| `routeToFirst` | `alpha` (entry 1) |
| `routeToLast` | `beta` (entry 2) |

- **An empty or absent list throws** — it does not fall through:
  ```
  BIP2230E: Error detected whilst processing a message in node '....route_routeToFirst'.
  BIP4256E: The RouteToLabel node 'route_routeToFirst' was unable to locate a
            'labelName' element in the local environment.
  ```
  Guard the list, or wire the `failure` terminal.
- `labelName` on the Label node is mandatory and is matched literally against
  `DestinationData.labelName`.

## ROUTE_ORDER_MF — the FlowOrder node

No properties at all. Terminals `InTerminal.in`, `OutTerminal.first`,
`OutTerminal.second`, `OutTerminal.failure`. It guarantees that the whole
`first` branch completes before `second` starts — the same message goes down
both.

Proven by chaining two Trace nodes on `first` and one on `second`, all appending
to one file:

```
FIRST-branch-step1  13:01:35.648144
FIRST-branch-step2  13:01:35.648850
SECOND-branch       13:01:35.649008
```

Put the reply on the `second` branch — both branches receive the message, so a
reply node on each would try to answer the same request twice.

## Passthru

`ComIbmPassthru` is proven incidentally by
`../subflow_terminals.subflow` / `MQ_SUBFLOW_APP`. It has no properties worth
documenting; it exists to make a wiring point.

## Choosing between them

| Need | Use |
|---|---|
| route on message content, declaratively | Route (`filterTable` XPath) |
| route on a condition needing ESQL | Filter |
| route on a list computed at run time, or a loop back to a common branch | RouteToLabel + Label |
| force one branch to finish before another starts | FlowOrder |
| route without a new node, inside an existing Compute | `PROPAGATE TO TERMINAL` + `computeMode="exception"` (see `DB_ENRICH_APP`) |
