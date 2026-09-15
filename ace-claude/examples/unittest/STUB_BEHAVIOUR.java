import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static com.ibm.integration.test.v1.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.ibm.integration.test.v1.*;

/*
 * Executable documentation of how NodeStub actually behaves. Every assertion
 * here was established empirically - several contradict what the API names
 * suggest, and each one cost a debugging cycle to find.
 */
public class STUB_BEHAVIOUR {

	@AfterEach
	public void cleanup() {
		TestSetup.restoreAllMocks();
	}

	private static SpyObjectReference node(String name) throws Exception {
		return new SpyObjectReference().application("REST_DEMO_APP")
				.messageFlow("REST_INVENTORY_MF").node(name);
	}

	private NodeSpy stopAtReply() throws Exception {
		NodeSpy reply = new NodeSpy(node("reply"));
		reply.setStopAtInputTerminal("in");
		return reply;
	}

	/**
	 * A node can only be mocked ONCE per test - constructing a second NodeSpy on
	 * the same node raises "Node already mocked: ... You can only mock a Data
	 * Flow Node once". So the input-node spy is created once and reused for every
	 * message this test fires.
	 */
	private NodeSpy inputNode() throws Exception {
		return new NodeSpy(node("/rest/inventory"));
	}

	private void fire(NodeSpy in, String body) throws Exception {
		TestMessageAssembly t = new TestMessageAssembly();
		t.buildJSONMessage(body);
		in.propagate(t, "out");
	}

	private void fire(String body) throws Exception {
		fire(inputNode(), body);
	}

	/**
	 * THE TRAP: calling evaluate() on a stubbed node runs the REAL node. The
	 * stub must be reached THROUGH the flow via propagate() on the input node.
	 * isTrainedForCall() reports true in both cases, so it cannot tell them apart.
	 */
	@Test
	public void stubIsTrainedRegardlessOfHowItIsDriven() throws Exception {
		NodeStub rest = new NodeStub(node("get_inventory"));
		TestMessageAssembly canned = new TestMessageAssembly();
		canned.buildJSONMessage("{\"stubbed\":true}");
		rest.onCall().propagatesMessage("in", "out", canned);

		assertTrue(rest.isTrainedForCall("in"),
				"isTrainedForCall is true even when the stub is about to be bypassed");

		NodeSpy buildReply = new NodeSpy(node("build_reply"));
		stopAtReply();
		fire("{}");

		// Driven through the flow, the CANNED message is what downstream sees.
		assertThat(buildReply, propagatedJSONFromTerminalOnCall(
				"{\"inventory\":{\"stubbed\":true}}", "out", 1));
	}

	/** propagatesInputMessage is a pass-through stub: no canned body required. */
	@Test
	public void passThroughStubForwardsTheInputMessage() throws Exception {
		NodeStub rest = new NodeStub(node("get_inventory"));
		rest.onCall().propagatesInputMessage("in", "out");

		NodeSpy buildReply = new NodeSpy(node("build_reply"));
		stopAtReply();
		fire("{\"echoed\":true}");

		assertThat(buildReply, propagatedJSONFromTerminalOnCall(
				"{\"inventory\":{\"echoed\":true}}", "out", 1));
	}

	/**
	 * A stub DOES carry LocalEnvironment, so ESQL reading
	 * LocalEnvironment.WrittenDestination.* can be covered - provided the path is
	 * written in the DOTTED-FROM-ROOT form.
	 */
	@Test
	public void stubCarriesLocalEnvironmentWithDottedPaths() throws Exception {
		NodeStub rest = new NodeStub(node("get_inventory"));
		TestMessageAssembly canned = new TestMessageAssembly();
		canned.buildJSONMessage("{\"available\":5}");
		canned.localEnvironmentPath("WrittenDestination.REST.URL").setValue("https://example.invalid/x");
		canned.localEnvironmentPath("WrittenDestination.REST.StatusCode").setValue(201);
		canned.localEnvironmentPath("WrittenDestination.REST.Method").setValue("GET");
		rest.onCall().propagatesMessage("in", "out", canned);

		NodeSpy buildReply = new NodeSpy(node("build_reply"));
		stopAtReply();
		fire("{}");

		TestMessageAssembly out = buildReply.propagatedMessageAssembly("out", 1);
		assertThat(out, hasMessageTreeElement("JSON.Data.calledUrl").isString().equals("https://example.invalid/x"));
		assertThat(out, hasMessageTreeElement("JSON.Data.statusCode").isInteger().equals(201));
		assertThat(out, hasMessageTreeElement("JSON.Data.method").isString().equals("GET"));
	}

	/**
	 * THE SILENT TRAP: a "$."-prefixed path builds a literal "$" wrapper element
	 * that the flow never sees. setValue() succeeds, the assembly serializes
	 * happily, and the value simply never arrives - no error anywhere.
	 */
	@Test
	public void dollarPrefixedLocalEnvironmentPathIsSilentlyIgnored() throws Exception {
		NodeStub rest = new NodeStub(node("get_inventory"));
		TestMessageAssembly canned = new TestMessageAssembly();
		canned.buildJSONMessage("{\"available\":5}");
		canned.localEnvironmentPath("$.WrittenDestination.REST.StatusCode").setValue(201);
		rest.onCall().propagatesMessage("in", "out", canned);

		NodeSpy buildReply = new NodeSpy(node("build_reply"));
		stopAtReply();
		fire("{}");

		// It serialized into the assembly...
		assertTrue(canned.getLocalEnvironmentTreeSerializedForm().contains("iib:name=\"$\""),
				"the $ becomes a literal wrapper element");
		// ...but the flow never saw it: no statusCode in the reply.
		assertThat(buildReply, propagatedJSONFromTerminalOnCall(
				"{\"inventory\":{\"available\":5}}", "out", 1));
	}

	/**
	 * onCall() is NOT a per-invocation queue, unlike Mockito's consecutive
	 * stubbing. Training twice does not make call 2 behave differently.
	 */
	@Test
	public void trainingTwiceDoesNotGivePerCallBehaviour() throws Exception {
		NodeStub rest = new NodeStub(node("get_inventory"));
		TestMessageAssembly first = new TestMessageAssembly();
		first.buildJSONMessage("{\"call\":1}");
		TestMessageAssembly second = new TestMessageAssembly();
		second.buildJSONMessage("{\"call\":2}");
		rest.onCall().propagatesMessage("in", "out", first);
		rest.onCall().propagatesMessage("in", "out", second);

		NodeSpy buildReply = new NodeSpy(node("build_reply"));
		stopAtReply();
		NodeSpy in = inputNode();
		fire(in, "{}");
		fire(in, "{}");

		assertThat(buildReply, terminalPropagateCountIs("out", 2));
		assertThat(buildReply, propagatedJSONFromTerminalOnCall("{\"inventory\":{\"call\":1}}", "out", 1));
		assertThat(buildReply, propagatedJSONFromTerminalOnCall("{\"inventory\":{\"call\":1}}", "out", 2));
	}

	/** restoreAllMocks puts the real node back, so stubs do not leak between tests. */
	@Test
	public void restoreAllMocksUntrainsTheStub() throws Exception {
		NodeStub rest = new NodeStub(node("get_inventory"));
		TestMessageAssembly canned = new TestMessageAssembly();
		canned.buildJSONMessage("{\"x\":1}");
		rest.onCall().propagatesMessage("in", "out", canned);
		assertTrue(rest.isTrainedForCall("in"));

		TestSetup.restoreAllMocks();

		NodeStub fresh = new NodeStub(node("get_inventory"));
		assertFalse(fresh.isTrainedForCall("in"), "a fresh stub starts untrained");
	}
}
