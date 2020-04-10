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
import org.cimug.compare.uml1_3.ModelElementTaggedValue;
import org.cimug.compare.uml1_3.PackageType;
import org.cimug.compare.uml1_3.TaggedValueType;

public class PackageProperties {

	private Set<String> tagNames = new HashSet<String>();
	private static final Map<String, String> TAG_NAME_MAP = new HashMap<String, String>();

	{
		TAG_NAME_MAP.put("documentation", "Notes");
		TAG_NAME_MAP.put("complexity", "Complexity");
		TAG_NAME_MAP.put("author", "Author");
		TAG_NAME_MAP.put("phase", "Phase");
		TAG_NAME_MAP.put("status", "Status");
		TAG_NAME_MAP.put("version", "Version");
	}

	private PackageType baselinePackage;
	private PackageType targetPackage;

	private List<TaggedValueType> baselineTaggedValues;
	private List<TaggedValueType> targetTaggedValues;

	private Properties properties;

	public PackageProperties(PackageType baselinePackage, PackageType targetPackage) {
		this.baselinePackage = baselinePackage;
		this.targetPackage = targetPackage;
		//
		if (this.baselinePackage != null) {
			ModelElementTaggedValue baselineElement = this.baselinePackage.getModelElementTaggedValue();
			if (baselineElement != null && baselineElement.getTaggedValues() != null) {
				this.baselineTaggedValues = baselineElement.getTaggedValues();
				baselineElement.getTaggedValues().forEach(taggedValue -> tagNames.add(taggedValue.getTag()));
			}
		}
		//
		if (this.targetPackage != null) {
			ModelElementTaggedValue targetElement = this.targetPackage.getModelElementTaggedValue();
			if (targetElement.getTaggedValues() != null && targetElement.getTaggedValues() != null) {
				this.targetTaggedValues = targetElement.getTaggedValues();
				targetElement.getTaggedValues().forEach(taggedValue -> tagNames.add(taggedValue.getTag()));
			}
		}

		processDiffs(baselinePackage, targetPackage);
	}

	protected void processDiffs(PackageType baselinePackage, PackageType targetPackage) {
		Properties properties = new Properties(new ArrayList<Property>());

		properties.getProperty().add(new Property("Alias", null, null, Status.Identical.toString()));
		properties.getProperty().add(new Property("Keywords", null, null, Status.Identical.toString()));
		properties.getProperty().add(new Property("Multiplicity", null, null, Status.Identical.toString()));
		properties.getProperty().add(new Property("Persistence", null, null, Status.Identical.toString()));
		properties.getProperty().add(new Property("Stereotype", null, null, Status.Identical.toString()));
		properties.getProperty().add(new Property("Classifier", null, null, Status.Identical.toString()));
		properties.getProperty().add(new Property("Visibility", null, null, Status.Identical.toString()));
		properties.getProperty().add(new Property("Concurrency", null, null, Status.Identical.toString()));
		properties.getProperty().add(new Property("Cardinality", null, null, Status.Identical.toString()));
		properties.getProperty().add(new Property("Style", null, null, Status.Identical.toString()));
		properties.getProperty().add(new Property("IsSpec", null, null, Status.Identical.toString()));
		properties.getProperty().add(new Property("Type", "Package", "Package", Status.Identical.toString()));

		if (baselinePackage != null && targetPackage != null) { // Changed or Identical
			// Changed or Identical
			properties.getProperty().add(new Property("Name", //
					(baselinePackage.getName() != null ? baselinePackage.getName() : null), //
					(targetPackage.getName() != null ? targetPackage.getName() : null), //
					getStatus(baselinePackage.getName(), targetPackage.getName()) //
			));
			properties.getProperty()
					.add(new Property("Abstract",
							(baselinePackage.getIsAbstract() != null ? baselinePackage.getIsAbstract() : null), //
							(targetPackage.getIsAbstract() != null ? targetPackage.getIsAbstract() : null), //
							getStatus(baselinePackage.getIsAbstract(), targetPackage.getIsAbstract()) //
			));
			properties.getProperty()
					.add(new Property("Scope",
							(baselinePackage.getVisibility() != null ? baselinePackage.getVisibility() : null), //
							(targetPackage.getVisibility() != null ? targetPackage.getVisibility() : null), //
							getStatus(baselinePackage.getVisibility(), targetPackage.getVisibility()) //
			));
			properties.getProperty()
					.add(new Property("IsLeaf",
							(baselinePackage.getIsLeaf() != null ? baselinePackage.getIsLeaf() : null), //
							(targetPackage.getIsLeaf() != null ? targetPackage.getIsLeaf() : null), //
							getStatus(baselinePackage.getIsLeaf(), targetPackage.getIsLeaf()) //
			));
			properties.getProperty()
					.add(new Property("IsRoot",
							(baselinePackage.getIsRoot() != null ? baselinePackage.getIsRoot() : null), //
							(targetPackage.getIsRoot() != null ? targetPackage.getIsRoot() : null), //
							getStatus(baselinePackage.getIsRoot(), targetPackage.getIsRoot()) //
			));
		} else if (baselinePackage != null) { // Baseline only
			// Baseline only
			properties.getProperty().add(new Property("Name", //
					(baselinePackage.getName() != null ? baselinePackage.getName() : null), //
					null, // null model
					getStatus(baselinePackage.getName(), null) //
			));
			properties.getProperty()
					.add(new Property("Abstract",
							(baselinePackage.getIsAbstract() != null ? baselinePackage.getIsAbstract() : null), //
							null, // null model
							getStatus(baselinePackage.getIsAbstract(), null) //
			));
			properties.getProperty()
					.add(new Property("Scope",
							(baselinePackage.getVisibility() != null ? baselinePackage.getVisibility() : null), //
							null, // null model
							getStatus(baselinePackage.getVisibility(), null) //
			));
			properties.getProperty()
					.add(new Property("IsLeaf",
							(baselinePackage.getIsLeaf() != null ? baselinePackage.getIsLeaf() : null), //
							null, // null model
							getStatus(baselinePackage.getIsLeaf(), null) //
			));
			properties.getProperty()
					.add(new Property("IsRoot",
							(baselinePackage.getIsRoot() != null ? baselinePackage.getIsRoot() : null), //
							null, // null model
							getStatus(baselinePackage.getIsRoot(), null) //
			));
		} else { // Model only
			properties.getProperty().add(new Property("Name", //
					null, // null baseline
					(targetPackage.getName() != null ? targetPackage.getName() : null), //
					getStatus(null, targetPackage.getName()) //
			));
			properties.getProperty().add(new Property("Abstract", //
					null, // null baseline
					(targetPackage.getIsAbstract() != null ? targetPackage.getIsAbstract() : null), //
					getStatus(null, targetPackage.getIsAbstract()) //
			));
			properties.getProperty().add(new Property("Scope", //
					null, // null baseline
					(targetPackage.getVisibility() != null ? targetPackage.getVisibility() : null), //
					getStatus(null, targetPackage.getVisibility()) //
			));
			properties.getProperty().add(new Property("IsLeaf", //
					null, // null baseline
					(targetPackage.getIsLeaf() != null ? targetPackage.getIsLeaf() : null), //
					getStatus(null, targetPackage.getIsLeaf()) //
			));
			properties.getProperty().add(new Property("IsRoot", //
					null, // null baseline
					(targetPackage.getIsRoot() != null ? targetPackage.getIsRoot() : null), //
					getStatus(null, targetPackage.getIsRoot()) //
			));
		}

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

	public Status getStatus() {
		if (this.baselinePackage == null)
			return Status.ModelOnly;
		if (this.targetPackage == null)
			return Status.BaselineOnly;
		if (this.baselinePackage.getParentPackageGUID() != null && this.targetPackage.getParentPackageGUID() != null
				&& !this.baselinePackage.getParentPackageGUID().equals(this.targetPackage.getParentPackageGUID()))
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