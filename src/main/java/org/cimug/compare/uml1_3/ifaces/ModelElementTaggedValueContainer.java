package org.cimug.compare.uml1_3.ifaces;

import org.cimug.compare.uml1_3.ModelElementTaggedValue;
import org.cimug.compare.uml1_3.TaggedValueType;

public interface ModelElementTaggedValueContainer {
	ModelElementTaggedValue getModelElementTaggedValue();

	default TaggedValueType getTaggedValue(String tagName) {
		return getModelElementTaggedValue().getTaggedValue(tagName);
	}

	default String getTheValue(String tagName) {
		return (getTaggedValue(tagName) != null ? getTaggedValue(tagName).getTheValue() : null);
	}
	
}
