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
 * Checks the report's layout in a real browser (#219, #225): the things the text-based tests cannot
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

	/**
	 * Opens the enlarged view of a diagram by clicking one of its images (after {@code prep}, a
	 * script run on the diagram's .diag first) and returns the page, with the view at Fit.
	 */
	private static Page enlarge(File html, int width, String diagram, String side, String prep) {
		Page page = open(html, width);
		page.evaluate("([name, side, prep]) => { const d = [...document.querySelectorAll('.node.dia > .row')].find(r => r.dataset.name === name)"
				+ ".parentElement.querySelector('.diag'); if (prep) new Function('diag', prep)(d);"
				+ " d.querySelector('.dimg[data-side=\"' + side + '\"]').scrollIntoView(); d.querySelector('.dimg[data-side=\"' + side + '\"] img').click(); }",
				java.util.Arrays.asList(diagram, side, prep == null ? "" : prep));
		page.waitForFunction("() => document.querySelector('#lightbox.on .lb-stage .dimg img') && document.querySelector('#lightbox .lb-tools .lvl').textContent !== ''");
		return page;
	}

	/** Each pane's image in the enlarged view: [left, top] in its pane, its width, and its natural width. */
	@SuppressWarnings("unchecked")
	private static List<List<Number>> panes(Page page) {
		return (List<List<Number>>) page.evaluate("() => [...document.querySelectorAll('#lightbox .lb-stage')].map(s => {"
				+ " const r = s.getBoundingClientRect(), i = s.querySelector('img'), g = i.getBoundingClientRect();"
				+ " return [g.left - r.left, g.top - r.top, g.width, i.naturalWidth]; })");
	}

	private static void sameView(String where, List<List<Number>> p, double dx, double dy) {
		assertEquals(where + ": two panes", 2, p.size());
		double s = p.get(0).get(2).doubleValue() / p.get(0).get(3).doubleValue();
		assertEquals(where + ": one scale for both images", s, p.get(1).get(2).doubleValue() / p.get(1).get(3).doubleValue(), 0.002);
		assertEquals(where + ": across", dx * s, p.get(1).get(0).doubleValue() - p.get(0).get(0).doubleValue(), 1.0);
		assertEquals(where + ": down", dy * s, p.get(1).get(1).doubleValue() - p.get(0).get(1).doubleValue(), 1.0);
	}

	@Test
	public void bothImagesOpenSideBySideAndMoveTogether() {
		for (int width : WIDTHS) {
			Page page = enlarge(normal, width, "CoreOverview", "destination", null);
			String where = "CoreOverview at " + width + " px";
			assertEquals(where + ": labelled panes", "Baseline,Destination",
					page.evaluate("() => [...document.querySelectorAll('#lightbox .lb-lbl')].map(l => l.textContent).join(',')"));
			sameView(where + " at Fit", panes(page), 0, 0);
			// zoom on one pane and drag in the other: both images follow
			page.mouse().move(width / 4.0, 500);
			page.mouse().wheel(0, -400);
			page.mouse().move(width * 0.75, 500);
			page.mouse().down();
			page.mouse().move(width * 0.75 - 120, 440, new com.microsoft.playwright.Mouse.MoveOptions().setSteps(5));
			page.mouse().up();
			List<List<Number>> p = panes(page);
			assertTrue(where + ": zoomed in", p.get(0).get(2).doubleValue() / p.get(0).get(3).doubleValue() > 1);
			sameView(where + " after zooming and dragging", p, 0, 0);
			assertEquals(where + ": no Align setting on a diagram EA did not shift", 0, ((Number) page.evaluate("() => document.querySelectorAll('#lightbox .seg').length")).intValue());
			page.keyboard().press("Escape");
			assertEquals(where + ": Esc closes the view", Boolean.FALSE, page.evaluate("() => document.querySelector('#lightbox').classList.contains('on')"));
			page.close();
		}
	}

	@Test
	public void aShiftedDiagramLinesUpOnTheDiagram() {
		for (int width : WIDTHS) {
			// the shift the report computed (ReportPreProcessorTest checks how): destination minus baseline
			Page page = enlarge(normal, width, "CoreOverview", "baseline", "diag.dataset.shift = '-40,25';");
			String where = "a shifted CoreOverview at " + width + " px";
			sameView(where + ", on the diagram", panes(page), 40, -25);
			assertTrue(where + ": the hint gives the shift", ((String) page.evaluate("() => document.querySelector('#lightbox .lb-hint').textContent"))
					.endsWith("EA shifted this diagram \u221240, 25 px"));
			page.click("#lightbox .seg button:has-text('top-left corners')");
			sameView(where + ", at the corners", panes(page), 0, 0);
			page.click("#lightbox .seg button:has-text('on the diagram')");
			sameView(where + ", on the diagram again", panes(page), 40, -25);
			page.close();
		}
	}

	@Test
	public void theEnlargedViewShowsTheDiagramsHighlightKeyAcrossTheTop() {
		for (int width : WIDTHS) {
			Page page = enlarge(normal, width, "CoreOverview", "destination", null);
			String where = "at " + width + " px";
			@SuppressWarnings("unchecked")
			Map<String, Object> k = (Map<String, Object>) page.evaluate("() => { const k = document.querySelector('#lightbox .lb-key .hl-legend').getBoundingClientRect(),"
					+ " p = [...document.querySelectorAll('#lightbox .lb-pane')].map(e => e.getBoundingClientRect()),"
					+ " t = document.querySelector('#lightbox .lb-tools').getBoundingClientRect(),"
					+ " page = [...document.querySelectorAll('.node.dia > .row')].find(r => r.dataset.name === 'CoreOverview').parentElement.querySelector('.hl-legend');"
					+ " return { left: k.left - p[0].left, right: p[1].right - k.right, belowTools: k.top - t.bottom, abovePanes: p[0].top - k.bottom,"
					+ "   keys: document.querySelector('#lightbox .lb-key .hl-legend').textContent, pageKeys: page.textContent }; }");
			assertEquals(where + ": the key starts at the baseline pane", 0, ((Number) k.get("left")).doubleValue(), 1);
			assertEquals(where + ": and ends at the destination pane", 0, ((Number) k.get("right")).doubleValue(), 1);
			assertTrue(where + ": beneath the controls", ((Number) k.get("belowTools")).doubleValue() >= 0);
			assertTrue(where + ": above the images", ((Number) k.get("abovePanes")).doubleValue() >= 0);
			assertEquals(where + ": the diagram's own key", k.get("pageKeys"), k.get("keys"));
			page.close();
			// one image (here the baseline image taken away): the single view, with the key above the image
			page = enlarge(normal, width, "CoreOverview", "destination", "diag.querySelector('.dimg[data-side=\"baseline\"]').remove();");
			Number gap = (Number) page.evaluate("() => document.querySelector('#lightbox .lb-stage').getBoundingClientRect().top"
					+ " - document.querySelector('#lightbox .lb-key').getBoundingClientRect().bottom");
			assertEquals(where + ": one image, no panes", 0, ((Number) page.evaluate("() => document.querySelectorAll('#lightbox .lb-pane').length")).intValue());
			assertTrue(where + ": one image, the key above it", gap.doubleValue() >= 0);
			page.close();
			// highlights turned off: no key
			page = enlarge(normal, width, "CoreOverview", "destination", "document.body.classList.add('no-hl');");
			assertEquals(where + ": no key with highlights off", 0, ((Number) page.evaluate("() => document.querySelectorAll('#lightbox .lb-key').length")).intValue());
			page.close();
		}
	}
}
