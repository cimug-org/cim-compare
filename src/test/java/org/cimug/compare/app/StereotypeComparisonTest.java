package org.cimug.compare.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;

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
 * End to end, from two XMI exports to the comparison XML, the enriched XML and
 * the report (#118): elements with several stereotypes. The two exports are
 * made from {@code multiroot/rootB-Extensions.xmi} by giving its class, its
 * attribute and its generalization stereotypes the way EA writes them: the
 * first as the {@code stereotype} tagged value, the full list in
 * {@code $ea_xref_property}.
 *
 * <pre>
 *                 baseline                  destination
 * ExtLine         ShadowExtension, util     ShadowExtension        a stereotype removed
 * ratingMVA       alpha, beta               beta, alpha            order only
 * Generalization  UTIL                      util                   case only
 * </pre>
 */
public class StereotypeComparisonTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private static final Charset CP1252 = Charset.forName("windows-1252");
	private static final XPath XP = XPathFactory.newInstance().newXPath();

	private static String xref(List<String> names) {
		StringBuilder d = new StringBuilder();
		for (String n : names)
			d.append("@STEREO;Name=").append(n).append(";GUID={00000000-0000-0000-0000-000000000000};@ENDSTEREO;");
		return "$XREFPROP=$XID={E0C07000-0000-0000-0000-0000000000AA}$XID;$NAM=Stereotypes$NAM;$TYP=element property$TYP;"
				+ "$VIS=Public$VIS;$PAR=0$PAR;$DES=" + d + "$DES;$CLT={E0C07000-0000-0000-0000-000000000003}$CLT;"
				+ "$SUP=&lt;none&gt;$SUP;$ENDXREF;";
	}

	/** Tagged values for a stereotype list, inserted after the given anchor tag. */
	private static String withStereotypes(String xmi, String anchor, List<String> names, boolean appendToXref) {
		String tags = "<UML:TaggedValue tag=\"stereotype\" value=\"" + names.get(0) + "\" />";
		if (!appendToXref)
			tags += "<UML:TaggedValue tag=\"$ea_xref_property\" value=\"" + xref(names) + "\" />";
		int i = xmi.indexOf(anchor);
		assertTrue("anchor in fixture: " + anchor, i > 0);
		i = xmi.indexOf("/>", i) + 2;
		xmi = xmi.substring(0, i) + tags + xmi.substring(i);
		if (appendToXref) {
			// the class already has an $ea_xref_property (its custom properties): EA adds the Stereotypes entry to it
			int c = xmi.indexOf("$ENDXREF;\"", xmi.indexOf("tag=\"$ea_xref_property\"", i));
			xmi = xmi.substring(0, c) + "$ENDXREF;" + xref(names) + xmi.substring(c + "$ENDXREF;".length());
		}
		return xmi;
	}

	private File export(String name, List<String> cls, List<String> attr, List<String> gen) throws Exception {
		File src = new File(getClass().getClassLoader().getResource("multiroot/rootB-Extensions.xmi").toURI());
		String xmi = new String(Files.readAllBytes(src.toPath()), CP1252);
		xmi = withStereotypes(xmi, "<UML:TaggedValue tag=\"package_name\" value=\"LineExtensions\"", cls, true);
		xmi = withStereotypes(xmi, "<UML:TaggedValue tag=\"ea_guid\" value=\"{E0C07000-0000-0000-0000-000000000006}\"", attr, false);
		xmi = withStereotypes(xmi, "<UML:TaggedValue tag=\"ea_type\" value=\"Generalization\"", gen, false);
		File f = tmp.newFile(name);
		Files.write(f.toPath(), xmi.getBytes(CP1252));
		return f;
	}

	private static String str(Document d, String xpath) throws Exception {
		return XP.evaluate(xpath, d);
	}

	@Test
	public void everyStereotypeIsComparedAndReported() throws Exception {
		File baseline = export("baseline.xmi", java.util.Arrays.asList("ShadowExtension", "util"),
				java.util.Arrays.asList("alpha", "beta"), java.util.Arrays.asList("UTIL"));
		File destination = export("destination.xmi", java.util.Arrays.asList("ShadowExtension"),
				java.util.Arrays.asList("beta", "alpha"), java.util.Arrays.asList("util"));
		File xml = new File(tmp.getRoot(), "comparison.xml");
		DiffXMLGenerator.main(new String[] { baseline.getPath(), destination.getPath(), xml.getPath(), "jpg" });

		Document log = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xml);
		String cls = "//CompareItem[@name='ExtLine' and @type='Class']/Properties/Property[@name='Stereotype']";
		assertEquals("ShadowExtension, util", str(log, cls + "/@baseline"));
		assertEquals("ShadowExtension", str(log, cls + "/@model"));
		assertEquals("Changed", str(log, cls + "/@status"));
		String att = "//CompareItem[@name='ratingMVA']/Properties/Property[@name='Stereotype']";
		assertEquals("alpha, beta", str(log, att + "/@baseline"));
		assertEquals("beta, alpha", str(log, att + "/@model"));
		assertEquals("Changed", str(log, att + "/@status"));
		String gen = "//CompareItem[@guid='{E0C07000-0000-0000-0000-000000000004}']/Properties/Property[@name='Stereotype']";
		assertEquals("UTIL", str(log, gen + "/@baseline"));
		assertEquals("util", str(log, gen + "/@model"));

		Document enriched = new ReportPreProcessor(false, null).process(log);
		assertEquals("ShadowExtension", str(enriched, "//Class[@name='ExtLine']/@stereotype"));
		assertEquals("", str(enriched, "//Class[@name='ExtLine']/Properties/Property[@name='Stereotype']/@tag"));
		assertEquals("order only", str(enriched, "//Attribute[@name='ratingMVA']/Properties/Property[@name='Stereotype']/@tag"));
		assertEquals("case only", str(enriched, "//Link/Properties/Property[@name='Stereotype']/@tag"));

		File html = new File(tmp.getRoot(), "report.html");
		new ReportRenderer(false, null, false, "jpg").render(xml, html);
		String page = new String(Files.readAllBytes(html.toPath()), "UTF-8");
		assertTrue(page.contains("<tr data-status=\"changed\"><th class=\"k\">Stereotype</th><td>ShadowExtension, util</td><td>ShadowExtension</td></tr>"));
		assertTrue(page.contains("<td>alpha, beta</td><td>beta, alpha<span class=\"notation\">order only</span></td>"));
		assertTrue(page.contains("<td>UTIL</td><td>util<span class=\"notation\">case only</span></td>"));
	}
}
