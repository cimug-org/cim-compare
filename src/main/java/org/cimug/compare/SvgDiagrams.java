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
 * SVG diagram images (issue #139).
 * <p>
 * EA's XMI export writes diagram images only as EMF, BMP, GIF, PNG or JPG; an SVG of
 * each diagram is written afterwards with {@code Project.PutDiagramImageToFile}. The
 * highlight boxes in the XMI ({@code imgL;imgT;imgR;imgB}) are in the pixels of the
 * image the XMI export wrote (the JPG), which EA draws at a larger scale than the SVG.
 * The SVG draws each element at its diagram position ({@code Left;Top;Right;Bottom}),
 * less one shift for the whole diagram. The shift is found by matching the SVG's
 * boxes to the elements of the same width and height; each element's box is then
 * re-expressed in the SVG's coordinates.
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
		Map<String, int[]> bySize = new HashMap<>();
		Map<String, Integer> votes = new HashMap<>();
		Matcher m = RECT.matcher(svg);
		java.util.List<double[]> rects = new java.util.ArrayList<>();
		while (m.find())
			rects.add(new double[] { Double.parseDouble(m.group(1)), Double.parseDouble(m.group(2)),
					Double.parseDouble(m.group(3)), Double.parseDouble(m.group(4)) });
		int[] best = null;
		int bestVotes = 0;
		for (DiagramElement e : elements) {
			int[] g = position(e);
			if (g == null)
				continue;
			for (double[] r : rects) {
				if (Math.round(r[2]) != g[2] - g[0] || Math.round(r[3]) != g[3] - g[1])
					continue;
				int dx = g[0] - (int) Math.round(r[0]), dy = g[1] - (int) Math.round(r[1]);
				String key = dx + "," + dy;
				bySize.putIfAbsent(key, new int[] { dx, dy });
				int n = votes.merge(key, 1, Integer::sum);
				if (n > bestVotes) {
					bestVotes = n;
					best = bySize.get(key);
				}
			}
		}
		return best;
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
	 * For each element with a highlight box, its box in the image the XMI export wrote
	 * ("l,t,r,b") mapped to its box in the SVG, given the diagram's {@link #shift}.
	 */
	public static Map<String, String> boxMap(List<DiagramElement> elements, int[] shift) {
		Map<String, String> map = new HashMap<>();
		if (elements == null || shift == null)
			return map;
		for (DiagramElement e : elements) {
			int[] img = DiagramElementDiff.box(e), g = position(e);
			if (img == null || g == null)
				continue;
			map.put(img[0] + "," + img[1] + "," + img[2] + "," + img[3],
					(g[0] - shift[0]) + "," + (g[1] - shift[1]) + "," + (g[2] - shift[0]) + "," + (g[3] - shift[1]));
		}
		return map;
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
