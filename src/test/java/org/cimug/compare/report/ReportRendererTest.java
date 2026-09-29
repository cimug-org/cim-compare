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
		assertFalse("no highlight toggle without diagrams", page.contains("<input id=\"chk-hl\""));
		assertTrue("redline", page.contains("<ins>") || page.contains("<ins "));
		assertFalse("diagrams only with --include-diagrams", page.contains("CoreOverview"));
	}

	@Test
	public void headerShowsTheLogoInline() throws Exception {
		File xml = copyFixture();
		File html = new File(tmp.getRoot(), "logo.html");
		new ReportRenderer(false, null, false, "jpg").render(xml, html);
		String page = read(html);
		int header = page.indexOf("<header class=\"hdr\">");
		int brand = page.indexOf("<a class=\"brand\" href=\"https://github.com/cimug-org/cim-compare\"");
		assertTrue("logo link opens the header", header >= 0 && brand > header && brand < page.indexOf("<div class=\"title\">"));
		assertTrue("logo inlined as SVG, not escaped", page.contains("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 253.04 62.40\""));
		assertTrue("logo ids prefixed", page.contains("id=\"ccl-g1\"") && !page.contains("id=\"g1\""));
		assertFalse("no external image", page.contains("<img src=\"logo"));
	}

	@Test
	public void summaryCountsFilterTheirOwnKind() throws Exception {
		File xml = copyFixture();
		File html = new File(tmp.getRoot(), "summary.html");
		new ReportRenderer(false, null, false, "jpg").render(xml, html);
		String page = read(html);
		// #78: each count names its kind, so a Classes count filters classes only
		assertTrue(page.matches("(?s).*<span class=\"cnt s-added\" data-kind=\"class\" data-status=\"added\" title=\"show only added classes\">.*"));
		assertTrue(page.matches("(?s).*<span class=\"cnt s-changed\" data-kind=\"package\" data-status=\"changed\" title=\"show only changed packages\">.*"));
		assertTrue("filter status line", page.contains("<span id=\"flt-status\" class=\"clear\">Click a count to show only those items</span>"));
		assertFalse("counts without a kind", page.matches("(?s).*<span class=\"cnt [^\"]*\" data-status=.*"));
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
	public void associationEndsShowTheClassThenTheRole() throws Exception {
		File xml = copyFixture();
		File html = new File(tmp.getRoot(), "links.html");
		new ReportRenderer(false, null, false, "jpg").render(xml, html);
		String page = read(html);
		// Equipment role Equipment(s) [0..*] → IdentifiedObject role unspecified [0..1 → 1]
		assertTrue(page.contains("<span class=\"end-sig\"><a href=\"#EAID_00000000_0000_0000_0000_000000000117\">Equipment</a> <span class=\"role-part\"><span class=\"role-lbl\">role</span>&nbsp;"
				+ "<del>Equipment</del><ins>Equipments</ins>&nbsp;<span class=\"card\">[0..*]</span>&nbsp;→</span></span> "));
		assertTrue(page.contains("<a href=\"#EAID_00000000_0000_0000_0000_000000000111\">IdentifiedObject</a> <span class=\"role-part\"><span class=\"role-lbl\">role</span>&nbsp;"
				+ "<i>unspecified</i>&nbsp;<span class=\"card\">[<del>0..1</del><ins>1</ins>]</span></span></span>"));
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
		// package outline: the old name in brackets on the next line; tooltip reworded
		assertTrue(page.contains("<span class=\"r-renamed\">[renamed from IEC61970]</span>"));
		assertTrue(page.contains("title=\"Grid (renamed from IEC61970)\""));
		// attribute whose only change is EA's «deprecated»: the new name shows it
		assertTrue(page.contains("«deprecated» aliasName<span class=\"info\""));
		assertTrue(page.contains("<span class=\"renamed-from\">renamed from aliasName</span>"));
	}

	@Test
	public void attributeMetadataViewShowsTheNewNameAsItIs() throws Exception {
		File xml = copyFixture();
		File html = new File(tmp.getRoot(), "meta.html");
		new ReportRenderer(false, null, false, "jpg").render(xml, html);
		String page = read(html);
		// #67: in an attribute's ⓘ metadata table the Name row shows the new name plainly
		int meta = page.indexOf("Attribute metadata — aliasName");
		assertTrue("aliasName metadata view", meta > 0);
		String view = page.substring(meta, page.indexOf("</tr>", page.indexOf("<th class=\"k\">Name</th>", meta)));
		assertTrue(view, view.contains("<td>aliasName</td><td>«deprecated» aliasName</td>"));
		assertFalse(view, view.contains("<del>") || view.contains("<ins>"));
		// the class Metadata table still redlines a changed name
		assertTrue(page.contains("<del>ConductingEquipmentBase</del><ins>Equipment</ins>"));
	}

	@Test
	public void diagramsAreIncludedOnRequest() throws Exception {
		File xml = copyFixture();
		File html = new File(tmp.getRoot(), "with-diagrams.html");
		new ReportRenderer(false, null, true, "png").render(xml, html);
		String page = read(html);
		assertTrue(page.contains("CoreOverview"));
		assertTrue("image file name from the diagram GUID", page.contains("EAID_00000000_0000_0000_0000_000000000118.png"));
		assertFalse("layout-only diagram with nothing to highlight", page.contains("DERLayout"));
		// a diagram whose only change is its name: listed, metadata only, no images
		assertTrue(page.contains("DocWires"));
		assertFalse(page.contains("EAID_00000000_0000_0000_0000_000000000124.png"));
		assertTrue("a diagram connector change alone still shows the images",
				page.contains("EAID_00000000_0000_0000_0000_000000000123.png"));
		assertTrue("header toggle, on by default",
				page.contains("<input id=\"chk-hl\" type=\"checkbox\" checked> Diagram highlights"));
	}

	@Test
	public void diagramHighlightsAreDrawnOverTheImages() throws Exception {
		File xml = copyFixture();
		File html = new File(tmp.getRoot(), "highlights.html");
		new ReportRenderer(false, null, true, "png").render(xml, html);
		String page = read(html);
		assertTrue(page.contains("<span class=\"hl hl-added\" data-box=\"500,40,640,90\" title=\"ACLineSegment: added\"></span>"));
		assertTrue(page.contains("<span class=\"hl hl-moved\" data-box=\"40,20,200,80\" title=\"IdentifiedObject: moved or resized\"></span>"));
		assertTrue("legend lists the kinds present, in order",
				page.matches("(?s).*class=\"hl-legend\">\\s*<span class=\"hl-key\"><span class=\"hl-sw hl-added\">.*hl-removed.*hl-changed.*hl-restyled.*"));
		assertTrue(page.contains("<div class=\"dimg\" data-side=\"baseline\"><img loading=\"lazy\" src=\"Images-baseline/EAID_00000000_0000_0000_0000_000000000118.png\""));
		// connectors: a table under the images, removed before labels moved
		assertTrue(page.contains("<span class=\"cx-n\">1 removed · 1 labels moved</span>"));
		assertTrue(page.contains("<div class=\"cx-row\" tabindex=\"0\" data-side=\"baseline\" data-ends=\"300,200,420,260;40,120,200,200\">"
				+ "<span class=\"cx-k cx-removed\">removed</span><span class=\"cx-name\">Plant \u2013 PowerSystemResource</span></div>"));
		assertTrue(page.indexOf("cx-removed\">removed") < page.indexOf("cx-labels-moved\">labels moved"));
	}

	@Test
	public void imagesTheReportDoesNotShowAreDeleted() throws Exception {
		File xml = copyFixture();
		String[] shown = { "118", "119", "123" }; // CoreOverview, CoreLayout, WiresConnectors
		String[] unshown = { "124", "131" }; // DocWires (renamed only), DERLayout (nothing to highlight)
		File other = null;
		for (String side : new String[] { "Images-baseline", "Images-destination" }) {
			File dir = tmp.newFolder(side);
			for (String n : shown)
				new File(dir, "EAID_00000000_0000_0000_0000_000000000" + n + ".png").createNewFile();
			for (String n : unshown)
				new File(dir, "EAID_00000000_0000_0000_0000_000000000" + n + ".png").createNewFile();
			other = new File(dir, "EAID_11111111_1111_1111_1111_111111111111.png");
			other.createNewFile();
		}
		new ReportRenderer(false, null, true, "png").render(xml, new File(tmp.getRoot(), "comparison-report.html"));
		for (String side : new String[] { "Images-baseline", "Images-destination" }) {
			File dir = new File(tmp.getRoot(), side);
			for (String n : shown)
				assertTrue(side + " " + n + " kept", new File(dir, "EAID_00000000_0000_0000_0000_000000000" + n + ".png").isFile());
			for (String n : unshown)
				assertFalse(side + " " + n + " deleted", new File(dir, "EAID_00000000_0000_0000_0000_000000000" + n + ".png").exists());
			assertTrue("a file not belonging to a diagram in the comparison is left alone",
					new File(dir, "EAID_11111111_1111_1111_1111_111111111111.png").isFile());
		}
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
