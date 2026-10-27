package org.cimug.compare;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * The value of {@code --package}: one package name, or several separated by
 * commas (issue #94), e.g. {@code Model,UtilityExtensions} or, quoted on the command line
 * because a name contains a space, {@code "Grid, European Standards"}.
 *
 * <p>
 * Names are split on commas and trimmed; spaces inside a name are kept. Empty
 * entries and repeats are dropped and the order given is kept. A package whose
 * own name contains a comma can still be given on its own: callers first check
 * whether the whole value names a package ({@link #isList(String)} tells them
 * when that check is needed).
 * </p>
 */
public final class PackageNames {

	private PackageNames() {
	}

	/** True when the value contains a comma, i.e. may be a list. */
	public static boolean isList(String value) {
		return value != null && value.indexOf(',') >= 0;
	}

	/**
	 * @return the names in the order given, or an empty list for a null or blank
	 *         value
	 */
	public static List<String> parse(String value) {
		LinkedHashSet<String> names = new LinkedHashSet<String>();
		if (value != null)
			for (String part : value.split(",")) {
				String name = part.trim();
				if (!name.isEmpty())
					names.add(name);
			}
		return new ArrayList<String>(names);
	}

	/** The names as one value, for messages: {@code Model, UtilityExtensions}. */
	public static String join(List<String> names) {
		StringBuilder b = new StringBuilder();
		for (String n : names)
			b.append(b.length() > 0 ? ", " : "").append(n);
		return b.toString();
	}
}
