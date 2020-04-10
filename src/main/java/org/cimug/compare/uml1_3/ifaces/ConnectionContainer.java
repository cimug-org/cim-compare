/**
 * 
 */
package org.cimug.compare.uml1_3.ifaces;

import java.io.Serializable;

import javax.xml.bind.JAXBElement;

import org.cimug.compare.uml1_3.AssociationConnectionType;
import org.cimug.compare.uml1_3.AssociationEndType;

/**
 * @author tviegut
 *
 */
public interface ConnectionContainer extends ContentContainer {

	default AssociationConnectionType getAssociationConnection() {
		for (Serializable item : getContent()) {
			if (item instanceof AssociationConnectionType) {
				return (AssociationConnectionType) item;
			} else if (item instanceof JAXBElement
					&& ((JAXBElement) item).getValue() instanceof AssociationConnectionType) {
				return (AssociationConnectionType) ((JAXBElement) item).getValue();
			}
		}
		return null;
	}

	default AssociationEndType getSourceAssocationEnd() {
		if (getAssociationConnection() != null) {
			for (AssociationEndType end : getAssociationConnection().getAssociationEnds()) {
				if ("source".equals(end.getModelElementTaggedValue().getTaggedValue("ea_end").getTheValue())) {
					return end;
				}
			}
		}
		return null;
	}

	default AssociationEndType getDestinationAssocationEnd() {
		if (getAssociationConnection() != null) {
			for (AssociationEndType end : getAssociationConnection().getAssociationEnds()) {
				if ("target".equals(end.getModelElementTaggedValue().getTaggedValue("ea_end").getTheValue())) {
					return end;
				}
			}
		}
		return null;
	}

}
