package org.cimug.compare;

import java.util.List;

import org.cimug.compare.uml1_3.ifaces.GetValueFunction;

@FunctionalInterface
public interface GetNameFunction<T extends GetValueFunction<String>> {
	T apply(String name, List<T> values);
}
