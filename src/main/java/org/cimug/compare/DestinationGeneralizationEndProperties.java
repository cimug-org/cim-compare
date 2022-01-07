package org.cimug.compare;

import org.cimug.compare.app.PreProcessor;
import org.cimug.compare.uml1_3.ClassType;
import org.cimug.compare.uml1_3.GeneralizationType;

public class DestinationGeneralizationEndProperties extends AbstractGeneralizationEndProperties {

	public DestinationGeneralizationEndProperties(PreProcessor preProcessor, GeneralizationType baselineGeneralization,
			GeneralizationType targetGeneralization) {
		super(preProcessor, baselineGeneralization, targetGeneralization);
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
	
	protected void initializeDeprecated(PreProcessor preProcessor, GeneralizationType baselineGeneralization, GeneralizationType targetGeneralization) {
		//
		this.baselineDeprecated = false;
		this.targetDeprecated = false;
		//
		if (baselineGeneralization != null && targetGeneralization != null) {
			String supertypeXmiId = baselineGeneralization.getSupertype();
			//
			ClassType baselineDestinationEndClass = (preProcessor.getBaselineClassesXmiIds().containsKey(supertypeXmiId)
					? preProcessor.getBaselineClassesXmiIds().get(supertypeXmiId)
					: (preProcessor.getBaselineDeletedClassesXmiIds().containsKey(supertypeXmiId)
							? preProcessor.getBaselineDeletedClassesXmiIds().get(supertypeXmiId)
							: preProcessor.getBaselineMovedClassesXmiIds().get(supertypeXmiId)));

			this.baselineDeprecated = (baselineDestinationEndClass != null ? baselineDestinationEndClass.isDeprecated() : false);
				
			supertypeXmiId = targetGeneralization.getSupertype();
			ClassType targetDestinationEndClass = (preProcessor.getTargetClassesXmiIds().containsKey(supertypeXmiId)
							? preProcessor.getTargetClassesXmiIds().get(supertypeXmiId)
							: preProcessor.getTargetNewClassesXmiIds().get(supertypeXmiId));

			this.targetDeprecated = (targetDestinationEndClass != null ? targetDestinationEndClass.isDeprecated() : false);
		} else if (baselineGeneralization != null) {
			String supertypeXmiId = baselineGeneralization.getSupertype();

			ClassType baselineDestinationEndClass = (preProcessor.getBaselineClassesXmiIds().containsKey(supertypeXmiId)
					? preProcessor.getBaselineClassesXmiIds().get(supertypeXmiId)
					: (preProcessor.getBaselineDeletedClassesXmiIds().containsKey(supertypeXmiId)
							? preProcessor.getBaselineDeletedClassesXmiIds().get(supertypeXmiId)
							: preProcessor.getBaselineMovedClassesXmiIds().get(supertypeXmiId)));

			this.baselineDeprecated = (baselineDestinationEndClass != null ? baselineDestinationEndClass.isDeprecated() : false);
		} else if (targetGeneralization != null) {
			String supertypeXmiId = targetGeneralization.getSupertype();

			ClassType targetDestinationEndClass = (preProcessor.getTargetClassesXmiIds().containsKey(supertypeXmiId)
					? preProcessor.getTargetClassesXmiIds().get(supertypeXmiId)
					: preProcessor.getTargetNewClassesXmiIds().get(supertypeXmiId));

			this.targetDeprecated = (targetDestinationEndClass != null ? targetDestinationEndClass.isDeprecated() : false);
		}
	}

}