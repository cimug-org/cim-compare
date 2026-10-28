package org.cimug.compare;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.cimug.compare.logs.CompareItem;
import org.cimug.compare.logs.Properties;
import org.cimug.compare.logs.Property;
import org.cimug.compare.uml1_3.DiagramElement;
import org.junit.Test;

/**
 * SVG diagram images (#139). The numbers are from a real EA 17 export of a class
 * diagram (the JPG from the XMI export and the SVG from PutDiagramImageToFile): EA
 * drew the JPG 1.3646 times larger than the SVG, and the SVG at the diagram
 * positions less a shift of (294, 72).
 */
public class SvgDiagramsTest {

	private static DiagramElement el(String subject, int l, int t, int r, int b, int il, int it, int ir, int ib) {
		DiagramElement e = new DiagramElement();
		e.setSubject(subject);
		e.setGeometry("Left=" + l + ";Top=" + t + ";Right=" + r + ";Bottom=" + b + ";imgL=" + il + ";imgT=" + it + ";imgR="
				+ ir + ";imgB=" + ib + ";");
		return e;
	}

	private static String rect(int x, int y, int w, int h) {
		return "<g><rect x=\"" + x + "\" y=\"" + y + "\" width=\"" + w + "\" height=\"" + h
				+ "\" rx=\"0.00\" shape-rendering=\"auto\"  /></g>\n";
	}

	/** Seven elements of the real diagram, and the SVG's boxes for them (plus the frame). */
	private static final List<DiagramElement> ELEMENTS = Arrays.asList(
			el("EAID_A", 1222, 462, 1412, 532, 1266, 546, 1526, 641),
			el("EAID_B", 703, 112, 837, 192, 558, 68, 741, 177),
			el("EAID_C", 386, 410, 743, 500, 126, 475, 613, 598),
			el("EAID_D", 865, 394, 1049, 554, 779, 453, 1030, 671),
			el("EAID_E", 669, 635, 1056, 742, 512, 782, 1040, 928),
			el("EAID_F", 329, 517, 499, 635, 48, 621, 280, 782),
			el("EAID_G", 329, 645, 595, 819, 48, 796, 411, 1033));

	private static final String SVG = "<?xml version=\"1.0\"?><svg xmlns=\"http://www.w3.org/2000/svg\" width=\"30.90cm\""
			+ " height=\"22.23cm\" viewBox=\"0 0 1168 840\">\n" + "<g><rect x=\"0\" y=\"0\" width=\"1168\" height=\"840\" shape-rendering=\"auto\"/></g>\n"
			+ rect(928, 390, 190, 70) + rect(409, 40, 134, 80) + rect(92, 338, 357, 90) + rect(571, 322, 184, 160)
			+ rect(375, 563, 387, 107) + rect(35, 445, 170, 118) + rect(35, 573, 266, 174)
			+ "<text style=\"font-family:Lucida Sans Unicode; font-size:10px;\">ProtectionFunction</text>"
			+ "<text style=\"font-family:Arial; font-size:16px;\">note</text></svg>";

	@Test
	public void theShiftIsFoundFromTheBoxesOfTheSameSize() {
		assertArrayEquals(new int[] { 294, 72 }, SvgDiagrams.shift(SVG, ELEMENTS));
	}

	@Test
	public void noShiftWhenNothingMatches() {
		assertNull(SvgDiagrams.shift("<svg viewBox=\"0 0 10 10\">" + rect(1, 1, 5, 5) + "</svg>", ELEMENTS));
	}

	@Test
	public void highlightBoxesMoveFromTheJpgToTheSvg() {
		Map<String, String> map = SvgDiagrams.boxMap(ELEMENTS, new int[] { 294, 72 });
		assertEquals("92,338,449,428", map.get("126,475,613,598"));
		assertEquals("928,390,1118,460", map.get("1266,546,1526,641"));
		assertEquals(7, map.size());
	}

	@Test
	public void boxesOfElementsAndConnectorsAreConverted() {
		Map<String, String> base = SvgDiagrams.boxMap(ELEMENTS, new int[] { 294, 72 });
		Map<String, String> dest = new java.util.HashMap<>(); // e.g. the destination SVG could not be matched
		Properties p = new Properties();
		p.getProperty().add(new Property("Box", "126,475,613,598", "126,475,613,598", "Moved"));
		p.getProperty().add(new Property("SourceBox", "1266,546,1526,641", null, "Changed"));
		p.getProperty().add(new Property("Change", "rerouted", null, "Changed"));
		List<CompareItem> items = new ArrayList<>();
		items.add(new CompareItem(p, "x", "DiagramObject", "{A}", "Moved"));
		SvgDiagrams.convert(items, base, dest);
		assertEquals("92,338,449,428", p.getProperty().get(0).getBaseline());
		assertNull("no box rather than one in the wrong place", p.getProperty().get(0).getModel());
		assertEquals("928,390,1118,460", p.getProperty().get(1).getBaseline());
		assertEquals("rerouted", p.getProperty().get(2).getBaseline());
	}

	@Test
	public void fontsGetFallbacksForSystemsWithoutThem() {
		String out = SvgDiagrams.withFontFallbacks(SVG);
		assertTrue(out.contains("font-family:'Lucida Sans Unicode','Lucida Grande','DejaVu Sans',Arial,sans-serif;"));
		assertTrue(out.contains("font-family:Arial,Helvetica,'Liberation Sans',sans-serif;"));
		// already a list: left alone
		assertEquals("<x style=\"font-family:Georgia,serif;\"/>", SvgDiagrams.withFontFallbacks("<x style=\"font-family:Georgia,serif;\"/>"));
	}

	@Test
	public void onlyRealSvgCounts() {
		assertTrue(SvgDiagrams.isSvg(SVG));
		assertFalse(SvgDiagrams.isSvg("\u0001\u0000\u0000\u0000l\u0000 EMF data"));
		assertFalse(SvgDiagrams.isSvg(null));
	}
	@Test
	public void aClassEaDrewWiderThanItsStoredSizeGetsAHighlightAsWideAsTheDrawing() {
		// from a real EA 17 diagram: ControlFunctionBlock is stored 217 wide but drawn 252 wide,
		// its attribute lines being longer than the stored box
		List<DiagramElement> one = Arrays.asList(el("EAID_X", 661, 82, 878, 218, 0, 0, 0, 0));
		String svg = "<svg viewBox=\"0 0 900 300\">" + rect(367, 10, 252, 136) + "</svg>";
		Map<String, String> map = SvgDiagrams.boxMap(one, new int[] { 294, 72 }, svg);
		assertEquals("367,10,619,146", map.get("0,0,0,0"));
		// without the SVG, the stored size
		assertEquals("367,10,584,146", SvgDiagrams.boxMap(one, new int[] { 294, 72 }).get("0,0,0,0"));
	}

	@Test
	public void packagesAndNotesDrawnAsPolygonsCountForTheShift() {
		// a package: its tab and body drawn as one polygon, 200 x 120 overall
		List<DiagramElement> pkg = Arrays.asList(el("EAID_P", 400, 200, 600, 320, 0, 0, 0, 0));
		String svg = "<svg viewBox=\"0 0 500 400\"><polygon points=\"100 128 100 248 300 248 300 148 180 148 180 128 100 128\"/></svg>";
		assertArrayEquals(new int[] { 300, 72 }, SvgDiagrams.shift(svg, pkg));
	}
}
