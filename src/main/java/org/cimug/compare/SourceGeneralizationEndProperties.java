package org.cimug.compare;

import org.cimug.compare.app.PreProcessor;
import org.cimug.compare.uml1_3.ClassType;
import org.cimug.compare.uml1_3.GeneralizationType;

public class SourceGeneralizationEndProperties extends AbstractGeneralizationEndProperties {

	public SourceGeneralizationEndProperties(PreProcessor preProcessor, GeneralizationType baselineGeneralization,
			GeneralizationType targetGeneralization) {
		super(preProcessor, baselineGeneralization, targetGeneralization);
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

	protected void initializeDeprecated(PreProcessor preProcessor, GeneralizationType baselineGeneralization,
			GeneralizationType targetGeneralization) {
		//
		this.baselineDeprecated = false;
		this.targetDeprecated = false;
		//
		if (baselineGeneralization != null && targetGeneralization != null) {
			String subtypeXmiId = baselineGeneralization.getSubtype();

			ClassType baselineSourceEndClass = (preProcessor.getBaselineClassesXmiIds().containsKey(subtypeXmiId)
					? preProcessor.getBaselineClassesXmiIds().get(subtypeXmiId)
					: null);

			this.baselineDeprecated = (baselineSourceEndClass != null ? baselineSourceEndClass.isDeprecated() : false);

			subtypeXmiId = targetGeneralization.getSubtype();
			ClassType targetSourceEndClass = (preProcessor.getTargetClassesXmiIds().containsKey(subtypeXmiId)
					? preProcessor.getTargetClassesXmiIds().get(subtypeXmiId)
					: preProcessor.getTargetNewClassesXmiIds().get(subtypeXmiId));

			this.targetDeprecated = (targetSourceEndClass != null ? targetSourceEndClass.isDeprecated() : false);
		} else if (baselineGeneralization != null) {
			String subtypeXmiId = baselineGeneralization.getSubtype();

			ClassType baselineSourceEndClass = (preProcessor.getBaselineClassesXmiIds().containsKey(subtypeXmiId)
					? preProcessor.getBaselineClassesXmiIds().get(subtypeXmiId)
					: null);

			this.baselineDeprecated = (baselineSourceEndClass != null ? baselineSourceEndClass.isDeprecated() : false);
		} else if (targetGeneralization != null) {
			String subtypeXmiId = targetGeneralization.getSubtype();
			
			ClassType targetSourceEndClass = (preProcessor.getTargetClassesXmiIds().containsKey(subtypeXmiId)
					? preProcessor.getTargetClassesXmiIds().get(subtypeXmiId)
					: preProcessor.getTargetNewClassesXmiIds().get(subtypeXmiId));

			this.targetDeprecated = (targetSourceEndClass != null ? targetSourceEndClass.isDeprecated() : false);
		}
	}

}