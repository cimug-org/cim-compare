package org.cimug.compare;

import org.cimug.compare.uml1_3.AssociationEndType;
import org.cimug.compare.uml1_3.AssociationType;
import org.cimug.compare.uml1_3.TaggedValueType;

/**
 * Source End
 */
public class DestinationAssociationEndProperties extends AbstractAssociationEndProperties {

	public DestinationAssociationEndProperties(AssociationEndType baseline, AssociationType baselineParent,
			AssociationEndType target, AssociationType targetParent) {
		super(baseline, baselineParent, target, targetParent);
	}

	@Override
	protected void initializeTagNamesMap() {
		tagNamesMap.put("ea_targetName", "End");
		tagNamesMap.put("stereotype", "Stereotype");
		tagNamesMap.put("description", "RoleNote");
		tagNamesMap.put("containment", "Containment");
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

}