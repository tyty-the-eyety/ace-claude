# Enabling ODBC for ESQL database access (ACE 13.0.2.2, standalone IntegrationServer)

The exact steps used to go from zero to a runtime-proven ESQL→PostgreSQL flow
on 2026-07-15. Concrete values are this environment's (dev server work dir
`/path/to/dev-server`, vault key `devVaultKey123`, PostgreSQL 17
on LXC `db.example.com`, db `mydb`, db user `dbuser`); substitute your own.
Worked example app: `examples/odbc/` (and workspace proof `DB_PG_ODBC_APP`).

ODBC is the **only** database path available from ESQL Compute nodes (JDBC is
Java/built-in-DB-nodes only — see `DB_ENRICH_APP/odbc-vs-jdbc.md`).

---

## Step 1 — Confirm the shipped driver (no OS packages needed)

ACE bundles DataDirect wire-protocol ODBC drivers; nothing needs installing:

```bash
ls /opt/IBM/ace-13.0.2.2/server/ODBC/drivers/lib/
# UKpsql95.so  ← PostgreSQL      UKora95.so ← Oracle
# UKsqls95.so  ← SQL Server      UKase95.so ← Sybase   (+ DB2 via dsdriver/)
```

`mqsiprofile` already puts `server/ODBC/drivers/lib` on `LD_LIBRARY_PATH` —
verify with `echo $LD_LIBRARY_PATH | tr ':' '\n' | grep ODBC` if in doubt.

## Step 2 — Make the database remotely reachable (server side, one-time)

Test first: `bash -c 'cat < /dev/null > /dev/tcp/<DB_HOST>/5432'` —
"Connection refused" means the DB is listening on localhost only (Postgres
default). On the Postgres host:

```bash
# /etc/postgresql/17/main/postgresql.conf  (find it: psql -c 'SHOW config_file')
listen_addresses = '*'

# /etc/postgresql/17/main/pg_hba.conf — append:
host    mydb    dbuser    192.0.2.0/24    scram-sha-256

systemctl restart postgresql
```

Re-run the /dev/tcp test until the port is open.

## Step 3 — Create odbc.ini

Template: `/opt/IBM/ace-13.0.2.2/server/ODBC/unixodbc/odbc.ini`. Ours lives at
`/path/to/dev-server/odbc.ini` (kept with the work dir, outside
the source workspace):

```ini
[ODBC Data Sources]
PGDB=DataDirect ODBC PostgreSQL Wire Protocol

[PGDB]
Driver=/opt/IBM/ace-13.0.2.2/server/ODBC/drivers/lib/UKpsql95.so
Description=DataDirect ODBC PostgreSQL Wire Protocol
Database=mydb
HostName=db.example.com
PortNumber=5432
```

Both sections are required: the `[ODBC Data Sources]` listing AND the per-DSN
stanza. The stanza name (`PGDB`) is the DSN you reference everywhere else.

## Step 4 — Store the DB credential in the server vault

On a standalone (work-dir) server use `mqsicredentials` against the vault —
NOT the `mqsisetdbparms -n odbc::DSN` form from integration-node-based docs:

```bash
. /opt/IBM/ace-13.0.2.2/server/bin/mqsiprofile   # once per shell
mqsicredentials --work-dir /path/to/dev-server \
  --create --vault-key devVaultKey123 \
  --credential-type odbc --credential-name PGDB \
  --username dbuser --password <DB_PASSWORD>

# verify:
mqsicredentials --work-dir /path/to/dev-server \
  --report --vault-key devVaultKey123
```

**The credential name must be exactly the DSN name** (`PGDB` ↔ `[PGDB]`) —
that is the entire lookup mechanism; there is no other linkage.

## Step 5 — Reference the DSN in the flow

Two places, both required:

1. Compute node attribute in the .msgflow:
   ```xml
   <nodes xmi:type="ComIbmCompute.msgnode:FCMComposite_1" ...
       computeExpression="esql://routine/#MyModule.Main"
       computeMode="message" dataSource="PGDB">
   ```
2. ESQL `PASSTHRU ... TO Database.PGDB` (see `examples/odbc/DB_PG_ODBC_MF.esql`
   for the proven multi-row SELECT→JSON-array and INSERT+RETURNING patterns).

## Step 6 — Start the server with ODBCINI set

```bash
export ODBCINI=/path/to/dev-server/odbc.ini
IntegrationServer --work-dir /path/to/dev-server \
  --admin-rest-api 7600 --http-port-number 7800 --vault-key devVaultKey123
```

`ODBCINI` must be exported **every server start** — there is no
server.conf.yaml equivalent, and without it every DB statement fails at
runtime with a data-source-not-found error.

## Step 7 — Smoke test

```bash
curl http://localhost:7800/pg/hosts          # SELECT → JSON array of rows
curl http://localhost:7800/pg/addhost \
  -H "Content-Type: application/json" \
  -d '{"name":"x","ip":"192.0.2.50","role":"test"}'   # INSERT ... RETURNING id
```

Both verified 2026-07-15 with zero errors on first run after steps 1–6.

---

## Gotchas (all observed, not theoretical)

- Postgres returns **lowercase column names**; ESQL field references are
  case-sensitive (`rowRef.id`, not `rowRef.ID`).
- `?` parameter markers with `VALUES(...)` and `INSERT ... RETURNING id` both
  pass straight through the DataDirect driver (RETURNING behaves like a
  SELECT result set — assign to `ref.rows[]`).
- `CAST(inet_col AS VARCHAR)` renders with the netmask (`192.0.2.2/32`);
  use Postgres `host(ip)` in the SQL to strip it.
- The JSON serializer escapes `/` as `\/` by default (valid JSON; change via
  server.conf.yaml `ResourceManagers→JSON→escapeMode: preserveForwardSlashes`).
- `DECLARE rowRef REFERENCE TO refResults.rows[1]` must come AFTER the
  PASSTHRU that populates `rows[]` (reference targets are bound at DECLARE).
