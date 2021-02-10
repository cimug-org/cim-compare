package org.cimug.compare;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cimug.compare.logs.Properties;
import org.cimug.compare.logs.Property;
import org.cimug.compare.uml1_3.AssociationEndType;
import org.cimug.compare.uml1_3.AssociationType;
import org.cimug.compare.uml1_3.ModelElementTaggedValue;
import org.cimug.compare.uml1_3.TaggedValueType;

public class AssociationProperties {

	private Set<String> tagNames = new HashSet<String>();
	private static final Map<String, String> TAG_NAME_MAP = new HashMap<String, String>();

	{
		TAG_NAME_MAP.put("stereotype", "Stereotype");
		TAG_NAME_MAP.put("direction", "Direction");
		TAG_NAME_MAP.put("documentation", "Notes");
		TAG_NAME_MAP.put("ea_type", "Type");
	}

	private AssociationType baselineAssociation;
	private AssociationType targetAssociation;

	private AssociationEndType baselineSourceAssociationEnd;
	private AssociationEndType targetSourceAssociationEnd;
	private AssociationEndType baselineDestinationAssociationEnd;
	private AssociationEndType targetDestinationAssociationEnd;

	private SourceAssociationEndProperties sourcePropsProcessor;
	private DestinationAssociationEndProperties destinationPropsProcessor;

	private List<TaggedValueType> baselineTaggedValues;
	private List<TaggedValueType> targetTaggedValues;

	private Properties properties;

	public AssociationProperties(AssociationType baselineAssociation, AssociationType targetAssociation) {
		this.baselineAssociation = baselineAssociation;
		this.targetAssociation = targetAssociation;
		//
		this.baselineSourceAssociationEnd = (baselineAssociation != null ? baselineAssociation.getSourceAssociationEnd()
				: null);
		this.targetSourceAssociationEnd = (targetAssociation != null ? targetAssociation.getSourceAssociationEnd()
				: null);

		this.baselineDestinationAssociationEnd = (baselineAssociation != null
				? baselineAssociation.getDestinationAssociationEnd()
				: null);
		this.targetDestinationAssociationEnd = (targetAssociation != null
				? targetAssociation.getDestinationAssociationEnd()
				: null);

		this.sourcePropsProcessor = new SourceAssociationEndProperties(baselineSourceAssociationEnd,
				baselineAssociation, targetSourceAssociationEnd, targetAssociation);
		this.destinationPropsProcessor = new DestinationAssociationEndProperties(baselineDestinationAssociationEnd,
				baselineAssociation, targetDestinationAssociationEnd, targetAssociation);
		//
		if (this.baselineAssociation != null) {
			ModelElementTaggedValue baselineElement = this.baselineAssociation.getModelElementTaggedValue();
			if (baselineElement != null) {
				if (baselineTaggedValues == null)
					this.baselineTaggedValues = new ArrayList<TaggedValueType>();
				this.baselineTaggedValues.addAll(baselineElement.getTaggedValues());
				baselineElement.getTaggedValues().forEach(taggedValue -> tagNames.add(taggedValue.getTag()));
			}
		}
		//
		if (this.targetAssociation != null) {
			ModelElementTaggedValue targetElement = this.targetAssociation.getModelElementTaggedValue();
			if (targetElement != null) {
				if (targetTaggedValues == null)
					this.targetTaggedValues = new ArrayList<TaggedValueType>();
				this.targetTaggedValues.addAll(targetElement.getTaggedValues());
				targetElement.getTaggedValues().forEach(taggedValue -> tagNames.add(taggedValue.getTag()));
			}
		}

		processDiffs(baselineAssociation, targetAssociation);
	}

	public AssociationEndType getBaselineSourceAssociationEnd() {
		return baselineSourceAssociationEnd;
	}

	public AssociationEndType getTargetSourceAssociationEnd() {
		return this.targetSourceAssociationEnd;
	}

	public AssociationEndType getBaselineDestinationAssociationEnd() {
		return baselineDestinationAssociationEnd;
	}

	public AssociationEndType getTargetDestinationAssociationEnd() {
		return targetDestinationAssociationEnd;
	}

	public SourceAssociationEndProperties getSourcePropsProcessor() {
		return sourcePropsProcessor;
	}

	public DestinationAssociationEndProperties getDestinationPropsProcessor() {
		return destinationPropsProcessor;
	}

	protected void processDiffs(AssociationType baselineAssociation, AssociationType targetAssociation) {
		Properties properties = new Properties(new ArrayList<Property>());

		properties.getProperty().add(new Property("Alias", null, null, (this.baselineTaggedValues == null
				? Status.ModelOnly.toString()
				: (this.targetTaggedValues == null ? Status.BaselineOnly.toString() : Status.Identical.toString()))));
		properties.getProperty()
				.add(new Property("Source Linked Feature", null, null,
						(this.baselineTaggedValues == null ? Status.ModelOnly.toString()
								: (this.targetTaggedValues == null ? Status.BaselineOnly.toString()
										: Status.Identical.toString()))));
		properties.getProperty()
				.add(new Property("Target Linked Feature", null, null,
						(this.baselineTaggedValues == null ? Status.ModelOnly.toString()
								: (this.targetTaggedValues == null ? Status.BaselineOnly.toString()
										: Status.Identical.toString()))));

		if (baselineAssociation != null && targetAssociation != null) {
			// Changed or Identical
			properties.getProperty().add(new Property("IsLeaf", //
					(baselineAssociation.getIsLeaf() != null ? baselineAssociation.getIsLeaf() : null), //
					(targetAssociation.getIsLeaf() != null ? targetAssociation.getIsLeaf() : null), //
					getStatus(baselineAssociation.getIsLeaf(), targetAssociation.getIsLeaf()) //
			));
			properties.getProperty().add(new Property("IsRoot", //
					(baselineAssociation.getIsRoot() != null ? baselineAssociation.getIsRoot() : null), //
					(targetAssociation.getIsRoot() != null ? targetAssociation.getIsRoot() : null), //
					getStatus(baselineAssociation.getIsRoot(), targetAssociation.getIsRoot()) //
			));
		} else if (baselineAssociation != null) {
			// Baseline only
			properties.getProperty()
					.add(new Property("IsLeaf",
							(baselineAssociation.getIsLeaf() != null ? baselineAssociation.getIsLeaf() : null), //
							null, // null model
							getStatus(baselineAssociation.getIsLeaf(), null) //
			));
			properties.getProperty()
					.add(new Property("IsRoot",
							(baselineAssociation.getIsRoot() != null ? baselineAssociation.getIsRoot() : null), null, // null
																														// model
							getStatus(baselineAssociation.getIsRoot(), null) //
			));
		} else {
			// Model only
			properties.getProperty().add(new Property("IsLeaf", //
					null, // null baseline
					(targetAssociation.getIsLeaf() != null ? targetAssociation.getIsLeaf() : null), //
					getStatus(null, targetAssociation.getIsLeaf()) //
			));
			properties.getProperty().add(new Property("IsRoot", //
					null, // null baseline
					(targetAssociation.getIsRoot() != null ? targetAssociation.getIsRoot() : null), //
					getStatus(null, targetAssociation.getIsRoot()) //
			));
		}

		for (String name : TAG_NAME_MAP.keySet()) {
			if (tagNames.contains(name)) {
				properties.getProperty().add(new Property(TAG_NAME_MAP.get(name), getValue(name, baselineTaggedValues),
						getValue(name, targetTaggedValues), getStatus(name)));
			} else {
				properties.getProperty()
						.add(new Property(TAG_NAME_MAP.get(name), null, null,
								(this.baselineTaggedValues == null ? Status.ModelOnly.toString()
										: (this.targetTaggedValues == null ? Status.BaselineOnly.toString()
												: Status.Identical.toString()))));
			}
		}

		// Sort the properties by property name...
		Collections.sort(properties.getProperty(), new Comparator<Property>() {
			@Override
			public int compare(Property p1, Property p2) {
				return p1.getName().compareTo(p2.getName());
			}
		});

		this.properties = properties;
	}

	public Properties getProperties() {
		return properties;
	}

	public boolean isIdentical() {
		for (Property property : properties.getProperty()) {
			if (!Status.Identical.toString().equals(property.getStatus())) {
				return false;
			}
		}
		return true;
	}

	private String getValue(String name, List<TaggedValueType> taggedValues) {
		if (taggedValues != null) {
			for (TaggedValueType tv : taggedValues) {
				if (tv.getTag().equals(name)) {
					return tv.getTheValue().replaceAll("\n", "");
				}
			}
		}
		return null;
	}

	public Status getStatus() {
		if (this.baselineTaggedValues == null)
			return Status.ModelOnly;
		if (this.targetTaggedValues == null)
			return Status.BaselineOnly;
		for (Property property : properties.getProperty()) {
			if (!Status.Identical.toString().equals(property.getStatus())) {
				return Status.Changed;
			}
		}
		return Status.Identical;
	}

	private String getStatus(String name) {
		Status status = Status.Changed; // Assume a status of 'Changed' until it is determined otherwise...
		if (baselineTaggedValues != null && targetTaggedValues == null) {
			status = Status.BaselineOnly;
		} else if (baselineTaggedValues == null && targetTaggedValues != null) {
			status = Status.ModelOnly;
		} else {
			String baselineValue = getValue(name, baselineTaggedValues);
			String modelValue = getValue(name, targetTaggedValues);

			return getStatus(baselineValue, modelValue);
		}
		return status.toString();
	}

	private String getStatus(String baselineValue, String targetValue) {
		Status status = Status.Changed; // Assume a status of 'Changed' until it is determined otherwise...
		if (baselineValue != null && targetValue == null) {
			status = Status.BaselineOnly;
		} else if (baselineValue == null && targetValue != null) {
			status = Status.ModelOnly;
		} else if ((baselineValue == null && targetValue == null) || baselineValue.equals(targetValue)) {
			status = Status.Identical;
		}
		return status.toString();
	}

}