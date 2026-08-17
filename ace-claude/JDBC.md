# Enabling JDBC for JavaCompute / built-in DB nodes (ACE 13.0.2.2, standalone IntegrationServer)

The exact steps used to go from zero to a runtime-proven JavaCompute→JDBC→
PostgreSQL flow (worked first try after these steps). Concrete
values are this environment's; substitute your own. Worked example:
`examples/jdbc/` (workspace proof `PG_JDBC_APP` + `PG_JDBC_JAVA`).

JDBC is the **only** database path for JavaCompute and the built-in Database
nodes (DatabaseInput/DatabaseRetrieve/DatabaseRoute). ESQL Compute nodes can
only use ODBC — see `ODBC.md`.

---

## Step 1 — Get a real JDBC driver jar

ACE's `server/jdbcConnector/ACpostgresql.jar` is a **shaded App Connect
driver** (`com.ibm.appconnect.jdbc.postgresql.PostgreSQLDriver`) with an
undocumented URL scheme — don't fight it. Download the official driver
instead and keep it at a stable path outside the workspace:

```bash
mkdir -p /path/to/jdbc-drivers
curl -sL -o /path/to/jdbc-drivers/postgresql-42.7.7.jar \
  https://repo1.maven.org/maven2/org/postgresql/postgresql/42.7.7/postgresql-42.7.7.jar
```

## Step 2 — JDBCProviders policy

No PostgreSQL template ships in `server/adminservices/PolicyTemplates/` (only
DB2/Oracle/MySQL/SQLServer/Informix/Sybase/Teradata/solidDB) — clone the
MySQL template's shape. `POLICY_DEMO_POLICIES/DevPG.policyxml`:

```xml
<policy policyType="JDBCProviders" policyName="DevPG" policyTemplate="">
  <connectionUrlFormat>jdbc:postgresql://[serverName]:[portNumber]/[databaseName]?user=[user]&amp;password=[password]</connectionUrlFormat>
  <databaseName>mydb</databaseName>
  <databaseType>PostgreSQL</databaseType>
  <databaseVersion>17</databaseVersion>
  <serverName>db.example.com</serverName>
  <portNumber>5432</portNumber>
  <jarsURL>/path/to/jdbc-drivers</jarsURL>
  <type4DriverClassName>org.postgresql.Driver</type4DriverClassName>
  <type4DatasourceClassName>org.postgresql.xa.PGXADataSource</type4DatasourceClassName>
  <securityIdentity>pgjdbc</securityIdentity>
  <maxConnectionPoolSize>10</maxConnectionPoolSize>
  <jdbcProviderXASupport>FALSE</jdbcProviderXASupport>
  <useDeployedJars>FALSE</useDeployedJars>
  <databaseSchemaNames>useProvidedSchemaNames</databaseSchemaNames>
  <environmentParms></environmentParms>
</policy>
```

The `[user]`/`[password]` tokens in `connectionUrlFormat` are substituted from
the credential named by `securityIdentity`. Remember JDBCProviders is a
**non-dynamic** policy type — redeploying it needs `--restart-all-applications`
on a running server (irrelevant when deploying to a stopped work dir).

> **`jarsURL` must point at YOUR driver directory** — it is an absolute path on
> the integration server's filesystem, so there is no portable default and the
> `/path/to/jdbc-drivers` shipped in `DevPG.policyxml` will not work as-is. Put
> the JDBC driver jar (e.g. `postgresql-42.7.7.jar`) in a directory of your
> choosing and set `jarsURL` to it. `serverName` and `databaseName` likewise
> need your own values.

## Step 3 — Vault credential (type jdbc, name = securityIdentity)

```bash
mqsicredentials --work-dir /path/to/dev-server \
  --create --vault-key devVaultKey123 \
  --credential-type jdbc --credential-name pgjdbc \
  --username dbuser --password <DB_PASSWORD>
```

The credential name must equal the policy's `securityIdentity` value.

## Step 4 — Java project (JavaCompute)

Minimal scaffold that `ibmint package` compiles itself (BIP8409I — no Toolkit
or pre-built jar needed):

- `.project`: builder `org.eclipse.jdt.core.javabuilder`; natures
  `org.eclipse.jdt.core.javanature` + `com.ibm.etools.mft.jcn.jcnnature`
- `.classpath`: `src` source folder, `JRE_CONTAINER`,
  `com.ibm.etools.mft.jcn.JCN_CONTAINER`, output `bin`
- Source in `src/` extending `MbJavaComputeNode` — see
  `examples/jdbc/PgJdbcQuery.java`. Key API facts (verified):
  - `JDBC_TransactionType` is a **nested enum of MbNode** (inherited — no
    extra import; standalone javac needs `jplugin2.jar` + `javacompute.jar`
    from `server/classes/` on the classpath)
  - `getJDBCType4Connection("{PolicyProject}:PolicyName",
    JDBC_TransactionType.MB_TRANSACTION_AUTO)` — the policy reference IS the
    provider name
  - Never close the Connection (ACE pools it); close ResultSet/Statement in
    finally
  - JSON output tree: `MbJSON.PARSER_NAME` → `MbJSON.DATA_ELEMENT_NAME` →
    `createElementAsLastChild(MbJSON.ARRAY, "name", null)` →
    `MbJSON.ARRAY_ITEM_NAME` children

The app's `.project` lists the Java project in `<projects>`, and both go on
the package command; the jar lands **inside the appzip**:

```bash
ibmint package --input-path <WORKSPACE> --output-bar-file bars/PG_JDBC_APP.bar \
  --project PG_JDBC_APP --project PG_JDBC_JAVA --project POLICY_DEMO_POLICIES
```

## Step 5 — Flow

`WSInput(/jdbc/hosts, JSON) → JavaCompute(javaClass="PgJdbcQuery") → WSReply`.
The JavaCompute node needs only `javaClass="<fqcn>"`.

## Step 6 — Deploy, start, smoke test

No ODBCINI needed for JDBC (that's the ODBC path only). Start the server as
normal (vault key required) and:

```bash
curl http://localhost:7800/jdbc/hosts
# → {"hosts":[{"id":1,"name":"adguard","ip":"192.0.2.2","role":"dns"}, ...]}
```

Verified with zero errors, first run.

## Gotchas

- Postgres `host(ip)` in the SQL avoids the `/32` netmask that a plain
  `CAST(ip AS VARCHAR)` produces (same as the ODBC path).
- `rs.getInt/getString` + `createElementAsLastChild(TYPE_NAME_VALUE, name,
  value)` produce correctly-typed JSON (int stays a JSON number).
- If user code must look up the credential itself, `userRetrievableCredentialTypes`
  in server.conf.yaml gates that — not needed for this pattern; the provider
  resolves `securityIdentity` internally.
