package org.cimug.compare.uml1_3.ifaces;

import org.cimug.compare.uml1_3.ModelElementStereotypeType;
import org.cimug.compare.uml1_3.StereotypeType;

public interface ModelElementStereotypeContainer {
	
	ModelElementStereotypeType getModelElementStereotype();

	default StereotypeType getStereotype() {
		return getModelElementStereotype().getStereotype();
	}

	default boolean isDeprecated() {
		return (getModelElementStereotype() != null && getModelElementStereotype().getStereotype() != null ? getModelElementStereotype().getStereotype().getName().toLowerCase().equals("deprecated") : false);
	}
}
