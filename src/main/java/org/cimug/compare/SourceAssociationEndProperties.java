package org.cimug.compare;

import org.cimug.compare.app.PreProcessor;
import org.cimug.compare.uml1_3.AssociationEndType;
import org.cimug.compare.uml1_3.AssociationType;
import org.cimug.compare.uml1_3.ClassType;
import org.cimug.compare.uml1_3.TaggedValueType;

/**
 * Source End
 */
public class SourceAssociationEndProperties extends AbstractAssociationEndProperties {

	public SourceAssociationEndProperties(PreProcessor preProcessor, ClassType baselineClass,
			AssociationEndType baseline, AssociationType baselineParent, ClassType targetClass,
			AssociationEndType target, AssociationType targetParent) {
		super(preProcessor, baselineClass, baseline, baselineParent, targetClass, target, targetParent);
	}

	protected String getEndName() {
		return "ea_sourceName";
	}

	protected String getTheValue(TaggedValueType tv) {
		switch (tv.getTag())
			{
			case "lt":
				return tv.getTheValue().replaceFirst("\\+", "");
			default:
				return tv.getTheValue().replaceAll("\n", "");
			}

	}

	protected void initializeDeprecated(PreProcessor preProcessor, AssociationType baselineAssociation,
			AssociationType targetAssociation) {
		//
		this.baselineDeprecated = false;
		this.targetDeprecated = false;
		//
		if (baselineAssociation != null && targetAssociation != null) {
			String sourceXmiId = baselineAssociation.getSourceAssociationEnd().getType();

			ClassType baselineSourceEndClass = (preProcessor.getBaselineClassesXmiIds().containsKey(sourceXmiId)
					? preProcessor.getBaselineClassesXmiIds().get(sourceXmiId)
					: null);

			this.baselineDeprecated = (baselineSourceEndClass != null ? baselineSourceEndClass.isDeprecated() : false);

			sourceXmiId = targetAssociation.getSourceAssociationEnd().getType();
			ClassType targetSourceEndClass = (preProcessor.getTargetClassesXmiIds().containsKey(sourceXmiId)
					? preProcessor.getTargetClassesXmiIds().get(sourceXmiId)
					: preProcessor.getTargetNewClassesXmiIds().get(sourceXmiId));

			this.targetDeprecated = (targetSourceEndClass != null ? targetSourceEndClass.isDeprecated() : false);
		} else if (baselineAssociation != null) {
			String sourceXmiId = baselineAssociation.getSourceAssociationEnd().getType();

			ClassType baselineSourceEndClass = (preProcessor.getBaselineClassesXmiIds().containsKey(sourceXmiId)
					? preProcessor.getBaselineClassesXmiIds().get(sourceXmiId)
					: null);

			this.baselineDeprecated = (baselineSourceEndClass != null ? baselineSourceEndClass.isDeprecated() : false);
		} else if (targetAssociation != null) {
			String sourceXmiId = targetAssociation.getSourceAssociationEnd().getType();

			ClassType targetSourceEndClass = (preProcessor.getTargetClassesXmiIds().containsKey(sourceXmiId)
					? preProcessor.getTargetClassesXmiIds().get(sourceXmiId)
					: preProcessor.getTargetNewClassesXmiIds().get(sourceXmiId));

			this.targetDeprecated = (targetSourceEndClass != null ? targetSourceEndClass.isDeprecated() : false);
		}
	}

}