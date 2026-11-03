package org.cimug.compare.report;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * One consistent set of rules for notes, ⓘ pop-ups and hover/inline display
 * (issue #199), checked on the mini comparison fixture.
 */
public class ConsistencyRulesTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private String render(boolean full) throws Exception {
		File xml = tmp.newFile("comparison-report-" + full + ".xml");
		InputStream in = getClass().getClassLoader().getResourceAsStream(ReportPreProcessorTest.FIXTURE);
		try {
			Files.copy(in, xml.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} finally {
			in.close();
		}
		File html = new File(tmp.getRoot(), "report-" + full + ".html");
		new ReportRenderer(full, null, false, "jpg").render(xml, html);
		String page = new String(Files.readAllBytes(html.toPath()), StandardCharsets.UTF_8);
		return page.substring(page.indexOf("<div class=\"wrap\">")); // the report body, not the CSS and JS
	}

	/** The detail and pop-up of the node whose row is named {@code name}. */
	private static String node(String page, String name) {
		int row = page.indexOf("data-name=\"" + name + "\" data-kind=\"⚙\"");
		assertTrue(name, row > 0);
		int next = page.indexOf("<div class=\"node ", row);
		return page.substring(row, next < 0 ? page.length() : next);
	}

	private static int count(String s, String what) {
		return s.split(Pattern.quote(what), -1).length - 1;
	}

	@Test
	public void notesAreNamedAsInTheComparisonXml() throws Exception {
		String page = render(false);
		assertTrue(page.contains("<div class=\"lbl\">Class notes (destination, redlined)</div>"));
		assertTrue(page.contains("<div class=\"lbl\">Class notes (baseline)</div>")); // the removed class
		assertTrue(page.contains("<div class=\"lbl\">Package notes</div>"));
		assertTrue(page.contains("<div class=\"lbl\">Attribute notes (destination, redlined)</div>"));
		assertTrue(page.contains("<i class=\"nodesc\">No notes defined in the model.</i>"));
		assertFalse(page.contains("escription</div>"));
		assertFalse(page.contains("No notes available."));
		// one box on the page (shown in inline mode) and the same box in the pop-up
		assertFalse(page.contains("Baseline description"));
		assertTrue(page.contains("<div class=\"desc one plain\">"));
		assertTrue(page.contains("<div class=\"desc one pdesc\">"));
	}

	@Test
	public void everyPopupHasTheSameLayout() throws Exception {
		String page = render(false);
		String eq = node(page, "Switch");
		String pop = eq.substring(eq.indexOf("<div class=\"notes-src\">"), eq.indexOf("<span class=\"badge\""));
		int notes = pop.indexOf("desc one pdesc"), meta = pop.indexOf("Class metadata — changes"),
				tags = pop.indexOf("Tagged values — changes");
		assertTrue(pop, notes > 0 && meta > notes && tags > meta);
		// only changed rows; an empty table keeps its header and says "no changes"
		assertFalse(pop, pop.contains("data-status=\"identical\""));
		assertTrue(page.contains("<td colspan=\"3\" class=\"none-cell\"><i>no changes</i></td>"));
		assertFalse(page.contains("no metadata changes"));
		// the browser's own tooltip no longer covers the pop-up
		assertFalse(Pattern.compile("class=\"info[^\"]*\"[^>]*title=").matcher(page).find());
	}

	@Test
	public void hoverModeHidesNotesMetadataAndTaggedValues() throws Exception {
		String page = render(false);
		assertTrue(page.contains("<div class=\"sec hov-hide\"><div class=\"sec-h\"><span class=\"ico\">▤</span>Metadata</div>"));
		// a change only the pop-up shows marks the ⓘ, and an item with nothing else says so
		assertTrue(node(page, "Switch").contains("<span class=\"info info-chg\""));
		assertTrue(page.contains("<div class=\"sec hov-only\"><div class=\"hov-note\">Class 'Switch' has no other changes. The <svg class=\"ii\""));
		assertTrue(page.contains(" shows its notes and every changed property.</div></div>"));
	}

	@Test
	public void addedAndRemovedClassesAreLaidOutLikeChangedOnes() throws Exception {
		String page = render(false);
		assertFalse(page.contains("Show full metadata"));
		assertFalse(page.contains("<details class=\"more\">"));
		String plant = node(page, "Plant"); // removed
		assertTrue(plant.contains("<div class=\"linkB\" data-status=\"deleted\""));
		assertFalse(plant.contains("<ul class=\"plain\">"));
		assertTrue(plant.contains("<td class=\"void\"></td>"));
		// section headings carry no counts
		assertFalse(Pattern.compile("(Attributes|Links) <span class=\"n\">").matcher(page).find());
	}

	@Test
	public void theMissingSideIsHatchedEverywhere() throws Exception {
		String page = render(false);
		// no table drops a column for a side that does not exist
		assertFalse(page.contains("Destination — new in model"));
		assertFalse(page.contains("Baseline — removed from model"));
		assertEquals(0, count(page, "<thead><tr><th></th><th>Baseline</th></tr></thead>"));
		// one wording, lower case
		assertFalse(page.contains("does not exist"));
		assertFalse(page.contains("REMOVED FROM MODEL"));
		assertTrue(page.contains("<span class=\"gone\">removed from model</span>"));
	}

	@Test
	public void linkEndNamesBreakBetweenWords() throws Exception {
		String page = render(false);
		assertTrue(page.contains("Power<wbr>System<wbr>Resource"));
	}

	@Test
	public void anUnchangedItemInAFullReportShowsItsValues() throws Exception {
		String page = render(true);
		String io = node(page, "IdentifiedObject");
		assertTrue(io, io.contains("<div class=\"lbl sp\">Class metadata</div><div class=\"tw\"><table class=\"t one-col\"><thead><tr><th></th><th>Value</th></tr></thead>"));
		assertTrue(io, io.contains("<div class=\"lbl sp\">Tagged values</div>"));
		assertTrue(io, io.contains("Class 'IdentifiedObject' is unchanged. The "));
		assertTrue(io, io.contains(" shows its notes and metadata."));
		// an unchanged link shows its ends' values, like its metadata
		Matcher m = Pattern.compile("<div class=\"linkB\" data-status=\"identical\".*?<div class=\"sub\">Source end</div><div class=\"tw\"><table class=\"t one-col\">", Pattern.DOTALL).matcher(page);
		assertTrue(m.find());
		// rows empty on both sides are left out
		assertFalse(Pattern.compile("<th class=\"k\">[^<]*</th><td></td><td></td></tr>").matcher(page).find());
	}

	@Test
	public void aNormalReportKeepsChangesOnlyForUnchangedPackages() throws Exception {
		String page = render(false);
		assertFalse(page.contains("one-col"));
		assertFalse(page.contains("is unchanged."));
	}

	@Test
	public void aChangedItemsPopupListsChangesOnlyInANormalReportAndEveryPropertyInAFullOne() throws Exception {
		// a normal report: changes only, "— changes", an empty table says "no changes"
		String normalPage = render(false), fullPage = render(true);
		String normal = node(normalPage, "Switch");
		assertTrue(normal.contains("<div class=\"lbl sp\">Class metadata — changes</div>"));
		assertFalse(normal.substring(normal.indexOf("<div class=\"notes-src\">")).contains("data-status=\"identical\""));
		// --full: every property, baseline beside destination, the changed rows tinted
		String full = node(fullPage, "Switch");
		String pop = full.substring(full.indexOf("<div class=\"notes-src\">"), full.indexOf("<span class=\"badge\""));
		assertTrue(pop, pop.contains("<div class=\"lbl sp\">Class metadata</div><div class=\"tw\"><table class=\"t\"><thead><tr><th></th><th>Baseline</th><th>Destination</th></tr></thead>"));
		assertTrue(pop, pop.contains("data-status=\"identical\""));
		assertTrue(pop, Pattern.compile("<tr data-status=\"(changed|added|deleted)\"").matcher(pop).find());
		assertFalse(pop, pop.contains("— changes"));
		assertFalse(pop, pop.contains("<i>no changes</i>"));
		// the legend and the hover note say what the pop-up shows
		assertTrue(fullPage.contains("shows the notes and every property</span>"));
		assertTrue(normalPage.contains("shows the notes and every changed property</span>"));
		assertTrue(full.contains("Class 'Switch' has no other changes. The ") && full.contains(" shows its notes and every property."));
		assertTrue(normal.contains(" shows its notes and every changed property."));
	}

	@Test
	public void inInlineModeEachAttributeHasVisibleControlsForItsDetails() throws Exception {
		String page = render(false);
		int row = page.indexOf("data-name=\"phaseCode\"");
		String cell = page.substring(row, page.indexOf("</td>", row));
		assertTrue(cell, cell.contains("<td class=\"nm c-changed\"><span class=\"xchev\" role=\"button\" tabindex=\"0\" aria-expanded=\"false\""));
		assertTrue(cell, cell.contains("<span class=\"xdet\" role=\"button\" tabindex=\"0\">show details</span>"));
		assertEquals("one ▶ per attribute row", count(page, "<tr class=\"meta\">"), count(page, "class=\"xchev\""));
		// closed by default; Expand all opens them in inline mode, Collapse all closes them; hidden in hover mode and print
		assertFalse(page.contains("<tr class=\"meta open\">"));
		assertTrue(page.contains("if (body.classList.contains('meta-inline')) allDetails(true);"));
	}

	@Test
	public void theStylesFollowTheRules() throws Exception {
		File xml = tmp.newFile("c.xml");
		InputStream in = getClass().getClassLoader().getResourceAsStream(ReportPreProcessorTest.FIXTURE);
		try {
			Files.copy(in, xml.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} finally {
			in.close();
		}
		File html = new File(tmp.getRoot(), "css.html");
		new ReportRenderer(false, null, false, "jpg").render(xml, html);
		String page = new String(Files.readAllBytes(html.toPath()), StandardCharsets.UTF_8);
		assertTrue(page.contains("body:not(.meta-inline) .hov-hide { display: none; }"));
		assertTrue(page.contains(".pop .lbl.sp, tr.meta .lbl.sp { margin-top: 24px; }"));
		assertTrue(page.contains("--diag-panel: #aab3bd; --diag-img: #d9dee5;"));
		// #199: the inline-mode ▶ / "show details" controls are hidden in hover mode and in print
		assertTrue(page.contains("body:not(.meta-inline) .xchev, body:not(.meta-inline) .xdet { display: none; }"));
		assertTrue(page.contains("@media print { .xchev, .xdet { display: none !important; } }"));
		// #199: inline mode shows everything on the page, so no ⓘ anywhere carries the dot
		assertTrue(page.contains("body.meta-inline .info.info-chg::after { display: none; }"));
		// #204: an unchanged diagram's one image, at its own size, centred in a full-width panel
		assertTrue(page.contains(".diag.single > div { align-items: center; }"));
		// dark mode, option B "stronger", in both dark blocks (system setting and explicit theme)
		assertEquals(2, count(page, "--diag-panel: #6b7785; --diag-img: #56616d; --hatch: #ffffff17;"));
		assertTrue(page.contains("table.t td.void { background: repeating-linear-gradient("));
		assertTrue(page.contains("<span class=\"pbn\">Wait for diagram images to load in large reports.</span>"));
	}
}
