package org.cimug.compare;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.cimug.compare.logs.CompareItem;
import org.cimug.compare.logs.Properties;
import org.cimug.compare.logs.Property;
import org.cimug.compare.uml1_3.DiagramElement;

/**
 * Element-level differences between the baseline and destination copies of one
 * diagram (issue #37), for highlighting on the exported diagram images.
 * <p>
 * EA writes each diagram element's box in the exported image's own pixel
 * coordinates ({@code imgL;imgT;imgR;imgB} in the element's geometry), so the
 * boxes can be drawn over the images as they are. Elements without those
 * coordinates (connectors) are not highlighted.
 * <p>
 * Each difference is returned as a {@code CompareItem} of type
 * {@code DiagramObject} whose status is one of:
 * <ul>
 * <li>{@code Model only} — the element was added to the diagram;</li>
 * <li>{@code Baseline only} — the element was removed from the diagram;</li>
 * <li>{@code Moved} — the element was moved or resized;</li>
 * <li>{@code Changed} — only the element's style (colours, fonts, etc.) changed;</li>
 * <li>{@code Identical} — the element's box and style did not change. These are
 * listed too, because the element's class may still have changed.</li>
 * </ul>
 * and whose {@code Box} property holds the box on each side as
 * {@code left,top,right,bottom}. Whether the element's class itself changed is
 * decided later from the class comparison, when the report is built.
 * <p>
 * EA frequently shifts a whole diagram when it is exported (every element moves
 * by the same amount), so moves are measured after taking away the most common
 * shift among the elements present on both sides.
 */
public class DiagramElementDiff {

	public static final String TYPE = "DiagramObject";
	public static final String CONNECTOR_TYPE = "DiagramConnector";

	/** Connector geometry keys that describe its route. */
	private static final String[] ROUTE = { "SX", "SY", "EX", "EY", "EDGE" };
	/** Connector geometry keys that position its labels (role names, multiplicities, name). */
	private static final String[] LABELS = { "LLT", "LLB", "$LLB", "LRT", "LRB", "LMT", "LMB", "IRHS", "ILHS" };

	/** Pixels of difference tolerated before a box counts as moved or resized. */
	static final int TOLERANCE = 3;

	private DiagramElementDiff() {
	}

	/**
	 * @param baseline    elements of the baseline diagram (may be null)
	 * @param destination elements of the destination diagram (may be null)
	 * @param nameOf      display name for an element's subject (xmi.id), or null
	 */
	public static List<CompareItem> compare(List<DiagramElement> baseline, List<DiagramElement> destination,
			Function<String, String> nameOf) {
		Map<String, DiagramElement> b = boxed(baseline);
		Map<String, DiagramElement> d = boxed(destination);

		// The whole-diagram shift: the most common offset of the shared elements.
		Map<String, Integer> offsets = new HashMap<>();
		int dx = 0, dy = 0, best = 0;
		for (Map.Entry<String, DiagramElement> e : b.entrySet()) {
			DiagramElement t = d.get(e.getKey());
			if (t == null)
				continue;
			int[] bb = box(e.getValue()), tb = box(t);
			int ox = tb[0] - bb[0], oy = tb[1] - bb[1];
			int n = offsets.merge(ox + "," + oy, 1, Integer::sum);
			if (n > best) {
				best = n;
				dx = ox;
				dy = oy;
			}
		}

		List<CompareItem> out = new ArrayList<>();
		for (Map.Entry<String, DiagramElement> e : b.entrySet()) {
			DiagramElement t = d.get(e.getKey());
			int[] bb = box(e.getValue());
			if (t == null) {
				out.add(item(e.getKey(), nameOf, Status.BaselineOnly, bb, null));
				continue;
			}
			int[] tb = box(t);
			boolean resized = Math.abs((bb[2] - bb[0]) - (tb[2] - tb[0])) > TOLERANCE
					|| Math.abs((bb[3] - bb[1]) - (tb[3] - tb[1])) > TOLERANCE;
			boolean moved = Math.abs(tb[0] - bb[0] - dx) > TOLERANCE || Math.abs(tb[1] - bb[1] - dy) > TOLERANCE;
			if (resized || moved)
				out.add(item(e.getKey(), nameOf, Status.Moved, bb, tb));
			else if (!same(e.getValue().getStyle(), t.getStyle()))
				out.add(item(e.getKey(), nameOf, Status.Changed, bb, tb));
			else
				out.add(item(e.getKey(), nameOf, Status.Identical, bb, tb));
		}
		for (Map.Entry<String, DiagramElement> e : d.entrySet())
			if (!b.containsKey(e.getKey()))
				out.add(item(e.getKey(), nameOf, Status.ModelOnly, null, box(e.getValue())));
		out.addAll(connectors(baseline, destination, b, d, nameOf));
		return out;
	}

	/**
	 * Changed connectors. EA does not store the route it computes for a
	 * connector, so connectors are not drawn; each change is reported with the
	 * boxes of the two elements it joins, and the report lists them. Only
	 * changed connectors are returned, each with a {@code Change} property of
	 * {@code added}, {@code removed}, {@code rerouted}, {@code labels moved} or
	 * {@code restyled}, and {@code SourceBox} / {@code TargetBox} properties.
	 */
	private static List<CompareItem> connectors(List<DiagramElement> baseline, List<DiagramElement> destination,
			Map<String, DiagramElement> bBoxed, Map<String, DiagramElement> dBoxed, Function<String, String> nameOf) {
		Map<String, DiagramElement> b = unboxed(baseline), d = unboxed(destination);
		Map<String, DiagramElement> bDuid = byDuid(bBoxed), dDuid = byDuid(dBoxed);
		int[] shift = canvasShift(bBoxed, dBoxed);
		List<CompareItem> out = new ArrayList<>();
		for (Map.Entry<String, DiagramElement> e : b.entrySet()) {
			DiagramElement t = d.get(e.getKey());
			if (t == null) {
				out.add(connector(e.getKey(), "removed", Status.BaselineOnly, e.getValue(), bDuid, true, nameOf));
				continue;
			}
			String change = connectorChange(e.getValue(), t, shift);
			if (change != null)
				out.add(connector(e.getKey(), change, "rerouted".equals(change) ? Status.Moved : Status.Changed, t,
						dDuid, false, nameOf));
		}
		for (Map.Entry<String, DiagramElement> e : d.entrySet())
			if (!b.containsKey(e.getKey()))
				out.add(connector(e.getKey(), "added", Status.ModelOnly, e.getValue(), dDuid, false, nameOf));
		return out;
	}

	private static String connectorChange(DiagramElement b, DiagramElement d, int[] shift) {
		Map<String, String> gb = geometry(b.getGeometry()), gd = geometry(d.getGeometry());
		Map<String, String> sb = geometry(b.getStyle()), sd = geometry(d.getStyle());
		for (String k : ROUTE)
			if (!same(gb.get(k), gd.get(k)))
				return "rerouted";
		if (!path(gb.get("Path"), shift).equals(path(gd.get("Path"), null)))
			return "rerouted";
		for (String k : LABELS)
			if (!same(gb.get(k), gd.get(k)))
				return "labels moved";
		sb.remove("SOID");
		sb.remove("EOID");
		sd.remove("SOID");
		sd.remove("EOID");
		return sb.equals(sd) ? null : "restyled";
	}

	/** A connector's bend points ("x:y$x:y$"), moved by the whole-diagram shift. */
	private static List<String> path(String path, int[] shift) {
		List<String> pts = new ArrayList<>();
		if (path != null)
			for (String p : path.split("\\$")) {
				String[] xy = p.split(":");
				if (xy.length != 2)
					continue;
				try {
					int x = Integer.parseInt(xy[0].trim()), y = Integer.parseInt(xy[1].trim());
					// EA's y axis runs upward in bend points and downward in Top
					pts.add(shift == null ? x + ":" + y : (x + shift[0]) + ":" + (y - shift[1]));
				} catch (NumberFormatException ignored) {
					pts.add(p);
				}
			}
		return pts;
	}

	/** The most common canvas (Left, Top) offset of the elements on both sides. */
	private static int[] canvasShift(Map<String, DiagramElement> b, Map<String, DiagramElement> d) {
		Map<String, Integer> offsets = new HashMap<>();
		int[] best = { 0, 0 };
		int most = 0;
		for (Map.Entry<String, DiagramElement> e : b.entrySet()) {
			DiagramElement t = d.get(e.getKey());
			if (t == null)
				continue;
			Map<String, String> gb = geometry(e.getValue().getGeometry()), gd = geometry(t.getGeometry());
			try {
				int ox = Integer.parseInt(gd.get("Left").trim()) - Integer.parseInt(gb.get("Left").trim());
				int oy = Integer.parseInt(gd.get("Top").trim()) - Integer.parseInt(gb.get("Top").trim());
				int n = offsets.merge(ox + "," + oy, 1, Integer::sum);
				if (n > most) {
					most = n;
					best = new int[] { ox, oy };
				}
			} catch (RuntimeException ignored) {
				// no canvas position
			}
		}
		return best;
	}

	private static CompareItem connector(String subject, String change, Status status, DiagramElement c,
			Map<String, DiagramElement> byDuid, boolean baselineSide, Function<String, String> nameOf) {
		Map<String, String> style = geometry(c.getStyle());
		DiagramElement src = byDuid.get(style.get("SOID")), dst = byDuid.get(style.get("EOID"));
		String name = endName(src, nameOf) + " \u2013 " + endName(dst, nameOf);
		Properties p = new Properties();
		p.getProperty().add(baselineSide ? new Property("Change", change, null, status.toString())
				: new Property("Change", null, change, status.toString()));
		p.getProperty().add(boxProperty("SourceBox", src, baselineSide, status));
		p.getProperty().add(boxProperty("TargetBox", dst, baselineSide, status));
		return new CompareItem(p, name, CONNECTOR_TYPE, DiffUtils.convertXmiIdToEAGUID(subject), status.toString());
	}

	private static Property boxProperty(String name, DiagramElement end, boolean baselineSide, Status status) {
		String box = end == null ? null : text(box(end));
		return baselineSide ? new Property(name, box, null, status.toString())
				: new Property(name, null, box, status.toString());
	}

	private static String endName(DiagramElement end, Function<String, String> nameOf) {
		String n = (end == null || nameOf == null) ? null : nameOf.apply(end.getSubject());
		// e.g. a class from another model that the diagram shows but the export does not define
		return n == null || n.isEmpty() ? "(not in this model)" : n;
	}

	/** Connectors (elements without image coordinates), by subject. */
	private static Map<String, DiagramElement> unboxed(List<DiagramElement> elements) {
		Map<String, DiagramElement> m = new LinkedHashMap<>();
		if (elements != null)
			for (DiagramElement e : elements)
				if (e.getSubject() != null && box(e) == null && geometry(e.getStyle()).containsKey("SOID"))
					m.putIfAbsent(e.getSubject(), e);
		return m;
	}

	/** Boxed elements by their diagram-object id (DUID), which connectors refer to. */
	private static Map<String, DiagramElement> byDuid(Map<String, DiagramElement> boxed) {
		Map<String, DiagramElement> m = new HashMap<>();
		for (DiagramElement e : boxed.values()) {
			String duid = geometry(e.getStyle()).get("DUID");
			if (duid != null)
				m.put(duid, e);
		}
		return m;
	}

	/** Elements with image coordinates, by subject (first one wins). */
	private static Map<String, DiagramElement> boxed(List<DiagramElement> elements) {
		Map<String, DiagramElement> m = new LinkedHashMap<>();
		if (elements != null)
			for (DiagramElement e : elements)
				if (e.getSubject() != null && box(e) != null)
					m.putIfAbsent(e.getSubject(), e);
		return m;
	}

	/** The element's box in image pixels, or null when EA did not write one. */
	static int[] box(DiagramElement e) {
		Map<String, String> g = geometry(e.getGeometry());
		try {
			if (g.containsKey("imgL") && g.containsKey("imgT") && g.containsKey("imgR") && g.containsKey("imgB"))
				return new int[] { Integer.parseInt(g.get("imgL").trim()), Integer.parseInt(g.get("imgT").trim()),
						Integer.parseInt(g.get("imgR").trim()), Integer.parseInt(g.get("imgB").trim()) };
		} catch (NumberFormatException ignored) {
			// fall through: no usable box
		}
		return null;
	}

	static Map<String, String> geometry(String geometry) {
		Map<String, String> g = new HashMap<>();
		if (geometry != null)
			for (String kv : geometry.split(";")) {
				int i = kv.indexOf('=');
				if (i > 0)
					g.put(kv.substring(0, i).trim(), kv.substring(i + 1));
			}
		return g;
	}

	private static boolean same(String a, String b) {
		return a == null ? b == null : a.equals(b);
	}

	private static CompareItem item(String subject, Function<String, String> nameOf, Status status, int[] baseline,
			int[] destination) {
		String name = nameOf == null ? null : nameOf.apply(subject);
		Properties p = new Properties();
		p.getProperty().add(new Property("Box", text(baseline), text(destination), status.toString()));
		return new CompareItem(p, name == null ? "" : name, TYPE, DiffUtils.convertXmiIdToEAGUID(subject),
				status.toString());
	}

	private static String text(int[] box) {
		return box == null ? null : box[0] + "," + box[1] + "," + box[2] + "," + box[3];
	}
}
