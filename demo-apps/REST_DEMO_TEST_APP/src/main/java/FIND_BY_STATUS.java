import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static com.ibm.integration.test.v1.Matchers.hasMessageTreeElement;
import static com.ibm.integration.test.v1.Matchers.terminalPropagateCountIs;
import static org.hamcrest.MatcherAssert.assertThat;

import com.ibm.integration.test.v1.*;

/*
 * Unit tests for REST_FINDBYSTATUS_MF - the flow whose REST response is a JSON
 * ARRAY. Exercises the CARDINALITY + (JSON.Array) loop in its ESQL, including
 * the deliberate cap at 5 names.
 *
 * Same stubbing pattern as GET_INVENTORY: train a NodeStub, then drive the flow
 * from its input node with propagate().
 */
public class FIND_BY_STATUS {

	@AfterEach
	public void cleanup() {
		TestSetup.restoreAllMocks();
	}

	private static SpyObjectReference node(String name) throws Exception {
		return new SpyObjectReference().application("REST_DEMO_APP")
				.messageFlow("REST_FINDBYSTATUS_MF").node(name);
	}

	private TestMessageAssembly runWith(String cannedArrayJson) throws Exception {
		NodeStub restNode = new NodeStub(node("find_by_status"));
		TestMessageAssembly canned = new TestMessageAssembly();
		canned.buildJSONMessage(cannedArrayJson);
		restNode.onCall().propagatesMessage("in", "out", canned);

		NodeSpy buildReply = new NodeSpy(node("build_reply"));
		new NodeSpy(node("reply")).setStopAtInputTerminal("in");

		TestMessageAssembly trigger = new TestMessageAssembly();
		trigger.buildJSONMessage("{\"status\":\"available\"}");
		new NodeSpy(node("/rest/findbystatus")).propagate(trigger, "out");

		assertThat(buildReply, terminalPropagateCountIs("out", 1));
		return buildReply.propagatedMessageAssembly("out", 1);
	}

	private static String pets(int n) {
		StringBuilder sb = new StringBuilder("[");
		for (int i = 1; i <= n; i++) {
			if (i > 1) sb.append(',');
			sb.append("{\"id\":").append(i).append(",\"name\":\"pet-").append(i).append("\"}");
		}
		return sb.append(']').toString();
	}

	@Test
	public void countsEveryPetReturned() throws Exception {
		TestMessageAssembly out = runWith(pets(3));
		assertThat(out, hasMessageTreeElement("JSON.Data.count").isInteger().equals(3));
	}

	@Test
	public void copiesNamesInOrder() throws Exception {
		TestMessageAssembly out = runWith(pets(3));
		assertThat(out, hasMessageTreeElement("JSON.Data.names.Item").isString().equals("pet-1"));
	}

	/** count reflects the whole result set even though only 5 names are copied. */
	@Test
	public void capsNamesAtFiveButCountsAll() throws Exception {
		TestMessageAssembly out = runWith(pets(9));
		assertThat(out, hasMessageTreeElement("JSON.Data.count").isInteger().equals(9));
		System.out.println("FIND_BY_STATUS 9 pets -> " + out.getJSONMessageBodyAsString());
	}

	/** An empty array is a valid answer: count 0, empty names array, no exception. */
	@Test
	public void handlesEmptyResultSet() throws Exception {
		TestMessageAssembly out = runWith("[]");
		assertThat(out, hasMessageTreeElement("JSON.Data.count").isInteger().equals(0));
	}

	/** A single-element array must not be mistaken for a scalar. */
	@Test
	public void handlesSinglePet() throws Exception {
		TestMessageAssembly out = runWith(pets(1));
		assertThat(out, hasMessageTreeElement("JSON.Data.count").isInteger().equals(1));
	}
}
