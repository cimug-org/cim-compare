package org.cimug.compare;

import org.cimug.compare.uml1_3.GeneralizationType;

public class DestinationGeneralizationEndProperties extends AbstractGeneralizationEndProperties {

	public DestinationGeneralizationEndProperties(GeneralizationType baselineGeneralization,
			GeneralizationType targetGeneralization) {
		super(baselineGeneralization, targetGeneralization);
	}

	@Override
	protected void initializeTagNamesMap() {
		tagNamesMap.put("ea_targetName", "Name");
		tagNamesMap.put("dst_changeable", "IsAggregation");
		tagNamesMap.put("dst_aggregation", "Cardinality");
		tagNamesMap.put("dst_containment", "Containment");
		tagNamesMap.put("ea_targetName", "End");
		tagNamesMap.put("dst_isNavigable", "IsNavigable");
		tagNamesMap.put("dst_isOrdered", "Ordering");
		tagNamesMap.put("dst_visibility", "Scope");
	}

}