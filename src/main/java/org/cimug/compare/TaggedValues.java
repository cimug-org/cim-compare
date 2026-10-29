package org.cimug.compare;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.cimug.compare.logs.CompareItem;
import org.cimug.compare.logs.Properties;
import org.cimug.compare.logs.Property;
import org.cimug.compare.uml1_3.AssociationEndType;
import org.cimug.compare.uml1_3.AssociationType;
import org.cimug.compare.uml1_3.AttributeType;
import org.cimug.compare.uml1_3.ClassType;
import org.cimug.compare.uml1_3.ModelElementTaggedValue;
import org.cimug.compare.uml1_3.PackageType;
import org.cimug.compare.uml1_3.TaggedValueType;
import org.cimug.compare.uml1_3.ifaces.PackageContainer;
import org.cimug.compare.xmi1_1.XMIContentType;

/**
 * #169: user-defined tagged values (such as {@code baseuri} or {@code baseprefix}) on
 * packages, classes, attributes, associations and association ends, compared and
 * recorded as EA's Compare Utility records them: a {@code Tagged Values} group under
 * the element, one item per tag with Name, Notes and Value properties.
 * <p>
 * EA's XMI 1.1 export writes a user-defined tag either inside its element's
 * {@code ModelElement.taggedValue} or on its own under {@code XMI.content}, naming its
 * element with {@code modelElement}. Either way it carries an {@code xmi.id}, which
 * EA's own tags (ea_guid, stereotype, documentation, ...) never have.
 */
public final class TaggedValues {

	/** Items that never get a Tagged Values group, nor do the items under them. */
	private static final java.util.Set<String> NOT_TAGGED = new java.util.HashSet<String>(
			java.util.Arrays.asList("DiagramObject", "DiagramConnector", "Diagram"));

	/** The type and name EA gives the group of an element's tags. */
	public static final String GROUP = "Tagged Values";

	private TaggedValues() {
	}

	/** A user-defined tag: it has its own xmi.id. */
	static boolean userDefined(TaggedValueType tv) {
		return tv != null && tv.getXmiId() != null && !tv.getXmiId().isEmpty() && tv.getTag() != null;
	}

	/** The user-defined tags inside an element's ModelElement.taggedValue. */
	static List<TaggedValueType> inline(ModelElementTaggedValue tagged) {
		List<TaggedValueType> out = new ArrayList<TaggedValueType>();
		if (tagged != null && tagged.getTaggedValues() != null)
			for (TaggedValueType tv : tagged.getTaggedValues())
				if (userDefined(tv))
					out.add(tv);
		return out;
	}

	/**
	 * The user-defined tags of every package, class, attribute and association in one
	 * model, by the element's GUID; association ends under "Src" / "Dst" + the
	 * association's GUID.
	 */
	public static Map<String, List<TaggedValueType>> index(XMIContentType content) {
		Map<String, List<TaggedValueType>> map = new HashMap<String, List<TaggedValueType>>();
		if (content == null)
			return map;
		if (content.getModel() != null)
			collect(content.getModel(), map);
		for (TaggedValueType tv : content.getTaggedValues())
			if (userDefined(tv) && tv.getModelElement() != null)
				add(map, DiffUtils.convertXmiIdToEAGUID(tv.getModelElement()), tv);
		return map;
	}

	private static void collect(PackageContainer container, Map<String, List<TaggedValueType>> map) {
		for (PackageType p : container.getPackages()) {
			for (TaggedValueType tv : inline(p.getModelElementTaggedValue()))
				add(map, p.getGUID(), tv);
			collect(p, map);
		}
		for (ClassType c : container.getClasses()) {
			for (TaggedValueType tv : inline(c.getModelElementTaggedValue()))
				add(map, c.getGUID(), tv);
			for (AttributeType a : c.getAttributes())
				for (TaggedValueType tv : inline(a.getModelElementTaggedValue()))
					add(map, a.getGUID(), tv);
		}
		for (AssociationType a : container.getAssociations()) {
			for (TaggedValueType tv : inline(a.getModelElementTaggedValue()))
				add(map, a.getGUID(), tv);
			end(map, "Src" + a.getGUID(), a.getSourceAssociationEnd());
			end(map, "Dst" + a.getGUID(), a.getDestinationAssociationEnd());
		}
	}

	private static void end(Map<String, List<TaggedValueType>> map, String key, AssociationEndType end) {
		if (end != null)
			for (TaggedValueType tv : inline(end.getModelElementTaggedValue()))
				add(map, key, tv);
	}

	private static void add(Map<String, List<TaggedValueType>> map, String key, TaggedValueType tv) {
		if (key == null || key.isEmpty())
			return;
		List<TaggedValueType> list = map.get(key);
		if (list == null)
			map.put(key, list = new ArrayList<TaggedValueType>());
		// the same tag can be read twice (an association listed under two packages)
		for (TaggedValueType t : list)
			if (t.getXmiId().equals(tv.getXmiId()))
				return;
		list.add(tv);
	}

	private static String value(TaggedValueType tv) {
		if (tv == null)
			return null;
		String v = tv.getTheValue();
		if (v == null)
			v = tv.getValue(); // a memo value, written as the element's text
		return v == null || v.isEmpty() ? null : v;
	}

	/**
	 * The Tagged Values group of an element whose GUID is {@code ownerGuid}, comparing
	 * its baseline and destination tags; null when it has none on either side. A tag is
	 * matched by its xmi.id, as EA matches it; tags left over are matched by name, in
	 * order, so a tag a tool wrote again with a new id is reported as the same tag.
	 */
	public static CompareItem group(String ownerGuid, List<TaggedValueType> baseline, List<TaggedValueType> model) {
		if (baseline == null)
			baseline = new ArrayList<TaggedValueType>();
		if (model == null)
			model = new ArrayList<TaggedValueType>();
		if (baseline.isEmpty() && model.isEmpty())
			return null;
		Map<TaggedValueType, TaggedValueType> pairs = new LinkedHashMap<TaggedValueType, TaggedValueType>();
		List<TaggedValueType> left = new ArrayList<TaggedValueType>(baseline), right = new ArrayList<TaggedValueType>(model);
		for (TaggedValueType m : model)
			for (TaggedValueType b : left)
				if (b.getXmiId().equals(m.getXmiId())) {
					pairs.put(m, b);
					left.remove(b);
					right.remove(m);
					break;
				}
		for (TaggedValueType m : new ArrayList<TaggedValueType>(right))
			for (TaggedValueType b : left)
				if (b.getTag().equals(m.getTag())) {
					pairs.put(m, b);
					left.remove(b);
					right.remove(m);
					break;
				}
		CompareItem group = new CompareItem(null, GROUP, GROUP, ownerGuid + GROUP, Status.Identical.toString());
		for (TaggedValueType m : model)
			group.getCompareItem().add(item(pairs.get(m), m));
		for (TaggedValueType b : left)
			group.getCompareItem().add(item(b, null));
		return group;
	}

	private static CompareItem item(TaggedValueType b, TaggedValueType m) {
		TaggedValueType tv = m != null ? m : b;
		Properties props = new Properties();
		String bn = b == null ? null : b.getTag(), mn = m == null ? null : m.getTag();
		String bv = value(b), mv = value(m);
		Status status = b == null ? Status.ModelOnly
				: m == null ? Status.BaselineOnly
						: (eq(bn, mn) && eq(bv, mv) ? Status.Identical : Status.Changed);
		props.getProperty().add(new Property("Name", bn, mn, side(status, eq(bn, mn))));
		props.getProperty().add(new Property("Notes", null, null, Status.Identical.toString()));
		props.getProperty().add(new Property("Value", bv, mv, side(status, eq(bv, mv))));
		String guid = DiffUtils.convertXmiIdToEAGUID(tv.getXmiId());
		return new CompareItem(props, tv.getTag() + " = " + (value(tv) == null ? "" : value(tv)), "", guid,
				status.toString());
	}

	private static String side(Status item, boolean same) {
		if (item == Status.ModelOnly || item == Status.BaselineOnly)
			return item.toString();
		return (same ? Status.Identical : Status.Changed).toString();
	}

	private static boolean eq(String a, String b) {
		return a == null ? b == null : a.equals(b);
	}

	/** Whether any tag in a Tagged Values group was added, removed or changed. */
	public static boolean changed(CompareItem group) {
		if (group == null)
			return false;
		for (CompareItem i : group.getCompareItem())
			if (!Status.Identical.toString().equals(i.getStatus()))
				return true;
		return false;
	}

	/**
	 * Adds a Tagged Values group to every package, class, attribute and association in
	 * a comparison (and to an association's ends), from the two models' tags. An element
	 * that was otherwise unchanged but whose tags changed becomes Changed, so it is
	 * shown and counted; EA's own log leaves it Identical.
	 */
	public static void attach(CompareItem root, XMIContentType baseline, XMIContentType destination) {
		Map<String, List<TaggedValueType>> b = index(baseline), d = index(destination);
		if (b.isEmpty() && d.isEmpty())
			return;
		attach(root, b, d);
	}

	static void attach(CompareItem item, Map<String, List<TaggedValueType>> b, Map<String, List<TaggedValueType>> d) {
		if (item == null || NOT_TAGGED.contains(item.getType()))
			return; // a diagram's objects and connectors share their element's GUID
		List<CompareItem> kids = new ArrayList<CompareItem>(item.getCompareItem());
		String guid = item.getGuid();
		boolean association = "Association".equals(item.getName());
		if (guid != null && !GROUP.equals(item.getType()) && !"Src".equals(item.getType())
				&& !"Dst".equals(item.getType()) && !"Links".equals(item.getType())) {
			CompareItem group = group(guid, b.get(guid), d.get(guid));
			if (group != null && !hasGroup(item)) {
				item.getCompareItem().add(group);
				promote(item, group);
			}
		}
		for (CompareItem k : kids) {
			if (association && ("Src".equals(k.getType()) || "Dst".equals(k.getType()))) {
				String key = k.getType() + guid;
				CompareItem group = group(k.getGuid(), b.get(key), d.get(key));
				if (group != null && !hasGroup(k)) {
					k.getCompareItem().add(group);
					if (promote(k, group))
						promote(item, group);
				}
				continue;
			}
			attach(k, b, d);
		}
	}

	private static boolean hasGroup(CompareItem item) {
		for (CompareItem k : item.getCompareItem())
			if (GROUP.equals(k.getType()))
				return true;
		return false;
	}

	/** An Identical element whose tags changed becomes Changed. */
	private static boolean promote(CompareItem item, CompareItem group) {
		if (changed(group) && Status.Identical.toString().equals(item.getStatus())) {
			item.setStatus(Status.Changed.toString());
			return true;
		}
		return changed(group);
	}
}
