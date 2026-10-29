package org.cimug.compare;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cimug.compare.logs.CompareItem;
import org.cimug.compare.logs.Property;

/**
 * Changes a diagram shows (#145): a diagram is changed when its picture would differ,
 * which happens when its layout changed or when an element drawn on it changed in a
 * way the diagram shows. These are the rules for the second case, used by the
 * comparison (to find such diagrams) and by the report (to outline such classes).
 * <ul>
 * <li>A class present in both models, whose box shows: its name, stereotype,
 * abstractness or package prefix; an attribute added, removed, or changed in something
 * drawn; its generalization (the parent's name drawn in the box).</li>
 * <li>An association present in both models, whose connector shows: its name,
 * stereotype or direction; an end's role name, multiplicity, navigability,
 * aggregation, qualifier, constraint or visibility.</li>
 * </ul>
 */
public final class DrawnChanges {

	private DrawnChanges() {
	}

	/** Class properties drawn in a class's box on a diagram. */
	public static final Set<String> CLASS_PROPS = Collections
			.unmodifiableSet(new HashSet<String>(Arrays.asList("Name", "Stereotype", "Abstract", "ParentPackage")));
	/** Attribute properties drawn in a class's box on a diagram. */
	public static final Set<String> ATTRIBUTE_PROPS = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
			"Name", "Stereotype", "Type", "LowerBound", "UpperBound", "Default", "Scope", "Static", "Const", "IsLiteral")));

	/** Association properties drawn on its connector, with how the report names them. */
	private static final Map<String, String> LINK_PROPS = labels("Name", "name", "Stereotype", "stereotype", "Direction",
			"direction");
	/** Association end properties drawn on its connector, with how the report names them. */
	private static final Map<String, String> END_PROPS = labels("Role", "role name", "Cardinality", "multiplicity",
			"Stereotype", "stereotype", "IsNavigable", "navigability", "IsAggregation", "aggregation", "Qualifier",
			"qualifier", "Constraint", "constraint", "Scope", "visibility");

	private static Map<String, String> labels(String... kv) {
		Map<String, String> m = new LinkedHashMap<String, String>();
		for (int i = 0; i + 1 < kv.length; i += 2)
			m.put(kv[i], kv[i + 1]);
		return Collections.unmodifiableMap(m);
	}

	private static final String ADDED = "Model only", DELETED = "Baseline only", IDENTICAL = "Identical";

	/** The EAIDs of the classes with a change their box on a diagram shows. */
	public static Set<String> classes(CompareItem root) {
		Set<String> out = new LinkedHashSet<String>();
		walk(root, out, null);
		return out;
	}

	/**
	 * The associations with a change their connector shows, by EAID, each with what
	 * changed for the report: "multiplicity 0..* → 1..*" for one change with both
	 * values, else the changed labels ("role name, multiplicity").
	 */
	public static Map<String, String> connectors(CompareItem root) {
		Map<String, String> out = new LinkedHashMap<String, String>();
		walk(root, null, out);
		return out;
	}

	private static void walk(CompareItem item, Set<String> classes, Map<String, String> connectors) {
		if (item == null)
			return;
		if (classes != null && "Class".equals(item.getType()) && classShows(item))
			classes.add(eaid(item.getGuid()));
		if (connectors != null && "".equals(item.getType()) && !"Generalization".equals(item.getName())) {
			String what = connectorShows(item);
			if (what != null)
				connectors.putIfAbsent(eaid(item.getGuid()), what);
		}
		for (CompareItem c : item.getCompareItem())
			walk(c, classes, connectors);
	}

	/** The rule of ReportPreProcessor.changeShowsOnDiagram, on the comparison's own items. */
	static boolean classShows(CompareItem cls) {
		String st = cls.getStatus();
		if (ADDED.equals(st) || DELETED.equals(st))
			return false; // the element itself is added to or removed from the diagram
		if (changedAny(cls, CLASS_PROPS))
			return true;
		for (CompareItem c : cls.getCompareItem()) {
			if ("Attribute".equals(c.getType())) {
				String as = c.getStatus();
				if (ADDED.equals(as) || DELETED.equals(as) || changedAny(c, ATTRIBUTE_PROPS))
					return true;
			} else if ("Links".equals(c.getType())) {
				for (CompareItem l : c.getCompareItem())
					if ("Generalization".equals(l.getName()) && hasChanges(l))
						return true;
			}
		}
		return false;
	}

	/** What an association's connector shows changed, or null when nothing it draws changed. */
	static String connectorShows(CompareItem link) {
		String st = link.getStatus();
		if (st == null || ADDED.equals(st) || DELETED.equals(st))
			return null; // the connector itself is added to or removed from the diagram
		Set<String> what = new LinkedHashSet<String>();
		Property only = null;
		int n = 0;
		for (Property p : changed(link, LINK_PROPS.keySet())) {
			what.add(LINK_PROPS.get(p.getName()));
			only = p;
			n++;
		}
		for (CompareItem end : link.getCompareItem())
			if ("Src".equals(end.getType()) || "Dst".equals(end.getType()))
				for (Property p : changed(end, END_PROPS.keySet())) {
					what.add(END_PROPS.get(p.getName()));
					only = p;
					n++;
				}
		if (what.isEmpty())
			return null;
		if (n == 1 && only.getBaseline() != null && only.getModel() != null && !only.getBaseline().isEmpty()
				&& !only.getModel().isEmpty())
			return what.iterator().next() + " " + only.getBaseline() + " → " + only.getModel();
		return String.join(", ", what);
	}

	private static boolean changedAny(CompareItem item, Set<String> names) {
		return !changed(item, names).isEmpty();
	}

	private static List<Property> changed(CompareItem item, Set<String> names) {
		List<Property> out = new java.util.ArrayList<Property>();
		if (item.getProperties() != null)
			for (Property p : item.getProperties().getProperty())
				if (names.contains(p.getName()) && p.getStatus() != null && !IDENTICAL.equals(p.getStatus()))
					out.add(p);
		return out;
	}

	private static boolean hasChanges(CompareItem item) {
		if (item.getStatus() != null && !IDENTICAL.equals(item.getStatus()))
			return true;
		if (item.getProperties() != null)
			for (Property p : item.getProperties().getProperty())
				if (p.getStatus() != null && !IDENTICAL.equals(p.getStatus()))
					return true;
		for (CompareItem c : item.getCompareItem())
			if (hasChanges(c))
				return true;
		return false;
	}

	/** {6CA575E7-020B-40d8-B841-027BF8B51BE9} → EAID_6CA575E7_020B_40d8_B841_027BF8B51BE9. */
	public static String eaid(String guid) {
		String g = guid == null ? "" : guid.trim();
		if (g.startsWith("{") && g.endsWith("}"))
			g = g.substring(1, g.length() - 1);
		return g.startsWith("EAID_") ? g : "EAID_" + g.replace('-', '_');
	}
}
