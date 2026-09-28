package org.cimug.compare.report;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Tests for the report pre-pass over the small hand-written comparison in
 * {@code src/test/resources/report/mini-comparison.xml}; the comment at the top
 * of that file lists which element exercises which case.
 */
public class ReportPreProcessorTest {

	static final String FIXTURE = "report/mini-comparison.xml";

	private static final XPath XP = XPathFactory.newInstance().newXPath();

	static Document fixture() throws Exception {
		InputStream in = ReportPreProcessorTest.class.getClassLoader().getResourceAsStream(FIXTURE);
		assertNotNull("test fixture missing from classpath: " + FIXTURE, in);
		try {
			return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
		} finally {
			in.close();
		}
	}

	private static Document enrich(boolean full, String packageFilter) throws Exception {
		return new ReportPreProcessor(full, packageFilter).process(fixture());
	}

	private static Element one(Document d, String xpath) throws Exception {
		return (Element) XP.evaluate(xpath, d, XPathConstants.NODE);
	}

	private static int count(Document d, String xpath) throws Exception {
		return ((NodeList) XP.evaluate(xpath, d, XPathConstants.NODESET)).getLength();
	}

	private static String str(Document d, String xpath) throws Exception {
		return XP.evaluate(xpath, d);
	}

	private static Element clazz(Document d, String name) throws Exception {
		return one(d, "//Class[@name='" + name + "']");
	}

	private static Element pkg(Document d, String name) throws Exception {
		return one(d, "//Package[@name='" + name + "']");
	}

	// ------------------------------------------------------------ header

	@Test
	public void headerCarriesVersionsTimestampScopeAndGenerator() throws Exception {
		Document d = enrich(false, null);
		Element r = d.getDocumentElement();
		assertEquals("ComparisonReport", r.getNodeName());
		assertEquals("IEC61970CIM17v40", r.getAttribute("baselineVersion"));
		assertEquals("IEC61970CIM18v16", r.getAttribute("destinationVersion"));
		assertEquals("2026-09-27 10:00:00", r.getAttribute("comparedOn"));
		assertEquals("TC57CIM (full model)", r.getAttribute("scope"));
		assertEquals("false", r.getAttribute("full"));
		assertEquals(ReportPreProcessor.GENERATOR, r.getAttribute("generator"));
	}

	@Test
	public void rootPackageNotNamedModelIsRendered() throws Exception {
		// 1.x entered the tree at a package named "Model" and produced an empty
		// report for any other root (#43).
		Document d = enrich(false, null);
		assertEquals(1, count(d, "/ComparisonReport/Package"));
		assertEquals("TC57CIM", str(d, "/ComparisonReport/Package/@name"));
	}

	@Test
	public void rejectsInputThatIsNotAComparison() throws Exception {
		Document notComparison = DocumentBuilderFactory.newInstance().newDocumentBuilder()
				.parse(new InputSource(new StringReader("<Something/>")));
		try {
			new ReportPreProcessor(false, null).process(notComparison);
			fail("expected IllegalArgumentException");
		} catch (IllegalArgumentException expected) {
			assertTrue(expected.getMessage().contains("CompareResults"));
		}
	}

	// ------------------------------------------------------------ minimal / full

	@Test
	public void minimalOutputDropsIdenticalElements() throws Exception {
		Document d = enrich(false, null);
		assertNull("identical class", clazz(d, "IdentifiedObject"));
		assertNull("wholly identical package", pkg(d, "IEC61968"));
		assertNull("identical class in identical package", clazz(d, "Customer"));
		assertEquals("identical generalization", 0, count(d, "//Class[@name='PowerSystemResource']/Link"));
	}

	@Test
	public void fullOutputKeepsIdenticalElements() throws Exception {
		Document d = enrich(true, null);
		assertEquals("true", d.getDocumentElement().getAttribute("full"));
		assertEquals("identical", clazz(d, "IdentifiedObject").getAttribute("status"));
		assertEquals("identical", pkg(d, "IEC61968").getAttribute("status"));
		assertNotNull(clazz(d, "Customer"));
		assertEquals(1, count(d, "//Class[@name='PowerSystemResource']/Link[@kind='Generalization']"));
	}

	// ------------------------------------------------------------ statuses

	@Test
	public void classMarkedIdenticalWithChangedAttributeIsChanged() throws Exception {
		Document d = enrich(false, null);
		assertEquals("changed", clazz(d, "PowerSystemResource").getAttribute("status"));
		assertEquals("changed", str(d, "//Class[@name='PowerSystemResource']/Attribute[@name='name']/@status"));
	}

	@Test
	public void baselineOnlyItemsAreClassifiedAndMarkedDeleted() throws Exception {
		Document d = enrich(false, null);
		assertEquals("deleted", clazz(d, "Plant").getAttribute("status"));
		assertEquals("deleted", pkg(d, "Retired").getAttribute("status"));
		Element link = one(d, "//Class[@name='Plant']/Link");
		assertNotNull("baseline-only generalization under a baseline-only Links item", link);
		assertEquals("Generalization", link.getAttribute("kind"));
		assertEquals("deleted", link.getAttribute("status"));
		assertEquals("source", str(d, "//Class[@name='Plant']/Link/End[1]/@side"));
		assertEquals("target", str(d, "//Class[@name='Plant']/Link/End[2]/@side"));
	}

	@Test
	public void addedMovedAndRenamedElements() throws Exception {
		Document d = enrich(false, null);
		Element added = clazz(d, "ACLineSegment");
		assertEquals("added", added.getAttribute("status"));
		assertEquals("concrete", added.getAttribute("stereotype"));

		Element moved = clazz(d, "Switch");
		assertEquals("moved", moved.getAttribute("status"));
		assertEquals("Core", moved.getAttribute("fromPackage"));

		assertEquals("ConductingEquipmentBase", clazz(d, "Equipment").getAttribute("renamedFrom"));
		assertEquals("IEC61970", pkg(d, "Grid").getAttribute("renamedFrom"));
	}

	@Test
	public void statusesAreNormalised() {
		assertEquals("deleted", ReportPreProcessor.normalize("Baseline only"));
		assertEquals("added", ReportPreProcessor.normalize("Model only"));
		assertEquals("moved", ReportPreProcessor.normalize("Moved"));
		assertEquals("changed", ReportPreProcessor.normalize("Changed"));
		assertEquals("identical", ReportPreProcessor.normalize("Identical"));
		assertEquals("identical", ReportPreProcessor.normalize(null));
	}

	// ------------------------------------------------------------ diagrams

	@Test
	public void diagramWithChangedPropertiesIsShown() throws Exception {
		Document d = enrich(false, null);
		Element dg = one(d, "//Diagram[@name='CoreOverview']");
		assertNotNull(dg);
		assertEquals("changed", dg.getAttribute("status"));
		assertEquals("EAID_00000000_0000_0000_0000_000000000118", dg.getAttribute("eaid"));
	}

	// #37: highlight boxes for the diagram images

	private static String highlight(Document d, String name, String side) throws Exception {
		return str(d, "//Diagram[@name='CoreOverview']/Highlight[@name='" + name + "' and @side='" + side
				+ "']/@kind");
	}

	@Test
	public void diagramHighlightsFollowTheElementAndItsClass() throws Exception {
		Document d = enrich(false, null);
		// added and removed elements: destination and baseline only
		assertEquals("added", highlight(d, "ACLineSegment", "destination"));
		assertEquals("", highlight(d, "ACLineSegment", "baseline"));
		assertEquals("removed", highlight(d, "Plant", "baseline"));
		assertEquals("", highlight(d, "Plant", "destination"));
		// box unchanged, but the class's attributes changed (class itself Identical)
		assertEquals("changed", highlight(d, "PowerSystemResource", "destination"));
		assertEquals("", highlight(d, "PowerSystemResource", "baseline"));
		// nothing changed: no box
		assertEquals("", highlight(d, "IdentifiedObject", "destination"));
		// moved, and the class changed: changed wins
		assertEquals("changed", highlight(d, "Equipment", "destination"));
		// moved only: both sides, each in its own position
		assertEquals("moved", highlight(d, "Switch", "baseline"));
		assertEquals("moved", highlight(d, "Switch", "destination"));
		assertEquals("260,40,380,90",
				str(d, "//Diagram[@name='CoreOverview']/Highlight[@name='Switch' and @side='baseline']/@box"));
		assertEquals("260,120,380,170",
				str(d, "//Diagram[@name='CoreOverview']/Highlight[@name='Switch' and @side='destination']/@box"));
		// style only
		assertEquals("restyled", highlight(d, "Customer", "destination"));
	}

	@Test
	public void layoutOnlyDiagramStaysHiddenAndUnhighlighted() throws Exception {
		assertNull(one(enrich(false, null), "//Diagram[@name='CoreLayout']"));
		assertNull(one(enrich(true, null), "//Diagram[@name='CoreLayout']/Highlight"));
	}

	@Test
	public void layoutOnlyDiagramIsHidden() throws Exception {
		Document d = enrich(false, null);
		assertNull(one(d, "//Diagram[@name='CoreLayout']"));
	}

	@Test
	public void packageWhoseOnlyChangeIsADiagramLayoutIsHidden() throws Exception {
		Document d = enrich(false, null);
		assertNull("DER holds nothing but a layout-only diagram change", pkg(d, "DER"));
	}

	@Test
	public void layoutOnlyDiagramIsIdenticalInFullOutput() throws Exception {
		Document d = enrich(true, null);
		assertEquals("identical", str(d, "//Diagram[@name='CoreLayout']/@status"));
		assertEquals("identical", str(d, "//Diagram[@name='DERLayout']/@status"));
	}

	// ------------------------------------------------------------ enrichment

	@Test
	public void changedNotesCarryBothTextsAndARedline() throws Exception {
		Document d = enrich(false, null);
		String notes = "//Attribute[@name='name']/Notes";
		assertEquals("changed", str(d, notes + "/@status"));
		assertEquals("The name is any free text string.", str(d, notes + "/Baseline"));
		assertEquals("The name is any free human readable string.", str(d, notes + "/Destination"));
		assertEquals(1, count(d, notes + "/Redline/del"));
		assertEquals(1, count(d, notes + "/Redline/ins"));
		assertEquals("text", str(d, notes + "/Redline/del").trim());
		assertEquals("human readable", str(d, notes + "/Redline/ins").trim());
		// the property row carries the same redline
		assertEquals(1, count(d, "//Attribute[@name='name']/Properties/Property[@name='Notes']/Redline/ins"));
	}

	@Test
	public void changedNamesAreRedlinedButTypesAreNot() throws Exception {
		// Names are redlined (Todd, 2026-09-28); a changed type is shown by the
		// report as the new type in blue, so the pre-pass computes no redline for it.
		Document d = enrich(false, null);
		String name = "//Class[@name='Equipment']/Properties/Property[@name='Name']";
		assertEquals("changed", str(d, name + "/@status"));
		assertEquals("ConductingEquipmentBase", str(d, name + "/Redline/del"));
		assertEquals("Equipment", str(d, name + "/Redline/ins"));
		assertEquals(1, count(d, "//Package[@name='Grid']/Properties/Property[@name='Name']/Redline"));
		assertEquals(0, count(d, "//Attribute[@name='ratedS']//Redline"));
	}

	@Test
	public void unchangedNotesHaveNoRedline() throws Exception {
		Document d = enrich(true, null);
		assertEquals("identical", str(d, "//Class[@name='IdentifiedObject']/Notes/@status"));
		assertEquals(0, count(d, "//Class[@name='IdentifiedObject']/Notes/Redline"));
	}

	@Test
	public void attributeTypesAndLinkEndsResolveToClassIds() throws Exception {
		Document d = enrich(false, null);
		String io = "EAID_00000000_0000_0000_0000_000000000111";
		Element a = one(d, "//Attribute[@name='name']");
		assertEquals(io, a.getAttribute("baselineTypeId"));
		assertEquals(io, a.getAttribute("modelTypeId"));
		assertEquals("the target's element id is the same", io, clazz(enrich(true, null), "IdentifiedObject").getAttribute("id"));

		// deleted Plant → PowerSystemResource: only the baseline side resolves
		Element target = one(d, "//Class[@name='Plant']/Link/End[@side='target']");
		assertEquals("EAID_00000000_0000_0000_0000_000000000112", target.getAttribute("baselineClassId"));
		assertFalse(target.hasAttribute("modelClassId"));
	}

	@Test
	public void idsAreDerivedFromGuidsLikeEaImageNames() {
		assertEquals("EAID_6CA575E7_020B_40d8_B841_027BF8B51BE9",
				ReportPreProcessor.eaid("{6CA575E7-020B-40d8-B841-027BF8B51BE9}"));
	}

	@Test
	public void elementIdsAreUnique() throws Exception {
		Document d = enrich(true, null);
		NodeList ids = (NodeList) XP.evaluate("//*[@id]/@id", d, XPathConstants.NODESET);
		java.util.Set<String> seen = new java.util.HashSet<String>();
		for (int i = 0; i < ids.getLength(); i++)
			assertTrue("duplicate id " + ids.item(i).getNodeValue(), seen.add(ids.item(i).getNodeValue()));
	}

	// ------------------------------------------------------------ counts

	@Test
	public void summaryCountsMatchTheRenderedElements() throws Exception {
		Document d = enrich(false, null);
		assertEquals("1", str(d, "/ComparisonReport/Summary/Count[@kind='class'][@status='added']/@n"));
		assertEquals("1", str(d, "/ComparisonReport/Summary/Count[@kind='class'][@status='deleted']/@n"));
		assertEquals("1", str(d, "/ComparisonReport/Summary/Count[@kind='class'][@status='moved']/@n"));
		// PowerSystemResource, Equipment, GridCIMVersion
		assertEquals("3", str(d, "/ComparisonReport/Summary/Count[@kind='class'][@status='changed']/@n"));
		assertEquals("1", str(d, "/ComparisonReport/Summary/Count[@kind='diagram'][@status='changed']/@n"));
		assertEquals("1", str(d, "/ComparisonReport/Summary/Count[@kind='package'][@status='deleted']/@n"));
		assertEquals("", str(d, "/ComparisonReport/Summary/Count[@kind='class'][@status='identical']/@n"));
	}

	@Test
	public void packageCountsCoverTheWholeSubtree() throws Exception {
		Document d = enrich(false, null);
		Element grid = pkg(d, "Grid");
		// Core: PowerSystemResource, Plant, Equipment; Wires: ACLineSegment, Switch; Grid: GridCIMVersion
		assertEquals("6", grid.getAttribute("changedClasses"));
		assertEquals("1", grid.getAttribute("changedDiagrams"));
		Element core = pkg(d, "Core");
		assertEquals("classes directly in Core, identical included", "4", core.getAttribute("classCount"));
		assertEquals("2", core.getAttribute("diagramCount"));
		assertEquals("3", core.getAttribute("changedClasses"));
	}

	// ------------------------------------------------------------ --package

	@Test
	public void packageFilterMatchesTheDestinationName() throws Exception {
		Document d = enrich(false, "Grid");
		assertEquals("IEC61970 → Grid", d.getDocumentElement().getAttribute("scope"));
		assertEquals(1, count(d, "/ComparisonReport/Package"));
		assertEquals("Grid", str(d, "/ComparisonReport/Package/@name"));
		assertNull("outside the scope", pkg(d, "TC57CIM"));
	}

	@Test
	public void packageFilterMatchesTheBaselineNameOfARenamedPackage() throws Exception {
		Document d = enrich(false, "IEC61970");
		assertEquals("IEC61970 → Grid", d.getDocumentElement().getAttribute("scope"));
		assertEquals("Grid", str(d, "/ComparisonReport/Package/@name"));
	}

	@Test
	public void packageFilterOnANestedPackage() throws Exception {
		Document d = enrich(false, " Wires ");
		assertEquals("Wires", d.getDocumentElement().getAttribute("scope"));
		assertEquals("Wires", str(d, "/ComparisonReport/Package/@name"));
		assertNull(clazz(d, "PowerSystemResource"));
		assertEquals("", str(d, "/ComparisonReport/Summary/Count[@kind='class'][@status='deleted']/@n"));
	}

	@Test
	public void blankPackageFilterMeansTheWholeModel() throws Exception {
		assertEquals("TC57CIM (full model)", enrich(false, "  ").getDocumentElement().getAttribute("scope"));
	}

	@Test
	public void unknownPackageIsAnError() throws Exception {
		try {
			enrich(false, "NoSuchPackage");
			fail("expected IllegalArgumentException");
		} catch (IllegalArgumentException expected) {
			assertTrue(expected.getMessage(), expected.getMessage().contains("'NoSuchPackage'"));
		}
	}
}
