import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static com.ibm.integration.test.v1.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ibm.integration.test.v1.*;

/*
 * The four ways to assert on a propagated message, as a reference. Note that
 * the API uses TWO DIFFERENT PATH SYNTAXES:
 *
 *   hasMessageTreeElement / messagePath : dotted from Root, no $ and no slashes
 *                                         -> "JSON.Data.inventory.available"
 *   ignorePath                          : slash-delimited with a LEADING slash
 *                                         -> "/JSON/Data/drop"
 *
 * A "$."-prefixed path is only correct when BUILDING an assembly
 * (localEnvironmentPath("$.WrittenDestination...")), never when reading one.
 */
public class ASSERTION_STYLES {

	@AfterEach
	public void cleanup() {
		TestSetup.restoreAllMocks();
	}

	private static SpyObjectReference node(String name) throws Exception {
		return new SpyObjectReference().application("REST_DEMO_APP")
				.messageFlow("REST_INVENTORY_MF").node(name);
	}

	private TestMessageAssembly reply() throws Exception {
		NodeStub rest = new NodeStub(node("get_inventory"));
		TestMessageAssembly canned = new TestMessageAssembly();
		canned.buildJSONMessage("{\"available\":42,\"pending\":7}");
		rest.onCall().propagatesMessage("in", "out", canned);
		NodeSpy buildReply = new NodeSpy(node("build_reply"));
		new NodeSpy(node("reply")).setStopAtInputTerminal("in");
		TestMessageAssembly t = new TestMessageAssembly();
		t.buildJSONMessage("{}");
		new NodeSpy(node("/rest/inventory")).propagate(t, "out");
		return buildReply.propagatedMessageAssembly("out", 1);
	}

	/** 1. Whole-body JSON compare - brittle but shows everything at once. */
	@Test
	public void wholeBodyJsonCompare() throws Exception {
		assertEquals("{\"inventory\":{\"available\":42,\"pending\":7}}",
				reply().getJSONMessageBodyAsString());
	}

	/** 2. Tree matcher with type checking - the most precise option. */
	@Test
	public void treeMatcherWithTypes() throws Exception {
		TestMessageAssembly out = reply();
		assertThat(out, hasMessageTreeElement("JSON.Data.inventory.available").isInteger().equals(42));
		assertThat(out, hasMessageTreeElement("JSON.Data.inventory.pending").isInteger().equals(7));
	}

	/** 2b. ignoreTypes() compares the value as text, whatever its logical type. */
	@Test
	public void treeMatcherIgnoringTypes() throws Exception {
		assertThat(reply(), hasMessageTreeElement("JSON.Data.inventory.available").ignoreTypes().equals("42"));
	}

	/**
	 * 3. Direct read via messagePath(). This DOES work on a propagated assembly,
	 * but only with the dotted-from-Root form and only on a LEAF - a folder path
	 * raises BIP2111.
	 */
	@Test
	public void directReadOfALeaf() throws Exception {
		assertEquals("42", reply().messagePath("JSON.Data.inventory.available").getValueAsString());
	}

	/** 4. Compare against a built assembly, ignoring a path that is allowed to differ. */
	@Test
	public void compareAgainstBuiltAssemblyIgnoringAPath() throws Exception {
		TestMessageAssembly expected =
				TestMessageAssembly.buildAssemblyFromJSONBody("{\"inventory\":{\"available\":42,\"pending\":999}}");
		assertThat(reply(), equalsMessage(expected).ignorePath("/JSON/Data/inventory/pending", true));
	}

	/** The serialized tree is the quickest way to discover a path or a value type. */
	@Test
	public void serializedTreeShowsPathsAndTypes() throws Exception {
		String xml = reply().getMessageTreeSerializedForm();
		System.out.println("ASSERTION_STYLES serialized tree = " + xml);
		org.junit.jupiter.api.Assertions.assertTrue(xml.contains("iib:valueType=\"INTEGER\""));
	}
}
