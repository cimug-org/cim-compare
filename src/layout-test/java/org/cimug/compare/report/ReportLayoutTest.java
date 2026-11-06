package org.cimug.compare.report;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

/**
 * Checks the report's layout in a real browser (#219): the things the text-based tests cannot
 * see. Opt-in: {@code mvn verify -Playout} (see pom.xml). Uses the mini comparison fixture with a
 * real EA diagram image (373 px wide, narrower than a diagram panel at every width tested), so a
 * stretched image frame would put the highlight boxes off the image at once.
 */
public class ReportLayoutTest {

	private static final int[] WIDTHS = { 1440, 2560, 3840 };

	@ClassRule
	public static TemporaryFolder tmp = new TemporaryFolder();

	private static Playwright playwright;
	private static Browser browser;
	private static File normal, full;

	@BeforeClass
	public static void renderAndLaunch() throws Exception {
		normal = render("normal", false);
		full = render("full", true);
		playwright = Playwright.create();
		BrowserType.LaunchOptions options = new BrowserType.LaunchOptions();
		String chromium = System.getProperty("layout.chromium");
		if (chromium != null && !chromium.isEmpty())
			options.setExecutablePath(Paths.get(chromium));
		browser = playwright.chromium().launch(options);
	}

	@AfterClass
	public static void close() {
		if (browser != null)
			browser.close();
		if (playwright != null)
			playwright.close();
	}

	/** The fixture, with an unchanged diagram added for --full, and an image for every diagram. */
	private static File render(String name, boolean fullReport) throws Exception {
		File dir = tmp.newFolder(name);
		String xml;
		try (InputStream in = ReportLayoutTest.class.getClassLoader().getResourceAsStream(ReportPreProcessorTest.FIXTURE)) {
			xml = new String(readAll(in), StandardCharsets.UTF_8);
		}
		String unchanged = "<CompareItem name=\"DocUnchanged\" type=\"Diagram\" guid=\"{00000000-0000-0000-0000-000000000998}\" status=\"Identical\">"
				+ "<Properties><Property name=\"DiagramType\" model=\"ClassDiagram\" baseline=\"ClassDiagram\" status=\"Identical\"/>"
				+ "<Property name=\"Name\" model=\"DocUnchanged\" baseline=\"DocUnchanged\" status=\"Identical\"/></Properties></CompareItem>\n";
		String anchor = "<!-- #37: a diagram whose only change is its name (nothing on it changed) -->";
		assertTrue(xml.contains(anchor));
		xml = xml.replace(anchor, unchanged + anchor);
		File comparison = new File(dir, "comparison-report.xml");
		Files.write(comparison.toPath(), xml.getBytes(StandardCharsets.UTF_8));

		Path svg = Paths.get(ReportLayoutTest.class.getClassLoader().getResource("svg/DocGeneration.svg").toURI());
		Matcher m = Pattern.compile("type=\"Diagram\" guid=\"\\{([0-9A-Fa-f-]+)\\}\"").matcher(xml);
		while (m.find()) {
			String file = "EAID_" + m.group(1).replace('-', '_') + ".svg";
			for (String side : new String[] { "Images-baseline", "Images-destination" }) {
				File d = new File(dir, side);
				d.mkdirs();
				Files.copy(svg, new File(d, file).toPath(), StandardCopyOption.REPLACE_EXISTING);
			}
		}
		File html = new File(dir, "comparison-report.html");
		new ReportRenderer(fullReport, null, true, "svg").render(comparison, html);
		return html;
	}

	private static byte[] readAll(InputStream in) throws Exception {
		java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
		byte[] b = new byte[8192];
		for (int n; (n = in.read(b)) > 0;)
			out.write(b, 0, n);
		return out.toByteArray();
	}

	/** Opens the report at the given window width with every node open and every image loaded. */
	private static Page open(File html, int width) {
		Page page = browser.newPage(new Browser.NewPageOptions().setViewportSize(width, 1200));
		page.navigate(html.toURI().toString());
		page.evaluate("() => { document.querySelectorAll('.node').forEach(n => n.classList.remove('closed'));"
				+ " document.querySelectorAll('img').forEach(i => i.loading = 'eager'); }");
		page.waitForFunction("() => [...document.querySelectorAll('.diag img')].every(i => i.complete && i.naturalWidth > 0)");
		page.waitForTimeout(300); // report.js places the highlight boxes once each image has loaded
		return page;
	}

	@SuppressWarnings("unchecked")
	@Test
	public void highlightBoxesStayOnTheDiagramImage() {
		for (int width : WIDTHS) {
			Page page = open(normal, width);
			List<Map<String, Object>> panels = (List<Map<String, Object>>) page.evaluate("() => [...document.querySelectorAll('.diag .dimg')].map(d => {"
					+ " const i = d.querySelector('img').getBoundingClientRect(), f = d.getBoundingClientRect();"
					// where report.js means each box to be: its data-box in image pixels, 4 px out, clipped to the image, scaled to the image on screen
					// (measured inside the image's border; boxes the fixture places wholly beyond the stand-in image are skipped)
					+ " const img = d.querySelector('img'), w = img.naturalWidth, h = img.naturalHeight, bw = parseFloat(getComputedStyle(img).borderLeftWidth) || 0;"
					+ " const x0 = i.left + bw, y0 = i.top + bw, sx = (i.width - 2 * bw) / w, sy = (i.height - 2 * bw) / h;"
					+ " const on = [...d.querySelectorAll('.hl')].filter(s => { const b = s.getAttribute('data-box').split(',').map(Number); return b[0] - 4 < w && b[1] - 4 < h; });"
					+ " const out = on.filter(s => { const b = s.getAttribute('data-box').split(',').map(Number);"
					+ "   const l = Math.max(0, b[0] - 4), t = Math.max(0, b[1] - 4), r = Math.min(w, b[2] + 4), bt = Math.min(h, b[3] + 4);"
					+ "   const g = s.getBoundingClientRect();"
					+ "   return Math.abs(g.left - (x0 + l * sx)) > 1.5 || Math.abs(g.top - (y0 + t * sy)) > 1.5"
					+ "     || Math.abs(g.width - (r - l) * sx) > 1.5 || Math.abs(g.height - (bt - t) * sy) > 1.5; }).length;"
					+ " return { name: d.closest('.node').querySelector(':scope > .row').dataset.name + ':' + d.dataset.side,"
					+ "   frameVsImage: Math.round(Math.abs(f.width - i.width) + Math.abs(f.height - i.height)), boxes: on.length, outside: out }; })");
			assertTrue("at " + width + " px the fixture shows diagram images", !panels.isEmpty());
			int boxes = 0;
			for (Map<String, Object> p : panels) {
				String where = p.get("name") + " at " + width + " px";
				assertTrue(where + ": the frame the boxes are placed in has the image's size", ((Number) p.get("frameVsImage")).intValue() <= 2);
				assertEquals(where + ": highlight boxes not where their image coordinates put them", 0, ((Number) p.get("outside")).intValue());
				boxes += ((Number) p.get("boxes")).intValue();
			}
			assertTrue("at " + width + " px some highlight boxes are drawn", boxes > 0);
			page.close();
		}
	}

	@Test
	public void thePageUsesTheWindowWidthWithoutScrollingSideways() {
		for (int width : WIDTHS) {
			Page page = open(normal, width);
			Number wrap = (Number) page.evaluate("() => document.querySelector('.wrap').getBoundingClientRect().width");
			Number scroll = (Number) page.evaluate("() => document.documentElement.scrollWidth");
			assertEquals("the content fills a " + width + " px window", width, wrap.intValue());
			assertTrue("no sideways scrolling at " + width + " px", scroll.intValue() <= width);
			page.close();
		}
	}

	@Test
	public void anUnchangedDiagramsImageIsCentredInAFullReport() {
		for (int width : WIDTHS) {
			Page page = open(full, width);
			Number offset = (Number) page.evaluate("() => { const r = [...document.querySelectorAll('.node.dia > .row')].find(r => r.dataset.name === 'DocUnchanged');"
					+ " const panel = r.parentElement.querySelector('.diag.single > div').getBoundingClientRect();"
					+ " const img = r.parentElement.querySelector('.diag.single img').getBoundingClientRect();"
					+ " return Math.abs((panel.left + panel.width / 2) - (img.left + img.width / 2)); }");
			assertTrue("the unchanged diagram's image is centred at " + width + " px (off by " + offset + " px)", offset.doubleValue() <= 1.5);
			page.close();
		}
	}
}
