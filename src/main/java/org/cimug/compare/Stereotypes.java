package org.cimug.compare;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cimug.compare.uml1_3.TaggedValueType;

/**
 * The full stereotype list of an element in an EA XMI 1.1 export (#118).
 *
 * <p>
 * EA writes an element's first stereotype as its {@code stereotype} tagged
 * value (and as {@code UML:Stereotype}), and all of them, in order, in its
 * {@code $ea_xref_property} tagged value:
 * {@code $XREFPROP=…$NAM=Stereotypes$NAM;…$DES=@STEREO;Name=A;…@ENDSTEREO;@STEREO;Name=B;…@ENDSTEREO;$DES;…$ENDXREF;}.
 * The list is returned as UML 2.5.1 shows it inside one pair of guillemets:
 * the names separated by ", ".
 * </p>
 */
public final class Stereotypes {

	public static final String SEPARATOR = ", ";

	private static final String XREF = "$ea_xref_property";
	private static final Pattern ENTRY = Pattern.compile("\\$XREFPROP=(.*?)\\$ENDXREF;", Pattern.DOTALL);
	private static final Pattern NAME = Pattern.compile("@STEREO;Name=([^;]*);");

	private Stereotypes() {
	}

	/**
	 * @param fallback the single stereotype to use when the element has no list,
	 *                 e.g. from {@code UML:Stereotype}; may be null
	 * @return the stereotypes separated by ", ", the {@code stereotype} tagged
	 *         value or {@code fallback} when there is no list, or null
	 */
	public static String of(List<TaggedValueType> taggedValues, String fallback) {
		String single = null;
		if (taggedValues != null) {
			for (TaggedValueType tv : taggedValues) {
				String tag = tv.getTag();
				if (XREF.equals(tag)) {
					List<String> names = names(tv.getTheValue());
					if (!names.isEmpty())
						return join(names);
				} else if ("stereotype".equals(tag) && single == null && tv.getTheValue() != null) {
					single = tv.getTheValue().replaceAll("\n", "");
				}
			}
		}
		return single != null ? single : fallback;
	}

	public static String of(List<TaggedValueType> taggedValues) {
		return of(taggedValues, null);
	}

	/** The names in the Stereotypes entry of an {@code $ea_xref_property} value, in order. */
	static List<String> names(String xref) {
		List<String> names = new ArrayList<String>();
		if (xref == null)
			return names;
		Matcher e = ENTRY.matcher(xref);
		while (e.find()) {
			String entry = e.group(1);
			if (!entry.contains("$NAM=Stereotypes$NAM;"))
				continue;
			Matcher n = NAME.matcher(entry);
			while (n.find()) {
				String name = n.group(1).trim();
				if (!name.isEmpty())
					names.add(name);
			}
			if (!names.isEmpty())
				break;
		}
		return names;
	}

	static String join(List<String> names) {
		StringBuilder b = new StringBuilder();
		for (String n : names)
			b.append(b.length() > 0 ? SEPARATOR : "").append(n);
		return b.toString();
	}

	/** The names of a list as returned by {@link #of}. */
	public static List<String> split(String list) {
		List<String> names = new ArrayList<String>();
		if (list != null)
			for (String n : list.split(","))
				if (!n.trim().isEmpty())
					names.add(n.trim());
		return names;
	}
}
