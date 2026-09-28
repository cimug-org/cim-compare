package org.cimug.compare;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cimug.compare.logs.CompareItem;
import org.cimug.compare.logs.Property;
import org.cimug.compare.uml1_3.DiagramElement;
import org.junit.Test;

/** #37: element-level differences between two copies of a diagram. */
public class DiagramElementDiffTest {

	private static final String A = "EAID_00000000_0000_0000_0000_00000000000A";
	private static final String B = "EAID_00000000_0000_0000_0000_00000000000B";
	private static final String C = "EAID_00000000_0000_0000_0000_00000000000C";
	private static final String D = "EAID_00000000_0000_0000_0000_00000000000D";
	private static final String E = "EAID_00000000_0000_0000_0000_00000000000E";
	private static final String LINK = "EAID_00000000_0000_0000_0000_0000000000FF";

	private static DiagramElement el(String subject, int l, int t, int r, int b, String style) {
		DiagramElement e = new DiagramElement();
		e.setSubject(subject);
		// canvas coordinates first (as EA writes them), then the image pixels
		e.setGeometry("Left=" + (l * 2) + ";Top=" + (t * 2) + ";Right=" + (r * 2) + ";Bottom=" + (b * 2) + ";imgL=" + l
				+ ";imgT=" + t + ";imgR=" + r + ";imgB=" + b + ";");
		e.setStyle(style);
		return e;
	}

	private static DiagramElement connector(String subject) {
		DiagramElement e = new DiagramElement();
		e.setSubject(subject);
		e.setGeometry("SX=0;SY=0;EX=0;EY=0;EDGE=2;$LLB=;Path=;");
		e.setStyle("Mode=3;EOID=X;SOID=Y;");
		return e;
	}

	private static Map<String, CompareItem> bySubject(List<CompareItem> items) {
		Map<String, CompareItem> m = new HashMap<>();
		for (CompareItem i : items)
			m.put(i.getGuid(), i);
		return m;
	}

	private static String guid(String eaid) {
		return DiffUtils.convertXmiIdToEAGUID(eaid);
	}

	private static Property box(CompareItem i) {
		return i.getProperties().getProperty().get(0);
	}

	@Test
	public void classifiesEachElement() {
		List<DiagramElement> baseline = Arrays.asList( //
				el(A, 10, 10, 100, 50, "s"), // unchanged
				el(B, 10, 100, 100, 150, "s"), // will move
				el(C, 200, 10, 300, 50, "s"), // will be restyled
				el(D, 200, 100, 300, 150, "s"), // will be removed
				connector(LINK));
		List<DiagramElement> destination = Arrays.asList( //
				el(A, 10, 10, 100, 50, "s"), //
				el(B, 10, 180, 100, 230, "s"), //
				el(C, 200, 10, 300, 50, "BCol=255;"), //
				el(E, 400, 10, 500, 60, "s"), // added
				connector(LINK));
		Map<String, String> names = new HashMap<>();
		names.put(E, "Added");
		Map<String, CompareItem> m = bySubject(DiagramElementDiff.compare(baseline, destination, names::get));

		assertEquals("Identical", m.get(guid(A)).getStatus());
		assertEquals("Moved", m.get(guid(B)).getStatus());
		assertEquals("Changed", m.get(guid(C)).getStatus());
		assertEquals("Baseline only", m.get(guid(D)).getStatus());
		assertEquals("Model only", m.get(guid(E)).getStatus());
		assertEquals("connectors have no image box", 5, m.size());
		assertTrue(m.values().stream().allMatch(i -> DiagramElementDiff.TYPE.equals(i.getType())));

		assertEquals("Added", m.get(guid(E)).getName());
		assertEquals("", m.get(guid(A)).getName());
		assertEquals("10,100,100,150", box(m.get(guid(B))).getBaseline());
		assertEquals("10,180,100,230", box(m.get(guid(B))).getModel());
		assertEquals("200,100,300,150", box(m.get(guid(D))).getBaseline());
		assertNull(box(m.get(guid(D))).getModel());
		assertNull(box(m.get(guid(E))).getBaseline());
	}

	@Test
	public void aShiftOfTheWholeDiagramIsNotAMove() {
		List<DiagramElement> baseline = new ArrayList<>(), destination = new ArrayList<>();
		for (String s : new String[] { A, B, C }) {
			int y = baseline.size() * 100;
			baseline.add(el(s, 10, y, 100, y + 50, "s"));
			destination.add(el(s, 10 - 7, y + 40, 100 - 7, y + 90, "s"));
		}
		// D moves relative to the others
		baseline.add(el(D, 300, 0, 400, 50, "s"));
		destination.add(el(D, 300 - 7 + 120, 40, 400 - 7 + 120, 90, "s"));
		Map<String, CompareItem> m = bySubject(DiagramElementDiff.compare(baseline, destination, null));
		assertEquals("Identical", m.get(guid(A)).getStatus());
		assertEquals("Identical", m.get(guid(B)).getStatus());
		assertEquals("Identical", m.get(guid(C)).getStatus());
		assertEquals("Moved", m.get(guid(D)).getStatus());
	}

	@Test
	public void resizingCountsAsMovedAndSmallDifferencesAreIgnored() {
		List<DiagramElement> baseline = Arrays.asList(el(A, 10, 10, 100, 50, "s"), el(B, 10, 100, 100, 150, "s"));
		List<DiagramElement> destination = Arrays.asList(el(A, 10, 10, 100, 90, "s"), // taller
				el(B, 12, 101, 102, 152, "s")); // within the tolerance
		Map<String, CompareItem> m = bySubject(DiagramElementDiff.compare(baseline, destination, null));
		assertEquals("Moved", m.get(guid(A)).getStatus());
		assertEquals("Identical", m.get(guid(B)).getStatus());
	}

	@Test
	public void missingDiagramElementsAreTolerated() {
		assertEquals(0, DiagramElementDiff.compare(null, null, null).size());
		assertEquals("Model only",
				DiagramElementDiff.compare(null, Arrays.asList(el(A, 1, 1, 9, 9, "s")), null).get(0).getStatus());
	}
}
