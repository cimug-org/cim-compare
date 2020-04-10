package org.cimug.compare;

import java.util.Comparator;

public class StringComparator implements Comparator<String> {
	public int compare(String str1, String str2) {
		if (str1 == str2) {
			return 0;
		}
		if (str1 == null) {
			return -1;
		}
		if (str2 == null) {
			return 1;
		}
		return str1.compareTo(str2);
	}
}