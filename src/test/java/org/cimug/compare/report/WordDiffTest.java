package org.cimug.compare.report;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class WordDiffTest {

	private Document doc;
	private Element parent;
	private double ratio;

	@Before
	public void setUp() throws Exception {
		doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		parent = doc.createElement("Redline");
	}

	private void diff(String baseline, String destination) {
		parent = doc.createElement("Redline");
		ratio = WordDiff.appendRedline(doc, parent, baseline, destination);
	}

	/** Text of the redline as read from one side: plain text plus del (baseline) or ins (destination). */
	private String side(String keep) {
		StringBuilder sb = new StringBuilder();
		for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
			if (n.getNodeType() == Node.TEXT_NODE || keep.equals(n.getNodeName()))
				sb.append(n.getTextContent());
		}
		return sb.toString();
	}

	/** Compact form for assertions: text as-is, [-deleted-], {+inserted+}. */
	private String shape() {
		StringBuilder sb = new StringBuilder();
		for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
			if ("del".equals(n.getNodeName()))
				sb.append("[-").append(n.getTextContent()).append("-]");
			else if ("ins".equals(n.getNodeName()))
				sb.append("{+").append(n.getTextContent()).append("+}");
			else
				sb.append(n.getTextContent());
		}
		return sb.toString();
	}

	private int nodes(String name) {
		return parent.getElementsByTagName(name).getLength();
	}

	@Test
	public void identicalTextIsPlain() {
		diff("Same text.", "Same text.");
		assertEquals("Same text.", shape());
		assertEquals(0, nodes("del") + nodes("ins"));
		assertEquals(1.0, ratio, 0.0);
	}

	@Test
	public void nullIsTreatedAsEmpty() {
		diff(null, null);
		assertEquals("", shape());
		assertEquals(1.0, ratio, 0.0);
	}

	@Test
	public void addedTextIsOneInsertion() {
		diff("", "New description.");
		assertEquals("{+New description.+}", shape());
		assertEquals(0.0, ratio, 0.0);
	}

	@Test
	public void removedTextIsOneDeletion() {
		diff("Old description.", null);
		assertEquals("[-Old description.-]", shape());
		assertEquals(0.0, ratio, 0.0);
	}

	@Test
	public void insertedWordIsMarkedInPlace() {
		diff("The quick fox jumps.", "The quick brown fox jumps.");
		assertEquals("The quick {+brown +}fox jumps.", shape());
	}

	@Test
	public void replacedWordIsDeletionThenInsertion() {
		diff("Length of the segment in metres.", "Length of the segment in meters.");
		assertEquals("Length of the segment in [-metres-]{+meters+}.", shape());
		assertTrue(ratio > WordDiff.REWRITE_THRESHOLD);
	}

	@Test
	public void punctuationIsItsOwnToken() {
		diff("Value, in kV.", "Value; in kV.");
		assertEquals("Value[-,-]{+;+} in kV.", shape());
	}

	@Test
	public void bothSidesAreRecoverableFromTheRedline() {
		String[][] pairs = {
				{ "A switch is a generic device.", "A switch is a generic device designed to close, or open, or both." },
				{ "Remove   extra  spaces", "Remove extra spaces" },
				{ "line one\nline two", "line one\nline 2\nline three" },
				{ "x", "y" },
				{ "Positive sequence series resistance of the entire line section.",
						"Zero sequence series resistance of the entire line section (r0)." } };
		for (String[] p : pairs) {
			diff(p[0], p[1]);
			assertEquals("baseline side of " + shape(), p[0], side("del"));
			assertEquals("destination side of " + shape(), p[1], side("ins"));
		}
	}

	@Test
	public void mostlyDifferentTextIsShownAsARewrite() {
		diff("Used to describe the type of load applied to the transformer winding.",
				"Deprecated: see PowerTransformerEnd for the replacement concept.");
		assertTrue("ratio " + ratio, ratio < WordDiff.REWRITE_THRESHOLD);
		assertEquals(1, nodes("del"));
		assertEquals(1, nodes("ins"));
		assertEquals("del", parent.getFirstChild().getNodeName());
		assertEquals(" ", parent.getFirstChild().getNextSibling().getTextContent());
		assertEquals("ins", parent.getLastChild().getNodeName());
	}

	@Test
	public void shortTextsAreNeverRewritten() {
		// three words or fewer on one side: a word diff is still readable
		diff("Load area", "Energy area");
		assertEquals("[-Load-]{+Energy+} area", shape());
	}

	@Test
	public void veryLongTextFallsBackToWholeReplacement() {
		StringBuilder a = new StringBuilder(), b = new StringBuilder();
		for (int i = 0; i < 1100; i++) {
			a.append("word").append(i).append(' ');
			b.append("word").append(i).append(' ');
		}
		b.append("extra");
		diff(a.toString(), b.toString());
		assertEquals(0.0, ratio, 0.0);
		assertEquals(1, nodes("del"));
		assertEquals(1, nodes("ins"));
		assertEquals(a.toString(), parent.getFirstChild().getTextContent());
		assertEquals(b.toString(), parent.getLastChild().getTextContent());
	}

	@Test
	public void tokensConcatenateToTheOriginal() {
		String s = "  IEC61970CIM17v40: r_0 (ohm/km), 1.5e-3 \t\n end";
		List<String> t = WordDiff.tokenize(s);
		StringBuilder sb = new StringBuilder();
		for (String x : t)
			sb.append(x);
		assertEquals(s, sb.toString());
		assertTrue(t.contains("IEC61970CIM17v40"));
		assertTrue("underscore joins a word", t.contains("r_0"));
		assertTrue(t.contains(":"));
		assertTrue(t.contains("("));
		assertTrue("whitespace runs are single tokens", t.contains(" \t\n "));
	}
}
