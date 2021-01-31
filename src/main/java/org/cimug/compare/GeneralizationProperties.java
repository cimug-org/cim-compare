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
import org.cimug.compare.uml1_3.GeneralizationType;
import org.cimug.compare.uml1_3.ModelElementTaggedValue;
import org.cimug.compare.uml1_3.TaggedValueType;

public class GeneralizationProperties {

	private Set<String> tagNames = new HashSet<String>();
	private static final Map<String, String> TAG_NAME_MAP = new HashMap<String, String>();

	{
		TAG_NAME_MAP.put("stereotype", "Stereotype");
		TAG_NAME_MAP.put("direction", "Direction");
		TAG_NAME_MAP.put("documentation", "Notes");
		TAG_NAME_MAP.put("ea_type", "Type");
	}

	private GeneralizationType baselineGeneralization;
	private GeneralizationType targetGeneralization;

	private SourceGeneralizationEndProperties sourcePropsProcessor;
	private DestinationGeneralizationEndProperties destinationPropsProcessor;
	
	private List<TaggedValueType> baselineTaggedValues;
	private List<TaggedValueType> targetTaggedValues;

	private Properties properties;

	public GeneralizationProperties(GeneralizationType baselineGeneralization,
			GeneralizationType targetGeneralization) {
		this.baselineGeneralization = baselineGeneralization;
		this.targetGeneralization = targetGeneralization;
		//
		this.sourcePropsProcessor = new SourceGeneralizationEndProperties(baselineGeneralization,
				targetGeneralization);
		this.destinationPropsProcessor = new DestinationGeneralizationEndProperties(baselineGeneralization, targetGeneralization);
		//
		if (this.baselineGeneralization != null) {
			ModelElementTaggedValue baselineElement = this.baselineGeneralization.getModelElementTaggedValue();
			if (baselineElement != null) {
				if (this.baselineTaggedValues == null)
					this.baselineTaggedValues = new ArrayList<TaggedValueType>();
				this.baselineTaggedValues.addAll(baselineElement.getTaggedValues());
				baselineElement.getTaggedValues().forEach(taggedValue -> tagNames.add(taggedValue.getTag()));
			}
		}
		//
		if (this.targetGeneralization != null) {
			ModelElementTaggedValue targetElement = this.targetGeneralization.getModelElementTaggedValue();
			if (targetElement != null) {
				if (this.targetTaggedValues == null)
					this.targetTaggedValues = new ArrayList<TaggedValueType>();
				this.targetTaggedValues.addAll(targetElement.getTaggedValues()); 
				targetElement.getTaggedValues().forEach(taggedValue -> tagNames.add(taggedValue.getTag()));
			}
		}

		processDiffs(baselineGeneralization, targetGeneralization);
	}
	
	public SourceGeneralizationEndProperties getSourcePropsProcessor() {
		return this.sourcePropsProcessor;
	}

	public DestinationGeneralizationEndProperties getDestinationPropsProcessor() {
		return this.destinationPropsProcessor;
	}

	protected void processDiffs(GeneralizationType baselineGeneralization, GeneralizationType targetGeneralization) {
		Properties properties = new Properties(new ArrayList<Property>());

		properties.getProperty().add(new Property("Alias", null, null, Status.Identical.toString()));
		properties.getProperty().add(new Property("Name", null, null, Status.Identical.toString()));
		
		for (String name : TAG_NAME_MAP.keySet()) {
			if (tagNames.contains(name)) {
				properties.getProperty().add(new Property(TAG_NAME_MAP.get(name), getValue(name, baselineTaggedValues),
						getValue(name, targetTaggedValues), getStatus(name)));
			} else {
				properties.getProperty()
						.add(new Property(TAG_NAME_MAP.get(name), null, null, Status.Identical.toString()));
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

	/**
	 * Note that for generalization that the status is always indicated as "Changed"
	 * if dealing with a new associations.
	 * 
	 * @param name
	 * @return
	 */
	private String getStatus(String name) {
		Status status = Status.Identical; // Assume a status of 'Identical' until it is determined otherwise...
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