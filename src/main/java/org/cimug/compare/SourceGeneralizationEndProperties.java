package org.cimug.compare;

import org.cimug.compare.uml1_3.GeneralizationType;

public class SourceGeneralizationEndProperties extends AbstractGeneralizationEndProperties {

	public SourceGeneralizationEndProperties(GeneralizationType baselineGeneralization,
			GeneralizationType targetGeneralization) {
		super(baselineGeneralization, targetGeneralization);
	}

	@Override
	protected void initializeTagNamesMap() {
		tagNamesMap.put("ea_sourceName", "Name");
		tagNamesMap.put("src_aggregation", "Cardinality");
		tagNamesMap.put("src_containment", "Containment");
		tagNamesMap.put("src_changeable", "IsAggregation");
		tagNamesMap.put("ea_sourceName", "End");
		tagNamesMap.put("src_isNavigable", "IsNavigable");
		tagNamesMap.put("src_isOrdered", "Ordering");
		tagNamesMap.put("src_visibility", "Scope");
	}

}