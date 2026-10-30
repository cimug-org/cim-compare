package org.cimug.compare.report;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.cimug.compare.PackageNames;
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
 * ComparisonReport @baselineVersion @destinationVersion @comparedOn @scope @full @generator @releaseUrl
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
 *       Highlight @kind (added|removed|moved|changed|restyled) @side (baseline|destination)
 *                 @box (left,top,right,bottom in image pixels) @name
 *       Connector @kind (added|removed|rerouted|labels moved|restyled) @name ("A – B")
 *                 @side (the image it is on) @ends (the two end boxes, "l,t,r,b;l,t,r,b")
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

	public static final String GENERATOR = "cim-compare " + org.cimug.compare.Version.VERSION;

	/**
	 * Free-text properties, the only ones redlined in the report's metadata tables;
	 * a short value such as a name or alias sits beside its baseline value as it is (#179).
	 */
	private static final Set<String> TEXT_PROPS = new HashSet<String>(
			Arrays.asList("Notes", "RoleNote"));
	private static final Set<String> LINK_NAMES = new HashSet<String>(
			Arrays.asList("Generalization", "Association", "Aggregation"));

	private final boolean full;
	/** The --package value as given (one name or a comma list), or null. */
	private final String packageFilter;
	/** Package names already split by the caller, or null to split packageFilter. */
	private final List<String> packageNames;

	private Document out;
	/** class name → element id, for type / end-class links */
	private final Map<String, String> classIdIndex = new HashMap<String, String>();
	/**
	 * EAIDs of the classes with a change that shows in their box on a diagram
	 * (for the "changed" diagram highlight, #37); see {@link #changeShowsOnDiagram}.
	 */
	private final Set<String> visiblyChangedClasses = new HashSet<String>();

	/** Class properties drawn in a class's box on a diagram (shared with the comparison, #145). */
	private static final Set<String> DRAWN_CLASS_PROPS = org.cimug.compare.DrawnChanges.CLASS_PROPS;
	/** Attribute properties drawn in a class's box on a diagram. */
	private static final Set<String> DRAWN_ATTRIBUTE_PROPS = org.cimug.compare.DrawnChanges.ATTRIBUTE_PROPS;
	private final Map<String, Integer> counts = new LinkedHashMap<String, Integer>();
	private final Set<String> usedIds = new HashSet<String>();

	/**
	 * @param full
	 *            include identical elements (the {@code --full} option)
	 * @param packageFilter
	 *            restrict to this package and below ({@code --package}), or to
	 *            several packages given as a comma list (#94); null for the whole
	 *            model
	 */
	public ReportPreProcessor(boolean full, String packageFilter) {
		this.full = full;
		this.packageFilter = (packageFilter == null || packageFilter.trim().isEmpty()) ? null : packageFilter.trim();
		this.packageNames = null;
	}

	/**
	 * @param packageNames
	 *            restrict to these packages, in this order; null or empty for the
	 *            whole model
	 */
	public static ReportPreProcessor forPackages(boolean full, List<String> packageNames) {
		return new ReportPreProcessor(full, packageNames, true);
	}

	private ReportPreProcessor(boolean full, List<String> packageNames, boolean resolved) {
		this.full = full;
		this.packageFilter = null;
		this.packageNames = (packageNames == null || packageNames.isEmpty()) ? null
				: new ArrayList<String>(packageNames);
	}

	private boolean showEmptyValues;

	/**
	 * Report differences between a value and an empty value in EA's housekeeping
	 * fields too (the {@code --show-empty-values} option, #126); by default they
	 * are left out and counted.
	 */
	public ReportPreProcessor showEmptyValues(boolean show) {
		this.showEmptyValues = show;
		return this;
	}

	public Document process(Document in) throws ParserConfigurationException {
		int emptyValues = showEmptyValues ? 0 : EmptyValues.apply(in.getDocumentElement());
		out = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		Element root = out.createElement("ComparisonReport");
		out.appendChild(root);
		if (emptyValues > 0)
			root.setAttribute("emptyValuesNotShown", String.valueOf(emptyValues));

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
		if (!synthetic(eaModel) && "package".equals(kindOf(eaModel))) {
			// an EA compare log: its top item is the compared package itself, whose own
			// classes and diagrams belong in the report as much as its sub-packages'
			roots.add(eaModel);
		} else {
			for (Element c : children(eaModel, "CompareItem")) {
				if ("package".equals(kindOf(c)))
					roots.add(c);
			}
			if (roots.isEmpty())
				roots.add(eaModel);
		}

		String scope;
		List<String> names = selectedNames(roots);
		if (names != null) {
			List<Element> filtered = new ArrayList<Element>();
			StringBuilder labels = new StringBuilder();
			for (String name : names) {
				List<Element> found = new ArrayList<Element>();
				for (Element r : roots)
					collectPackagesNamed(r, name, found);
				if (found.isEmpty())
					throw new IllegalArgumentException("Package '" + name
							+ "' does not occur in the comparison (checked destination and baseline names).");
				for (Element f : found)
					if (!filtered.contains(f))
						filtered.add(f);
				Element first = found.get(0);
				String was = prop(first, "Name", "baseline"), now = attr(first, "name");
				labels.append(labels.length() > 0 ? ", " : "")
						.append((!was.isEmpty() && !was.equals(now)) ? was + " → " + now : now);
			}
			roots = withoutNested(filtered);
			scope = labels.toString();
			if (roots.size() < filtered.size())
				scope = scopeOf(roots);
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
		root.setAttribute("releaseUrl", org.cimug.compare.Version.RELEASE_URL);

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

	/** cim-compare's own top item, "EA Model", which stands for no package of the model. */
	private static boolean synthetic(Element item) {
		return item != null && attr(item, "guid").startsWith("MX_");
	}

	// ------------------------------------------------------------ packages

	private Element renderPackage(Element pkg) {
		String status = statusOf(pkg);
		if ("identical".equals(status) && tagsChanged(pkg))
			status = "changed"; // #169
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
			copyNotesAndProperties(end, ee, "added".equals(status) || "deleted".equals(status) ? status : statusOf(end));
			e.appendChild(ee);
		}
		return e;
	}

	// ------------------------------------------------------------ diagrams

	private Element renderDiagram(Element d) {
		String status = statusOf(d);
		// Diagrams are reported as Changed whenever anything (including element
		// positions) differs. Property changes are material; so are layout
		// changes that can be highlighted on the images (#37). Anything else
		// (connectors or labels moved, say) is not.
		if ("changed".equals(status) && !hasChangedProperties(d) && !hasHighlights(d))
			status = "identical";
		if (!full && "identical".equals(status))
			return null;
		Element e = element("Diagram", d, status);
		e.setAttribute("eaid", eaid(attr(d, "guid")));
		copyNotesAndProperties(d, e);
		if (!"identical".equals(status))
			addHighlights(d, e);
		count("diagram", status);
		return e;
	}

	/**
	 * #37: boxes to draw over the diagram images. Removed elements are marked
	 * on the baseline image; added, changed and restyled ones on the destination
	 * image; moved (or resized) ones on both. An element whose class changed in
	 * a way the box shows is marked "changed" whatever happened to its box.
	 */
	private void addHighlights(Element d, Element e) {
		for (Element o : children(d, "CompareItem")) {
			if ("diagramconnector".equals(kindOf(o))) {
				e.appendChild(connector(o));
				continue;
			}
			if (!"diagramobject".equals(kindOf(o)))
				continue;
			String layout = statusOf(o);
			String kind = "added".equals(layout) ? "added"
					: "deleted".equals(layout) ? "removed"
					: "moved".equals(layout) ? "moved"
					: "changed".equals(layout) ? "restyled" : null;
			if (!"added".equals(kind) && !"removed".equals(kind)
					&& visiblyChangedClasses.contains(eaid(attr(o, "guid"))))
				kind = "changed";
			if (kind == null)
				continue;
			String b = prop(o, "Box", "baseline"), m = prop(o, "Box", "model");
			if (("removed".equals(kind) || "moved".equals(kind)) && !b.isEmpty())
				e.appendChild(highlight(kind, "baseline", b, o));
			if (!"removed".equals(kind) && !m.isEmpty())
				e.appendChild(highlight(kind, "destination", m, o));
		}
	}

	/**
	 * A changed connector. EA does not store the route it draws, so the report
	 * lists connectors and outlines the two elements they join (on hover).
	 */
	private Element connector(Element o) {
		String kind = "added";
		String side = "destination";
		String status = statusOf(o);
		if ("deleted".equals(status)) {
			kind = "removed";
			side = "baseline";
		} else if (!"added".equals(status)) {
			kind = prop(o, "Change", "model");
		}
		String use = "baseline".equals(side) ? "baseline" : "model";
		String src = prop(o, "SourceBox", use), dst = prop(o, "TargetBox", use);
		Element c = out.createElement("Connector");
		c.setAttribute("kind", kind.isEmpty() ? "rerouted" : kind);
		c.setAttribute("side", side);
		c.setAttribute("name", attr(o, "name"));
		StringBuilder ends = new StringBuilder();
		for (String b : new String[] { src, dst })
			if (!b.isEmpty())
				ends.append(ends.length() > 0 ? ";" : "").append(b);
		c.setAttribute("ends", ends.toString());
		String what = prop(o, "Labels", "model"); // #145: what a "labels changed" connector shows changed
		if (!what.isEmpty())
			c.setAttribute("what", what);
		return c;
	}

	private Element highlight(String kind, String side, String box, Element o) {
		Element h = out.createElement("Highlight");
		h.setAttribute("kind", kind);
		h.setAttribute("side", side);
		h.setAttribute("box", box);
		h.setAttribute("name", attr(o, "name"));
		return h;
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
		copyNotesAndProperties(src, dst, dst.getAttribute("status"));
	}

	/**
	 * @param owner the status of the element the properties belong to. When it
	 *              exists on both sides (anything but added or deleted), a
	 *              property with a value on one side only is changed, not added
	 *              or deleted: EA leaves an empty value out of its export, but
	 *              the element still has the property (#107).
	 */
	private void copyNotesAndProperties(Element src, Element dst, String owner) {
		boolean bothSides = !"added".equals(owner) && !"deleted".equals(owner);
		copyTags(src, dst);
		Element props = firstChild(src, "Properties");
		if (props == null)
			return;
		Element notesOut = null;
		Element propsOut = out.createElement("Properties");
		for (Element p : children(props, "Property")) {
			String name = attr(p, "name");
			String st = normalize(attr(p, "status"));
			if (bothSides && ("added".equals(st) || "deleted".equals(st)))
				st = "changed";
			String b = p.hasAttribute("baseline") ? p.getAttribute("baseline") : null;
			String m = p.hasAttribute("model") ? p.getAttribute("model") : null;
			Element po = out.createElement("Property");
			po.setAttribute("name", name);
			po.setAttribute("status", st);
			if (b != null)
				po.setAttribute("baseline", b);
			if (m != null)
				po.setAttribute("model", m);
			if ("changed".equals(st)) {
				String tag = "Cardinality".equals(name) && sameMultiplicity(b, m) ? "notation only" // #109
						: "Stereotype".equals(name) ? stereotypeTag(b, m) : null; // #118
				if (tag != null)
					po.setAttribute("tag", tag);
			}
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

	/**
	 * #169: an element's user-defined tagged values, from its "Tagged Values" group (as
	 * EA's Compare Utility and cim-compare record them), as
	 * {@code <Tags><Tag name status baseline model/></Tags>}; every tag, so the ⓘ
	 * popup can list them all.
	 */
	private void copyTags(Element src, Element dst) {
		Element group = tagGroup(src);
		if (group == null)
			return;
		Element tags = out.createElement("Tags");
		for (Element t : children(group, "CompareItem")) {
			String name = prop(t, "Name", "model");
			if (name.isEmpty())
				name = prop(t, "Name", "baseline");
			if (name.isEmpty())
				continue;
			Element te = out.createElement("Tag");
			te.setAttribute("name", name);
			te.setAttribute("status", statusOf(t));
			Element v = valueProperty(t);
			if (v != null && v.hasAttribute("baseline"))
				te.setAttribute("baseline", v.getAttribute("baseline"));
			if (v != null && v.hasAttribute("model"))
				te.setAttribute("model", v.getAttribute("model"));
			tags.appendChild(te);
		}
		if (tags.hasChildNodes())
			dst.appendChild(tags);
	}

	private static Element valueProperty(Element tag) {
		Element props = firstChild(tag, "Properties");
		if (props != null)
			for (Element p : children(props, "Property"))
				if ("Value".equals(attr(p, "name")))
					return p;
		return null;
	}

	/** #169: an item's "Tagged Values" group, or null. */
	static Element tagGroup(Element item) {
		for (Element c : children(item, "CompareItem"))
			if (org.cimug.compare.TaggedValues.GROUP.equals(attr(c, "type")))
				return c;
		return null;
	}

	/** #169: whether any of an item's tagged values was added, removed or changed. */
	static boolean tagsChanged(Element item) {
		Element group = tagGroup(item);
		if (group != null)
			for (Element t : children(group, "CompareItem"))
				if (!"identical".equals(normalize(attr(t, "status"))))
					return true;
		return false;
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
		if ("DiagramObject".equals(t))
			return "diagramobject";
		if ("DiagramConnector".equals(t))
			return "diagramconnector";
		if ("Src".equals(t) || "Dst".equals(t))
			return "end";
		if (org.cimug.compare.TaggedValues.GROUP.equals(t))
			return "tags"; // #169: not an attribute
		Node gp = item.getParentNode();
		if (gp instanceof Element && org.cimug.compare.TaggedValues.GROUP.equals(attr((Element) gp, "type")))
			return "tag";
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
	private boolean hasChanges(Element item) {
		// A diagram reported as Changed with no changed properties and nothing to
		// highlight is not rendered (see renderDiagram), so it must not make its
		// package count as changed either.
		if ("diagram".equals(kindOf(item)) && "changed".equals(statusOf(item)) && !hasChangedProperties(item)
				&& !hasHighlights(item))
			return false;
		if (!"identical".equals(statusOf(item)))
			return true;
		if (hasChangedProperties(item))
			return true;
		for (Element c : children(item, "CompareItem"))
			if (hasChanges(c))
				return true;
		return false;
	}

	/** true when the diagram has at least one element to highlight (#37) */
	private boolean hasHighlights(Element d) {
		for (Element o : children(d, "CompareItem")) {
			if ("diagramconnector".equals(kindOf(o)))
				return true;
			if (!"diagramobject".equals(kindOf(o)))
				continue;
			if (!"identical".equals(statusOf(o)) || visiblyChangedClasses.contains(eaid(attr(o, "guid"))))
				return true;
		}
		return false;
	}

	/**
	 * How two stereotype lists that differ are alike (#118): "order only" when
	 * they hold the same stereotypes in another order, "case only" when only the
	 * case of a name changed, "order and case only" for both; null when a
	 * stereotype was added, removed or renamed.
	 */
	static String stereotypeTag(String a, String b) {
		java.util.List<String> x = org.cimug.compare.Stereotypes.split(a), y = org.cimug.compare.Stereotypes.split(b);
		if (x.isEmpty() || x.size() != y.size())
			return null;
		if (sorted(x, false).equals(sorted(y, false)))
			return x.equals(y) ? null : "order only";
		boolean sameOrder = true;
		for (int i = 0; i < x.size(); i++)
			if (!x.get(i).equalsIgnoreCase(y.get(i)))
				sameOrder = false;
		if (sameOrder)
			return "case only";
		return sorted(x, true).equals(sorted(y, true)) ? "order and case only" : null;
	}

	private static java.util.List<String> sorted(java.util.List<String> names, boolean ignoreCase) {
		java.util.List<String> l = new java.util.ArrayList<String>();
		for (String n : names)
			l.add(ignoreCase ? n.toLowerCase(java.util.Locale.ROOT) : n);
		java.util.Collections.sort(l);
		return l;
	}

	/**
	 * True when two multiplicities differ only in notation (#109): {@code n..n}
	 * is {@code n} (e.g. {@code 1..1} = {@code 1}) and {@code 0..*} is {@code *}.
	 */
	static boolean sameMultiplicity(String a, String b) {
		if (a == null || b == null)
			return false;
		String x = canonicalMultiplicity(a), y = canonicalMultiplicity(b);
		return !x.isEmpty() && x.equals(y);
	}

	private static String canonicalMultiplicity(String m) {
		String s = m.replaceAll("\\s+", "");
		int dots = s.indexOf("..");
		if (dots > 0 && s.indexOf("..", dots + 2) < 0) {
			String lo = s.substring(0, dots), hi = s.substring(dots + 2);
			if (lo.equals(hi))
				return lo;
			if ("0".equals(lo) && "*".equals(hi))
				return "*";
		}
		return s;
	}

	private static boolean hasChangedProperties(Element item) {
		if (tagsChanged(item)) // #169
			return true;
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
				if (!guid.isEmpty() && changeShowsOnDiagram(c))
					visiblyChangedClasses.add(eaid(guid));
			}
		}
	}

	/**
	 * true when a class present in both models changed in a way its box on a
	 * diagram shows: its name, stereotype, abstractness or package prefix; an
	 * attribute added, removed, or changed in something drawn (name, stereotype,
	 * type, multiplicity, initial value, visibility, static, const); or its
	 * generalization (the parent's name drawn in the box). Descriptions and other
	 * undrawn properties are not counted: they are reported with the class.
	 */
	private boolean changeShowsOnDiagram(Element cls) {
		String st = statusOf(cls);
		if ("added".equals(st) || "deleted".equals(st))
			return false; // the element itself is added to or removed from the diagram
		if (changedAny(cls, DRAWN_CLASS_PROPS))
			return true;
		for (Element c : children(cls, "CompareItem")) {
			String kind = kindOf(c);
			if ("attribute".equals(kind)) {
				String as = statusOf(c);
				if ("added".equals(as) || "deleted".equals(as) || changedAny(c, DRAWN_ATTRIBUTE_PROPS))
					return true;
			} else if ("links".equals(kind)) {
				for (Element l : children(c, "CompareItem"))
					if ("link".equals(kindOf(l)) && "Generalization".equals(attr(l, "name")) && hasChanges(l))
						return true;
			}
		}
		return false;
	}

	private static boolean changedAny(Element item, Set<String> names) {
		Element props = firstChild(item, "Properties");
		if (props == null)
			return false;
		for (Element p : children(props, "Property"))
			if (names.contains(attr(p, "name")) && !"identical".equals(normalize(attr(p, "status"))))
				return true;
		return false;
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
	 * The --package names: null for the whole model. A value with commas is a list
	 * (#94), unless the whole value is itself the name of a package.
	 */
	private List<String> selectedNames(List<Element> roots) {
		if (packageNames != null)
			return packageNames;
		if (packageFilter == null)
			return null;
		if (PackageNames.isList(packageFilter)) {
			List<Element> whole = new ArrayList<Element>();
			for (Element r : roots)
				collectPackagesNamed(r, packageFilter, whole);
			if (!whole.isEmpty())
				return Collections.singletonList(packageFilter);
		}
		List<String> names = PackageNames.parse(packageFilter);
		return names.isEmpty() ? null : names;
	}

	/**
	 * Drops a selected package that lies inside another selected package; it is
	 * reported as part of the outer one (#94).
	 */
	private List<Element> withoutNested(List<Element> selected) {
		List<Element> kept = new ArrayList<Element>();
		for (Element p : selected) {
			Element outer = null;
			for (org.w3c.dom.Node a = p.getParentNode(); a instanceof Element && outer == null; a = a.getParentNode())
				if (selected.contains(a))
					outer = (Element) a;
			if (outer == null)
				kept.add(p);
			else
				System.out.println("\nWARNING:  Package '" + attr(p, "name") + "' is inside '" + attr(outer, "name")
						+ "', which is also given with --package; it is reported as part of '" + attr(outer, "name")
						+ "'.");
		}
		return kept;
	}

	private String scopeOf(List<Element> packages) {
		StringBuilder labels = new StringBuilder();
		for (Element p : packages) {
			String was = prop(p, "Name", "baseline"), now = attr(p, "name");
			labels.append(labels.length() > 0 ? ", " : "")
					.append((!was.isEmpty() && !was.equals(now)) ? was + " → " + now : now);
		}
		return labels.toString();
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
