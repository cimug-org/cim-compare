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

public abstract class AbstractGeneralizationEndProperties {

	private Set<String> tagNames = new HashSet<String>();
	protected Map<String, String> tagNamesMap = new HashMap<String, String>();

	private GeneralizationType baselineGeneralization;
	private GeneralizationType targetGeneralization;

	private List<TaggedValueType> baselineTaggedValues;
	private List<TaggedValueType> targetTaggedValues;

	private Properties properties;

	public AbstractGeneralizationEndProperties(GeneralizationType baselineGeneralization,
			GeneralizationType targetGeneralization) {
		//
		initializeTagNamesMap();
		//
		this.baselineGeneralization = baselineGeneralization;
		this.targetGeneralization = targetGeneralization;
		//
		if (this.baselineGeneralization != null) {
			ModelElementTaggedValue baselineElement = this.baselineGeneralization.getModelElementTaggedValue();
			if (baselineElement != null) {
				this.baselineTaggedValues = new ArrayList<TaggedValueType>();
				this.baselineTaggedValues.addAll(baselineElement.getTaggedValues());
				baselineElement.getTaggedValues().forEach(taggedValue -> tagNames.add(taggedValue.getTag()));
			}
		}
		//
		if (this.targetGeneralization != null) {
			ModelElementTaggedValue targetElement = this.targetGeneralization.getModelElementTaggedValue();
			if (targetElement != null) {
				this.targetTaggedValues = new ArrayList<TaggedValueType>();
				this.targetTaggedValues.addAll(targetElement.getTaggedValues());
				targetElement.getTaggedValues().forEach(taggedValue -> tagNames.add(taggedValue.getTag()));
			}
		}

		processDiffs(baselineGeneralization, targetGeneralization);
	}

	protected abstract void initializeTagNamesMap();
	
	protected abstract String getEndName();

	protected void processDiffs(GeneralizationType baselineGeneralization, GeneralizationType targetGeneralization) {
		Properties properties = new Properties(new ArrayList<Property>());

		properties.getProperty().add(new Property("Name", null, null, Status.Identical.toString()));
		properties.getProperty().add(new Property("Alias", null, null, Status.Identical.toString()));
		properties.getProperty().add(new Property("Cardinality", //
				(this.baselineTaggedValues != null && this.targetTaggedValues != null? "0" : (this.baselineTaggedValues != null ? "0" : null)), //
				(this.baselineTaggedValues != null && this.targetTaggedValues != null? "0" : (this.targetTaggedValues != null ? "0" : null)), //
				Status.Identical.toString()));

		for (String name : tagNamesMap.keySet()) {
			if (tagNames.contains(name)) {
				properties.getProperty().add(new Property(tagNamesMap.get(name), getValue(name, baselineTaggedValues),
						getValue(name, targetTaggedValues), getStatus(name)));
			} else {
				properties.getProperty()
						.add(new Property(tagNamesMap.get(name), null, null, Status.Identical.toString()));
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
					return tv.getTheValue();
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
		} else if (baselineTaggedValues == null && baselineTaggedValues != null) {
			status = Status.ModelOnly;
		} else {
			String baselineValue = getValue(name, baselineTaggedValues);
			String modelValue = getValue(name, targetTaggedValues);

			return getStatus(baselineValue, modelValue);
		}
		return status.toString();
	}
	
	public String getRoleName() {
		if (this.baselineTaggedValues == null)
			return getValue(getEndName(), targetTaggedValues);
		
		if (this.targetTaggedValues == null)
			return getValue(getEndName(), baselineTaggedValues);
		
		return getValue(getEndName(), targetTaggedValues);
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