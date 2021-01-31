package org.cimug.compare;

import org.cimug.compare.uml1_3.GeneralizationType;

public class SourceGeneralizationEndProperties extends AbstractGeneralizationEndProperties {

	public SourceGeneralizationEndProperties(GeneralizationType baselineGeneralization,
			GeneralizationType targetGeneralization) {
		super(baselineGeneralization, targetGeneralization);
	}
	
	protected String getEndName() {
		return "ea_sourceName";
	}

	@Override
	protected void initializeTagNamesMap() {
		tagNamesMap.put("src_aggregation", "IsAggregation");
		tagNamesMap.put("src_changeable", "IsChangeable");
		tagNamesMap.put("src_containment", "Containment");
		tagNamesMap.put("ea_sourceName", "End");
		tagNamesMap.put("src_isNavigable", "IsNavigable");
		tagNamesMap.put("src_isOrdered", "Ordering");
		tagNamesMap.put("src_visibility", "Scope");
		tagNamesMap.put("src_targetScope", "TargetScope");
	}

}