package org.cimug.compare.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.cimug.compare.report.ReportPreProcessor;
import org.cimug.compare.report.ReportRenderer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Document;

/**
 * #169, end to end from two XMI exports: user-defined tagged values on a package, a
 * class, an attribute, an association and an association end, in both forms EA writes
 * them: inside the element's ModelElement.taggedValue, and on their own under
 * XMI.content naming their element with modelElement (as EA writes package tags).
 * Both exports are made from {@code multiroot/rootB-Extensions.xmi}, with a
 * self-association added to ExtLine.
 *
 * <pre>
 *                         baseline              destination
 * LineExtensions baseuri  http://example.com/a#  http://example.com/b#  changed (same tag id)
 * LineExtensions baseprefix  ext                 ext                    unchanged (written again with a new id)
 * ExtLine owner           Planning              -                      removed
 * ratingMVA unit          MVA                   kVA                    changed
 * Association baseuri     -                     http://example.com/b#  added
 * Association baseprefix  -                     ext                    added
 * source end note         -                     reviewed               added
 * </pre>
 */
public class TaggedValueComparisonTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private static final Charset CP1252 = Charset.forName("windows-1252");
	private static final XPath XP = XPathFactory.newInstance().newXPath();

	private static final String PKG = "EAID_E0C07000_0000_0000_0000_000000000002";
	private static final String ASSOC = "EAID_E0C07000_0000_0000_0000_0000000000A1";

	private static String tag(String id, String name, String value) {
		return "<UML:TaggedValue xmi.id=\"" + id + "\" tag=\"" + name + "\" value=\"" + value + "\"/>";
	}

	private static String standalone(String id, String name, String value, String element) {
		return "<UML:TaggedValue tag=\"" + name + "\" xmi.id=\"" + id + "\" value=\"" + value + "\" modelElement=\"" + element
				+ "\"/>";
	}

	private static String association(String assocTags, String sourceTags) {
		return "<UML:Association xmi.id=\"" + ASSOC + "\" visibility=\"public\" isRoot=\"false\" isLeaf=\"false\" isAbstract=\"false\">"
				+ "<UML:ModelElement.taggedValue>"
				+ "<UML:TaggedValue tag=\"style\" value=\"3\"/><UML:TaggedValue tag=\"ea_type\" value=\"Association\"/>"
				+ "<UML:TaggedValue tag=\"direction\" value=\"Unspecified\"/>"
				+ "<UML:TaggedValue tag=\"ea_sourceName\" value=\"ExtLine\"/><UML:TaggedValue tag=\"ea_targetName\" value=\"ExtLine\"/>"
				+ "<UML:TaggedValue tag=\"ea_sourceType\" value=\"Class\"/><UML:TaggedValue tag=\"ea_targetType\" value=\"Class\"/>"
				+ assocTags + "</UML:ModelElement.taggedValue><UML:Association.connection>"
				+ "<UML:AssociationEnd visibility=\"public\" multiplicity=\"0..*\" name=\"Parts\" aggregation=\"none\" isOrdered=\"false\""
				+ " targetScope=\"instance\" changeable=\"none\" isNavigable=\"true\" type=\"EAID_E0C07000_0000_0000_0000_000000000003\">"
				+ "<UML:ModelElement.taggedValue><UML:TaggedValue tag=\"containment\" value=\"Unspecified\"/>"
				+ "<UML:TaggedValue tag=\"ea_end\" value=\"source\"/>" + sourceTags
				+ "</UML:ModelElement.taggedValue></UML:AssociationEnd>"
				+ "<UML:AssociationEnd visibility=\"public\" multiplicity=\"0..1\" name=\"Whole\" aggregation=\"none\" isOrdered=\"false\""
				+ " targetScope=\"instance\" changeable=\"none\" isNavigable=\"true\" type=\"EAID_E0C07000_0000_0000_0000_000000000003\">"
				+ "<UML:ModelElement.taggedValue><UML:TaggedValue tag=\"containment\" value=\"Unspecified\"/>"
				+ "<UML:TaggedValue tag=\"ea_end\" value=\"target\"/></UML:ModelElement.taggedValue></UML:AssociationEnd>"
				+ "</UML:Association.connection></UML:Association>";
	}

	private static String insertAfter(String xmi, String anchor, String text) {
		int i = xmi.indexOf(anchor);
		assertTrue("anchor in fixture: " + anchor, i > 0);
		i = xmi.indexOf("/>", i) + 2;
		return xmi.substring(0, i) + text + xmi.substring(i);
	}

	private File export(String name, boolean destination) throws Exception {
		File src = new File(getClass().getClassLoader().getResource("multiroot/rootB-Extensions.xmi").toURI());
		String xmi = new String(Files.readAllBytes(src.toPath()), CP1252);
		// the class's own tag, and the attribute's
		if (!destination)
			xmi = insertAfter(xmi, "<UML:TaggedValue tag=\"package_name\" value=\"LineExtensions\"",
					tag("EAID_7A600000_0000_0000_0000_000000000003", "owner", "Planning"));
		xmi = insertAfter(xmi, "<UML:TaggedValue tag=\"ea_guid\" value=\"{E0C07000-0000-0000-0000-000000000006}\"",
				tag("EAID_7A600000_0000_0000_0000_000000000004", "unit", destination ? "kVA" : "MVA"));
		// the association, inside LineExtensions after ExtLine
		String end = "</UML:Namespace.ownedElement></UML:Package></UML:Namespace.ownedElement></UML:Package>";
		int i = xmi.indexOf(end);
		assertTrue(i > 0);
		String assoc = destination
				? association(tag("EAID_7A600000_0000_0000_0000_000000000006", "baseuri", "http://example.com/b#")
						+ tag("EAID_7A600000_0000_0000_0000_000000000007", "baseprefix", "ext"),
						tag("EAID_7A600000_0000_0000_0000_000000000008", "note", "reviewed"))
				: association("", "");
		xmi = xmi.substring(0, i) + assoc + xmi.substring(i);
		// the package's tags, on their own after the model
		String pkgTags = standalone("EAID_7A600000_0000_0000_0000_000000000001", "baseuri",
				destination ? "http://example.com/b#" : "http://example.com/a#", PKG)
				+ standalone(destination ? "EAID_7A600000_0000_0000_0000_000000000005" : "EAID_7A600000_0000_0000_0000_000000000002",
						"baseprefix", "ext", PKG);
		int m = xmi.indexOf("</UML:Model>") + "</UML:Model>".length();
		xmi = xmi.substring(0, m) + pkgTags + xmi.substring(m);
		File f = tmp.newFile(name);
		Files.write(f.toPath(), xmi.getBytes(CP1252));
		return f;
	}

	private static String str(Document d, String xpath) throws Exception {
		return XP.evaluate(xpath, d);
	}

	@Test
	public void taggedValuesAreComparedOnEveryKindOfElement() throws Exception {
		File xml = new File(tmp.getRoot(), "comparison.xml");
		DiffXMLGenerator.main(new String[] { export("baseline.xmi", false).getPath(), export("destination.xmi", true).getPath(),
				xml.getPath(), "jpg" });
		Document log = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xml);

		// EA's form: a "Tagged Values" group under the element, one item per tag
		String pkg = "//CompareItem[@name='LineExtensions']/CompareItem[@type='Tagged Values']";
		assertEquals("{E0C07000-0000-0000-0000-000000000002}Tagged Values", str(log, pkg + "/@guid"));
		String uri = pkg + "/CompareItem[@guid='{7A600000-0000-0000-0000-000000000001}']";
		assertEquals("Changed", str(log, uri + "/@status"));
		assertEquals("baseuri = http://example.com/b#", str(log, uri + "/@name"));
		assertEquals("http://example.com/a#", str(log, uri + "/Properties/Property[@name='Value']/@baseline"));
		assertEquals("http://example.com/b#", str(log, uri + "/Properties/Property[@name='Value']/@model"));
		assertEquals("Identical", str(log, uri + "/Properties/Property[@name='Name']/@status"));
		// written again with a new id: matched by name
		assertEquals("Identical", str(log, pkg + "/CompareItem[starts-with(@name,'baseprefix')]/@status"));
		assertEquals("1", str(log, "count(" + pkg + "/CompareItem[starts-with(@name,'baseprefix')])"));

		String cls = "//CompareItem[@name='ExtLine' and @type='Class']";
		assertEquals("Baseline only", str(log, cls + "/CompareItem[@type='Tagged Values']/CompareItem[starts-with(@name,'owner')]/@status"));
		assertEquals("Changed", str(log, cls + "/@status"));
		String att = "//CompareItem[@name='ratingMVA']/CompareItem[@type='Tagged Values']/CompareItem";
		assertEquals("Changed", str(log, att + "/@status"));
		assertEquals("kVA", str(log, att + "/Properties/Property[@name='Value']/@model"));

		String assoc = "(//CompareItem[@name='Association' and @guid='{E0C07000-0000-0000-0000-0000000000A1}'])[1]";
		assertEquals("Changed", str(log, assoc + "/@status")); // otherwise identical; its tags changed
		assertEquals("Model only", str(log, assoc + "/CompareItem[@type='Tagged Values']/CompareItem[starts-with(@name,'baseuri')]/@status"));
		assertEquals("ext", str(log, assoc + "/CompareItem[@type='Tagged Values']/CompareItem[starts-with(@name,'baseprefix')]/Properties/Property[@name='Value']/@model"));
		assertEquals("reviewed", str(log, assoc + "/CompareItem[@type='Src']/CompareItem[@type='Tagged Values']/CompareItem/Properties/Property[@name='Value']/@model"));
		assertEquals("0", str(log, "count(" + assoc + "/CompareItem[@type='Dst']/CompareItem)"));

		// the enriched XML: every tag, with its status
		Document enriched = new ReportPreProcessor(false, null).process(log);
		assertEquals("changed", str(enriched, "//Package[@name='LineExtensions']/@status"));
		assertEquals("changed", str(enriched, "//Package[@name='LineExtensions']/Tags/Tag[@name='baseuri']/@status"));
		assertEquals("identical", str(enriched, "//Package[@name='LineExtensions']/Tags/Tag[@name='baseprefix']/@status"));
		assertEquals("deleted", str(enriched, "//Class[@name='ExtLine']/Tags/Tag[@name='owner']/@status"));
		assertEquals("changed", str(enriched, "//Attribute[@name='ratingMVA']/@status"));
		assertEquals("MVA", str(enriched, "//Attribute[@name='ratingMVA']/Tags/Tag/@baseline"));
		assertEquals("changed", str(enriched, "(//Link[@kind='Association'])[1]/@status"));
		assertEquals("added", str(enriched, "(//Link[@kind='Association'])[1]/Tags/Tag[@name='baseprefix']/@status"));
		assertEquals("reviewed", str(enriched, "(//Link[@kind='Association'])[1]/End[@side='source']/Tags/Tag/@model"));

		// the report
		File html = new File(tmp.getRoot(), "report.html");
		new ReportRenderer(false, null, false, "jpg").render(xml, html);
		String page = new String(Files.readAllBytes(html.toPath()), "UTF-8");
		assertTrue(page.contains("<tr data-status=\"changed\"><th class=\"k\">baseuri</th><td>http://example.com/a#</td><td>http://example.com/b#</td></tr>"));
		assertTrue(page.contains("<tr data-status=\"deleted\"><th class=\"k\">owner</th><td>Planning</td><td class=\"nil\">(no tag)</td></tr>"));
		assertTrue(page.contains("<tr data-status=\"added\"><th class=\"k\">baseprefix</th><td class=\"nil\">(no tag)</td><td>ext</td></tr>"));
		assertTrue(page.contains("<tr data-status=\"added\"><th class=\"k\">note</th><td class=\"nil\">(no tag)</td><td>reviewed</td></tr>"));
		assertTrue("a section with the tag icon", page.contains("<svg class=\"tg\""));
		// the attribute's tag change is in its ⓘ metadata, and marks the row
		assertTrue(page.contains("<tr data-status=\"changed\"><th class=\"k\">unit</th><td>MVA</td><td>kVA</td></tr>"));
		assertTrue(page.contains("metadata changed - hover"));
	}
}
