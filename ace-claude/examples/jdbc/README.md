# JDBC from JavaCompute — PostgreSQL via JDBCProviders policy (runtime-proven)

Read `ace-claude/JDBC.md` (the step-by-step enablement runbook) first. Files here:
- `PG_JDBC_MF.msgflow` — WSInput(/jdbc/hosts) → JavaCompute(javaClass="PgJdbcQuery") → WSReply
- `PgJdbcQuery.java` — verified JavaCompute JDBC pattern (getJDBCType4Connection with a
  `{PolicyProject}:Policy` reference, pooled Connection never closed, MbJSON array output)
- `DevPG.policyxml` — JDBCProviders policy for PostgreSQL (no IBM template exists; this is the shape)
- `java-project.classpath` — the .classpath for the Java project (JCN container + src/bin)

Java project .project natures: `org.eclipse.jdt.core.javanature` + `com.ibm.etools.mft.jcn.jcnnature`;
the app's .project lists the Java project in <projects>; `ibmint package` compiles the Java itself
(BIP8409I) — pass both projects plus the policy project.
