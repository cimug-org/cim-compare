package org.cimug.compare.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathFactory;

import org.cimug.compare.app.CIMModelComparisonGenerator.DiagramImage;
import org.cimug.compare.report.ReportRenderer;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Document;

/**
 * #141: images drawn one diagram at a time after the comparison, for the diagrams
 * that changed. EA is stood in for by a drawer that writes the files EA would: an SVG
 * drawing the classes at their diagram positions less (294, 72), and for a raster
 * format an image 1.3646 times the SVG and 6 pixels taller.
 * <p>
 * As in {@link SvgImagesTest}, the destination moves the diagram's first class 100 to
 * the right; both XMI files are exported without images.
 */
public class PerDiagramImagesTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private static final Charset CP1252 = Charset.forName("windows-1252");
	private static final String DIAGRAM = "EAID_E0C07000_0000_0000_0000_000000000005";

	@After
	public void reset() {
		GUIDBasedDiffReportGeneratorImpl.drawer = null;
	}

	/** Writes what EA would, and records what it was asked to draw. */
	private class FakeEa extends PerDiagramImages {
		final Set<String> baseline = new LinkedHashSet<String>(), destination = new LinkedHashSet<String>();
		boolean draws = true;

		FakeEa(DiagramImage format) {
			super(new File("baseline.qeax"), new File("destination.qeax"), tmp.getRoot(), format);
		}

		@Override
		protected void drawSide(boolean side, Set<String> ids) {
			(side ? baseline : destination).addAll(ids);
			if (!draws) {
				unavailable = side(side) + " model: EA did not write any SVG image";
				return;
			}
			try {
				for (String id : ids) {
					File svg = svgFile(side, id);
					svg.getParentFile().mkdirs();
					int x1 = side ? 100 : 200;
					Files.write(svg.toPath(), ("<?xml version=\"1.0\"?><svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 700 500\">"
							+ "<rect x=\"" + x1 + "\" y=\"100\" width=\"200\" height=\"100\" rx=\"0.00\"/>"
							+ "<rect x=\"100\" y=\"300\" width=\"200\" height=\"100\" rx=\"0.00\"/></svg>").getBytes(StandardCharsets.UTF_8));
					if (format() != DiagramImage.SVG) {
						File image = imageFile(side, id);
						image.getParentFile().mkdirs();
						ImageIO.write(new BufferedImage(940, 688, BufferedImage.TYPE_INT_RGB), format().ext(), image);
					}
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	private File export(String name, int left1) throws Exception {
		File src = new File(getClass().getClassLoader().getResource("multiroot/rootB-Extensions.xmi").toURI());
		String xmi = new String(Files.readAllBytes(src.toPath()), CP1252);
		xmi = xmi.replace("geometry=\"100,100,300,200,\"",
				"geometry=\"Left=" + left1 + ";Top=172;Right=" + (left1 + 200) + ";Bottom=272;\"");
		xmi = xmi.replace("geometry=\"100,300,300,400,\"", "geometry=\"Left=394;Top=372;Right=594;Bottom=472;\"");
		File f = new File(tmp.getRoot(), name);
		Files.write(f.toPath(), xmi.getBytes(CP1252));
		return f;
	}

	private File compare(FakeEa ea, int destinationLeft) throws Exception {
		File baseline = export("baseline.xmi", 394), destination = export("destination.xmi", destinationLeft);
		File xml = new File(tmp.getRoot(), "comparison.xml");
		GUIDBasedDiffReportGeneratorImpl.drawer = ea;
		try {
			DiffXMLGenerator.main(new String[] { baseline.getPath(), destination.getPath(), xml.getPath(), ea.format().ext() });
		} finally {
			GUIDBasedDiffReportGeneratorImpl.drawer = null;
			ea.finish();
		}
		return xml;
	}

	private static String box(File xml, String side) throws Exception {
		Document d = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xml);
		return XPathFactory.newInstance().newXPath().evaluate("//CompareItem[@type='DiagramObject' and @guid="
				+ "'{E0C07000-0000-0000-0000-000000000003}']/Properties/Property[@name='Box']/@" + side, d);
	}

	@Test
	public void onlyTheDiagramsThatChangedAreDrawn() throws Exception {
		FakeEa ea = new FakeEa(DiagramImage.SVG);
		compare(ea, 394); // nothing moved
		assertTrue(ea.baseline.isEmpty());
		assertTrue(ea.destination.isEmpty());

		ea = new FakeEa(DiagramImage.SVG);
		compare(ea, 494);
		assertEquals(java.util.Collections.singleton(DIAGRAM), ea.baseline);
		assertEquals(java.util.Collections.singleton(DIAGRAM), ea.destination);
	}

	@Test
	public void svgHighlightsAreInTheSvgsCoordinates() throws Exception {
		File xml = compare(new FakeEa(DiagramImage.SVG), 494);
		assertEquals("100,100,300,200", box(xml, "baseline"));
		assertEquals("200,100,400,200", box(xml, "model"));
		assertTrue(new File(tmp.getRoot(), "Images-destination/" + DIAGRAM + ".svg").isFile());
	}

	@Test
	public void pngHighlightsAreTheSvgBoxesScaledAndMovedDown() throws Exception {
		File xml = compare(new FakeEa(DiagramImage.PNG), 494);
		// (100,100,300,200) x 1.3646, 6 lower
		assertEquals("136,142,409,279", box(xml, "baseline"));
		assertEquals("273,142,546,279", box(xml, "model"));
		assertTrue(new File(tmp.getRoot(), "Images-baseline/" + DIAGRAM + ".png").isFile());
		// the measuring-stick SVGs are gone, and none is among the images
		assertFalse(new File(tmp.getRoot(), ".svg-measure").exists());
		assertFalse(new File(tmp.getRoot(), "Images-baseline/" + DIAGRAM + ".svg").exists());
	}

	@Test
	public void jpgWorksTheSameWay() throws Exception {
		File xml = compare(new FakeEa(DiagramImage.JPG), 494);
		assertEquals("136,142,409,279", box(xml, "baseline"));
	}

	@Test
	public void whenEaDrawsNoSvgTheRunIsToldToExportWithImages() throws Exception {
		FakeEa ea = new FakeEa(DiagramImage.SVG);
		ea.draws = false;
		compare(ea, 494);
		assertEquals("Baseline model: EA did not write any SVG image", ea.unavailable);
		assertTrue("the destination is not tried", ea.destination.isEmpty());
	}

	@Test
	public void theRasterScaleComesFromTheImageHeightWhenItDiffers() {
		assertEquals(1.3646, PerDiagramImages.rasterScale(840, 1150), 0);
		assertEquals(1.3646, PerDiagramImages.rasterScale(901, 1235), 0);
		// e.g. drawn at another display scale
		assertEquals((1500 - 6) / 840.0, PerDiagramImages.rasterScale(840, 1500), 1e-9);
		assertEquals(1.3646, PerDiagramImages.rasterScale(-1, 1150), 0);
	}

	@Test
	public void aRasterDiagramEaCouldNotDrawSaysSo() throws Exception {
		File xml = compare(new FakeEa(DiagramImage.PNG), 494);
		new File(tmp.getRoot(), "Images-destination/" + DIAGRAM + ".png").delete();
		File html = new File(tmp.getRoot(), "report.html");
		new ReportRenderer(false, null, true, "png").render(xml, html);
		String page = new String(Files.readAllBytes(html.toPath()), StandardCharsets.UTF_8);
		assertTrue(page.contains("src=\"Images-baseline/" + DIAGRAM + ".png\""));
		assertFalse(page.contains("src=\"Images-destination/" + DIAGRAM));
		assertTrue(page.contains("<div class=\"dnone\">No image: EA could not draw this diagram.</div>"));
	}

	@Test
	public void imageHeightIsReadFromTheFile() throws Exception {
		File f = tmp.newFile("x.png");
		ImageIO.write(new BufferedImage(10, 37, BufferedImage.TYPE_INT_RGB), "png", f);
		assertEquals(37, PerDiagramImages.imageHeight(f));
		assertEquals(-1, PerDiagramImages.imageHeight(new File(tmp.getRoot(), "missing.png")));
	}
}
