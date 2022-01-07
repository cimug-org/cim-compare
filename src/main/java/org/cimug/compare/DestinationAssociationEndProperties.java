package org.cimug.compare;

import org.cimug.compare.app.PreProcessor;
import org.cimug.compare.uml1_3.AssociationEndType;
import org.cimug.compare.uml1_3.AssociationType;
import org.cimug.compare.uml1_3.ClassType;
import org.cimug.compare.uml1_3.TaggedValueType;

/**
 * Source End
 */
public class DestinationAssociationEndProperties extends AbstractAssociationEndProperties {

	public DestinationAssociationEndProperties(PreProcessor preProcessor, ClassType baselineClass, AssociationEndType baseline,
			AssociationType baselineParent, ClassType targetClass, AssociationEndType target,
			AssociationType targetParent) {
		super(preProcessor, baselineClass, baseline, baselineParent, targetClass, target, targetParent);
	}

	protected String getEndName() {
		return "ea_targetName";
	}

	protected String getTheValue(TaggedValueType tv) {
		switch (tv.getTag())
			{
			case "rt":
				return tv.getTheValue().replaceFirst("\\+", "");
			default:
				return tv.getTheValue().replaceAll("\n", "");
			}
	}

	@Override
	protected void initializeDeprecated(PreProcessor preProcessor, AssociationType baselineAssociation,
			AssociationType targetAssociation) {
		//
		this.baselineDeprecated = false;
		this.targetDeprecated = false;
		//
		if (baselineAssociation != null && targetAssociation != null) {
			String destinationXmiId = baselineAssociation.getDestinationAssociationEnd().getType();
			//
			ClassType baselineDestinationEndClass = (preProcessor.getBaselineClassesXmiIds().containsKey(destinationXmiId)
					? preProcessor.getBaselineClassesXmiIds().get(destinationXmiId)
					: (preProcessor.getBaselineDeletedClassesXmiIds().containsKey(destinationXmiId)
							? preProcessor.getBaselineDeletedClassesXmiIds().get(destinationXmiId)
							: preProcessor.getBaselineMovedClassesXmiIds().get(destinationXmiId)));

			this.baselineDeprecated = (baselineDestinationEndClass != null ? baselineDestinationEndClass.isDeprecated() : false);
					
			destinationXmiId = targetAssociation.getDestinationAssociationEnd().getType();
			ClassType targetDestinationEndClass = (preProcessor.getTargetClassesXmiIds().containsKey(destinationXmiId)
							? preProcessor.getTargetClassesXmiIds().get(destinationXmiId)
							: preProcessor.getTargetNewClassesXmiIds().get(destinationXmiId));

			this.targetDeprecated = (targetDestinationEndClass != null ? targetDestinationEndClass.isDeprecated() : false);
		} else if (baselineAssociation != null) {
			String destinationXmiId = baselineAssociation.getDestinationAssociationEnd().getType();
			//
			ClassType baselineDestinationEndClass = (preProcessor.getBaselineClassesXmiIds().containsKey(destinationXmiId)
					? preProcessor.getBaselineClassesXmiIds().get(destinationXmiId)
					: (preProcessor.getBaselineDeletedClassesXmiIds().containsKey(destinationXmiId)
							? preProcessor.getBaselineDeletedClassesXmiIds().get(destinationXmiId)
							: preProcessor.getBaselineMovedClassesXmiIds().get(destinationXmiId)));

			this.baselineDeprecated = (baselineDestinationEndClass != null ? baselineDestinationEndClass.isDeprecated() : false);
		} else if (targetAssociation != null) {
			String destinationXmiId = targetAssociation.getDestinationAssociationEnd().getType();

			ClassType targetDestinationEndClass = (preProcessor.getTargetClassesXmiIds().containsKey(destinationXmiId)
					? preProcessor.getTargetClassesXmiIds().get(destinationXmiId)
					: preProcessor.getTargetNewClassesXmiIds().get(destinationXmiId));

			this.targetDeprecated = (targetDestinationEndClass != null ? targetDestinationEndClass.isDeprecated() : false);
		}
	}

}