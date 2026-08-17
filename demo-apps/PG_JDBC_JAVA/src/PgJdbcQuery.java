import com.ibm.broker.javacompute.MbJavaComputeNode;
import com.ibm.broker.plugin.MbElement;
import com.ibm.broker.plugin.MbException;
import com.ibm.broker.plugin.MbJSON;
import com.ibm.broker.plugin.MbMessage;
import com.ibm.broker.plugin.MbMessageAssembly;
import com.ibm.broker.plugin.MbOutputTerminal;
import com.ibm.broker.plugin.MbUserException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class PgJdbcQuery extends MbJavaComputeNode {

	public void evaluate(MbMessageAssembly inAssembly) throws MbException {
		MbOutputTerminal outTerminal = getOutputTerminal("out");
		MbMessage outMessage = new MbMessage();
		MbMessageAssembly outAssembly = new MbMessageAssembly(inAssembly, outMessage);

		PreparedStatement pstmt = null;
		ResultSet rs = null;

		try {
			// Provider is the JDBCProviders policy {POLICY_DEMO_POLICIES}:DevPG.
			// Do NOT close the Connection - ACE owns the pool.
			Connection conn = getJDBCType4Connection("{POLICY_DEMO_POLICIES}:DevPG",
					JDBC_TransactionType.MB_TRANSACTION_AUTO);

			pstmt = conn.prepareStatement(
					"SELECT id, name, host(ip) AS ip, role FROM homelab_hosts ORDER BY id");
			rs = pstmt.executeQuery();

			MbElement jsonRoot = outMessage.getRootElement()
					.createElementAsLastChild(MbJSON.PARSER_NAME);
			MbElement data = jsonRoot.createElementAsLastChild(MbElement.TYPE_NAME,
					MbJSON.DATA_ELEMENT_NAME, null);
			MbElement hosts = data.createElementAsLastChild(MbJSON.ARRAY, "hosts", null);

			while (rs.next()) {
				MbElement item = hosts.createElementAsLastChild(MbElement.TYPE_NAME,
						MbJSON.ARRAY_ITEM_NAME, null);
				item.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "id", rs.getInt("id"));
				item.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "name", rs.getString("name"));
				item.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "ip", rs.getString("ip"));
				item.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "role", rs.getString("role"));
			}

			outTerminal.propagate(outAssembly);

		} catch (Exception e) {
			throw new MbUserException(this, "evaluate", "", "", e.toString(), null);
		} finally {
			// Close ResultSet and PreparedStatement only - never the Connection
			try { if (rs != null) rs.close(); } catch (Exception ignore) { }
			try { if (pstmt != null) pstmt.close(); } catch (Exception ignore) { }
		}
	}
}
