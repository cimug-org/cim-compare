/**
 * 
 */
package org.cimug.compare.uml1_3.ifaces;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBElement;

import org.cimug.compare.uml1_3.AttributeType;
import org.cimug.compare.uml1_3.ClassifierFeatureType;
import org.cimug.compare.uml1_3.ModelElementStereotypeType;
import org.cimug.compare.uml1_3.ModelElementTaggedValue;
import org.cimug.compare.uml1_3.TaggedValueType;

public interface ContentsContainer
		extends ContentContainer, ModelElementStereotypeContainer, ModelElementTaggedValueContainer {

	default ModelElementStereotypeType getModelElementStereotype() {
		for (Serializable item : getContent()) {
			if (item instanceof ModelElementStereotypeType) {
				return (ModelElementStereotypeType) item;
			} else if (item instanceof JAXBElement
					&& ((JAXBElement) item).getValue() instanceof ModelElementStereotypeType) {
				return (ModelElementStereotypeType) ((JAXBElement) item).getValue();
			}
		}
		return null;
	}

	default ModelElementTaggedValue getModelElementTaggedValue() {
		for (Serializable item : getContent()) {
			if (item instanceof ModelElementTaggedValue) {
				return (ModelElementTaggedValue) item;
			} else if (item instanceof JAXBElement
					&& ((JAXBElement) item).getValue() instanceof ModelElementTaggedValue) {
				return (ModelElementTaggedValue) ((JAXBElement) item).getValue();
			}
		}
		return null;
	}

	default TaggedValueType getTaggedValue(String tagName) {
		for (Serializable item : getContent()) {
			if (item instanceof ModelElementTaggedValue) {
				ModelElementTaggedValue e = (ModelElementTaggedValue) item;
				return e.getTaggedValue(tagName);
			} else if (item instanceof JAXBElement
					&& ((JAXBElement) item).getValue() instanceof ModelElementTaggedValue) {
				ModelElementTaggedValue e = (ModelElementTaggedValue) ((JAXBElement) item).getValue();
				return e.getTaggedValue(tagName);
			}
		}
		return null;
	}

	default List<AttributeType> getAttributes() {
		for (Serializable item : getContent()) {
			if (item instanceof ClassifierFeatureType) {
				ClassifierFeatureType cf = (ClassifierFeatureType) item;
				return cf.getAttributes();
			} else if (item instanceof JAXBElement
					&& ((JAXBElement) item).getValue() instanceof ClassifierFeatureType) {
				ClassifierFeatureType cf = (ClassifierFeatureType) ((JAXBElement) item).getValue();
				return cf.getAttributes();
			}
		}
		return new ArrayList<AttributeType>();
	}

	default AttributeType getAttributeByName(String name) {
		List<AttributeType> attributes = getAttributes();
		for (AttributeType attribute : attributes) {
			if (attribute.getName().equals(name)) {
				return attribute;
			}
		}
		return null;
	}

	default AttributeType getAttributeByGUID(String xmiId) {
		List<AttributeType> attributes = getAttributes();
		for (AttributeType attribute : attributes) {
			if ((attribute.getModelElementTaggedValue() != null)
					&& (attribute.getModelElementTaggedValue().getTaggedValue("ea_guid") != null)) {
				if (attribute.getModelElementTaggedValue().getTaggedValue("ea_guid").getTheValue().equals(xmiId)) {
					return attribute;
				}
			}
		}
		return null;
	}

	default AttributeType getAttribute(String key) {
		List<AttributeType> attributes = getAttributes();
		for (AttributeType attribute : attributes) {
			if (attribute.getGUID().equals(key)) {
				return attribute;
			}
		}
		return null;
	}

}
