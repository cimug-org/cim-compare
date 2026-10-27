package org.cimug.compare.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathFactory;

import org.cimug.compare.report.ReportRenderer;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Document;

/**
 * --image-type=SVG (#139), end to end from two XMI exports with SVG diagram images
 * to the comparison XML and the report; and the JPG fallback.
 * <p>
 * The diagram of {@code multiroot/rootB-Extensions.xmi} is given the geometry EA 17
 * writes (the diagram position, and the box in the JPG the XMI export wrote, drawn
 * 1.3646 times larger); in the destination its first class is moved 100 to the right.
 * The SVGs draw the classes at the diagram positions less (294, 72).
 */
public class SvgImagesTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private static final Charset CP1252 = Charset.forName("windows-1252");
	private static final String DIAGRAM = "EAID_E0C07000_0000_0000_0000_000000000005";

	@After
	public void reset() {
		CIMModelComparisonGenerator.svgUnavailable(null);
	}

	private File export(String name, int left1) throws Exception {
		File src = new File(getClass().getClassLoader().getResource("multiroot/rootB-Extensions.xmi").toURI());
		String xmi = new String(Files.readAllBytes(src.toPath()), CP1252);
		int il = (int) Math.round((left1 - 294) * 1.3646);
		xmi = xmi.replace("geometry=\"100,100,300,200,\"", "geometry=\"Left=" + left1 + ";Top=172;Right=" + (left1 + 200)
				+ ";Bottom=272;imgL=" + il + ";imgT=150;imgR=" + (il + 273) + ";imgB=286;\"");
		xmi = xmi.replace("geometry=\"100,300,300,400,\"",
				"geometry=\"Left=394;Top=372;Right=594;Bottom=472;imgL=137;imgT=423;imgR=410;imgB=559;\"");
		File f = tmp.newFile(name);
		Files.write(f.toPath(), xmi.getBytes(CP1252));
		return f;
	}

	private void svg(String dir, int x1) throws Exception {
		File d = new File(tmp.getRoot(), dir);
		d.mkdirs();
		String svg = "<?xml version=\"1.0\"?><svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 700 500\">"
				+ "<rect x=\"" + x1 + "\" y=\"100\" width=\"200\" height=\"100\" rx=\"0.00\"/>"
				+ "<rect x=\"100\" y=\"300\" width=\"200\" height=\"100\" rx=\"0.00\"/></svg>";
		Files.write(new File(d, DIAGRAM + ".svg").toPath(), svg.getBytes(StandardCharsets.UTF_8));
	}

	private static String box(Document d, String side) throws Exception {
		return XPathFactory.newInstance().newXPath().evaluate("//CompareItem[@type='DiagramObject' and @guid="
				+ "'{E0C07000-0000-0000-0000-000000000003}']/Properties/Property[@name='Box']/@" + side, d);
	}

	@Test
	public void highlightsAreInTheSvgsCoordinates() throws Exception {
		File baseline = export("baseline.xmi", 394), destination = export("destination.xmi", 494);
		svg("Images-baseline", 100);
		svg("Images-destination", 200);
		File xml = new File(tmp.getRoot(), "comparison.xml");
		DiffXMLGenerator.main(new String[] { baseline.getPath(), destination.getPath(), xml.getPath(), "svg" });

		Document log = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xml);
		assertEquals("100,100,300,200", box(log, "baseline"));
		assertEquals("200,100,400,200", box(log, "model"));

		File html = new File(tmp.getRoot(), "report.html");
		new ReportRenderer(false, null, true, "svg").render(xml, html);
		String page = new String(Files.readAllBytes(html.toPath()), StandardCharsets.UTF_8);
		assertTrue(page.contains("src=\"Images-destination/" + DIAGRAM + ".svg\""));
		// the diagram's highlights are drawn in the SVG's coordinates (in the JPG's pixels this box is 137,423,410,559)
		assertTrue(page.contains("data-box=\"100,300,300,400\""));
		assertFalse(page.contains("data-box=\"137,423,410,559\""));
	}

	@Test
	public void withJpgImagesTheBoxesStayInTheJpgsPixels() throws Exception {
		File baseline = export("baseline.xmi", 394), destination = export("destination.xmi", 494);
		File xml = new File(tmp.getRoot(), "comparison.xml");
		DiffXMLGenerator.main(new String[] { baseline.getPath(), destination.getPath(), xml.getPath(), "jpg" });
		Document log = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xml);
		assertEquals("136,150,409,286", box(log, "baseline"));
	}

	private File images(String dir, String... names) throws Exception {
		File d = new File(tmp.getRoot(), dir);
		d.mkdirs();
		for (String n : names)
			Files.write(new File(d, n).toPath(), new byte[] { 1 });
		return d;
	}

	@Test
	public void whenEveryDiagramHasAnSvgTheJpgsAreRemoved() throws Exception {
		File b = images("Images-baseline", "EAID_1.jpg", "EAID_1.svg"), d = images("Images-destination", "EAID_1.jpg", "EAID_1.svg");
		assertTrue(CIMModelComparisonGenerator.finishSvgImages(tmp.getRoot()));
		assertFalse(new File(b, "EAID_1.jpg").exists());
		assertTrue(new File(b, "EAID_1.svg").exists());
		assertFalse(new File(d, "EAID_1.jpg").exists());
	}

	@Test
	public void whenAnSvgCouldNotBeWrittenBothModelsFallBackToJpg() throws Exception {
		File b = images("Images-baseline", "EAID_1.jpg", "EAID_1.svg"), d = images("Images-destination", "EAID_1.jpg");
		CIMModelComparisonGenerator.svgUnavailable("EA did not write the file");
		assertFalse(CIMModelComparisonGenerator.finishSvgImages(tmp.getRoot()));
		assertTrue(new File(b, "EAID_1.jpg").exists());
		assertFalse("no mix of SVG and JPG", new File(b, "EAID_1.svg").exists());
		assertTrue(new File(d, "EAID_1.jpg").exists());
	}

	@Test
	public void imageFileNamesBecomeGuids() {
		assertEquals("{70EC2267-19ED-48a1-973A-CD797502D5A8}",
				CIMModelComparisonGenerator.eaidToGuid("EAID_70EC2267_19ED_48a1_973A_CD797502D5A8"));
	}
}
