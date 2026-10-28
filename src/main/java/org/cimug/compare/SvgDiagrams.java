package org.cimug.compare;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cimug.compare.logs.CompareItem;
import org.cimug.compare.logs.Property;
import org.cimug.compare.uml1_3.DiagramElement;

/**
 * SVG diagram images (issue #139), and the placing of highlights on diagram images
 * drawn one at a time (#141).
 * <p>
 * EA's XMI export writes diagram images only as EMF, BMP, GIF, PNG or JPG; an SVG of
 * each diagram is written with {@code Project.PutDiagramImageToFile} (EA 16.1 build
 * 1629 and later). The SVG draws each element at its diagram position
 * ({@code Left;Top;Right;Bottom}, which the XMI has with or without images), less
 * one shift for the whole diagram. The shift is found by matching the SVG's boxes to
 * the elements of the same width and height, and each highlight box is moved into
 * the SVG's coordinates. (The image boxes, {@code imgL;imgT;imgR;imgB}, are in the
 * pixels of an image the XMI export wrote, which EA draws at a larger scale.)
 */
public final class SvgDiagrams {

	private SvgDiagrams() {
	}

	private static final Pattern RECT = Pattern.compile(
			"<rect\\s+x=\"(-?[0-9.]+)\"\\s+y=\"(-?[0-9.]+)\"\\s+width=\"([0-9.]+)\"\\s+height=\"([0-9.]+)\"");
	private static final Pattern FONT = Pattern.compile("font-family:([^;\"]+);");

	/** True when {@code text} is an SVG image with a viewBox (what EA writes). */
	public static boolean isSvg(String text) {
		if (text == null)
			return false;
		int i = text.indexOf("<svg");
		return i >= 0 && text.indexOf("viewBox=", i) > 0 && text.contains("</svg>");
	}

	/**
	 * Adds fallback fonts to each {@code font-family}: EA names only the font it drew
	 * with (e.g. Lucida Sans Unicode, which Mac and Linux do not have), so other
	 * systems would fall back to a serif font.
	 */
	public static String withFontFallbacks(String svg) {
		Matcher m = FONT.matcher(svg);
		StringBuffer out = new StringBuffer();
		while (m.find()) {
			String family = m.group(1).trim();
			String replacement = family.contains(",") ? family : fallbacks(family);
			m.appendReplacement(out, Matcher.quoteReplacement("font-family:" + replacement + ";"));
		}
		m.appendTail(out);
		return out.toString();
	}

	private static String fallbacks(String family) {
		String quoted = family.contains(" ") ? "'" + family + "'" : family;
		switch (family.toLowerCase()) {
		case "lucida sans unicode":
			return quoted + ",'Lucida Grande','DejaVu Sans',Arial,sans-serif";
		case "arial":
			return quoted + ",Helvetica,'Liberation Sans',sans-serif";
		case "times new roman":
			return quoted + ",Times,'Liberation Serif',serif";
		case "courier new":
			return quoted + ",Courier,'Liberation Mono',monospace";
		default:
			return quoted + ",sans-serif";
		}
	}

	/**
	 * The shift (dx, dy) between the diagram positions of {@code elements} and the
	 * boxes drawn in {@code svg}: the offset most often found between an element and
	 * an SVG box of the same width and height. Null when no element matches.
	 */
	public static int[] shift(String svg, List<DiagramElement> elements) {
		if (svg == null || elements == null)
			return null;
		Map<String, int[]> byKey = new HashMap<>();
		Map<String, Integer> votes = new HashMap<>();
		List<double[]> shapes = shapes(svg);
		int[] best = null;
		int bestVotes = 0;
		for (DiagramElement e : elements) {
			int[] g = position(e);
			if (g == null)
				continue;
			for (double[] r : shapes) {
				if (Math.round(r[2]) != g[2] - g[0] || Math.round(r[3]) != g[3] - g[1])
					continue;
				int dx = g[0] - (int) Math.round(r[0]), dy = g[1] - (int) Math.round(r[1]);
				String key = dx + "," + dy;
				byKey.putIfAbsent(key, new int[] { dx, dy });
				int n = votes.merge(key, 1, Integer::sum);
				if (n > bestVotes) {
					bestVotes = n;
					best = byKey.get(key);
				}
			}
		}
		return best;
	}

	private static final Pattern POLYGON = Pattern.compile("<polygon\\s+points=\"([-0-9.\\s,]+)\"");

	/**
	 * The boxes EA drew, as {x, y, width, height}: its rectangles (classes, enumerations,
	 * boundaries) and the bounding boxes of its polygons (packages with their tab,
	 * notes with their folded corner, ...).
	 */
	static List<double[]> shapes(String svg) {
		List<double[]> out = new java.util.ArrayList<>();
		Matcher m = RECT.matcher(svg);
		while (m.find())
			out.add(new double[] { Double.parseDouble(m.group(1)), Double.parseDouble(m.group(2)),
					Double.parseDouble(m.group(3)), Double.parseDouble(m.group(4)) });
		m = POLYGON.matcher(svg);
		while (m.find()) {
			String[] v = m.group(1).trim().split("[\\s,]+");
			if (v.length < 6)
				continue;
			double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
			try {
				for (int i = 0; i + 1 < v.length; i += 2) {
					double x = Double.parseDouble(v[i]), y = Double.parseDouble(v[i + 1]);
					minX = Math.min(minX, x);
					maxX = Math.max(maxX, x);
					minY = Math.min(minY, y);
					maxY = Math.max(maxY, y);
				}
			} catch (NumberFormatException ignored) {
				continue;
			}
			out.add(new double[] { minX, minY, maxX - minX, maxY - minY });
		}
		return out;
	}

	/** The element's diagram position (Left, Top, Right, Bottom), or null. */
	static int[] position(DiagramElement e) {
		Map<String, String> g = DiagramElementDiff.geometry(e.getGeometry());
		try {
			if (g.containsKey("Left") && g.containsKey("Top") && g.containsKey("Right") && g.containsKey("Bottom"))
				return new int[] { Integer.parseInt(g.get("Left").trim()), Integer.parseInt(g.get("Top").trim()),
						Integer.parseInt(g.get("Right").trim()), Integer.parseInt(g.get("Bottom").trim()) };
		} catch (NumberFormatException ignored) {
			// no usable position
		}
		return null;
	}

	/**
	 * For each element, the highlight box the comparison gave it ("l,t,r,b": its box in
	 * the image the XMI export wrote, or its diagram position when the XMI was exported
	 * without images) mapped to its box in the SVG, given the diagram's {@link #shift}.
	 */
	public static Map<String, String> boxMap(List<DiagramElement> elements, int[] shift) {
		return boxMap(elements, shift, null);
	}

	/**
	 * As {@link #boxMap(List, int[])}, and where the SVG ({@code svg}, may be null) has
	 * a box at the element's place that is larger than the element's stored size, the
	 * highlight takes the drawn size: EA widens a class whose attribute lines do not
	 * fit when it draws it, without changing the size stored in the model.
	 */
	public static Map<String, String> boxMap(List<DiagramElement> elements, int[] shift, String svg) {
		Map<String, String> map = new HashMap<>();
		if (elements == null || shift == null)
			return map;
		List<double[]> shapes = svg == null ? java.util.Collections.<double[]>emptyList() : shapes(svg);
		for (DiagramElement e : elements) {
			int[] g = position(e), img = DiagramElementDiff.box(e);
			if (g == null)
				continue;
			int[] key = img != null ? img : g;
			int l = g[0] - shift[0], t = g[1] - shift[1], r = g[2] - shift[0], b = g[3] - shift[1];
			for (double[] s : shapes)
				if (Math.abs(s[0] - l) <= 1 && Math.abs(s[1] - t) <= 1 && Math.abs(s[3] - (b - t)) <= 1
						&& s[2] > r - l + 1)
					r = Math.max(r, l + (int) Math.round(s[2]));
			map.put(key[0] + "," + key[1] + "," + key[2] + "," + key[3], l + "," + t + "," + r + "," + b);
		}
		return map;
	}

	private static final Pattern VIEWBOX = Pattern.compile("viewBox=\"\\s*(-?[0-9.]+)[\\s,]+(-?[0-9.]+)[\\s,]+([0-9.]+)[\\s,]+([0-9.]+)");

	/** The height of an SVG's viewBox; -1 when it has none. */
	public static double height(String svg) {
		Matcher m = svg == null ? null : VIEWBOX.matcher(svg);
		return m != null && m.find() ? Double.parseDouble(m.group(4)) : -1;
	}

	/**
	 * #141: boxes from {@link #boxMap} moved into a raster image EA drew of the same
	 * diagram: {@code scale} times larger, and {@code dy} pixels lower.
	 */
	public static Map<String, String> scale(Map<String, String> boxes, double scale, int dy) {
		Map<String, String> out = new HashMap<>();
		for (Map.Entry<String, String> e : boxes.entrySet()) {
			String[] v = e.getValue().split(",");
			out.put(e.getKey(), Math.round(Integer.parseInt(v[0]) * scale) + "," + (Math.round(Integer.parseInt(v[1]) * scale) + dy)
					+ "," + Math.round(Integer.parseInt(v[2]) * scale) + "," + (Math.round(Integer.parseInt(v[3]) * scale) + dy));
		}
		return out;
	}

	/**
	 * Re-expresses the highlight boxes of a diagram's element and connector items
	 * ({@code Box}, {@code SourceBox}, {@code TargetBox}) in the SVGs' coordinates. A
	 * box with no mapping on its side (e.g. the SVG of that side could not be matched)
	 * is removed, so no highlight is drawn in the wrong place.
	 */
	public static void convert(List<CompareItem> items, Map<String, String> baseline, Map<String, String> destination) {
		for (CompareItem item : items) {
			if (item.getProperties() == null)
				continue;
			for (Property p : item.getProperties().getProperty()) {
				String n = p.getName();
				if (!"Box".equals(n) && !"SourceBox".equals(n) && !"TargetBox".equals(n))
					continue;
				if (p.getBaseline() != null)
					p.setBaseline(baseline.get(p.getBaseline()));
				if (p.getModel() != null)
					p.setModel(destination.get(p.getModel()));
			}
		}
	}
}
