package org.cimug.compare;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cimug.compare.app.PreProcessor;
import org.cimug.compare.logs.Properties;
import org.cimug.compare.logs.Property;
import org.cimug.compare.uml1_3.AssociationEndType;
import org.cimug.compare.uml1_3.AssociationType;
import org.cimug.compare.uml1_3.ClassType;
import org.cimug.compare.uml1_3.ModelElementTaggedValue;
import org.cimug.compare.uml1_3.TaggedValueType;

public abstract class AbstractAssociationEndProperties {

	private static final String DEPRECATED = "«deprecated»";

	private Set<String> tagNames = new HashSet<String>();
	private Map<String, String> tagNamesMap = new HashMap<String, String>();

	protected boolean baselineDeprecated;
	protected boolean targetDeprecated;

	private ClassType baselineClass;
	private ClassType targetClass;

	private List<TaggedValueType> baselineTaggedValues;
	private List<TaggedValueType> targetTaggedValues;

	private Properties properties = new Properties(new ArrayList<Property>());

	public AbstractAssociationEndProperties(PreProcessor preProcessor, ClassType baselineClass, AssociationEndType baseline,
			AssociationType baselineParent, ClassType targetClass, AssociationEndType target,
			AssociationType targetParent) {
		super();
		//
		this.baselineClass = baselineClass;
		this.targetClass = targetClass;
		//
		initializeTagNamesMap();
		//
		initializeDeprecated(preProcessor, baselineParent, targetParent);
		//
		if (baseline != null) {
			ModelElementTaggedValue baselineElement = baseline.getModelElementTaggedValue();
			if (baselineElement != null) {
				if (this.baselineTaggedValues == null)
					this.baselineTaggedValues = new ArrayList<TaggedValueType>();
				this.baselineTaggedValues.addAll(baselineElement.getTaggedValues());
				baselineElement.getTaggedValues().forEach(taggedValue -> tagNames.add(taggedValue.getTag()));
			}
		}
		//
		if (baselineParent != null) {
			ModelElementTaggedValue baselineElement = baselineParent.getModelElementTaggedValue();
			if (baselineElement != null) {
				if (this.baselineTaggedValues == null)
					this.baselineTaggedValues = new ArrayList<TaggedValueType>();
				this.baselineTaggedValues.addAll(baselineElement.getTaggedValues());
				baselineElement.getTaggedValues().forEach(taggedValue -> tagNames.add(taggedValue.getTag()));
			}
		}
		//
		if (target != null) {
			ModelElementTaggedValue targetElement = target.getModelElementTaggedValue();
			if (targetElement != null) {
				if (this.targetTaggedValues == null)
					this.targetTaggedValues = new ArrayList<TaggedValueType>();
				this.targetTaggedValues.addAll(targetElement.getTaggedValues());
				targetElement.getTaggedValues().forEach(taggedValue -> tagNames.add(taggedValue.getTag()));
			}
		}
		//
		if (targetParent != null) {
			ModelElementTaggedValue targetElement = targetParent.getModelElementTaggedValue();
			if (targetElement != null) {
				if (this.targetTaggedValues == null)
					this.targetTaggedValues = new ArrayList<TaggedValueType>();
				this.targetTaggedValues.addAll(targetElement.getTaggedValues());
				targetElement.getTaggedValues().forEach(taggedValue -> tagNames.add(taggedValue.getTag()));
			}
		}
		//
		processDiffs(baseline, baselineParent, target, targetParent);
	}

	protected abstract void initializeDeprecated(PreProcessor preProcessor, AssociationType baselineAssociation,
			AssociationType targetAssociation);
	
	protected abstract String getEndName();

	protected void initializeTagNamesMap() {
		tagNamesMap.put(getEndName(), "End");
		tagNamesMap.put("stereotype", "Stereotype");
		tagNamesMap.put("description", "RoleNote");
		tagNamesMap.put("containment", "Containment");
	}

	protected void processDiffs(AssociationEndType baselineAssociationEnd, AssociationType baselineParent,
			AssociationEndType targetAssociationEnd, AssociationType targetParent) {
		Properties properties = new Properties(new ArrayList<Property>());

		properties.getProperty().add(new Property("Alias", null, null, (this.baselineTaggedValues == null
				? Status.ModelOnly.toString()
				: (this.targetTaggedValues == null ? Status.BaselineOnly.toString() : Status.Identical.toString()))));
		properties.getProperty().add(new Property("Constraint", null, null, (this.baselineTaggedValues == null
				? Status.ModelOnly.toString()
				: (this.targetTaggedValues == null ? Status.BaselineOnly.toString() : Status.Identical.toString()))));
		properties.getProperty().add(new Property("roleType", null, null, (this.baselineTaggedValues == null
				? Status.ModelOnly.toString()
				: (this.targetTaggedValues == null ? Status.BaselineOnly.toString() : Status.Identical.toString()))));
		properties.getProperty().add(new Property("Qualifier", null, null, (this.baselineTaggedValues == null
				? Status.ModelOnly.toString()
				: (this.targetTaggedValues == null ? Status.BaselineOnly.toString() : Status.Identical.toString()))));

		if (baselineAssociationEnd != null && targetAssociationEnd != null) {
			// Changed or Identical
			properties.getProperty().add(new Property("Ordering", //
					(baselineAssociationEnd.getIsOrdered() != null ? baselineAssociationEnd.getIsOrdered() : null), //
					(targetAssociationEnd.getIsOrdered() != null ? targetAssociationEnd.getIsOrdered() : null), //
					getStatus(baselineAssociationEnd.getIsOrdered(), targetAssociationEnd.getIsOrdered()) //
			));
			properties.getProperty().add(new Property("IsAggregation", //
					(baselineAssociationEnd.getAggregation() != null ? baselineAssociationEnd.getAggregation() : null), //
					(targetAssociationEnd.getAggregation() != null ? targetAssociationEnd.getAggregation() : null), //
					getStatus(baselineAssociationEnd.getAggregation(), targetAssociationEnd.getAggregation()) //
			));
			properties.getProperty().add(new Property("IsChangeable", //
					(baselineAssociationEnd.getChangeable() != null ? baselineAssociationEnd.getChangeable() : null), //
					(targetAssociationEnd.getChangeable() != null ? targetAssociationEnd.getChangeable() : null), //
					getStatus(baselineAssociationEnd.getChangeable(), targetAssociationEnd.getChangeable()) //
			));
			properties.getProperty().add(new Property("Scope", //
					(baselineAssociationEnd.getVisibility() != null ? baselineAssociationEnd.getVisibility() : null), //
					(targetAssociationEnd.getVisibility() != null ? targetAssociationEnd.getVisibility() : null), //
					getStatus(baselineAssociationEnd.getVisibility(), targetAssociationEnd.getVisibility()) //
			));
			properties.getProperty().add(new Property("TargetScope", //
					(baselineAssociationEnd.getTargetScope() != null ? baselineAssociationEnd.getTargetScope() : null), //
					(targetAssociationEnd.getTargetScope() != null ? targetAssociationEnd.getTargetScope() : null), //
					getStatus(baselineAssociationEnd.getTargetScope(), targetAssociationEnd.getTargetScope()) //
			));
			properties.getProperty().add(new Property("IsNavigable", //
					(baselineAssociationEnd.getIsNavigable() != null ? baselineAssociationEnd.getIsNavigable() : null), //
					(targetAssociationEnd.getIsNavigable() != null ? targetAssociationEnd.getIsNavigable() : null), //
					getStatus(baselineAssociationEnd.getIsNavigable(), targetAssociationEnd.getIsNavigable()) //
			));
			properties.getProperty().add(new Property("Role", //
					(baselineAssociationEnd.getName() != null ? baselineAssociationEnd.getName() : null), //
					(targetAssociationEnd.getName() != null ? targetAssociationEnd.getName() : null), //
					getStatus(baselineAssociationEnd.getName(), targetAssociationEnd.getName()) //
			));
			properties.getProperty().add(new Property("Cardinality", //
					(baselineAssociationEnd.getMultiplicity() != null ? baselineAssociationEnd.getMultiplicity()
							: null), //
					(targetAssociationEnd.getMultiplicity() != null ? targetAssociationEnd.getMultiplicity() : null), //
					getStatus(baselineAssociationEnd.getMultiplicity(), targetAssociationEnd.getMultiplicity()) //
			));
		} else if (baselineAssociationEnd != null) {
			// Baseline only
			properties.getProperty().add(new Property("Ordering",
					(baselineAssociationEnd.getIsOrdered() != null ? baselineAssociationEnd.getIsOrdered() : null), //
					null, // null model
					getStatus(baselineAssociationEnd.getIsOrdered(), null) //
			));
			properties.getProperty().add(new Property("IsAggregation",
					(baselineAssociationEnd.getAggregation() != null ? baselineAssociationEnd.getAggregation() : null), //
					null, // null model
					getStatus(baselineAssociationEnd.getAggregation(), null) //
			));
			properties.getProperty().add(new Property("IsChangeable",
					(baselineAssociationEnd.getChangeable() != null ? baselineAssociationEnd.getChangeable() : null), //
					null, // null model
					getStatus(baselineAssociationEnd.getChangeable(), null) //
			));
			properties.getProperty().add(new Property("Scope",
					(baselineAssociationEnd.getVisibility() != null ? baselineAssociationEnd.getVisibility() : null), //
					null, // null model
					getStatus(baselineAssociationEnd.getVisibility(), null) //
			));
			properties.getProperty().add(new Property("TargetScope",
					(baselineAssociationEnd.getTargetScope() != null ? baselineAssociationEnd.getTargetScope() : null), //
					null, // null model
					getStatus(baselineAssociationEnd.getTargetScope(), null) //
			));
			properties.getProperty().add(new Property("IsNavigable",
					(baselineAssociationEnd.getIsNavigable() != null ? baselineAssociationEnd.getIsNavigable() : null), //
					null, // null model
					getStatus(baselineAssociationEnd.getIsNavigable(), null) //
			));
			properties.getProperty()
					.add(new Property("Role",
							(baselineAssociationEnd.getName() != null ? baselineAssociationEnd.getName() : null), //
							null, // null model
							getStatus(baselineAssociationEnd.getName(), null) //
			));
			properties.getProperty()
					.add(new Property("Cardinality",
							(baselineAssociationEnd.getMultiplicity() != null ? baselineAssociationEnd.getMultiplicity()
									: null), //
							null, // null model
							getStatus(baselineAssociationEnd.getMultiplicity(), null) //
			));
		} else {
			// Model only
			properties.getProperty().add(new Property("Ordering", //
					null, // null baseline
					(targetAssociationEnd.getIsOrdered() != null ? targetAssociationEnd.getIsOrdered() : null), //
					getStatus(null, targetAssociationEnd.getIsOrdered()) //
			));
			properties.getProperty().add(new Property("IsAggregation", //
					null, // null baseline
					(targetAssociationEnd.getAggregation() != null ? targetAssociationEnd.getAggregation() : null), //
					getStatus(null, targetAssociationEnd.getAggregation()) //
			));
			properties.getProperty().add(new Property("IsChangeable", //
					null, // null baseline
					(targetAssociationEnd.getChangeable() != null ? targetAssociationEnd.getChangeable() : null), //
					getStatus(null, targetAssociationEnd.getChangeable()) //
			));
			properties.getProperty().add(new Property("Scope", //
					null, // null baseline
					(targetAssociationEnd.getVisibility() != null ? targetAssociationEnd.getVisibility() : null), //
					getStatus(null, targetAssociationEnd.getVisibility()) //
			));
			properties.getProperty().add(new Property("TargetScope", //
					null, // null baseline
					(targetAssociationEnd.getTargetScope() != null ? targetAssociationEnd.getTargetScope() : null), //
					getStatus(null, targetAssociationEnd.getTargetScope()) //
			));
			properties.getProperty().add(new Property("IsNavigable", //
					null, // null baseline
					(targetAssociationEnd.getIsNavigable() != null ? targetAssociationEnd.getIsNavigable() : null), //
					getStatus(null, targetAssociationEnd.getIsNavigable()) //
			));
			properties.getProperty().add(new Property("Role", //
					null, // null baseline
					(targetAssociationEnd.getName() != null ? targetAssociationEnd.getName() : null), //
					getStatus(null, targetAssociationEnd.getName()) //
			));
			properties.getProperty().add(new Property("Cardinality", //
					null, // null baseline
					(targetAssociationEnd.getMultiplicity() != null ? targetAssociationEnd.getMultiplicity() : null), //
					getStatus(null, targetAssociationEnd.getMultiplicity()) //
			));
		}

		for (String name : tagNamesMap.keySet()) {
			if (tagNames.contains(name)) {

				String baselineValue = getValue(name, baselineTaggedValues);
				String targetValue = getValue(name, targetTaggedValues);

				if (getEndName().equals(name)) {
					if (this.baselineDeprecated) {
						baselineValue = DEPRECATED + " " + baselineValue;
					}

					if (this.targetDeprecated) {
						targetValue = DEPRECATED + " " + targetValue;
					}
				}
				properties.getProperty()
						.add(new Property(tagNamesMap.get(name), baselineValue, targetValue, getStatus(name)));
			} else {
				properties.getProperty()
						.add(new Property(tagNamesMap.get(name), null, null,
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
		return this.properties;
	}

	private String getValue(String name, List<TaggedValueType> taggedValues) {
		if (taggedValues != null) {
			for (TaggedValueType tv : taggedValues) {
				if (tv.getTag().equals(name)) {
					return getTheValue(tv).replaceAll("\n", "");
				}
			}
		}
		return null;
	}

	/**
	 * Default implementation. May be overridden if custom conversions or processing
	 * is to be done on the value.
	 * 
	 * @param tv
	 * @return
	 */
	protected String getTheValue(TaggedValueType tv) {
		return tv.getTheValue().replaceAll("\n", "");
	}

	public Status getStatus() {
		if (baselineTaggedValues != null && targetTaggedValues == null) {
			return Status.BaselineOnly;
		} else if (baselineTaggedValues == null && targetTaggedValues != null) {
			return Status.ModelOnly;
		} else {
			for (Property property : properties.getProperty()) {
				if (!Status.Identical.toString().equals(property.getStatus())) {
					return Status.Changed;
				}
			}
			return Status.Identical;
		}
	}

	protected String getStatus(String name) {
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

	protected String getStatus(String baselineValue, String targetValue) {
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