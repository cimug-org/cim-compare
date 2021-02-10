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
import org.cimug.compare.uml1_3.ClassType;
import org.cimug.compare.uml1_3.ModelElementTaggedValue;
import org.cimug.compare.uml1_3.PackageType;
import org.cimug.compare.uml1_3.TaggedValueType;

public class ClassProperties {

	private Set<String> tagNames = new HashSet<String>();
	private static final Map<String, String> TAG_NAME_MAP = new HashMap<String, String>();

	{
		TAG_NAME_MAP.put("alias", "Alias");
		TAG_NAME_MAP.put("documentation", "Notes");
		TAG_NAME_MAP.put("complexity", "Complexity");
		TAG_NAME_MAP.put("author", "Author");
		TAG_NAME_MAP.put("phase", "Phase");
		TAG_NAME_MAP.put("status", "Status");
		TAG_NAME_MAP.put("version", "Version");
		TAG_NAME_MAP.put("gentype", "GenType");
		TAG_NAME_MAP.put("package_name", "ParentPackage");
		TAG_NAME_MAP.put("ea_stype", "Type");
		TAG_NAME_MAP.put("isSpecification", "IsSpec");
		TAG_NAME_MAP.put("stereotype", "Stereotype");
	}

	private ClassType baselineClass;
	private ClassType targetClass;

	private List<TaggedValueType> baselineTaggedValues;
	private List<TaggedValueType> targetTaggedValues;

	private Properties properties;

	public ClassProperties(ClassType baselineClass, PackageType baselineParentPackage, ClassType targetClass,
			PackageType targetParentPackage) {
		this.baselineClass = baselineClass;
		this.targetClass = targetClass;
		//
		if (this.baselineClass != null) {
			ModelElementTaggedValue baselineElement = this.baselineClass.getModelElementTaggedValue();
			if (baselineElement != null && baselineElement.getTaggedValues() != null) {
				this.baselineTaggedValues = baselineElement.getTaggedValues();
				baselineElement.getTaggedValues().forEach(taggedValue -> tagNames.add(taggedValue.getTag()));
			}
		}
		//
		if (this.targetClass != null) {
			ModelElementTaggedValue targetElement = this.targetClass.getModelElementTaggedValue();
			if (targetElement != null && targetElement.getTaggedValues() != null
					&& targetElement.getTaggedValues() != null) {
				this.targetTaggedValues = targetElement.getTaggedValues();
				targetElement.getTaggedValues().forEach(taggedValue -> tagNames.add(taggedValue.getTag()));
			}
		}

		processDiffs(baselineClass, targetClass);
	}

	protected void processDiffs(ClassType baselineClass, ClassType targetClass) {
		Properties properties = new Properties(new ArrayList<Property>());

		properties.getProperty().add(new Property("Keywords", null, null, (this.baselineTaggedValues == null
				? Status.ModelOnly.toString()
				: (this.targetTaggedValues == null ? Status.BaselineOnly.toString() : Status.Identical.toString()))));
		properties.getProperty().add(new Property("GenFile", null, null, (this.baselineTaggedValues == null
				? Status.ModelOnly.toString()
				: (this.targetTaggedValues == null ? Status.BaselineOnly.toString() : Status.Identical.toString()))));
		properties.getProperty().add(new Property("Multiplicity", null, null, (this.baselineTaggedValues == null
				? Status.ModelOnly.toString()
				: (this.targetTaggedValues == null ? Status.BaselineOnly.toString() : Status.Identical.toString()))));
		properties.getProperty().add(new Property("Persistence", null, null, (this.baselineTaggedValues == null
				? Status.ModelOnly.toString()
				: (this.targetTaggedValues == null ? Status.BaselineOnly.toString() : Status.Identical.toString()))));
		properties.getProperty().add(new Property("Stereotype", null, null, (this.baselineTaggedValues == null
				? Status.ModelOnly.toString()
				: (this.targetTaggedValues == null ? Status.BaselineOnly.toString() : Status.Identical.toString()))));
		properties.getProperty().add(new Property("Classifier", null, null, (this.baselineTaggedValues == null
				? Status.ModelOnly.toString()
				: (this.targetTaggedValues == null ? Status.BaselineOnly.toString() : Status.Identical.toString()))));
		properties.getProperty().add(new Property("Visibility", null, null, (this.baselineTaggedValues == null
				? Status.ModelOnly.toString()
				: (this.targetTaggedValues == null ? Status.BaselineOnly.toString() : Status.Identical.toString()))));
		properties.getProperty().add(new Property("Concurrency", null, null, (this.baselineTaggedValues == null
				? Status.ModelOnly.toString()
				: (this.targetTaggedValues == null ? Status.BaselineOnly.toString() : Status.Identical.toString()))));
		properties.getProperty().add(new Property("Cardinality", null, null, (this.baselineTaggedValues == null
				? Status.ModelOnly.toString()
				: (this.targetTaggedValues == null ? Status.BaselineOnly.toString() : Status.Identical.toString()))));
		properties.getProperty().add(new Property("Style", null, null, (this.baselineTaggedValues == null
				? Status.ModelOnly.toString()
				: (this.targetTaggedValues == null ? Status.BaselineOnly.toString() : Status.Identical.toString()))));

		if (baselineClass != null && targetClass != null) {
			// Changed or Identical
			properties.getProperty().add(new Property("Name", //
					(baselineClass.getName() != null ? baselineClass.getName() : null), //
					(targetClass.getName() != null ? targetClass.getName() : null), //
					getStatus(baselineClass.getName(), targetClass.getName()) //
			));
			properties.getProperty().add(new Property("Abstract", //
					(baselineClass.getIsAbstract() != null ? baselineClass.getIsAbstract() : null), //
					(targetClass.getIsAbstract() != null ? targetClass.getIsAbstract() : null), //
					getStatus(baselineClass.getIsAbstract(), targetClass.getIsAbstract()) //
			));
			properties.getProperty().add(new Property("Scope", //
					(baselineClass.getVisibility() != null ? baselineClass.getVisibility() : null), //
					(targetClass.getVisibility() != null ? targetClass.getVisibility() : null), //
					getStatus(baselineClass.getVisibility(), targetClass.getVisibility()) //
			));
			properties.getProperty().add(new Property("IsLeaf", //
					(baselineClass.getIsLeaf() != null ? baselineClass.getIsLeaf() : null), //
					(targetClass.getIsLeaf() != null ? targetClass.getIsLeaf() : null), //
					getStatus(baselineClass.getIsLeaf(), targetClass.getIsLeaf()) //
			));
			properties.getProperty().add(new Property("IsRoot", //
					(baselineClass.getIsRoot() != null ? baselineClass.getIsRoot() : null), //
					(targetClass.getIsRoot() != null ? targetClass.getIsRoot() : null), //
					getStatus(baselineClass.getIsRoot(), targetClass.getIsRoot()) //
			));
		} else if (baselineClass != null) {
			// Baseline only
			properties.getProperty().add(new Property("Name", //
					(baselineClass.getName() != null ? baselineClass.getName() : null), //
					null, // null model
					getStatus(baselineClass.getName(), null) //
			));
			properties.getProperty()
					.add(new Property("Abstract",
							(baselineClass.getIsAbstract() != null ? baselineClass.getIsAbstract() : null), //
							null, // null model
							getStatus(baselineClass.getIsAbstract(), null) //
			));
			properties.getProperty()
					.add(new Property("Scope",
							(baselineClass.getVisibility() != null ? baselineClass.getVisibility() : null), //
							null, // null model
							getStatus(baselineClass.getVisibility(), null) //
			));
			properties.getProperty()
					.add(new Property("IsLeaf", (baselineClass.getIsLeaf() != null ? baselineClass.getIsLeaf() : null), //
							null, // null model
							getStatus(baselineClass.getIsLeaf(), null) //
			));
			properties.getProperty()
					.add(new Property("IsRoot", (baselineClass.getIsRoot() != null ? baselineClass.getIsRoot() : null),
							null, // null model
							getStatus(baselineClass.getIsRoot(), null) //
			));
		} else {
			// Model only
			properties.getProperty().add(new Property("Name", //
					null, // null baseline
					(targetClass.getName() != null ? targetClass.getName() : null), //
					getStatus(null, targetClass.getName()) //
			));
			properties.getProperty().add(new Property("Abstract", //
					null, // null baseline
					(targetClass.getIsAbstract() != null ? targetClass.getIsAbstract() : null), //
					getStatus(null, targetClass.getIsAbstract()) //
			));
			properties.getProperty().add(new Property("Scope", //
					null, // null baseline
					(targetClass.getVisibility() != null ? targetClass.getVisibility() : null), //
					getStatus(null, targetClass.getVisibility()) //
			));
			properties.getProperty().add(new Property("IsLeaf", //
					null, // null baseline
					(targetClass.getIsLeaf() != null ? targetClass.getIsLeaf() : null), //
					getStatus(null, targetClass.getIsLeaf()) //
			));
			properties.getProperty().add(new Property("IsRoot", //
					null, // null baseline
					(targetClass.getIsRoot() != null ? targetClass.getIsRoot() : null), //
					getStatus(null, targetClass.getIsRoot()) //
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

	public Status getStatus() {
		if (this.baselineClass == null)
			return Status.ModelOnly;
		if (this.targetClass == null)
			return Status.BaselineOnly;
		if (this.baselineClass.getParentPackageGUID() != null && this.targetClass.getParentPackageGUID() != null
				&& !this.baselineClass.getParentPackageGUID().equals(this.targetClass.getParentPackageGUID()))
			return Status.Moved;
		for (Property property : properties.getProperty()) {
			if (!Status.Identical.toString().equals(property.getStatus())) {
				return Status.Changed;
			}
		}
		return Status.Identical;
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