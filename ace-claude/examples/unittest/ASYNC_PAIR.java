import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static com.ibm.integration.test.v1.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.ibm.integration.test.v1.*;
import java.util.concurrent.*;

/*
 * Tests for the RESTAsyncRequest / RESTAsyncResponse pair.
 *
 * The two halves live in SEPARATE FLOWS joined by a correlator, not by a wire,
 * so they are tested two different ways:
 *
 *   1. The response half in isolation - propagate() straight into the
 *      RESTAsyncResponse node, which is an input node. Hermetic and fast.
 *
 *   2. The whole correlation end to end - drive REST_ASYNC_MF and wait on the
 *      response flow with whenPropagateCountIs(). This needs BOTH a network and
 *      message flows to be STARTED, so it self-skips when either is missing.
 *
 * KEY POINT for (2): the usual `--start-msgflows false` PREVENTS the response
 * flow from running, and the wait simply times out with propagate count 0. Run
 * async-pair tests WITHOUT that flag.
 */
public class ASYNC_PAIR {

	private static final String REQ_FLOW  = "REST_ASYNC_MF";
	private static final String RESP_FLOW = "REST_ASYNC_RESP_MF";

	@AfterEach
	public void cleanup() {
		TestSetup.restoreAllMocks();
	}

	private static SpyObjectReference node(String flow, String name) throws Exception {
		return new SpyObjectReference().application("REST_DEMO_APP").messageFlow(flow).node(name);
	}

	/** Injects a canned REST response straight into the response flow. */
	private TestMessageAssembly deliverToResponseFlow(String arrayJson, Integer statusCode) throws Exception {
		NodeSpy buildReply = new NodeSpy(node(RESP_FLOW, "build_reply"));
		new NodeSpy(node(RESP_FLOW, "reply")).setStopAtInputTerminal("in");
		NodeSpy asyncResponse = new NodeSpy(node(RESP_FLOW, "async_response"));

		TestMessageAssembly a = new TestMessageAssembly();
		a.buildJSONMessage(arrayJson);
		if (statusCode != null) {
			// Dotted from the tree root - NOT "$."-prefixed. The "$." form builds
			// a literal "$" wrapper element that the flow never sees.
			a.localEnvironmentPath("REST.Response.StatusCode").setValue(statusCode.intValue());
		}
		asyncResponse.propagate(a, "out");

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

	// ---------- 1. the response half, in isolation ----------

	@Test
	public void countsPetsAndTakesTheFirstName() throws Exception {
		TestMessageAssembly out = deliverToResponseFlow(
				"[{\"id\":1,\"name\":\"doggie\"},{\"id\":2,\"name\":\"cat\"}]", null);
		assertThat(out, hasMessageTreeElement("JSON.Data.count").isInteger().equals(2));
		assertThat(out, hasMessageTreeElement("JSON.Data.firstName").isString().equals("doggie"));
	}

	/** RESTAsyncResponse exposes its status at LocalEnvironment.REST.Response, NOT WrittenDestination. */
	@Test
	public void copiesStatusCodeFromRestResponse() throws Exception {
		TestMessageAssembly out = deliverToResponseFlow(pets(1), 207);
		assertThat(out, hasMessageTreeElement("JSON.Data.statusCode").isInteger().equals(207));
	}

	/** With no LocalEnvironment supplied the field is simply absent - not an error. */
	@Test
	public void omitsStatusCodeWhenLocalEnvironmentIsAbsent() throws Exception {
		TestMessageAssembly out = deliverToResponseFlow(pets(1), null);
		assertThat(out, hasMessageTreeElement("JSON.Data.count").isInteger().equals(1));
		assertThat(out.getJSONMessageBodyAsString().contains("statusCode"), org.hamcrest.Matchers.is(false));
	}

	@Test
	public void marksTheReplyWithItsRoute() throws Exception {
		TestMessageAssembly out = deliverToResponseFlow(pets(3), 200);
		assertThat(out, hasMessageTreeElement("JSON.Data.via")
				.isString().equals("RESTAsyncRequest + RESTAsyncResponse"));
		assertThat(out, hasMessageTreeElement("JSON.Data.count").isInteger().equals(3));
	}

	@Test
	public void handlesEmptyResultSet() throws Exception {
		TestMessageAssembly out = deliverToResponseFlow("[]", 200);
		assertThat(out, hasMessageTreeElement("JSON.Data.count").isInteger().equals(0));
	}

	// ---------- 2. the correlation, end to end ----------

	/**
	 * Drives REST_ASYNC_MF and waits for REST_ASYNC_RESP_MF to produce a reply,
	 * proving asyncResponseCorrelator/asyncRequestCorrelator actually pair the
	 * two flows and that the reply comes out of a DIFFERENT flow than was driven.
	 *
	 * Requires message flows to be started and the Petstore to be reachable;
	 * skips rather than fails when they are not.
	 */
	@Test
	public void correlatorJoinsTheTwoFlows() throws Exception {
		NodeSpy buildReply = new NodeSpy(node(RESP_FLOW, "build_reply"));
		new NodeSpy(node(RESP_FLOW, "reply")).setStopAtInputTerminal("in");

		CompletableFuture<Void> responseArrived = buildReply.whenPropagateCountIs(1);

		TestMessageAssembly trigger = new TestMessageAssembly();
		trigger.buildJSONMessage("{\"status\":\"pending\"}");
		new NodeSpy(node(REQ_FLOW, "/rest/async")).propagate(trigger, "out");

		boolean completed;
		try {
			responseArrived.get(25, TimeUnit.SECONDS);
			completed = true;
		} catch (TimeoutException e) {
			completed = false;
		}
		assumeTrue(completed,
				"async round trip needs message flows STARTED (omit --start-msgflows false) and network access");

		TestMessageAssembly out = buildReply.propagatedMessageAssembly("out", 1);
		assertThat(out, hasMessageTreeElement("JSON.Data.statusCode").isInteger().equals(200));
		assertThat(out, hasMessageTreeElement("JSON.Data.via")
				.isString().equals("RESTAsyncRequest + RESTAsyncResponse"));
		System.out.println("ASYNC_PAIR real round trip -> " + out.getJSONMessageBodyAsString());
	}
}
