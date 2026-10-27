package org.cimug.compare.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBIntrospector;
import javax.xml.parsers.DocumentBuilderFactory;

import org.cimug.compare.report.ReportRenderer;
import org.cimug.compare.uml1_3.PackageType;
import org.cimug.compare.xmi1_1.XMI;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Merging the per-root XMI exports of an EA project with several root nodes
 * (issue #88). The fixtures are two small exports built from real EA XMI 1.1
 * elements: root {@code Model} (Core › IdentifiedObject) and root {@code ERCOT}
 * (ErcotExtensions › ErcotLine, which generalizes IdentifiedObject in the other
 * root, with a diagram, EA stubs under XMI.extensions as EA writes them, and
 * anonymous {@code eaxmiid} data types).
 */
public class XmiMergerTest {

	private static final String UML = "omg.org/UML1.3";
	private static final String IDENTIFIED_OBJECT = "EAID_055C5806_E9D8_4a7a_9307_DAF7C31763C7";

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private File fixture(String name) throws Exception {
		File f = new File(tmp.getRoot(), name);
		InputStream in = getClass().getClassLoader().getResourceAsStream("multiroot/" + name);
		try {
			Files.copy(in, f.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} finally {
			in.close();
		}
		return f;
	}

	private File merged() throws Exception {
		File out = new File(tmp.getRoot(), "merged.xmi");
		XmiMerger.merge(Arrays.asList(fixture("rootA-Model.xmi"), fixture("rootB-ERCOT.xmi")), out);
		return out;
	}

	private static Document parse(File f) throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		return dbf.newDocumentBuilder().parse(f);
	}

	private static List<Element> children(Element parent, String ns, String local) {
		List<Element> l = new ArrayList<Element>();
		for (org.w3c.dom.Node n = parent.getFirstChild(); n != null; n = n.getNextSibling())
			if (n instanceof Element && local.equals(n.getLocalName())
					&& (ns == null ? n.getNamespaceURI() == null : ns.equals(n.getNamespaceURI())))
				l.add((Element) n);
		return l;
	}

	private static Element only(Element parent, String ns, String local) {
		List<Element> l = children(parent, ns, local);
		assertEquals(local, 1, l.size());
		return l.get(0);
	}

	private static Element ownedElement(Document d) {
		Element content = only(d.getDocumentElement(), null, "XMI.content");
		Element model = only(content, UML, "Model");
		return only(model, UML, "Namespace.ownedElement");
	}

	@Test
	public void rootsAreSiblingPackagesUnderOneModel() throws Exception {
		Document d = parse(merged());
		Element model = (Element) ownedElement(d).getParentNode();
		assertEquals("EA Model", model.getAttribute("name"));
		assertEquals("model id comes from the first root's export", "MX_EAID_6952FF0A_7BCE_4b37_88F7_399320BB07D0",
				model.getAttribute("xmi.id"));

		List<String> packages = new ArrayList<String>();
		int rootClasses = 0;
		for (Element e : children(ownedElement(d), UML, "Package"))
			packages.add(e.getAttribute("name"));
		for (Element e : children(ownedElement(d), UML, "Class"))
			if ("EARootClass".equals(e.getAttribute("name")))
				rootClasses++;
		assertEquals(Arrays.asList("Model", "ERCOT"), packages);
		assertEquals("EARootClass kept once", 1, rootClasses);
	}

	@Test
	public void anonymousIdsFromLaterRootsAreRenamed() throws Exception {
		Document d = parse(merged());
		List<String> ids = new ArrayList<String>();
		for (Element e : children(ownedElement(d), UML, "DataType"))
			ids.add(e.getAttribute("xmi.id"));
		assertTrue(ids.contains("eaxmiid1"));
		assertTrue(ids.contains("eaxmiid1_1"));
		assertEquals("no id defined twice", ids.size(), new java.util.HashSet<String>(ids).size());

		NodeList refs = d.getElementsByTagNameNS(UML, "Classifier");
		boolean renamedRef = false;
		for (int i = 0; i < refs.getLength(); i++)
			if ("eaxmiid1_1".equals(((Element) refs.item(i)).getAttribute("xmi.idref")))
				renamedRef = true;
		assertTrue("ErcotLine.ratingMVA's type reference follows the rename", renamedRef);
	}

	@Test
	public void stubsForElementsInAnotherRootAreDropped() throws Exception {
		File out = new File(tmp.getRoot(), "merged.xmi");
		XmiMerger.Result r = XmiMerger.merge(Arrays.asList(fixture("rootA-Model.xmi"), fixture("rootB-ERCOT.xmi")),
				out);
		assertEquals(1, r.stubsResolved);
		assertEquals(0, r.duplicateCount);

		// EA writes its stubs under XMI.extensions.
		Document d = parse(out);
		Element content = only(d.getDocumentElement(), null, "XMI.content");
		Element ext = only(d.getDocumentElement(), null, "XMI.extensions");
		List<String> stubs = new ArrayList<String>();
		for (Element s : children(ext, null, "EAStub"))
			stubs.add(s.getAttribute("name"));
		assertEquals(Arrays.asList("Elsewhere"), stubs);
		assertEquals(0, children(content, null, "EAStub").size());
		assertEquals("diagram kept", 1, children(content, UML, "Diagram").size());
	}

	@Test
	public void extensionsFromEveryRootAreKept() throws Exception {
		Element ext = only(parse(merged()).getDocumentElement(), null, "XMI.extensions");
		assertEquals("Enterprise Architect 2.5", ext.getAttribute("xmi.extender"));
		assertEquals(2, children(ext, null, "EANoteLink").size());
		assertEquals("stub resolved by root A left out", 1, children(ext, null, "EAStub").size());
	}

	@Test
	public void newlinesInAttributeValuesSurvive() throws Exception {
		NodeList classes = parse(merged()).getElementsByTagNameNS(UML, "TaggedValue");
		String doc = null;
		for (int i = 0; i < classes.getLength(); i++) {
			Element tv = (Element) classes.item(i);
			if ("documentation".equals(tv.getAttribute("tag"))
					&& tv.getAttribute("value").startsWith("An ERCOT line."))
				doc = tv.getAttribute("value");
		}
		assertEquals("An ERCOT line.\nSecond line.\n", doc);
	}

	@Test
	public void sameElementInTwoRootsIsReported() throws Exception {
		File a = fixture("rootA-Model.xmi");
		File a2 = new File(tmp.getRoot(), "rootA-copy.xmi");
		Files.copy(a.toPath(), a2.toPath());
		XmiMerger.Result r = XmiMerger.merge(Arrays.asList(a, a2), new File(tmp.getRoot(), "dup.xmi"));
		assertTrue(r.duplicateCount > 0);
		assertTrue(r.duplicateIds.containsKey(IDENTIFIED_OBJECT));
	}

	@Test
	public void mergedFileLoadsAsAnXmiModel() throws Exception {
		XMI xmi = (XMI) JAXBIntrospector.getValue(
				JAXBContext.newInstance(XMI.class).createUnmarshaller().unmarshal(merged()));
		List<String> names = new ArrayList<String>();
		for (PackageType p : xmi.getXMIContent().getModel().getPackages())
			names.add(p.getName());
		assertEquals(Arrays.asList("Model", "ERCOT"), names);
		assertEquals(1, xmi.getXMIContent().getDiagrams().size());
		assertEquals("2.5", xmi.getXMIHeader().getXMIDocumentation().getXMIExporterVersion());
	}

	/**
	 * EA writes a connector between classes in two roots into both roots' exports
	 * (seen on a real four-root project: 644 generalizations, associations and
	 * note links, every pair identical). Root A here also carries ErcotLine's
	 * generalization, as EA's export of Model would.
	 */
	private File rootAWithGeneralizationCopy(boolean altered) throws Exception {
		java.nio.charset.Charset cp1252 = java.nio.charset.Charset.forName("windows-1252");
		String b = new String(Files.readAllBytes(fixture("rootB-ERCOT.xmi").toPath()), cp1252);
		java.util.regex.Matcher m = java.util.regex.Pattern
				.compile("<UML:Generalization .*?</UML:Generalization>", java.util.regex.Pattern.DOTALL).matcher(b);
		assertTrue(m.find());
		String copy = m.group().replace("\n", "\n\t\t\t\t\t"); // different indentation, as in EA
		if (altered)
			copy = copy.replace("tag=\"seqno\" value=\"0\"", "tag=\"seqno\" value=\"7\"");
		String a = new String(Files.readAllBytes(fixture("rootA-Model.xmi").toPath()), cp1252);
		int at = a.indexOf("</UML:Class>") + "</UML:Class>".length();
		File f = new File(tmp.getRoot(), altered ? "rootA-altered.xmi" : "rootA-copy.xmi");
		Files.write(f.toPath(), (a.substring(0, at) + "\n" + copy + a.substring(at)).getBytes(cp1252));
		return f;
	}

	@Test
	public void identicalConnectorInTwoRootsIsKeptOnce() throws Exception {
		File out = new File(tmp.getRoot(), "merged.xmi");
		XmiMerger.Result r = XmiMerger.merge(Arrays.asList(rootAWithGeneralizationCopy(false), fixture("rootB-ERCOT.xmi")),
				out);
		assertEquals(1, r.identicalCopiesDropped);
		assertEquals("no duplicate warning", 0, r.duplicateCount);
		NodeList gens = parse(out).getElementsByTagNameNS(UML, "Generalization");
		int n = 0;
		for (int i = 0; i < gens.getLength(); i++)
			if ("EAID_E0C07000_0000_0000_0000_000000000004".equals(((Element) gens.item(i)).getAttribute("xmi.id")))
				n++;
		assertEquals(1, n);
	}

	@Test
	public void differingCopiesOfAConnectorAreReported() throws Exception {
		XmiMerger.Result r = XmiMerger.merge(
				Arrays.asList(rootAWithGeneralizationCopy(true), fixture("rootB-ERCOT.xmi")),
				new File(tmp.getRoot(), "merged.xmi"));
		assertEquals(0, r.identicalCopiesDropped);
		assertEquals(1, r.duplicateCount);
		assertTrue(r.duplicateIds.containsKey("EAID_E0C07000_0000_0000_0000_000000000004"));
	}

	/**
	 * Several --package names on XMI input (#94): the report shows the packages in
	 * the order given, and the scope line lists them.
	 */
	@Test
	public void packageListOnXmiInput() throws Exception {
		File baseline = fixture("rootA-Model.xmi");
		File destination = merged();
		File html = new File(tmp.getRoot(), "list-report.html");
		CIMModelComparisonGenerator.main(new String[] { baseline.getAbsolutePath(), destination.getAbsolutePath(),
				html.getAbsolutePath(), "--package=ERCOT, Model" });
		Element report = parse(new File(tmp.getRoot(), "list-report-enriched.xml")).getDocumentElement();
		assertEquals("ERCOT, Model", report.getAttribute("scope"));
		List<String> top = new ArrayList<String>();
		for (Element p : children(report, null, "Package"))
			top.add(p.getAttribute("name"));
		assertEquals(Arrays.asList("ERCOT", "Model"), top);
	}

	/** Each root's export writes an Images folder; all end up in the side's folder. */
	@Test
	public void imagesFromEveryRootEndUpInOneFolder() throws Exception {
		File images = tmp.newFolder("Images");
		File side = new File(tmp.getRoot(), "Images-destination");
		Files.write(new File(images, "EAID_1.jpg").toPath(), new byte[] { 1 });
		CIMModelComparisonGenerator.moveImages(images, side);
		assertFalse(images.exists());

		assertTrue(images.mkdir());
		Files.write(new File(images, "EAID_2.jpg").toPath(), new byte[] { 2 });
		CIMModelComparisonGenerator.moveImages(images, side);
		assertFalse(images.exists());
		assertEquals(Arrays.asList("EAID_1.jpg", "EAID_2.jpg"), sorted(side.list()));
	}

	private static List<String> sorted(String[] names) {
		List<String> l = new ArrayList<String>(Arrays.asList(names));
		java.util.Collections.sort(l);
		return l;
	}

	/**
	 * End to end: the single-root project compared with the same project after an
	 * ERCOT root was added. ERCOT is reported as added, its generalization to
	 * IdentifiedObject (in the other root) resolves, and nothing in Model changes.
	 */
	@Test
	public void comparisonReportsTheAddedRoot() throws Exception {
		File baseline = fixture("rootA-Model.xmi");
		File destination = merged();
		File comparison = new File(tmp.getRoot(), "comparison-report.xml");
		DiffXMLGenerator.main(new String[] { baseline.getAbsolutePath(), destination.getAbsolutePath(),
				comparison.getAbsolutePath(), "JPG" });

		String xml = new String(Files.readAllBytes(comparison.toPath()), "UTF-8");
		assertTrue("generalization end names the class in the other root", xml.contains("IdentifiedObject"));

		File html = new File(tmp.getRoot(), "comparison-report.html");
		File enriched = new ReportRenderer(false, null, false, "jpg").render(comparison, html);
		Element report = parse(enriched).getDocumentElement();
		assertEquals("Model, ERCOT (full model)", report.getAttribute("scope"));

		Element ercot = null;
		for (Element p : children(report, null, "Package"))
			if ("ERCOT".equals(p.getAttribute("name")))
				ercot = p;
		assertNotNull("ERCOT is a top-level package in the report", ercot);

		// ErcotLine is added; IdentifiedObject (in Model) is changed only because it
		// gains ErcotLine's generalization, i.e. the link resolves at both ends.
		Element summary = only(report, null, "Summary");
		java.util.Map<String, String> classes = new java.util.HashMap<String, String>();
		for (Element c : children(summary, null, "Count"))
			if ("class".equals(c.getAttribute("kind")))
				classes.put(c.getAttribute("status"), c.getAttribute("n"));
		assertEquals("{added=1, changed=1}", new java.util.TreeMap<String, String>(classes).toString());
		assertTrue(xml.contains("Source: (ErcotLine)"));
		String page = new String(Files.readAllBytes(html.toPath()), "UTF-8");
		assertTrue(page.contains("ErcotLine"));
		assertFalse("the synthetic model is not shown", page.contains(">EA Model<"));
	}
}
