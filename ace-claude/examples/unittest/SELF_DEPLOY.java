import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static com.ibm.integration.test.v1.Matchers.hasMessageTreeElement;
import static com.ibm.integration.test.v1.Matchers.terminalPropagateCountIs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.ibm.integration.test.v1.*;

/*
 * A test project CAN deploy its own application - TestSetup.deployBarFile()
 * works, and the freshly deployed flow is immediately spy-able and drivable.
 *
 * !! SIDE EFFECT: this is a REAL deployment, not an in-memory one. The app is
 * unpacked into <work-dir>/run/ and stays there after the run. A suite that uses
 * this is not side-effect free, which is why it is isolated in its own class.
 *
 * Fixture: DEPLOY_PROBE_APP, packaged to <BARS>/DEPLOY_PROBE.bar and deliberately
 * NOT deployed to the work dir beforehand.
 */
public class SELF_DEPLOY {

	private static final String BAR_DIR = "/path/to/probebars";
	private static final String APP     = "DEPLOY_PROBE_APP";
	private static final String FLOW    = "PROBE_MF";

	@AfterEach
	public void cleanup() {
		TestSetup.restoreAllMocks();
	}

	private static SpyObjectReference node(String name) throws Exception {
		return new SpyObjectReference().application(APP).messageFlow(FLOW).node(name);
	}

	@Test
	public void deploysItsOwnBarThenDrivesTheFlow() throws Exception {
		// Either a search path + bare filename, or an absolute path, works.
		TestSetup.setBarFileSearchPath(BAR_DIR);
		assertTrue(TestSetup.deployBarFile("DEPLOY_PROBE.bar"), "deployBarFile should report success");

		NodeSpy build = new NodeSpy(node("probe_build"));
		new NodeSpy(node("probe_reply")).setStopAtInputTerminal("in");

		TestMessageAssembly t = new TestMessageAssembly();
		t.buildJSONMessage("{\"echo\":\"hello\"}");
		new NodeSpy(node("/probe/in")).propagate(t, "out");

		assertThat(build, terminalPropagateCountIs("out", 1));
		TestMessageAssembly out = build.propagatedMessageAssembly("out", 1);
		assertThat(out, hasMessageTreeElement("JSON.Data.deployedByTest").isBoolean().equals(true));
		assertThat(out, hasMessageTreeElement("JSON.Data.echo").isString().equals("hello"));
	}

	@Test
	public void acceptsAnAbsoluteBarPath() throws Exception {
		assertTrue(TestSetup.deployBarFile(BAR_DIR + "/DEPLOY_PROBE.bar"));
	}

	/**
	 * isTrainedForCall() takes the INPUT TERMINAL name, not a call index - which
	 * is consistent with the native layer having no call-index parameter at all.
	 */
	@Test
	public void isTrainedForCallTakesATerminalName() throws Exception {
		NodeStub stub = new NodeStub(new SpyObjectReference().application("REST_DEMO_APP")
				.messageFlow("REST_INVENTORY_MF").node("get_inventory"));
		TestMessageAssembly c = new TestMessageAssembly();
		c.buildJSONMessage("{}");
		stub.onCall().propagatesMessage("in", "out", c);

		assertTrue(stub.isTrainedForCall("in"), "the trained input terminal");
		assertFalse(stub.isTrainedForCall("out"), "an output terminal is not a call");
		assertFalse(stub.isTrainedForCall("1"), "not a call index");
		assertFalse(stub.isTrainedForCall("bogus"));
	}
}
