package org.cimug.compare;

import org.cimug.compare.uml1_3.GeneralizationType;

public class DestinationGeneralizationEndProperties extends AbstractGeneralizationEndProperties {

	public DestinationGeneralizationEndProperties(GeneralizationType baselineGeneralization,
			GeneralizationType targetGeneralization) {
		super(baselineGeneralization, targetGeneralization);
	}
	
	protected String getEndName() {
		return "ea_targetName";
	}

	@Override
	protected void initializeTagNamesMap() {
		tagNamesMap.put("dst_aggregation", "IsAggregation");
		tagNamesMap.put("dst_changeable", "IsChangeable");
		tagNamesMap.put("dst_containment", "Containment");
		tagNamesMap.put("ea_targetName", "End");
		tagNamesMap.put("dst_isNavigable", "IsNavigable");
		tagNamesMap.put("dst_isOrdered", "Ordering");
		tagNamesMap.put("dst_visibility", "Scope");
		tagNamesMap.put("dst_targetScope", "TargetScope");
	}

}