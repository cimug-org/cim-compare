package org.cimug.compare.report;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Stage 3a of cim-compare: turns the EA CompareLog-shaped comparison XML into
 * an <em>enriched</em> XML document that the report stylesheet consumes.
 *
 * <p>
 * The input format was designed for compatibility with Enterprise Architect's
 * own compare log, which leaves several things implicit. This pass makes them
 * explicit once, so the presentation layer never has to reason about them:
 * </p>
 * <ul>
 * <li>Elements only present in the baseline have {@code type=""} and are
 * recognised by the properties they carry; they are classified here into
 * package / class / attribute / diagram / link.</li>
 * <li>A class (or link) can be marked {@code Identical} while its attributes
 * (or role ends) changed; an <em>effective</em> status is derived.</li>
 * <li>Word-level redlines for changed text properties are computed here (see
 * {@link WordDiff}) so that nothing runs in the browser at load time.</li>
 * <li>Type and end-class names are resolved to element ids so the report can
 * link to them.</li>
 * <li>Unless {@code full} is requested, identical elements are dropped.</li>
 * <li>Summary counts and header metadata (versions, timestamp, scope) are
 * attached to the root.</li>
 * </ul>
 *
 * <p>
 * Output shape (all names are elements unless prefixed with @):
 * </p>
 *
 * <pre>
 * ComparisonReport @baselineVersion @destinationVersion @comparedOn @scope @full @generator
 *   Summary
 *     Count @kind (package|class|diagram) @status @n
 *   Package @name @guid @id @status @classCount @diagramCount
 *           @changedClasses @changedDiagrams (whole subtree) [@renamedFrom] [@fromPackage]
 *     Notes @status  Baseline | Destination | Redline (mixed: text, del, ins)
 *     Properties  Property @name @status [@baseline] [@model] [Redline]
 *     Package ...   (nested)
 *     Class @name @guid @id @status [@stereotype] [@renamedFrom] [@fromPackage]
 *       Notes, Properties (as above)
 *       Attribute @name @guid @id @status [@renamedFrom] [@baselineTypeId] [@modelTypeId]
 *         Properties
 *       Link @kind (Generalization|Association|Aggregation) @guid @id @status
 *         Properties
 *         End @side (source|target) @guid @status [@baselineClassId] [@modelClassId]
 *           Properties
 *     Diagram @name @guid @id @eaid @status [@renamedFrom]
 *       Notes, Properties
 * </pre>
 *
 * <p>
 * Status values are normalised to: {@code identical}, {@code changed},
 * {@code moved}, {@code added} (only in destination), {@code deleted} (only in
 * baseline). For classes and links the status attribute already carries the
 * effective status.
 * </p>
 */
public class ReportPreProcessor {

	public static final String GENERATOR = "cim-compare 2.0.0";

	private static final Set<String> TEXT_PROPS = new HashSet<String>(
			Arrays.asList("Notes", "RoleNote", "Alias", "Name"));
	private static final Set<String> LINK_NAMES = new HashSet<String>(
			Arrays.asList("Generalization", "Association", "Aggregation"));

	private final boolean full;
	private final String packageFilter;

	private Document out;
	/** class name → element id, for type / end-class links */
	private final Map<String, String> classIdIndex = new HashMap<String, String>();
	private final Map<String, Integer> counts = new LinkedHashMap<String, Integer>();
	private final Set<String> usedIds = new HashSet<String>();

	/**
	 * @param full
	 *            include identical elements (the {@code --full} option)
	 * @param packageFilter
	 *            restrict to this package and below ({@code --package}), or null
	 */
	public ReportPreProcessor(boolean full, String packageFilter) {
		this.full = full;
		this.packageFilter = (packageFilter == null || packageFilter.trim().isEmpty()) ? null : packageFilter.trim();
	}

	public Document process(Document in) throws ParserConfigurationException {
		out = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		Element root = out.createElement("ComparisonReport");
		out.appendChild(root);

		Element compareResults = firstChild(firstChild(in.getDocumentElement(), "ComparePackage"), "CompareResults");
		Element comparePackage = firstChild(in.getDocumentElement(), "ComparePackage");
		if (compareResults == null)
			throw new IllegalArgumentException("Not a cim-compare / EA comparison XML (no CompareResults element)");

		// Index every class by name first (type links may point anywhere).
		indexClasses(in.getDocumentElement());

		// The top-level CompareItem is EA's synthetic "EA Model"; its Package
		// children are the model roots. Deleted roots appear with type="".
		Element eaModel = firstChild(compareResults, "CompareItem");
		List<Element> roots = new ArrayList<Element>();
		for (Element c : children(eaModel, "CompareItem")) {
			if ("package".equals(kindOf(c)))
				roots.add(c);
		}
		if (roots.isEmpty())
			roots.add(eaModel);

		String scope;
		if (packageFilter != null) {
			List<Element> filtered = new ArrayList<Element>();
			for (Element r : roots)
				collectPackagesNamed(r, packageFilter, filtered);
			if (filtered.isEmpty())
				throw new IllegalArgumentException("Package '" + packageFilter
						+ "' does not occur in the comparison (checked destination and baseline names).");
			roots = filtered;
			Element first = filtered.get(0);
			String was = prop(first, "Name", "baseline"), now = attr(first, "name");
			scope = (!was.isEmpty() && !was.equals(now)) ? was + " → " + now : now;
		} else {
			StringBuilder sb = new StringBuilder();
			for (Element r : roots) {
				if (sb.length() > 0)
					sb.append(", ");
				sb.append(attr(r, "name"));
			}
			scope = sb.toString() + " (full model)";
		}

		String[] versions = versionLabels(in.getDocumentElement(), roots);
		root.setAttribute("baselineVersion", versions[0]);
		root.setAttribute("destinationVersion", versions[1]);
		root.setAttribute("comparedOn", comparePackage == null ? "" : attr(comparePackage, "comparedOn"));
		root.setAttribute("scope", scope);
		root.setAttribute("full", Boolean.toString(full));
		root.setAttribute("generator", GENERATOR);

		Element summary = out.createElement("Summary");
		root.appendChild(summary);

		for (Element r : roots) {
			Element p = renderPackage(r);
			if (p != null)
				root.appendChild(p);
		}

		for (Map.Entry<String, Integer> e : counts.entrySet()) {
			String[] ks = e.getKey().split("/");
			Element c = out.createElement("Count");
			c.setAttribute("kind", ks[0]);
			c.setAttribute("status", ks[1]);
			c.setAttribute("n", e.getValue().toString());
			summary.appendChild(c);
		}
		return out;
	}

	// ------------------------------------------------------------ packages

	private Element renderPackage(Element pkg) {
		String status = statusOf(pkg);
		if (!full && !hasChanges(pkg))
			return null;
		Element e = element("Package", pkg, status);
		copyNotesAndProperties(pkg, e);
		count("package", status);
		int classes = 0, diagrams = 0;
		for (Element c : children(pkg, "CompareItem")) {
			String k = kindOf(c);
			if ("class".equals(k))
				classes++;
			else if ("diagram".equals(k))
				diagrams++;
		}
		e.setAttribute("classCount", Integer.toString(classes));
		e.setAttribute("diagramCount", Integer.toString(diagrams));

		for (Element c : children(pkg, "CompareItem")) {
			String kind = kindOf(c);
			if ("package".equals(kind)) {
				Element sub = renderPackage(c);
				if (sub != null)
					e.appendChild(sub);
			}
		}
		for (Element c : children(pkg, "CompareItem")) {
			if ("class".equals(kindOf(c))) {
				Element cls = renderClass(c);
				if (cls != null)
					e.appendChild(cls);
			}
		}
		for (Element c : children(pkg, "CompareItem")) {
			if ("diagram".equals(kindOf(c))) {
				Element d = renderDiagram(c);
				if (d != null)
					e.appendChild(d);
			}
		}
		// Changed classes / diagrams in this package's whole subtree, for the
		// package outline (left rail) in the report.
		e.setAttribute("changedClasses", Integer.toString(countChanged(e, "Class")));
		e.setAttribute("changedDiagrams", Integer.toString(countChanged(e, "Diagram")));
		return e;
	}

	private static int countChanged(Element pkg, String tag) {
		NodeList all = pkg.getElementsByTagName(tag);
		int n = 0;
		for (int i = 0; i < all.getLength(); i++)
			if (!"identical".equals(((Element) all.item(i)).getAttribute("status")))
				n++;
		return n;
	}

	// ------------------------------------------------------------ classes

	private Element renderClass(Element cls) {
		String status = statusOf(cls);
		if ("identical".equals(status) && hasChanges(cls))
			status = "changed";
		if (!full && "identical".equals(status))
			return null;
		Element e = element("Class", cls, status);
		String stereo = "deleted".equals(status) ? prop(cls, "Stereotype", "baseline") : prop(cls, "Stereotype", "model");
		if (stereo.isEmpty() && attr(cls, "name").startsWith("«deprecated»"))
			stereo = "deprecated";
		if (!stereo.isEmpty())
			e.setAttribute("stereotype", stereo);
		copyNotesAndProperties(cls, e);
		count("class", status);

		for (Element c : children(cls, "CompareItem")) {
			String kind = kindOf(c);
			if ("attribute".equals(kind)) {
				Element a = renderAttribute(c);
				if (a != null)
					e.appendChild(a);
			} else if ("links".equals(kind)) {
				for (Element l : children(c, "CompareItem")) {
					if ("link".equals(kindOf(l))) {
						Element le = renderLink(l);
						if (le != null)
							e.appendChild(le);
					}
				}
			}
		}
		return e;
	}

	private Element renderAttribute(Element a) {
		String status = statusOf(a);
		if ("identical".equals(status) && hasChangedProperties(a))
			status = "changed";
		if (!full && "identical".equals(status))
			return null;
		Element e = element("Attribute", a, status);
		String bt = prop(a, "Type", "baseline"), mt = prop(a, "Type", "model");
		String bid = classId(bt), mid = classId(mt);
		if (bid != null)
			e.setAttribute("baselineTypeId", bid);
		if (mid != null)
			e.setAttribute("modelTypeId", mid);
		copyNotesAndProperties(a, e);
		return e;
	}

	private Element renderLink(Element l) {
		String status = statusOf(l);
		boolean endsChanged = false;
		for (Element end : children(l, "CompareItem"))
			if (hasChangedProperties(end))
				endsChanged = true;
		if ("identical".equals(status) && (hasChangedProperties(l) || endsChanged))
			status = "changed";
		if (!full && "identical".equals(status))
			return null;
		Element e = element("Link", l, status);
		e.setAttribute("kind", attr(l, "name"));
		copyNotesAndProperties(l, e);
		for (Element end : children(l, "CompareItem")) {
			String t = attr(end, "type");
			if (!"Src".equals(t) && !"Dst".equals(t))
				continue;
			Element ee = out.createElement("End");
			ee.setAttribute("side", "Src".equals(t) ? "source" : "target");
			ee.setAttribute("guid", attr(end, "guid"));
			ee.setAttribute("status", statusOf(end));
			String bc = classId(prop(end, "End", "baseline")), mc = classId(prop(end, "End", "model"));
			if (bc != null)
				ee.setAttribute("baselineClassId", bc);
			if (mc != null)
				ee.setAttribute("modelClassId", mc);
			copyNotesAndProperties(end, ee);
			e.appendChild(ee);
		}
		return e;
	}

	// ------------------------------------------------------------ diagrams

	private Element renderDiagram(Element d) {
		String status = statusOf(d);
		// Diagrams are reported as Changed whenever anything (including element
		// positions) differs; only property changes are material, as today.
		if ("changed".equals(status) && !hasChangedProperties(d))
			status = "identical";
		if (!full && "identical".equals(status))
			return null;
		Element e = element("Diagram", d, status);
		e.setAttribute("eaid", eaid(attr(d, "guid")));
		copyNotesAndProperties(d, e);
		count("diagram", status);
		return e;
	}

	// ------------------------------------------------------------ shared

	/** Creates the output element with the common attributes. */
	private Element element(String tag, Element src, String status) {
		Element e = out.createElement(tag);
		String name = attr(src, "name");
		String guid = attr(src, "guid");
		e.setAttribute("name", name);
		e.setAttribute("guid", guid);
		e.setAttribute("id", uniqueId(guid, name));
		e.setAttribute("status", status);
		if ("changed".equals(propStatus(src, "Name"))) {
			String from = prop(src, "Name", "baseline"), to = prop(src, "Name", "model");
			if (!from.isEmpty() && !to.isEmpty() && !from.equals(to))
				e.setAttribute("renamedFrom", from);
		}
		if ("moved".equals(status) && "changed".equals(propStatus(src, "ParentPackage")))
			e.setAttribute("fromPackage", prop(src, "ParentPackage", "baseline"));
		return e;
	}

	private void copyNotesAndProperties(Element src, Element dst) {
		Element props = firstChild(src, "Properties");
		if (props == null)
			return;
		Element notesOut = null;
		Element propsOut = out.createElement("Properties");
		for (Element p : children(props, "Property")) {
			String name = attr(p, "name");
			String st = normalize(attr(p, "status"));
			String b = p.hasAttribute("baseline") ? p.getAttribute("baseline") : null;
			String m = p.hasAttribute("model") ? p.getAttribute("model") : null;
			Element po = out.createElement("Property");
			po.setAttribute("name", name);
			po.setAttribute("status", st);
			if (b != null)
				po.setAttribute("baseline", b);
			if (m != null)
				po.setAttribute("model", m);
			if ("changed".equals(st) && TEXT_PROPS.contains(name)) {
				Element r = out.createElement("Redline");
				WordDiff.appendRedline(out, r, b, m);
				po.appendChild(r);
			}
			propsOut.appendChild(po);
			if ("Notes".equals(name) && ((b != null && !b.isEmpty()) || (m != null && !m.isEmpty()))) {
				notesOut = out.createElement("Notes");
				notesOut.setAttribute("status", st);
				Element bl = out.createElement("Baseline");
				bl.setTextContent(b == null ? "" : b);
				Element ds = out.createElement("Destination");
				ds.setTextContent(m == null ? "" : m);
				notesOut.appendChild(bl);
				notesOut.appendChild(ds);
				if ("changed".equals(st)) {
					Element r = out.createElement("Redline");
					WordDiff.appendRedline(out, r, b, m);
					notesOut.appendChild(r);
				}
			}
		}
		if (notesOut != null)
			dst.appendChild(notesOut);
		dst.appendChild(propsOut);
	}

	private void count(String kind, String status) {
		String k = kind + "/" + status;
		Integer n = counts.get(k);
		counts.put(k, n == null ? 1 : n + 1);
	}

	// ------------------------------------------------------------ classification

	/**
	 * Classifies a CompareItem. Items present in both models carry an explicit
	 * type; baseline-only items have {@code type=""} and are recognised by their
	 * properties and position.
	 */
	static String kindOf(Element item) {
		String t = attr(item, "type");
		if ("Package".equals(t))
			return "package";
		if ("Class".equals(t))
			return "class";
		if ("Attribute".equals(t))
			return "attribute";
		if ("Diagram".equals(t))
			return "diagram";
		if ("Links".equals(t))
			return "links";
		if ("Src".equals(t) || "Dst".equals(t))
			return "end";
		Node parent = item.getParentNode();
		if (parent instanceof Element && "Links".equals(attr((Element) parent, "type")))
			return "link";
		if (LINK_NAMES.contains(attr(item, "name")) && firstChild(item, "CompareItem") != null
				&& ("Src".equals(attr(firstChild(item, "CompareItem"), "type"))))
			return "link";
		if (hasProp(item, "DiagramType"))
			return "diagram";
		String type = prop(item, "Type", "baseline");
		if ("Class".equals(type))
			return "class";
		if ("Package".equals(type))
			return "package";
		if (hasProp(item, "LowerBound") || hasProp(item, "UpperBound"))
			return "attribute";
		if (hasProp(item, "Author") && hasProp(item, "ParentPackage"))
			return "package";
		return "attribute";
	}

	static String normalize(String eaStatus) {
		if (eaStatus == null)
			return "identical";
		if ("Baseline only".equals(eaStatus))
			return "deleted";
		if ("Model only".equals(eaStatus))
			return "added";
		if ("Moved".equals(eaStatus))
			return "moved";
		if ("Changed".equals(eaStatus))
			return "changed";
		return "identical";
	}

	private static String statusOf(Element item) {
		return normalize(attr(item, "status"));
	}

	/** true when the item or anything beneath it is not identical */
	private static boolean hasChanges(Element item) {
		if (!"identical".equals(statusOf(item)))
			return true;
		if (hasChangedProperties(item))
			return true;
		for (Element c : children(item, "CompareItem"))
			if (hasChanges(c))
				return true;
		return false;
	}

	private static boolean hasChangedProperties(Element item) {
		Element props = firstChild(item, "Properties");
		if (props == null)
			return false;
		for (Element p : children(props, "Property"))
			if (!"identical".equals(normalize(attr(p, "status"))))
				return true;
		return false;
	}

	// ------------------------------------------------------------ indexes / ids

	private void indexClasses(Element root) {
		NodeList all = root.getElementsByTagName("CompareItem");
		for (int i = 0; i < all.getLength(); i++) {
			Element c = (Element) all.item(i);
			if ("class".equals(kindOf(c))) {
				String name = stripDeprecated(attr(c, "name"));
				String guid = attr(c, "guid");
				if (!name.isEmpty() && !guid.isEmpty() && !classIdIndex.containsKey(name))
					classIdIndex.put(name, eaid(guid));
			}
		}
	}

	private String classId(String typeName) {
		if (typeName == null || typeName.isEmpty())
			return null;
		return classIdIndex.get(stripDeprecated(typeName));
	}

	private static String stripDeprecated(String name) {
		String n = name == null ? "" : name.trim();
		if (n.startsWith("«deprecated»"))
			n = n.substring("«deprecated»".length()).trim();
		return n;
	}

	/** {6CA575E7-020B-40d8-B841-027BF8B51BE9} → EAID_6CA575E7_020B_40d8_B841_027BF8B51BE9 (matches EA image file names). */
	static String eaid(String guid) {
		String g = guid == null ? "" : guid.trim();
		if (g.startsWith("{"))
			g = g.substring(1);
		if (g.endsWith("}"))
			g = g.substring(0, g.length() - 1);
		return "EAID_" + g.replace('-', '_');
	}

	private String uniqueId(String guid, String name) {
		String base = guid.isEmpty() ? "N_" + name.replaceAll("[^A-Za-z0-9_]", "_") : eaid(guid);
		base = base.replaceAll("[^A-Za-z0-9_.-]", "_");
		String id = base;
		int n = 1;
		while (usedIds.contains(id))
			id = base + "_" + (++n);
		usedIds.add(id);
		return id;
	}

	/**
	 * Collects the packages matching --package by name. A package renamed between
	 * versions matches either name (e.g. IEC61970 or Grid).
	 */
	private void collectPackagesNamed(Element pkg, String name, List<Element> into) {
		if ("package".equals(kindOf(pkg))
				&& (name.equals(attr(pkg, "name")) || name.equals(prop(pkg, "Name", "baseline")))) {
			into.add(pkg);
			return;
		}
		for (Element c : children(pkg, "CompareItem"))
			if ("package".equals(kindOf(c)))
				collectPackagesNamed(c, name, into);
	}

	// ------------------------------------------------------------ versions

	/**
	 * Baseline / destination labels, derived the same way the 1.x stylesheet did:
	 * from the {@code version} attribute default of the *CIMVersion classes, with
	 * the 61970 label alone when it differs and a concatenation otherwise. Falls
	 * back to the root package name.
	 */
	private String[] versionLabels(Element root, List<Element> roots) {
		String[][] groups = { { "IEC61970CIMVersion", "GridCIMVersion" },
				{ "IEC61968CIMVersion", "SupportCIMVersion", "EnterpriseCIMVersion" },
				{ "IEC62325CIMVersion", "MarketCIMVersion" } };
		String[] b = new String[3], m = new String[3];
		NodeList all = root.getElementsByTagName("CompareItem");
		Map<String, Element> byName = new HashMap<String, Element>();
		for (int i = 0; i < all.getLength(); i++) {
			Element c = (Element) all.item(i);
			if ("Class".equals(attr(c, "type")))
				byName.put(attr(c, "name"), c);
		}
		for (int g = 0; g < 3; g++) {
			for (String n : groups[g]) {
				Element c = byName.get(n);
				if (c == null)
					continue;
				for (Element a : children(c, "CompareItem")) {
					if ("version".equals(attr(a, "name"))) {
						String vb = prop(a, "Default", "baseline"), vm = prop(a, "Default", "model");
						if (b[g] == null && !vb.isEmpty())
							b[g] = vb;
						if (m[g] == null && !vm.isEmpty())
							m[g] = vm;
					}
				}
				if (b[g] != null || m[g] != null)
					break;
			}
		}
		String bl, dl;
		if (b[0] != null && m[0] != null && b[1] != null && m[1] != null && b[2] != null && m[2] != null) {
			if (b[0].equals(m[0])) {
				bl = b[0] + "_" + b[1] + "_" + b[2];
				dl = m[0] + "_" + m[1] + "_" + m[2];
			} else {
				bl = b[0];
				dl = m[0];
			}
		} else {
			bl = firstNonNull(b);
			dl = firstNonNull(m);
		}
		String fallback = roots.isEmpty() ? "Unspecified" : attr(roots.get(0), "name");
		if (bl == null)
			bl = fallback;
		if (dl == null)
			dl = fallback;
		return new String[] { bl, dl };
	}

	private static String firstNonNull(String[] a) {
		for (String s : a)
			if (s != null)
				return s;
		return null;
	}

	// ------------------------------------------------------------ DOM helpers

	static String attr(Element e, String name) {
		return e == null ? "" : e.getAttribute(name);
	}

	static Element firstChild(Element e, String tag) {
		if (e == null)
			return null;
		for (Node n = e.getFirstChild(); n != null; n = n.getNextSibling())
			if (n instanceof Element && tag.equals(n.getNodeName()))
				return (Element) n;
		return null;
	}

	static List<Element> children(Element e, String tag) {
		List<Element> out = new ArrayList<Element>();
		if (e == null)
			return out;
		for (Node n = e.getFirstChild(); n != null; n = n.getNextSibling())
			if (n instanceof Element && tag.equals(n.getNodeName()))
				out.add((Element) n);
		return out;
	}

	private static Element property(Element item, String name) {
		Element props = firstChild(item, "Properties");
		if (props == null)
			return null;
		for (Element p : children(props, "Property"))
			if (name.equals(attr(p, "name")))
				return p;
		return null;
	}

	static boolean hasProp(Element item, String name) {
		return property(item, name) != null;
	}

	/** value of a property on one side ("baseline" or "model"), never null */
	static String prop(Element item, String name, String side) {
		Element p = property(item, name);
		return p == null ? "" : attr(p, side);
	}

	static String propStatus(Element item, String name) {
		Element p = property(item, name);
		return p == null ? "identical" : normalize(attr(p, "status"));
	}
}
