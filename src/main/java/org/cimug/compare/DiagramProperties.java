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
import org.cimug.compare.uml1_3.Diagram;
import org.cimug.compare.uml1_3.ModelElementTaggedValue;
import org.cimug.compare.uml1_3.PackageType;
import org.cimug.compare.uml1_3.TaggedValueType;

public class DiagramProperties {

	private Set<String> tagNames = new HashSet<String>();
	private static final Map<String, String> TAG_NAME_MAP = new HashMap<String, String>();

	{
		TAG_NAME_MAP.put("author", "Author");
		TAG_NAME_MAP.put("documentation", "Notes");
		TAG_NAME_MAP.put("created_date", "CreatedDate");
		TAG_NAME_MAP.put("modified_date", "ModifiedDate");
		TAG_NAME_MAP.put("package", "Package");
	}

	private Diagram baselineDiagram;
	private Diagram targetDiagram;

	private List<TaggedValueType> baselineTaggedValues;
	private List<TaggedValueType> targetTaggedValues;

	private Properties properties;

	public DiagramProperties(Diagram baselineDiagram, PackageType baselineParentPackage, Diagram targetDiagram,
			PackageType targetParentPackage) {
		//
		this.baselineDiagram = baselineDiagram;
		this.targetDiagram = targetDiagram;
		//
		if (baselineDiagram != null) {
			ModelElementTaggedValue baselineElement = baselineDiagram.getModelElementTaggedValue();
			if (baselineElement != null && baselineElement.getTaggedValues() != null) {
				this.baselineTaggedValues = baselineElement.getTaggedValues();
				baselineElement.getTaggedValues().forEach(taggedValue -> tagNames.add(taggedValue.getTag()));
			}
		}
		//
		if (targetDiagram != null) {
			ModelElementTaggedValue targetElement = targetDiagram.getModelElementTaggedValue();
			if (targetElement != null && targetElement.getTaggedValues() != null
					&& targetElement.getTaggedValues() != null) {
				this.targetTaggedValues = targetElement.getTaggedValues();
				targetElement.getTaggedValues().forEach(taggedValue -> tagNames.add(taggedValue.getTag()));
			}
		}

		processDiffs(baselineDiagram, targetDiagram);
	}

	protected void processDiffs(Diagram baselineDiagram, Diagram targetDiagram) {
		Properties properties = new Properties(new ArrayList<Property>());

		if (baselineDiagram != null && targetDiagram != null) {
			// Changed or Identical
			properties.getProperty().add(new Property("Name", //
					(baselineDiagram.getName() != null ? baselineDiagram.getName() : null), //
					(targetDiagram.getName() != null ? targetDiagram.getName() : null), //
					getStatus(baselineDiagram.getName(), targetDiagram.getName()) //
			));
			properties.getProperty().add(new Property("DiagramType", //
					(baselineDiagram.getDiagramType() != null ? baselineDiagram.getDiagramType() : null), //
					(targetDiagram.getDiagramType() != null ? targetDiagram.getDiagramType() : null), //
					getStatus(baselineDiagram.getDiagramType(), targetDiagram.getDiagramType()) //
			));
		} else if (baselineDiagram != null) {
			// Baseline only
			properties.getProperty().add(new Property("Name", //
					(baselineDiagram.getName() != null ? baselineDiagram.getName() : null), //
					null, // null model
					getStatus(baselineDiagram.getName(), null) //
			));
			properties.getProperty()
					.add(new Property("DiagramType",
							(baselineDiagram.getDiagramType() != null ? baselineDiagram.getDiagramType() : null), //
							null, // null model
							getStatus(baselineDiagram.getDiagramType(), null) //
			));
		} else {
			// Model only
			properties.getProperty().add(new Property("Name", //
					null, // null baseline
					(targetDiagram.getName() != null ? targetDiagram.getName() : null), //
					getStatus(null, targetDiagram.getName()) //
			));
			properties.getProperty().add(new Property("DiagramType", //
					null, // null baseline
					(targetDiagram.getDiagramType() != null ? targetDiagram.getDiagramType() : null), //
					getStatus(null, targetDiagram.getDiagramType()) //
			));
		}

		for (String name : TAG_NAME_MAP.keySet()) {
			if (tagNames.contains(name)) {
				properties.getProperty().add(new Property(TAG_NAME_MAP.get(name), getValue(name, baselineTaggedValues),
						getValue(name, targetTaggedValues), getStatus(name)));
			} else {
				properties.getProperty()
						.add(new Property(TAG_NAME_MAP.get(name), null, null,(this.baselineTaggedValues == null ? Status.ModelOnly.toString() : (this.targetTaggedValues == null ? Status.BaselineOnly.toString() : Status.Identical.toString()))));
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
		if (this.baselineDiagram == null)
			return Status.ModelOnly;
		if (this.targetDiagram == null)
			return Status.BaselineOnly;
		if (this.baselineDiagram.getOwner() != null && this.targetDiagram.getOwner() != null
				&& !this.baselineDiagram.getOwner().equals(this.targetDiagram.getOwner()))
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