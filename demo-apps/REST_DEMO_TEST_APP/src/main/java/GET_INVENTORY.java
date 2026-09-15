import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static com.ibm.integration.test.v1.Matchers.propagatedJSONFromTerminalOnCall;
import static com.ibm.integration.test.v1.Matchers.terminalPropagateCountIs;
import static org.hamcrest.MatcherAssert.assertThat;

import com.ibm.integration.test.v1.NodeSpy;
import com.ibm.integration.test.v1.NodeStub;
import com.ibm.integration.test.v1.SpyObjectReference;
import com.ibm.integration.test.v1.TestMessageAssembly;
import com.ibm.integration.test.v1.TestSetup;

/*
 * Unit tests for REST_INVENTORY_MF.
 *
 * The REST Request node is STUBBED, so no HTTP call is made to
 * petstore.swagger.io. The tests are hermetic: they do not depend on a public
 * server whose data changes between runs, and they run in milliseconds.
 *
 * THE PATTERN THAT MAKES A STUB ACTUALLY INTERCEPT (all three parts matter):
 *
 *   1. NodeStub + onCall().propagatesMessage(inTerminal, outTerminal, canned)
 *      trains the replacement behaviour.
 *
 *   2. Drive the flow from its INPUT node with propagate(), NOT by calling
 *      evaluate() on the stub. Calling evaluate() on a stubbed node runs the
 *      REAL node - the HTTP call still goes out and the stub propagates an
 *      EMPTY message. isTrainedForCall() returns true either way, so the stub
 *      looks correctly configured while being silently bypassed.
 *
 *   3. setStopAtInputTerminal("in") on the HTTPReply node, which cannot reply
 *      without a real HTTP request.
 *
 * Also note NodeSpy.evaluate(assembly, boolean, terminal): the boolean means
 * "evaluate this node in ISOLATION", so true stops the flow at that node and
 * false lets it continue downstream - the opposite of how it reads.
 */
public class GET_INVENTORY {

	private static final String APP  = "REST_DEMO_APP";
	private static final String FLOW = "REST_INVENTORY_MF";

	@AfterEach
	public void cleanup() {
		TestSetup.restoreAllMocks();
	}

	private static SpyObjectReference node(String name) throws Exception {
		return new SpyObjectReference().application(APP).messageFlow(FLOW).node(name);
	}

	/** Runs the flow with the REST call stubbed out, and returns the build_reply spy. */
	private NodeSpy runWithStubbedInventory(String cannedJson) throws Exception {
		NodeStub restNode = new NodeStub(node("get_inventory"));
		TestMessageAssembly cannedResponse = new TestMessageAssembly();
		cannedResponse.buildJSONMessage(cannedJson);
		restNode.onCall().propagatesMessage("in", "out", cannedResponse);

		NodeSpy buildReply = new NodeSpy(node("build_reply"));

		NodeSpy replyNode = new NodeSpy(node("reply"));
		replyNode.setStopAtInputTerminal("in");

		TestMessageAssembly trigger = new TestMessageAssembly();
		trigger.buildJSONMessage("{}");
		new NodeSpy(node("/rest/inventory")).propagate(trigger, "out");

		assertThat(restNode, terminalPropagateCountIs("out", 1));
		return buildReply;
	}

	/** The connector response is nested under "inventory" and otherwise untouched. */
	@Test
	public void wrapsRestResponseUnderInventory() throws Exception {
		NodeSpy buildReply = runWithStubbedInventory("{\"available\":42,\"pending\":7,\"sold\":13}");

		assertThat(buildReply, terminalPropagateCountIs("out", 1));
		assertThat(buildReply, propagatedJSONFromTerminalOnCall(
				"{\"inventory\":{\"available\":42,\"pending\":7,\"sold\":13}}", "out", 1));
	}

	/** An empty inventory is a valid answer, not an error. */
	@Test
	public void handlesEmptyInventory() throws Exception {
		NodeSpy buildReply = runWithStubbedInventory("{}");

		assertThat(buildReply, terminalPropagateCountIs("out", 1));
		assertThat(buildReply, propagatedJSONFromTerminalOnCall("{\"inventory\":{}}", "out", 1));
	}

	/** Status keys with awkward characters survive the reshaping. */
	@Test
	public void passesThroughUnusualStatusKeys() throws Exception {
		NodeSpy buildReply = runWithStubbedInventory(
				"{\"Not Available\":1,\"available, sold\":2}");

		assertThat(buildReply, propagatedJSONFromTerminalOnCall(
				"{\"inventory\":{\"Not Available\":1,\"available, sold\":2}}", "out", 1));
	}
}
