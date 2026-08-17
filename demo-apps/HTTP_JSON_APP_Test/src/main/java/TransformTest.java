import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static com.ibm.integration.test.v1.Matchers.propagatedJSONFromTerminalOnCall;
import static com.ibm.integration.test.v1.Matchers.terminalPropagateCountIs;
import static org.hamcrest.MatcherAssert.assertThat;

import com.ibm.integration.test.v1.NodeSpy;
import com.ibm.integration.test.v1.SpyObjectReference;
import com.ibm.integration.test.v1.TestMessageAssembly;
import com.ibm.integration.test.v1.TestSetup;

/*
 * Unit tests for the build_json_resp Compute node in HTTP_JSON_MF.
 * The node under test is invoked directly with an injected message assembly -
 * no HTTP listener, queue, or client involved.
 */
public class TransformTest {

	@AfterEach
	public void cleanup() {
		TestSetup.restoreAllMocks();
	}

	private NodeSpy invoke(String jsonBody) throws Exception {
		SpyObjectReference nodeRef = new SpyObjectReference()
				.application("HTTP_JSON_APP")
				.messageFlow("HTTP_JSON_MF")
				.node("build_json_resp");
		NodeSpy spy = new NodeSpy(nodeRef);

		TestMessageAssembly input = new TestMessageAssembly();
		input.buildJSONMessage(jsonBody);

		spy.evaluate(input, true, "in");

		assertThat(spy, terminalPropagateCountIs("out", 1));
		return spy;
	}

	@Test
	public void fullNameIsConcatenated() throws Exception {
		NodeSpy spy = invoke("{\"firstName\":\"Ada\",\"lastName\":\"Lovelace\",\"age\":36}");
		assertThat(spy, propagatedJSONFromTerminalOnCall("{\"fullName\":\"Ada Lovelace\",\"isAdult\":true,\"status\":\"success\"}", "out", 1));
	}

	@Test
	public void adultAtEighteen() throws Exception {
		NodeSpy spy = invoke("{\"firstName\":\"Just\",\"lastName\":\"Eighteen\",\"age\":18}");
		assertThat(spy, propagatedJSONFromTerminalOnCall("{\"fullName\":\"Just Eighteen\",\"isAdult\":true,\"status\":\"success\"}", "out", 1));
	}

	@Test
	public void minorBelowEighteen() throws Exception {
		NodeSpy spy = invoke("{\"firstName\":\"Still\",\"lastName\":\"Seventeen\",\"age\":17}");
		assertThat(spy, propagatedJSONFromTerminalOnCall("{\"fullName\":\"Still Seventeen\",\"isAdult\":false,\"status\":\"success\"}", "out", 1));
	}
}
