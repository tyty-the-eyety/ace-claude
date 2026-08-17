# ODBC vs JDBC Database Access in IBM App Connect Enterprise

## Overview

ACE provides two database connectivity paths: ODBC (native, used by ESQL) and JDBC (Java-based, used by JavaCompute nodes and built-in database nodes). They cannot be mixed within a single transaction boundary. Choose one approach per flow based on the node types in use.

## When to Use Which

| Scenario | Use | Reason |
|----------|-----|--------|
| ESQL Compute node needs database access | ODBC | ESQL can ONLY use ODBC — JDBC is not available from ESQL |
| JavaCompute node needs database access | JDBC | Java code can ONLY use JDBC — ODBC is not available from Java |
| Built-in Database nodes (DatabaseInput, DatabaseRetrieve, DatabaseRoute) | JDBC | These nodes use JDBC internally |
| Containerised / CP4I deployment | JDBC preferred | server.conf.yaml is more Kubernetes-friendly than managing odbc.ini |
| Maximum performance from ESQL | ODBC | No JVM overhead — runs natively in the integration server process |
| XA distributed transactions | Pick one | Cannot coordinate XA across ODBC and JDBC in the same flow |

## Configuration

### ODBC Configuration

ODBC connections are configured outside ACE, at the operating system level.

**Linux (odbc.ini):**

The integration server reads from the odbc.ini file pointed to by the `ODBCINI` environment variable, or from `$HOME/.odbc.ini` by default. On ACE containers, set it via `server.conf.yaml` or environment variables.

```ini
# /var/mqsi/odbc.ini (or wherever ODBCINI points)

[MYDB_DSN]
Driver          = /opt/ibm/db2/lib/libdb2o.so
Description     = DB2 Production Database
Database        = MYDB
Hostname        = db2host.example.com
Port            = 50000
Protocol        = TCPIP
CurrentSchema   = APP_SCHEMA

[ORACLE_DSN]
Driver          = /opt/oracle/instantclient/libsqora.so.21.1
ServerName      = //oraclehost:1521/ORCL
```

**Windows:**

Use the ODBC Data Source Administrator (64-bit) to create System DSNs. The DSN name is what you reference in ESQL.

**Credentials:**

Set database credentials using mqsisetdbparms:
```bash
mqsisetdbparms INTEGRATION_NODE -n odbc::MYDB_DSN -u dbuser -p dbpassword

# For independent (standalone) integration servers:
mqsisetdbparms -w /var/mqsi/workdir -n odbc::MYDB_DSN -u dbuser -p dbpassword
```

On a standalone server **with a vault** (this project's dev server), use the
vault instead — credential name must equal the DSN (verified; see
`ace-claude/ODBC.md`):
```bash
mqsicredentials --work-dir <workdir> --create --vault-key <key> \
  --credential-type odbc --credential-name MYDB_DSN -u dbuser -p dbpassword
```

### JDBC Configuration

JDBC connections are configured in the integration server's `server.conf.yaml`.

```yaml
ResourceManagers:
  JDBCProviders:
    MyDB2Provider:
      databaseType: 'DB2'
      databaseSchemaNames: 'APP_SCHEMA'
      jarsURL: '/opt/ibm/db2/java'
      driverClassName: 'com.ibm.db2.jcc.DB2Driver'
      connectionUrlFormat: 'jdbc:db2://[serverName]:[portNumber]/[databaseName]'
      connectionUrlFormatAttr1: 'serverName=db2host.example.com'
      connectionUrlFormatAttr2: 'portNumber=50000'
      connectionUrlFormatAttr3: 'databaseName=MYDB'
      securityIdentity: 'mydb_security'
      maxPoolSize: 10
      
    MyOracleProvider:
      databaseType: 'Oracle'
      jarsURL: '/opt/oracle/jdbc/lib'
      driverClassName: 'oracle.jdbc.OracleDriver'
      connectionUrlFormat: 'jdbc:oracle:thin:@//[serverName]:[portNumber]/[databaseName]'
      connectionUrlFormatAttr1: 'serverName=oraclehost.example.com'
      connectionUrlFormatAttr2: 'portNumber=1521'
      connectionUrlFormatAttr3: 'databaseName=ORCL'
      securityIdentity: 'oracle_security'
```

**Credentials for JDBC:**
```bash
mqsisetdbparms -w /var/mqsi/workdir -n jdbc::mydb_security -u dbuser -p dbpassword
```

**Driver JAR placement:**

The JDBC driver JAR files must be accessible at the path specified in `jarsURL`. On containers, mount them or bake them into the image. Common driver JARs:
- DB2: `db2jcc4.jar`
- Oracle: `ojdbc11.jar`
- PostgreSQL: `postgresql-42.x.x.jar`
- SQL Server: `mssql-jdbc-12.x.x.jre11.jar`

## Coding Patterns

### ESQL with ODBC

#### Basic query with PASSTHRU

```esql
-- Simple SELECT returning a single row
DECLARE refResult ROW;
SET refResult = PASSTHRU('SELECT ORDER_ID, STATUS, AMOUNT FROM ORDERS WHERE ORDER_ID = ?'
  TO Database.MYDB_DSN
  VALUES(InputRoot.XMLNSC.Request.OrderId));

SET OutputRoot.XMLNSC.Response.OrderId = refResult.ORDER_ID;
SET OutputRoot.XMLNSC.Response.Status = refResult.STATUS;
SET OutputRoot.XMLNSC.Response.Amount = refResult.AMOUNT;
```

#### Query returning multiple rows

```esql
-- Multiple rows: PASSTHRU returns a list
DECLARE refResults ROW;
SET refResults.rows[] = PASSTHRU('SELECT ITEM_ID, DESCRIPTION, QTY FROM ORDER_ITEMS WHERE ORDER_ID = ?'
  TO Database.MYDB_DSN
  VALUES(refOrder.OrderId));

-- Iterate the results
DECLARE refRow REFERENCE TO refResults.rows[1];
DECLARE rowCount INTEGER CARDINALITY(refResults.rows[]);
DECLARE i INTEGER 1;
WHILE i <= rowCount DO
  SET OutputRoot.XMLNSC.Response.Items.Item[i].ItemId = refRow.ITEM_ID;
  SET OutputRoot.XMLNSC.Response.Items.Item[i].Description = refRow.DESCRIPTION;
  SET OutputRoot.XMLNSC.Response.Items.Item[i].Qty = refRow.QTY;
  MOVE refRow NEXTSIBLING REPEAT NAME;
  SET i = i + 1;
END WHILE;
```

#### INSERT / UPDATE / DELETE

```esql
-- INSERT
PASSTHRU('INSERT INTO AUDIT_LOG (FLOW_NAME, TIMESTAMP, MESSAGE_ID) VALUES (?, ?, ?)'
  TO Database.MYDB_DSN
  VALUES(MessageFlowLabel, CURRENT_TIMESTAMP, InputRoot.MQMD.MsgId));

-- UPDATE
PASSTHRU('UPDATE ORDERS SET STATUS = ?, UPDATED_AT = ? WHERE ORDER_ID = ?'
  TO Database.MYDB_DSN
  VALUES('PROCESSED', CURRENT_TIMESTAMP, refOrder.OrderId));

-- DELETE
PASSTHRU('DELETE FROM TEMP_STAGING WHERE BATCH_ID = ?'
  TO Database.MYDB_DSN
  VALUES(Environment.Variables.BatchId));
```

#### Error handling with SQLCODE

When `throwExceptionOnDatabaseError` is set to FALSE on the Compute node, you MUST check SQLCODE after every database operation:

```esql
PASSTHRU('INSERT INTO ORDERS (ID, DATA) VALUES (?, ?)'
  TO Database.MYDB_DSN
  VALUES(orderId, orderData));

IF SQLCODE <> 0 THEN
  -- SQLCODE is non-zero: the statement failed
  -- SQLERRORTEXT contains the driver error message
  THROW USER EXCEPTION MESSAGE 2951
    VALUES('Database insert failed. SQLCODE=' || CAST(SQLCODE AS CHAR)
           || ' SQLERRORTEXT=' || COALESCE(SQLERRORTEXT, 'unknown'));
END IF;
```

When `throwExceptionOnDatabaseError` is TRUE (the default and recommended setting), a non-zero SQLCODE automatically throws an exception — no manual check needed.

#### Using ESQL SELECT against a database (schema-qualified)

```esql
-- ESQL SELECT can query an external database when qualified with the DSN
-- This is an alternative to PASSTHRU for simple queries
SET OutputRoot.XMLNSC.Response.CustomerName =
  THE(SELECT ITEM T.CUST_NAME
      FROM Database.MYDB_DSN.APP_SCHEMA.CUSTOMERS AS T
      WHERE T.CUST_ID = refInput.CustomerId);
```

#### Stored procedures via PASSTHRU

```esql
-- Call a stored procedure
DECLARE refResult ROW;
SET refResult = PASSTHRU('CALL PROCESS_ORDER(?, ?, ?)'
  TO Database.MYDB_DSN
  VALUES(orderId, customerId, CURRENT_TIMESTAMP));

-- Result sets from stored procedures come back as rows
SET OutputRoot.XMLNSC.Response.ResultCode = refResult.RESULT_CODE;
```

### JavaCompute with JDBC

#### Basic JDBC lookup

```java
import com.ibm.broker.javacompute.MbJavaComputeNode;
import com.ibm.broker.plugin.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class FlowName_JavaCompute extends MbJavaComputeNode {

    public void evaluate(MbMessageAssembly inAssembly) throws MbException {
        MbOutputTerminal out = getOutputTerminal("out");
        MbMessage inMessage = inAssembly.getMessage();
        MbMessage outMessage = new MbMessage(inMessage);
        MbMessageAssembly outAssembly = new MbMessageAssembly(inAssembly, outMessage);

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            // Get JDBC connection using the provider name from server.conf.yaml
            conn = getJDBCType4Connection("MyDB2Provider", JDBC_TransactionType.MB_TRANSACTION_AUTO);

            // Prepare and execute query
            String orderId = (String) inMessage.getRootElement()
                .getFirstElementByPath("XMLNSC/Request/OrderId").getValue();

            pstmt = conn.prepareStatement(
                "SELECT ORDER_ID, STATUS, AMOUNT FROM ORDERS WHERE ORDER_ID = ?");
            pstmt.setString(1, orderId);
            rs = pstmt.executeQuery();

            // Build output message
            MbElement outRoot = outMessage.getRootElement();
            MbElement xmlnsc = outRoot.createElementAsLastChild(MbXMLNSC.PARSER_NAME);
            MbElement response = xmlnsc.createElementAsLastChild(MbElement.TYPE_NAME, "Response", null);

            if (rs.next()) {
                response.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "OrderId",
                    rs.getString("ORDER_ID"));
                response.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "Status",
                    rs.getString("STATUS"));
                response.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "Amount",
                    rs.getBigDecimal("AMOUNT"));
            }

            out.propagate(outAssembly);

        } catch (Exception e) {
            throw new MbUserException(this, "evaluate", "", "", e.toString(), null);
        } finally {
            // ALWAYS close ResultSet and PreparedStatement
            // Do NOT close the Connection — ACE manages the pool
            try { if (rs != null) rs.close(); } catch (Exception ignore) {}
            try { if (pstmt != null) pstmt.close(); } catch (Exception ignore) {}
        }
    }
}
```

#### Key JDBC rules in ACE JavaCompute

1. **Get the connection via `getJDBCType4Connection()`** — never create your own DriverManager connection. ACE manages the pool.
2. **Do NOT close the Connection object** — ACE reclaims it. Only close ResultSet and PreparedStatement.
3. **Transaction types:**
   - `MB_TRANSACTION_AUTO` — each statement auto-commits (most common)
   - `MB_TRANSACTION_BROKER` — ACE manages the commit at the end of the flow (use for transactional flows coordinated with MQ)
4. **The provider name** in `getJDBCType4Connection("MyDB2Provider", ...)` must match the key under `JDBCProviders` in server.conf.yaml.

#### Multiple rows in Java

```java
pstmt = conn.prepareStatement(
    "SELECT ITEM_ID, DESCRIPTION, QTY FROM ORDER_ITEMS WHERE ORDER_ID = ?");
pstmt.setString(1, orderId);
rs = pstmt.executeQuery();

MbElement items = response.createElementAsLastChild(MbElement.TYPE_NAME, "Items", null);
while (rs.next()) {
    MbElement item = items.createElementAsLastChild(MbElement.TYPE_NAME, "Item", null);
    item.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "ItemId",
        rs.getString("ITEM_ID"));
    item.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "Description",
        rs.getString("DESCRIPTION"));
    item.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "Qty",
        rs.getInt("QTY"));
}
```

## Side-by-Side Comparison

| Aspect | ODBC (ESQL) | JDBC (Java) |
|--------|------------|-------------|
| **Language** | ESQL | Java |
| **Config location** | odbc.ini (OS-level) | server.conf.yaml |
| **Credential command** | `mqsisetdbparms -n odbc::DSN` | `mqsisetdbparms -n jdbc::identity` |
| **Connection in code** | Implicit via `TO Database.DSN` | `getJDBCType4Connection("provider", txType)` |
| **Simple SELECT** | `THE(SELECT ... FROM Database.DSN.SCHEMA.TABLE ...)` | PreparedStatement + ResultSet |
| **Dynamic SQL** | `PASSTHRU('SQL' TO Database.DSN VALUES(...))` | PreparedStatement with `?` params |
| **Multiple rows** | `SET rows[] = PASSTHRU(...)` then iterate | `while (rs.next())` loop |
| **Error handling** | SQLCODE / throwExceptionOnDatabaseError | try-catch with MbUserException |
| **Connection lifecycle** | Managed by integration server | Managed by ACE pool — do NOT close Connection |
| **Transaction control** | Coordinated with MQ via flow-level XA | `MB_TRANSACTION_AUTO` or `MB_TRANSACTION_BROKER` |
| **Driver type** | Native C library (.so / .dll) | Java JAR file |
| **Performance** | Faster (no JVM overhead) | Slightly slower (JVM layer) |
| **Container-friendliness** | Harder (odbc.ini, native drivers) | Easier (YAML config, JAR files) |

## Common Gotchas

1. **ESQL cannot use JDBC.** If your Compute node needs a database, you must have ODBC configured. There is no workaround.

2. **JavaCompute cannot use ODBC.** If your Java code needs a database, you must have JDBC configured.

3. **Don't mix in the same transaction.** An ESQL PASSTHRU via ODBC and a JavaCompute JDBC call in the same flow are two separate transaction scopes. If the ESQL succeeds but the Java fails, the ESQL changes are already committed (or vice versa). For transactional integrity across multiple databases, use a single access method.

4. **ODBC driver architecture matters.** On 64-bit Linux, you need 64-bit ODBC drivers. The ACE integration server is 64-bit, so 32-bit drivers will not load.

5. **JDBC connection pool exhaustion.** If `maxPoolSize` is too low and flows are highly concurrent, threads will block waiting for connections. Monitor with `mqsireportproperties` or the web UI.

6. **NULL handling differs.** In ESQL, a NULL database column comes back as ESQL NULL. In Java, you must check `rs.wasNull()` after calling `rs.getString()` etc., because Java primitive getters return 0/false for NULL.

7. **Date/time handling.** ESQL uses CAST with FORMAT for date parsing. Java uses `rs.getTimestamp()` which returns `java.sql.Timestamp`. Make sure timezone handling is explicit in both cases.

8. **Schema qualification.** In ESQL SELECT (not PASSTHRU), you must fully qualify: `Database.DSN.SCHEMA.TABLE`. In PASSTHRU, the schema is part of the SQL string itself. In JDBC, the schema is typically set via `databaseSchemaNames` in server.conf.yaml or qualified in the SQL.
