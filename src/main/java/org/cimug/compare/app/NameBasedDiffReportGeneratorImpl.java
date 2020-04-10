package org.cimug.compare.app;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.cimug.compare.AssociationProperties;
import org.cimug.compare.AttributeProperties;
import org.cimug.compare.ClassProperties;
import org.cimug.compare.DestinationAssociationEndProperties;
import org.cimug.compare.DestinationGeneralizationEndProperties;
import org.cimug.compare.DiffUtils;
import org.cimug.compare.GeneralizationProperties;
import org.cimug.compare.PackageProperties;
import org.cimug.compare.SourceAssociationEndProperties;
import org.cimug.compare.SourceGeneralizationEndProperties;
import org.cimug.compare.Status;
import org.cimug.compare.logs.CompareItem;
import org.cimug.compare.logs.ComparePackage;
import org.cimug.compare.logs.CompareResults;
import org.cimug.compare.logs.EACompareLog;
import org.cimug.compare.logs.Properties;
import org.cimug.compare.uml1_3.AssociationEndType;
import org.cimug.compare.uml1_3.AssociationType;
import org.cimug.compare.uml1_3.AttributeType;
import org.cimug.compare.uml1_3.ClassType;
import org.cimug.compare.uml1_3.GeneralizationType;
import org.cimug.compare.uml1_3.Model;
import org.cimug.compare.uml1_3.PackageType;

class NameBasedDiffReportGeneratorImpl implements DiffReportGenerator {

	private Model baselineModel;
	private Model targetModel;
	private PreProcessor preProcessor;
	private File outputFile;

	public NameBasedDiffReportGeneratorImpl(Model baselineModel, Model targetModel, File outputFile)
			throws JAXBException {
		this.baselineModel = baselineModel;
		this.targetModel = targetModel;
		this.preProcessor = new PreProcessor(baselineModel, targetModel);
		this.outputFile = outputFile;
	}

	@Override
	public void processDiffReport() {
		try {
			EACompareLog compareLog = processDiffReport(baselineModel, targetModel);
			writeCompareLogFile(compareLog);
		} catch (FileNotFoundException | JAXBException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private void writeCompareLogFile(EACompareLog compareLog) throws JAXBException, FileNotFoundException {
		JAXBContext jaxbContext = JAXBContext.newInstance(EACompareLog.class);
		Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
		jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		if (this.outputFile != null) {
			jaxbMarshaller.marshal(compareLog, new PrintWriter(this.outputFile));
		} else {
			jaxbMarshaller.marshal(compareLog, new PrintWriter(System.out));
		}
	}

	private EACompareLog processDiffReport(Model baselineModel, Model targetModel) {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd H:mm:ss");
		String dateTime = formatter.format(new Date());

		CompareResults compareResults = parseModelRootPackage(baselineModel, targetModel);

		ComparePackage comparePackage = new ComparePackage(compareResults, targetModel.getName(),
				DiffUtils.convertRootModelXmiIdToEAGUID(targetModel.getXmiId()), "1", "system", dateTime);

		EACompareLog compareLog = new EACompareLog(comparePackage);
		return compareLog;
	}

	private CompareResults parseModelRootPackage(Model baselineModel, Model targetModel) {

		boolean hasChanges = true;

		Status rootPackageStatus = (!baselineModel.getKey().equals(targetModel.getKey()) ? Status.Changed
				: Status.Identical);

		CompareItem theRootPackage = new CompareItem(null, targetModel.getName(), "Package", targetModel.getKey(),
				rootPackageStatus.toString());

		/**
		 * Process child packages...
		 */
		Set<String> sortedPackageKeys = new TreeSet<String>(new org.cimug.compare.StringComparator());

		List<PackageType> baselineChildPackages = baselineModel.getPackages();
		baselineChildPackages.forEach(aPackage -> sortedPackageKeys.add(aPackage.getKey()));

		List<PackageType> targetChildPackages = targetModel.getPackages();
		targetChildPackages.forEach(aPackage -> sortedPackageKeys.add(aPackage.getKey()));

		for (String key : sortedPackageKeys) {
			PackageType baselineChildPackage = baselineModel.getPackage(key);
			PackageType targetChildPackage = targetModel.getPackage(key);

			if (baselineChildPackage == null) {
				theRootPackage.getCompareItem().add(parsePackage(null, targetChildPackage));
			} else if (targetChildPackage == null) {
				theRootPackage.getCompareItem().add(parsePackage(baselineChildPackage, null));
			} else {
				theRootPackage.getCompareItem().add(parsePackage(baselineChildPackage, targetChildPackage));
			}
		}

		/**
		 * Process child classes...
		 * 
		 * Set<String> sortedClassKeys = new TreeSet<String>(new
		 * org.cimug.compare.StringComparator());
		 * 
		 * List<Class> baselineChildClasses = baselineModel.getClasses();
		 * baselineChildClasses.forEach(aClass -> sortedClassKeys.add(aClass.getKey()));
		 * 
		 * List<Class> targetChildClasses = targetModel.getClasses();
		 * targetChildClasses.forEach(aClass -> sortedClassKeys.add(aClass.getKey()));
		 * 
		 * for (String key : sortedClassKeys) { Class baselineChildClass =
		 * baselineModel.getClass(key); Class targetChildClass =
		 * targetModel.getClass(key);
		 * 
		 * if (baselineChildClass == null) { Package targetParentPackage =
		 * preProcessor.getTargetPackagesGUIDs()
		 * .get(targetChildClass.getParentPackageGUID());
		 * theRootPackage.getCompareItem().add(parseClass(null, null, targetChildClass,
		 * targetParentPackage)); } else if (targetChildClass == null) { Package
		 * baselineParentPackage = preProcessor.getBaselinePackagesGUIDs()
		 * .get(baselineChildClass.getParentPackageGUID());
		 * theRootPackage.getCompareItem().add(parseClass(baselineChildClass,
		 * baselineParentPackage, null, null)); } else { Package baselineParentPackage =
		 * preProcessor.getBaselinePackagesGUIDs()
		 * .get(baselineChildClass.getParentPackageGUID()); Package targetParentPackage
		 * = preProcessor.getTargetPackagesGUIDs()
		 * .get(targetChildClass.getParentPackageGUID());
		 * theRootPackage.getCompareItem().add( parseClass(baselineChildClass,
		 * baselineParentPackage, targetChildClass, targetParentPackage)); } }
		 * 
		 */

		CompareResults compareResults = new CompareResults(theRootPackage, hasChanges);
		return compareResults;
	}

	private CompareItem parsePackage(PackageType baselinePackage, PackageType targetPackage) {

		CompareItem thePackage;

		PackageProperties processor = new PackageProperties(baselinePackage, targetPackage);
		Properties properties = processor.getProperties();

		// It is assumed that both packages will never be both null...
		if (baselinePackage == null) {
			// Only model package exists...
			thePackage = new CompareItem(properties, targetPackage.getName(), "Package", targetPackage.getKey(),
					Status.ModelOnly.toString());

			for (PackageType aTargetPackage : targetPackage.getPackages()) {
				thePackage.getCompareItem().add(parsePackage(null, aTargetPackage));
			}

			/**
			 * for (org.cimug.compare.xmi1_1.Class aTargetClass : targetPackage.getClasses()) {
			 * Package parentPackage =
			 * preProcessor.getTargetPackagesGUIDs().get(aTargetClass.getParentPackageGUID());
			 * thePackage.getCompareItem().add(parseClass(null, null, aTargetClass,
			 * parentPackage)); }
			 */
		} else if (targetPackage == null) {
			// Only baseline package exists...
			thePackage = new CompareItem(properties, baselinePackage.getName(), "Package", baselinePackage.getKey(),
					Status.BaselineOnly.toString());

			for (PackageType aBaselinePackage : baselinePackage.getPackages()) {
				thePackage.getCompareItem().add(parsePackage(aBaselinePackage, null));
			}

			/**
			 * for (org.cimug.compare.xmi1_1.Class aBaselineClass : baselinePackage.getClasses())
			 * { boolean isIn =
			 * preProcessor.getAllPackagesGUIDs().contains(aBaselineClass.getParentPackageGUID());
			 * boolean isInDeletedPackages =
			 * preProcessor.getBaselineDeletedPackages().containsKey(aBaselineClass.getParentPackageGUID());
			 * boolean isInDeletedPackagesGUIDs =
			 * preProcessor.getBaselineDeletedPackagesGUIDs().containsKey(aBaselineClass.getParentPackageGUID());
			 * boolean isInMovedPackages =
			 * preProcessor.getBaselineMovedPackages().containsKey(aBaselineClass.getParentPackageGUID());
			 * boolean isInMovedPackagesGUIDs =
			 * preProcessor.getBaselineMovedPackagesGUIDs().containsKey(aBaselineClass.getParentPackageGUID());
			 * Package parentPackage = preProcessor.getBaselinePackagesGUIDs()
			 * .get(aBaselineClass.getParentPackageGUID());
			 * thePackage.getCompareItem().add(parseClass(aBaselineClass, parentPackage,
			 * null, null)); }
			 */
		} else {
			// Both packages exist...
			thePackage = new CompareItem(properties, targetPackage.getName(), "Package", targetPackage.getKey(),
					Status.Changed.toString());

			/**
			 * Process child packages...
			 */
			Set<String> sortedKeys = new TreeSet<String>(new org.cimug.compare.StringComparator());

			List<PackageType> baselineChildPackages = baselinePackage.getPackages();
			baselineChildPackages.forEach(aPackage -> sortedKeys.add(aPackage.getKey()));

			List<PackageType> targetChildPackages = targetPackage.getPackages();
			targetChildPackages.forEach(aPackage -> sortedKeys.add(aPackage.getKey()));

			for (String key : sortedKeys) {
				PackageType baselineChildPackage = baselinePackage.getPackage(key);
				PackageType targetChildPackage = targetPackage.getPackage(key);

				if (baselineChildPackage == null) {
					thePackage.getCompareItem().add(parsePackage(null, targetChildPackage));
				} else if (targetChildPackage == null) {
					thePackage.getCompareItem().add(parsePackage(baselineChildPackage, null));
				} else {
					thePackage.getCompareItem().add(parsePackage(baselineChildPackage, targetChildPackage));
				}
			}

			/**
			 * Process child classes...
			 * 
			 * Set<String> sortedClassNames = new TreeSet<String>(new
			 * org.cimug.compare.StringComparator());
			 * 
			 * List<Class> baselineChildClasses = baselinePackage.getClasses();
			 * baselineChildClasses.forEach(aClass ->
			 * sortedClassNames.add(aClass.getName()));
			 * 
			 * List<Class> targetChildClasses = targetPackage.getClasses();
			 * targetChildClasses.forEach(aClass -> sortedClassNames.add(aClass.getName()));
			 * 
			 * for (String name : sortedClassNames) { Class baselineChildClass =
			 * baselinePackage.getClass(name); Class targetChildClass =
			 * targetPackage.getClass(name);
			 * 
			 * if (baselineChildClass == null) { Package targetParentPackage =
			 * preProcessor.getTargetPackagesGUIDs()
			 * .get(targetChildClass.getParentPackageGUID());
			 * thePackage.getCompareItem().add(parseClass(null, null, targetChildClass,
			 * targetParentPackage)); } else if (targetChildClass == null) { Package
			 * baselineParentPackage = preProcessor.getBaselinePackagesGUIDs()
			 * .get(baselineChildClass.getParentPackageGUID());
			 * thePackage.getCompareItem().add(parseClass(baselineChildClass,
			 * baselineParentPackage, null, null)); } else { Package baselineParentPackage =
			 * preProcessor.getBaselinePackagesGUIDs()
			 * .get(baselineChildClass.getParentPackageGUID()); Package targetParentPackage
			 * = preProcessor.getTargetPackagesGUIDs()
			 * .get(targetChildClass.getParentPackageGUID());
			 * thePackage.getCompareItem().add(parseClass(baselineChildClass,
			 * baselineParentPackage, targetChildClass, targetParentPackage)); } }
			 * 
			 */
		}

		return thePackage;
	}

	private CompareItem parseClass(ClassType baselineClass, PackageType baselineParentPackage, ClassType targetClass,
			PackageType targetParentPackage) {

		CompareItem theClass;

		ClassProperties propsProcessor = new ClassProperties(baselineClass, baselineParentPackage, targetClass,
				targetParentPackage);

		Properties properties = propsProcessor.getProperties();

		// It is assumed that both packages will never be both null...
		if (baselineClass == null) {
			// Only model class exists...
			theClass = new CompareItem(properties, new ArrayList<CompareItem>(), targetClass.getName(), "Class",
					targetClass.getKey(), Status.ModelOnly.toString());

			/**
			 * Process the Class's attributes...
			 */
			Set<String> sortedAttributeKeys = new TreeSet<String>(new org.cimug.compare.StringComparator());

			List<AttributeType> targetClassAttributes = targetClass.getAttributes();
			targetClassAttributes.forEach(attribute -> sortedAttributeKeys.add(attribute.getKey()));

			for (String key : sortedAttributeKeys) {
				AttributeType targetAttribute = targetClass.getAttribute(key);
				theClass.getCompareItem().add(parseAttribute(null, targetAttribute));
			}

			// Process the Class's links (i.e. associations, generalizations, aggregations,
			// dependencies)...
			theClass.getCompareItem().add(parseLinks(null, null, targetClass, targetParentPackage));
		} else if (targetClass == null) {
			// Only baseline class exists...
			theClass = new CompareItem(properties, new ArrayList<CompareItem>(), baselineClass.getName(), "Class",
					baselineClass.getKey(), Status.BaselineOnly.toString());

			/**
			 * Process the Class's attributes...
			 */
			Set<String> sortedAttributeKeys = new TreeSet<String>(new org.cimug.compare.StringComparator());

			List<AttributeType> baselineClassAttributes = baselineClass.getAttributes();
			baselineClassAttributes.forEach(attribute -> sortedAttributeKeys.add(attribute.getKey()));

			for (String key : sortedAttributeKeys) {
				AttributeType baselineAttribute = baselineClass.getAttribute(key);
				theClass.getCompareItem().add(parseAttribute(baselineAttribute, null));
			}

			// Process the Class's links (i.e. associations, generalizations,
			// aggregations)...
			theClass.getCompareItem().add(parseLinks(baselineClass, baselineParentPackage, null, null));
		} else {
			// Both classes exist...
			theClass = new CompareItem(properties, new ArrayList<CompareItem>(), targetClass.getName(), "Class",
					targetClass.getKey(), Status.Changed.toString());

			/**
			 * Process the Class's attributes...
			 */
			Set<String> sortedAttributeKeys = new TreeSet<String>(new org.cimug.compare.StringComparator());

			List<AttributeType> baselineClassAttributes = baselineClass.getAttributes();
			baselineClassAttributes.forEach(attribute -> sortedAttributeKeys.add(attribute.getKey()));

			List<AttributeType> targetClassAttributes = targetClass.getAttributes();
			targetClassAttributes.forEach(attribute -> sortedAttributeKeys.add(attribute.getKey()));

			for (String key : sortedAttributeKeys) {
				AttributeType baselineAttribute = baselineClass.getAttribute(key);
				AttributeType targetAttribute = targetClass.getAttribute(key);

				if (baselineAttribute == null) {
					theClass.getCompareItem().add(parseAttribute(null, targetAttribute));
				} else if (targetAttribute == null) {
					theClass.getCompareItem().add(parseAttribute(baselineAttribute, null));
				} else {
					theClass.getCompareItem().add(parseAttribute(baselineAttribute, targetAttribute));
				}
			}

			// Process the Class's links (i.e. associations, generalizations,
			// aggregations)...
			theClass.getCompareItem()
					.add(parseLinks(baselineClass, baselineParentPackage, targetClass, targetParentPackage));
		}

		return theClass;
	}

	private CompareItem parseAttribute(AttributeType baselineAttribute, AttributeType targetAttribute) {

		CompareItem theAttribute;

		AttributeProperties propsProcessor = new AttributeProperties(baselineAttribute, targetAttribute);

		Properties properties = propsProcessor.getProperties();

		// It is assumed that both packages will never be both null...
		if (baselineAttribute == null) {
			// Only model class exists...
			theAttribute = new CompareItem(properties, new ArrayList<CompareItem>(), targetAttribute.getName(),
					"Attribute", targetAttribute.getTheValue("ea_guid"), Status.ModelOnly.toString());
		} else if (targetAttribute == null) {
			// Only baseline class exists...
			theAttribute = new CompareItem(properties, new ArrayList<CompareItem>(), baselineAttribute.getName(),
					"Attribute", baselineAttribute.getTheValue("ea_guid"), Status.BaselineOnly.toString());
		} else {
			// Both classes exist...
			theAttribute = new CompareItem(properties, new ArrayList<CompareItem>(), targetAttribute.getName(),
					"Attribute", targetAttribute.getTheValue("ea_guid"), Status.Changed.toString());
		}

		return theAttribute;
	}

	private CompareItem parseLinks(ClassType baselineClass, PackageType baselineParentPackage, ClassType targetClass,
			PackageType targetParentPackage) {

		// NOTE: The status of the "Links" CompareItem is ALWAYS 'Identical'...
		CompareItem links = new CompareItem(null, "Links", "Links",
				(targetClass != null ? targetClass.getKey() : baselineClass.getKey()) + "Links",
				Status.Identical.toString());

		// TODO: FIX THIS SECTION
		if (baselineClass == null) {
			// Only model class exists...
			for (AssociationType targetAssociation : targetParentPackage.getAssociations()) {
				AssociationProperties propsProcessor = new AssociationProperties(null, targetAssociation);
				Properties properties = propsProcessor.getProperties();

				CompareItem associationCompareItem = new CompareItem(properties, "Association", "",
						targetAssociation.getKey(), propsProcessor.getStatus().toString());

				/**
				 * Create 'Src' end of the association...
				 */
				AssociationEndType targetSourceAssocationEnd = targetAssociation.getSourceAssocationEnd();
				SourceAssociationEndProperties sourcePropsProcessor = new SourceAssociationEndProperties(null, null,
						targetSourceAssocationEnd, targetAssociation);
				Properties sourceEndProperties = sourcePropsProcessor.getProperties();

				// Format of the Source CompareItem GUID is: 'Src-' + GUID of the class +
				// 'Links' + GUID of the source AssociationEnd + GUID of the Association
				String sourceGUID = "Src-" + targetClass.getKey() + "Links"
						+ DiffUtils.convertXmiIdToEAGUID(targetSourceAssocationEnd.getType())
						+ targetAssociation.getKey();
				CompareItem sourceEndCompareItem = new CompareItem(sourceEndProperties, "Source: ()", "Src", sourceGUID,
						Status.ModelOnly.toString());

				associationCompareItem.getCompareItem().add(sourceEndCompareItem);

				/**
				 * Create 'Dst' end of the association...
				 */
				AssociationEndType targetDestinationAssocationEnd = targetAssociation.getDestinationAssocationEnd();
				DestinationAssociationEndProperties destinationPropsProcessor = new DestinationAssociationEndProperties(
						null, null, targetDestinationAssocationEnd, targetAssociation);
				Properties destinationEndProperties = destinationPropsProcessor.getProperties();

				// Format of the Destination CompareItem GUID is: 'Dst-' + GUID of the class +
				// 'Links' + GUID of the target AssociationEnd + GUID of the Association
				String destinationGUID = "Dst-" + targetClass.getKey() + "Links"
						+ DiffUtils.convertXmiIdToEAGUID(targetDestinationAssocationEnd.getType())
						+ targetAssociation.getKey();
				CompareItem destinationEndCompareItem = new CompareItem(destinationEndProperties, "Target: ()", "Dst",
						destinationGUID, Status.ModelOnly.toString());
				associationCompareItem.getCompareItem().add(destinationEndCompareItem);

				// Finally we add the new association compare item to the links
				links.getCompareItem().add(associationCompareItem);
			}

			for (GeneralizationType targetGeneralization : targetParentPackage.getGeneralizations()) {
				GeneralizationProperties propsProcessor = new GeneralizationProperties(null, targetGeneralization);
				Properties properties = propsProcessor.getProperties();

				CompareItem generalizationCompareItem = new CompareItem(properties, "Generalization", "",
						targetGeneralization.getKey(), propsProcessor.getStatus().toString());

				/**
				 * Create 'Src' end of the association...
				 */
				AssociationEndType targetSourceAssocationEnd = targetGeneralization.getSourceAssocationEnd();
				SourceGeneralizationEndProperties sourcePropsProcessor = new SourceGeneralizationEndProperties(null,
						targetGeneralization);
				Properties sourceEndProperties = sourcePropsProcessor.getProperties();

				// Format of the Source CompareItem GUID is: 'Src-' + GUID of the class +
				// 'Links' + GUID of the source AssociationEnd + GUID of the Association
				String sourceGUID = "Src-" + targetClass.getKey() + "Links"
						+ DiffUtils.convertXmiIdToEAGUID(targetSourceAssocationEnd.getType())
						+ targetGeneralization.getKey();
				CompareItem sourceEndCompareItem = new CompareItem(sourceEndProperties, "Source: ()", "Src", sourceGUID,
						Status.ModelOnly.toString());

				generalizationCompareItem.getCompareItem().add(sourceEndCompareItem);

				/**
				 * Create 'Dst' end of the association...
				 */
				AssociationEndType targetDestinationAssociationEnd = targetGeneralization.getDestinationAssocationEnd();
				DestinationGeneralizationEndProperties destinationPropsProcessor = new DestinationGeneralizationEndProperties(
						null, targetGeneralization);
				Properties destinationEndProperties = destinationPropsProcessor.getProperties();

				// Format of the Destination CompareItem GUID is: 'Dst-' + GUID of the class +
				// 'Links' + GUID of the target AssociationEnd + GUID of the Association
				String destinationGUID = "Dst-" + targetClass.getKey() + "Links"
						+ DiffUtils.convertXmiIdToEAGUID(targetDestinationAssociationEnd.getType())
						+ targetGeneralization.getKey();
				CompareItem destinationEndCompareItem = new CompareItem(destinationEndProperties, "Target: ()", "Dst",
						destinationGUID, Status.ModelOnly.toString());
				generalizationCompareItem.getCompareItem().add(destinationEndCompareItem);

				// Finally we add the new generalization compare item to the links
				links.getCompareItem().add(generalizationCompareItem);
			}
		} else if (targetClass == null) {
			// Only baseline class exists...
			for (AssociationType baselineAssociation : baselineParentPackage.getAssociations()) {
				AssociationProperties propsProcessor = new AssociationProperties(null, baselineAssociation);
				Properties properties = propsProcessor.getProperties();

				CompareItem association = new CompareItem(properties, "Association", "", baselineAssociation.getKey(),
						propsProcessor.getStatus().toString());
				links.getCompareItem().add(association);
			}

			for (GeneralizationType baselineGeneralization : baselineParentPackage.getGeneralizations()) {
				GeneralizationProperties propsProcessor = new GeneralizationProperties(null, baselineGeneralization);
				Properties properties = propsProcessor.getProperties();

				CompareItem generalization = new CompareItem(properties, "Generalization", "",
						baselineGeneralization.getKey(), propsProcessor.getStatus().toString());
				links.getCompareItem().add(generalization);
			}
		} else {
			// Both classes exist...

			/**
			 * This is a bit more complex...
			 */
			for (AssociationType targetAssociation : targetParentPackage.getAssociations()) {
				AssociationProperties propsProcessor = new AssociationProperties(null, targetAssociation);
				Properties properties = propsProcessor.getProperties();

				CompareItem association = new CompareItem(properties, "Association", "", targetAssociation.getKey(),
						propsProcessor.getStatus().toString());
				links.getCompareItem().add(association);
			}

			for (GeneralizationType targetGeneralization : targetParentPackage.getGeneralizations()) {
				GeneralizationProperties propsProcessor = new GeneralizationProperties(null, targetGeneralization);
				Properties properties = propsProcessor.getProperties();

				CompareItem generalization = new CompareItem(properties, "Generalization", "",
						targetGeneralization.getKey(), propsProcessor.getStatus().toString());
				links.getCompareItem().add(generalization);
			}
		}

		return links;
	}

}
