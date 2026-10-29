package org.cimug.compare.report;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Leaves out differences between a value and an empty value in EA's
 * housekeeping fields (#126), unless {@code --show-empty-values} is given.
 *
 * <p>
 * Elements created or rewritten outside Sparx EA often have these fields empty,
 * while the same elements created in EA have the values EA gives every new
 * element. Such a difference carries no model meaning. A property difference is
 * not reported when one side is empty and the other side is:
 * </p>
 * <ul>
 * <li>any value of Complexity, Phase, Version or GenType;</li>
 * <li>{@code Not Specified} or {@code Unspecified} for Containment;</li>
 * <li>{@code false} or {@code 0} for IsLiteral, Derived, Const or Static.</li>
 * </ul>
 * <p>
 * The values were checked against elements created in EA's interface: new
 * classes get Complexity 1, Phase 1.0, Version 1.0 and the project's default
 * language as GenType; new attributes get Containment "Not Specified", Const and
 * Static false, and leave IsLiteral and Derived empty.
 * </p>
 * <p>
 * The comparison document is changed in place: such a property becomes
 * Identical, and an element whose only differences were these becomes Identical
 * too (diagrams and their contents are left alone).
 * </p>
 */
final class EmptyValues {

	private static final Set<String> HOUSEKEEPING = new HashSet<String>(
			Arrays.asList("Complexity", "Phase", "Version", "GenType"));
	private static final Set<String> NOT_SPECIFIED = new HashSet<String>(Arrays.asList("Containment"));
	private static final Set<String> FALSE = new HashSet<String>(
			Arrays.asList("IsLiteral", "Derived", "Const", "Static"));

	private int count;

	private EmptyValues() {
	}

	/** @return the number of property differences left out */
	static int apply(Element root) {
		EmptyValues e = new EmptyValues();
		e.visit(root);
		return e.count;
	}

	/** True when the difference between {@code value} and an empty value of this property is not reported. */
	static boolean neutral(String property, String value) {
		if (value == null || value.isEmpty())
			return false;
		if (HOUSEKEEPING.contains(property))
			return true;
		if (NOT_SPECIFIED.contains(property))
			return "Not Specified".equals(value) || "Unspecified".equals(value);
		if (FALSE.contains(property))
			return "false".equals(value) || "0".equals(value);
		return false;
	}

	/** @return true when something in this subtree was changed to Identical */
	private boolean visit(Element e) {
		boolean dirty = false;
		for (Node n = e.getFirstChild(); n != null; n = n.getNextSibling())
			if (n instanceof Element && "CompareItem".equals(n.getNodeName()) || n instanceof Element
					&& !"CompareItem".equals(n.getNodeName()) && !"Properties".equals(n.getNodeName()))
				dirty |= visit((Element) n);
		if (!"CompareItem".equals(e.getNodeName()))
			return dirty;
		String status = e.getAttribute("status");
		boolean oneSided = "Baseline only".equals(status) || "Model only".equals(status);
		Element props = child(e, "Properties");
		if (!oneSided && props != null) {
			for (Node n = props.getFirstChild(); n != null; n = n.getNextSibling()) {
				if (!(n instanceof Element) || !"Property".equals(n.getNodeName()))
					continue;
				Element p = (Element) n;
				String ps = p.getAttribute("status");
				if (!"Changed".equals(ps) && !"Baseline only".equals(ps) && !"Model only".equals(ps))
					continue;
				String b = p.getAttribute("baseline"), m = p.getAttribute("model");
				String value = b.isEmpty() ? m : m.isEmpty() ? b : null;
				if (value != null && neutral(p.getAttribute("name"), value)) {
					p.setAttribute("status", "Identical");
					count++;
					dirty = true;
				}
			}
		}
		if (dirty && "Changed".equals(status) && !e.getAttribute("type").startsWith("Diagram") && unchanged(e))
			e.setAttribute("status", "Identical");
		return dirty;
	}

	/** No property and no child item differs any more. */
	private static boolean unchanged(Element e) {
		for (Node n = e.getFirstChild(); n != null; n = n.getNextSibling()) {
			if (!(n instanceof Element))
				continue;
			Element c = (Element) n;
			if ("Properties".equals(c.getNodeName())) {
				for (Node q = c.getFirstChild(); q != null; q = q.getNextSibling())
					if (q instanceof Element && !"Identical".equals(((Element) q).getAttribute("status")))
						return false;
			} else if ("CompareItem".equals(c.getNodeName()) && !"Identical".equals(c.getAttribute("status"))) {
				return false;
			}
		}
		return true;
	}

	private static Element child(Element e, String name) {
		for (Node n = e.getFirstChild(); n != null; n = n.getNextSibling())
			if (n instanceof Element && name.equals(n.getNodeName()))
				return (Element) n;
		return null;
	}
}
