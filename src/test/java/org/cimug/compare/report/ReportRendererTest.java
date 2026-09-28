package org.cimug.compare.report;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Document;

/**
 * End-to-end check of stage 3 (pre-pass, enriched XML, XSLT 3.0 on Saxon-HE)
 * over the small fixture. It checks the wiring, not the page layout.
 */
public class ReportRendererTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private File copyFixture() throws Exception {
		File xml = tmp.newFile("comparison-report.xml");
		InputStream in = getClass().getClassLoader().getResourceAsStream(ReportPreProcessorTest.FIXTURE);
		try {
			Files.copy(in, xml.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} finally {
			in.close();
		}
		return xml;
	}

	private static String read(File f) throws Exception {
		return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
	}

	@Test
	public void writesASelfContainedReportAndTheEnrichedXml() throws Exception {
		File xml = copyFixture();
		File html = new File(tmp.getRoot(), "comparison-report.html");

		File enriched = new ReportRenderer(false, null, false, "jpg").render(xml, html);

		assertEquals(new File(tmp.getRoot(), "comparison-report-enriched.xml"), enriched);
		assertTrue("enriched XML written", enriched.isFile());
		Document e = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(enriched);
		assertEquals("ComparisonReport", e.getDocumentElement().getNodeName());

		assertTrue("report written", html.isFile());
		String page = read(html);
		assertTrue(page.contains("<style"));
		assertTrue(page.contains("<script"));
		assertFalse("no external stylesheet", page.contains("<link rel=\"stylesheet\""));
		assertFalse("no external script", page.matches("(?s).*<script[^>]*\\ssrc=.*"));
		for (String name : new String[] { "PowerSystemResource", "Plant", "ACLineSegment", "Switch" })
			assertTrue(name + " in report", page.contains(name));
		assertFalse("identical class left out of minimal output",
				page.contains("id=\"EAID_00000000_0000_0000_0000_000000000111\""));
		assertTrue("package outline", page.contains("id=\"rail\""));
		assertTrue("redline", page.contains("<ins>") || page.contains("<ins "));
		assertFalse("diagrams only with --include-diagrams", page.contains("CoreOverview"));
	}

	@Test
	public void changedTypeShowsTheNewTypeAndBoundsAreRedlined() throws Exception {
		File xml = copyFixture();
		File html = new File(tmp.getRoot(), "types.html");
		new ReportRenderer(false, null, false, "jpg").render(xml, html);
		String page = read(html);
		// ratedS: Float -> IdentifiedObject (a class, so a link), lower bound 0 -> 1
		assertTrue("new type shown, marked as changed",
				page.contains("<span class=\"chg\"><a href=\"#EAID_00000000_0000_0000_0000_000000000111\">IdentifiedObject</a></span>"));
		assertFalse("old type not struck", page.contains("<del>Float</del>"));
		assertTrue("changed bound redlined", page.contains("<del>0</del><ins>1</ins>"));
		// renamed class: its Name property is redlined in the property table
		assertTrue(page.contains("<del>ConductingEquipmentBase</del><ins>Equipment</ins>"));
	}

	@Test
	public void renamesShowTheNewNameAndRenamedFrom() throws Exception {
		File xml = copyFixture();
		File html = new File(tmp.getRoot(), "renames.html");
		new ReportRenderer(false, null, false, "jpg").render(xml, html);
		String page = read(html);
		// tree row: new name, then the old name in grey; no arrow
		assertTrue(page.contains("Equipment<span class=\"renamed-from\">renamed from ConductingEquipmentBase</span>"));
		assertTrue(page.contains("Grid<span class=\"renamed-from\">renamed from IEC61970</span>"));
		assertFalse(page.contains("<span class=\"arrow\">→</span>Equipment"));
		// attribute whose only change is EA's «deprecated»: the new name shows it
		assertTrue(page.contains("«deprecated» aliasName<span class=\"info\""));
		assertTrue(page.contains("<span class=\"renamed-from\">renamed from aliasName</span>"));
	}

	@Test
	public void diagramsAreIncludedOnRequest() throws Exception {
		File xml = copyFixture();
		File html = new File(tmp.getRoot(), "with-diagrams.html");
		new ReportRenderer(false, null, true, "png").render(xml, html);
		String page = read(html);
		assertTrue(page.contains("CoreOverview"));
		assertTrue("image file name from the diagram GUID", page.contains("EAID_00000000_0000_0000_0000_000000000118.png"));
		assertFalse("layout-only diagram", page.contains("CoreLayout"));
	}

	@Test
	public void scopeAppearsInTheReport() throws Exception {
		File xml = copyFixture();
		File html = new File(tmp.getRoot(), "grid.html");
		new ReportRenderer(false, "IEC61970", false, "jpg").render(xml, html);
		String page = read(html);
		assertTrue(page.contains("IEC61970 → Grid"));
		assertFalse("outside the scope", page.contains("TC57CIM"));
	}

	@Test
	public void enrichedXmlGoesBesideTheReportOrElseBesideTheInput() {
		File xml = new File("/data/in/comparison-report.xml");
		assertEquals(new File("/data/out", "comparison-report-enriched.xml"),
				ReportRenderer.enrichedFileFor(xml, new File("/data/out/report.html")));
		assertEquals(new File("/data/in", "comparison-report-enriched.xml").getAbsoluteFile(),
				ReportRenderer.enrichedFileFor(xml, null).getAbsoluteFile());
		assertEquals("comparison-report-enriched.xml",
				ReportRenderer.enrichedFileFor(new File("/x/comparison-report.XML"), null).getName());
	}
}
