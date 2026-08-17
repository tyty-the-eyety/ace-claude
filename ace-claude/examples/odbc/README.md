# ODBC from ESQL — PostgreSQL via PASSTHRU (runtime-proven, ACE 13.0.2.2 + PostgreSQL 17)

Copy-ready HTTP app proving the full ESQL→ODBC path: SELECT (multi-row → JSON
array) and parameterised INSERT with `RETURNING`. Workspace proof:
`DB_PG_ODBC_APP` against `mydb.homelab_hosts` on the Postgres LXC.

## Setup (all three pieces required)

1. **odbc.ini** — no OS packages needed; ACE ships DataDirect wire-protocol
   drivers in `<ACE_INSTALL>/server/ODBC/drivers/lib/` (PostgreSQL =
   `UKpsql95.so`; also Oracle/SQLServer/Sybase). Copy the `odbc.ini` here
   (template: `<ACE_INSTALL>/server/ODBC/unixodbc/odbc.ini`): an
   `[ODBC Data Sources]` list plus one stanza per DSN with
   `Driver`/`Database`/`HostName`/`PortNumber`. `LD_LIBRARY_PATH` already
   includes the driver dir via mqsiprofile.

2. **ODBCINI environment variable** — must point at the file for EVERY server
   start: `export ODBCINI=<work-dir>/odbc.ini` before `IntegrationServer ...`.
   Nothing in server.conf.yaml replaces this.

3. **Credential in the vault** — on a standalone (work-dir) server use
   `mqsicredentials`, not the mqsisetdbparms form from node-based docs:
   ```bash
   mqsicredentials --work-dir <WORKDIR> --create --vault-key <KEY> \
     --credential-type odbc --credential-name PGDB --username u --password p
   ```
   **The credential name must equal the DSN stanza name.**

## Flow/ESQL conventions (see DB_PG_ODBC_MF.*)

- Compute node needs `dataSource="PGDB"` (the DSN) as a node attribute, plus
  `TO Database.PGDB` in PASSTHRU.
- Multi-row: `SET refResults.rows[] = PASSTHRU('SELECT ...' TO Database.PGDB);`
  then `DECLARE rowRef REFERENCE TO refResults.rows[1]` (AFTER the PASSTHRU) +
  `WHILE LASTMOVE ... MOVE rowRef NEXTSIBLING REPEAT NAME`.
- Column names come back lowercase from Postgres — field refs are
  case-sensitive (`rowRef.id`, not `rowRef.ID`).
- `?` parameter markers + `VALUES(...)` work through the DataDirect driver,
  and so does Postgres `INSERT ... RETURNING id` (treat like a SELECT result).
- Postgres `inet` columns: `CAST(ip AS VARCHAR)` renders with the netmask
  (`192.0.2.2/32`); use `host(ip)` in the SQL to strip it.
- JSON output escapes `/` as `\/` by default (valid JSON); set
  `ResourceManagers→JSON→escapeMode: preserveForwardSlashes` in
  server.conf.yaml if that matters.

## Smoke test
```bash
curl http://localhost:7800/pg/hosts
curl http://localhost:7800/pg/addhost -H "Content-Type: application/json" \
  -d '{"name":"x","ip":"192.0.2.50","role":"test"}'
```

## Postgres-side remote access (was needed once on the LXC)
`listen_addresses = '*'` in postgresql.conf + a pg_hba.conf line
`host mydb dbuser 192.0.2.0/24 scram-sha-256`, then restart postgresql.
