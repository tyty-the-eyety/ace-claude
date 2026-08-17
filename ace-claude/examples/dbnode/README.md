# DatabaseRetrieve node — JDBC lookup/enrichment without code (runtime-proven)

`WSInput(/dbnode/customer, JSON) → DatabaseRetrieve → WSReply`, keyNotFound
terminal → Compute({"found":false}) → WSReply. Workspace proof: `DB_NODE_APP`
against PostgreSQL via the `{POLICY_DEMO_POLICIES}:DevPG` JDBCProviders policy
(see `ace-claude/JDBC.md` for the provider/credential setup — identical here).

Every rule below was reverse-engineered from
`server/classes/jdbcnodes.jar → DatabaseRetrieveNode` bytecode because none of
it is documented for hand-authored msgflow XML (the Toolkit generates all of
this from its grid UI):

1. **`sqlQuery` is executed VERBATIM** — the node does not build SQL from the
   grid at runtime. Hand-author the full prepared statement on the node:
   `sqlQuery="SELECT customers.tier FROM customers WHERE customers.cust_id = ? ORDER BY customers.tier ASC"`.
   Without it you get PgParameterMetaData "parameter index out of range".
2. **queryElements rows are repeated `<queryElements .../>` elements** (the
   standard msgflow table convention) and come in two kinds:
   - `operator="ASC"|"DESC"` → SELECT-column rows (added to the internal
     column list as `table.column`; ≥1 required or BIP2211E "zero column rows")
   - any other operator (`=`, `IS NULL`, `IS NOT NULL`, ...) → WHERE
     predicates; non-NULL operators require `valueType="Element"` (literal
     string) and `value` = an XPath into the incoming message
     (`$Body/Data/customerId` for a JSON body). Predicate rows bind the `?`
     markers in row order.
3. **`dataElements` `columnName` must be the qualified `table.column` form**
   (`customers.tier`, matching the column-list key) — the result row is a
   hashtable keyed that way; a bare column name silently resolves null →
   BIP6250E. `messageElement` is a `$OutputRoot/...` write path.
4. `copyMessage="true"` copies the input message and merges the retrieved
   elements in (enrichment); default false emits only the retrieved data.
5. `keyNotFound` is a primary semantic terminal (zero rows) — wire it;
   an unwired keyNotFound on an HTTP flow means a timed-out client.
6. `dataSourceName` accepts a `{policyProject}:policy` JDBCProviders
   reference, exactly like getJDBCType4Connection in JavaCompute.
