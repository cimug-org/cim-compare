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
import org.cimug.compare.uml1_3.AttributeType;
import org.cimug.compare.uml1_3.ModelElementTaggedValue;
import org.cimug.compare.uml1_3.TaggedValueType;

public class AttributeProperties {

	private static final String DEPRECATED = "«deprecated»";
	
	protected boolean baselineDeprecated;
	protected boolean targetDeprecated;
	
	private static final String IS_LITERAL = "IsLiteral=";
	private Set<String> tagNames = new HashSet<String>();

	private static final Map<String, String> TAG_NAME_MAP = new HashMap<String, String>();

	{
		TAG_NAME_MAP.put("duplicates", "AllowDuplicates");
		TAG_NAME_MAP.put("collection", "Collection");
		TAG_NAME_MAP.put("containment", "Containment");
		TAG_NAME_MAP.put("styleex", "IsLiteral");
		TAG_NAME_MAP.put("ordered", "IsOrdered");
		TAG_NAME_MAP.put("description", "Notes");
		TAG_NAME_MAP.put("length", "Length");
		TAG_NAME_MAP.put("lowerBound", "LowerBound");
		TAG_NAME_MAP.put("upperBound", "UpperBound");
		/**
		 * We are intentionally commenting out the "position" for the attribute as we do
		 * not want an attribute flagged as Change due to a change in position. Do not
		 * remove this comment or the code below...
		 */
		// TAG_NAME_MAP.put("position", "Position");
		// TAG_NAME_MAP.put("derived", "IsDerived");
		TAG_NAME_MAP.put("precision", "Precision");
		TAG_NAME_MAP.put("scale", "Scale");
		TAG_NAME_MAP.put("static", "Static");
		TAG_NAME_MAP.put("type", "Type");
	}

	private AttributeType baselineAttribute;
	private AttributeType targetAttribute;

	private List<TaggedValueType> baselineTaggedValues;
	private List<TaggedValueType> targetTaggedValues;

	private Properties properties;

	public AttributeProperties(AttributeType baselineAttribute, AttributeType targetAttribute) {
		this.baselineAttribute = baselineAttribute;
		this.targetAttribute = targetAttribute;
		//
		if (this.baselineAttribute != null) {
			
			this.baselineDeprecated = (this.baselineAttribute.isDeprecated());
			
			ModelElementTaggedValue baselineElement = this.baselineAttribute.getModelElementTaggedValue();
			if (baselineElement != null) {
				this.baselineTaggedValues = baselineElement.getTaggedValues();
				baselineElement.getTaggedValues().forEach(taggedValue -> tagNames.add(taggedValue.getTag()));
			}
		}
		//
		if (this.targetAttribute != null) {
			
			this.targetDeprecated = (this.targetAttribute.isDeprecated());
			
			ModelElementTaggedValue targetElement = this.targetAttribute.getModelElementTaggedValue();
			if (targetElement != null) {
				this.targetTaggedValues = targetElement.getTaggedValues();
				targetElement.getTaggedValues().forEach(taggedValue -> tagNames.add(taggedValue.getTag()));
			}
		}

		processDiffs(baselineAttribute, targetAttribute);
	}

	protected void processDiffs(AttributeType baselineAttribute, AttributeType targetAttribute) {
		Properties properties = new Properties(new ArrayList<Property>());

		properties.getProperty().add(new Property("Alias", null, null, (this.baselineTaggedValues == null
				? Status.ModelOnly.toString()
				: (this.targetTaggedValues == null ? Status.BaselineOnly.toString() : Status.Identical.toString()))));
		properties.getProperty().add(new Property("Qualifier", null, null, (this.baselineTaggedValues == null
				? Status.ModelOnly.toString()
				: (this.targetTaggedValues == null ? Status.BaselineOnly.toString() : Status.Identical.toString()))));
		properties.getProperty().add(new Property("Container", null, null, (this.baselineTaggedValues == null
				? Status.ModelOnly.toString()
				: (this.targetTaggedValues == null ? Status.BaselineOnly.toString() : Status.Identical.toString()))));

		if (baselineAttribute != null && targetAttribute != null) {
			// Changed or Identical
			String baselineAttributeName = (this.baselineAttribute.isDeprecated() ? DEPRECATED + " " : "") + this.baselineAttribute.getName();
			String targetAttributeName = (this.targetAttribute.isDeprecated() ? DEPRECATED + " " : "") + this.targetAttribute.getName();
			
			properties.getProperty().add(new Property("Name", //
					(baselineAttributeName != null ? baselineAttributeName : null), //
					(targetAttributeName != null ? targetAttributeName : null), //
					getStatus(baselineAttributeName, targetAttributeName) //
			));
			properties.getProperty().add(new Property("Scope", //
					(baselineAttribute.getVisibility() != null ? baselineAttribute.getVisibility() : null), //
					(targetAttribute.getVisibility() != null ? targetAttribute.getVisibility() : null), //
					getStatus(baselineAttribute.getVisibility(), targetAttribute.getVisibility()) //
			));
			/**
			 * We are intentionally commenting out the "Classifier" for the attribute as we
			 * do not want an attribute flagged as 'Changed' due to a change in the value of
			 * the classifier. Do not remove this comment or the code below...
			 */
			/*
			properties.getProperty().add(new Property("Classifier", //
					(baselineAttribute.getStructuralFeatureType() != null
							&& baselineAttribute.getStructuralFeatureType().getClassifier() != null
									? baselineAttribute.getStructuralFeatureType().getClassifier().getXmiIdref()
									: null), //
					(targetAttribute.getStructuralFeatureType() != null
							&& targetAttribute.getStructuralFeatureType().getClassifier() != null
									? targetAttribute.getStructuralFeatureType().getClassifier().getXmiIdref()
									: null), //
					getStatus(
							(baselineAttribute.getStructuralFeatureType() != null
									&& baselineAttribute.getStructuralFeatureType().getClassifier() != null
											? baselineAttribute.getStructuralFeatureType().getClassifier().getXmiIdref()
											: null),
							(targetAttribute.getStructuralFeatureType() != null
									&& targetAttribute.getStructuralFeatureType().getClassifier() != null
											? targetAttribute.getStructuralFeatureType().getClassifier().getXmiIdref()
											: null)) //
			));
			*/
			properties.getProperty().add(new Property("Default", //
					(baselineAttribute.getAttributeInitialValue() != null
							&& baselineAttribute.getAttributeInitialValue().getExpression() != null
									? baselineAttribute.getAttributeInitialValue().getExpression().getBody()
									: null), //
					(targetAttribute.getAttributeInitialValue() != null
							&& targetAttribute.getAttributeInitialValue().getExpression() != null
									? targetAttribute.getAttributeInitialValue().getExpression().getBody()
									: null), //
					getStatus(
							(baselineAttribute.getAttributeInitialValue() != null
									&& baselineAttribute.getAttributeInitialValue().getExpression() != null
											? baselineAttribute.getAttributeInitialValue().getExpression().getBody()
											: null),
							(targetAttribute.getAttributeInitialValue() != null
									&& targetAttribute.getAttributeInitialValue().getExpression() != null
											? targetAttribute.getAttributeInitialValue().getExpression().getBody()
											: null)) //
			));
			properties.getProperty().add(new Property("Const", //
					(baselineAttribute.getAttributeInitialValue() != null
							&& baselineAttribute.getAttributeInitialValue().getExpression() != null
							&& baselineAttribute.getAttributeInitialValue().getExpression().getBody() != null ? "true"
									: "false"), //
					(targetAttribute.getAttributeInitialValue() != null
							&& targetAttribute.getAttributeInitialValue().getExpression() != null
							&& targetAttribute.getAttributeInitialValue().getExpression().getBody() != null ? "true"
									: "false"), //
					getStatus(
							(baselineAttribute.getAttributeInitialValue() != null
									&& baselineAttribute.getAttributeInitialValue().getExpression() != null
									&& baselineAttribute.getAttributeInitialValue().getExpression().getBody() != null
											? "true"
											: "false"),
							(targetAttribute.getAttributeInitialValue() != null
									&& targetAttribute.getAttributeInitialValue().getExpression() != null
									&& targetAttribute.getAttributeInitialValue().getExpression().getBody() != null
											? "true"
											: "false")) //
			));
			properties.getProperty().add(new Property("Stereotype", //
					(baselineAttribute.getModelElementStereotype() != null
							&& baselineAttribute.getModelElementStereotype().getStereotype() != null
							&& baselineAttribute.getModelElementStereotype().getStereotype().getName() != null
									? baselineAttribute.getModelElementStereotype().getStereotype().getName()
									: null), //
					(targetAttribute.getModelElementStereotype() != null
							&& targetAttribute.getModelElementStereotype().getStereotype() != null
							&& targetAttribute.getModelElementStereotype().getStereotype().getName() != null
									? targetAttribute.getModelElementStereotype().getStereotype().getName()
									: null), //
					getStatus(
							(baselineAttribute.getModelElementStereotype() != null
									&& baselineAttribute.getModelElementStereotype().getStereotype() != null
									&& baselineAttribute.getModelElementStereotype().getStereotype().getName() != null
											? baselineAttribute.getModelElementStereotype().getStereotype().getName()
											: null),
							(targetAttribute.getModelElementStereotype() != null
									&& targetAttribute.getModelElementStereotype().getStereotype() != null
									&& targetAttribute.getModelElementStereotype().getStereotype().getName() != null
											? targetAttribute.getModelElementStereotype().getStereotype().getName()
											: null)) //
			));
		} else if (baselineAttribute != null) {
			// Baseline only
			String baselineAttributeName = (this.baselineAttribute.isDeprecated() ? DEPRECATED + " " : "") + this.baselineAttribute.getName();
			
			properties.getProperty().add(new Property("Name", //
					(baselineAttributeName != null ? baselineAttributeName : null), //
					null, // null model
					getStatus(baselineAttributeName, null) //
			));
			properties.getProperty().add(new Property("Scope", //
					(baselineAttribute.getVisibility() != null ? baselineAttribute.getVisibility() : null), //
					null, // null model
					getStatus(baselineAttribute.getVisibility(), null) //
			));
			/**
			 * We are intentionally commenting out the "Classifier" for the attribute as we
			 * do not want an attribute flagged as 'Changed' due to a change in the value of
			 * the classifier. Do not remove this comment or the code below...
			 */
			/*
			properties.getProperty().add(new Property("Classifier", //
					(baselineAttribute.getStructuralFeatureType() != null
							&& baselineAttribute.getStructuralFeatureType().getClassifier() != null
									? baselineAttribute.getStructuralFeatureType().getClassifier().getXmiIdref()
									: null), //
					null, // null model
					getStatus((baselineAttribute.getStructuralFeatureType() != null
							&& baselineAttribute.getStructuralFeatureType().getClassifier() != null
									? baselineAttribute.getStructuralFeatureType().getClassifier().getXmiIdref()
									: null),
							null) //
			));
			*/
			properties.getProperty().add(new Property("Default", //
					(baselineAttribute.getAttributeInitialValue() != null
							&& baselineAttribute.getAttributeInitialValue().getExpression() != null
									? baselineAttribute.getAttributeInitialValue().getExpression().getBody()
									: null), //
					null, // null model
					getStatus((baselineAttribute.getAttributeInitialValue() != null
							&& baselineAttribute.getAttributeInitialValue().getExpression() != null
									? baselineAttribute.getAttributeInitialValue().getExpression().getBody()
									: null),
							null) //
			));
			properties.getProperty().add(new Property("Const", //
					(baselineAttribute.getAttributeInitialValue() != null
							&& baselineAttribute.getAttributeInitialValue().getExpression() != null
							&& baselineAttribute.getAttributeInitialValue().getExpression().getBody() != null ? "true"
									: "false"), //
					null, // null model
					getStatus((baselineAttribute.getAttributeInitialValue() != null
							&& baselineAttribute.getAttributeInitialValue().getExpression() != null
							&& baselineAttribute.getAttributeInitialValue().getExpression().getBody() != null ? "true"
									: "false"),
							null) //
			));
			properties.getProperty().add(new Property("Stereotype", //
					(baselineAttribute.getModelElementStereotype() != null
							&& baselineAttribute.getModelElementStereotype().getStereotype() != null
							&& baselineAttribute.getModelElementStereotype().getStereotype().getName() != null
									? baselineAttribute.getModelElementStereotype().getStereotype().getName()
									: null), //
					null, // null model
					getStatus((baselineAttribute.getModelElementStereotype() != null
							&& baselineAttribute.getModelElementStereotype().getStereotype() != null
							&& baselineAttribute.getModelElementStereotype().getStereotype().getName() != null
									? baselineAttribute.getModelElementStereotype().getStereotype().getName()
									: null),
							null) //
			));
		} else {
			// Model only
			String targetAttributeName = (this.targetAttribute.isDeprecated() ? DEPRECATED + " " : "") + this.targetAttribute.getName();

			properties.getProperty().add(new Property("Name", //
					null, // null baseline
					(targetAttributeName != null ? targetAttributeName : null), //
					getStatus(null, targetAttributeName) //
			));
			properties.getProperty().add(new Property("Scope", //
					null, // null baseline
					(targetAttribute.getVisibility() != null ? targetAttribute.getVisibility() : null), //
					getStatus(null, targetAttribute.getVisibility()) //
			));
			/**
			 * We are intentionally commenting out the "Classifier" for the attribute as we
			 * do not want an attribute flagged as 'Changed' due to a change in the value of
			 * the classifier. Do not remove this comment or the code below...
			 */
			/*
			properties.getProperty().add(new Property("Classifier", //
					null, // null baseline
					(targetAttribute.getStructuralFeatureType() != null
							&& targetAttribute.getStructuralFeatureType().getClassifier() != null
									? targetAttribute.getStructuralFeatureType().getClassifier().getXmiIdref()
									: null), //
					getStatus(null,
							(targetAttribute.getStructuralFeatureType() != null
									&& targetAttribute.getStructuralFeatureType().getClassifier() != null
											? targetAttribute.getStructuralFeatureType().getClassifier().getXmiIdref()
											: null)) //
			));
			*/
			properties.getProperty().add(new Property("Default", //
					null, // null baseline
					(targetAttribute.getAttributeInitialValue() != null
							&& targetAttribute.getAttributeInitialValue().getExpression() != null
									? targetAttribute.getAttributeInitialValue().getExpression().getBody()
									: null), //
					getStatus(null,
							(targetAttribute.getAttributeInitialValue() != null
									&& targetAttribute.getAttributeInitialValue().getExpression() != null
											? targetAttribute.getAttributeInitialValue().getExpression().getBody()
											: null)) //
			));
			properties.getProperty().add(new Property("Const", //
					null, // null baseline
					(targetAttribute.getAttributeInitialValue() != null
							&& targetAttribute.getAttributeInitialValue().getExpression() != null
							&& targetAttribute.getAttributeInitialValue().getExpression().getBody() != null ? "true"
									: "false"), //
					getStatus(null,
							(targetAttribute.getAttributeInitialValue() != null
									&& targetAttribute.getAttributeInitialValue().getExpression() != null
									&& targetAttribute.getAttributeInitialValue().getExpression().getBody() != null
											? "true"
											: "false")) //
			));
			properties.getProperty().add(new Property("Stereotype", //
					null, // null baseline
					(targetAttribute.getModelElementStereotype() != null
							&& targetAttribute.getModelElementStereotype().getStereotype() != null
							&& targetAttribute.getModelElementStereotype().getStereotype().getName() != null
									? targetAttribute.getModelElementStereotype().getStereotype().getName()
									: null), //
					getStatus(null,
							(targetAttribute.getModelElementStereotype() != null
									&& targetAttribute.getModelElementStereotype().getStereotype() != null
									&& targetAttribute.getModelElementStereotype().getStereotype().getName() != null
											? targetAttribute.getModelElementStereotype().getStereotype().getName()
											: null)) //
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
					switch (name)
						{
						case "derived":
						case "ordered":
						case "duplicates":
						case "static":
							return (!"0".equals(tv.getTheValue()) ? "true" : "false");
						case "styleex":
							if (tv.getTheValue() != null && tv.getTheValue().contains(IS_LITERAL)) {
								String content = tv.getTheValue();
								int firstIndex = content.indexOf(IS_LITERAL);
								int lastIndex = ((content.indexOf(";", firstIndex) > -1)
										? content.indexOf(";", firstIndex)
										: content.length());
								String theValue = content.substring(firstIndex + IS_LITERAL.length(), lastIndex);
								return (!"0".equals(theValue) ? "true" : "false");
							}
							return null;
						default:
							return tv.getTheValue().replaceAll("\n", "");
						}
				}
			}
		}
		return null;
	}

	public Status getStatus() {
		if (this.baselineAttribute == null)
			return Status.ModelOnly;
		if (this.targetAttribute == null)
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
		if (targetAttribute == null) {
			status = Status.BaselineOnly;
		} else if (baselineAttribute == null) {
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
		if (targetAttribute == null) {
			status = Status.BaselineOnly;
		} else if (baselineAttribute == null) {
			status = Status.ModelOnly;
		} else if ((baselineValue != null && targetValue != null) && (baselineValue.equals(targetValue))) {
			status = Status.Identical;
		} else if (baselineValue == null && targetValue == null) {
			status = Status.Identical;
		}
		return status.toString();
	}

}