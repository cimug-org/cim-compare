package org.cimug.compare;

import org.cimug.compare.uml1_3.AssociationEndType;
import org.cimug.compare.uml1_3.AssociationType;
import org.cimug.compare.uml1_3.TaggedValueType;

/**
 * Source End
 */
public class SourceAssociationEndProperties extends AbstractAssociationEndProperties {

	public SourceAssociationEndProperties(AssociationEndType baseline, AssociationType baselineParent,
			AssociationEndType target, AssociationType targetParent) {
		super(baseline, baselineParent, target, targetParent);
	}

	@Override
	protected void initializeTagNamesMap() {
		tagNamesMap.put("lb", "Cardinality");
		tagNamesMap.put("ea_sourceName", "End");
		tagNamesMap.put("stereotype", "Stereotype");
		tagNamesMap.put("description", "RoleNote");
		tagNamesMap.put("containment", "Containment");
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

}