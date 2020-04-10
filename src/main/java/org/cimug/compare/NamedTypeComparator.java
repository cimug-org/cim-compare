package org.cimug.compare;

import java.util.Comparator;

import org.cimug.compare.uml1_3.ifaces.NamedType;

public class NamedTypeComparator<T extends NamedType> implements Comparator<T> {

	public int compare(T type1, T type2) {
		if (type1 == type2) {
			return 0;
		}
		if (type1 == null) {
			return -1;
		}
		if (type2 == null) {
			return 1;
		}

		String name1 = type1.getName();
		String name2 = type2.getName();

		if (name1 == name2) {
			return 0;
		}
		if (name1 == null) {
			return -1;
		}
		if (name2 == null) {
			return 1;
		}

		return name1.compareTo(name2);
	}

}