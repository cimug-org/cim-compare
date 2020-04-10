package org.cimug.compare.app;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
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
import org.cimug.compare.NamedTypeComparator;
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
import org.cimug.compare.uml1_3.ifaces.KeyIdentifier;

class GUIDBasedDiffReportGeneratorImpl implements DiffReportGenerator {

	private Model baselineModel;
	private Model targetModel;
	private PreProcessor preProcessor;
	private File outputFile;

	public GUIDBasedDiffReportGeneratorImpl(Model baselineModel, Model targetModel, File outputFile)
			throws JAXBException {
		this.baselineModel = baselineModel;
		this.targetModel = targetModel;
		this.preProcessor = new PreProcessor(baselineModel, targetModel);
		this.outputFile = outputFile;
	}

	/**
	 * Special static class used purely for sorting purposes. The hashCode and
	 * equals methods are specialized for the purposes of storing Attribute specific
	 * CompareItem(s) in Sets or Maps.
	 */
	static class AttributeCompareItem {

		private CompareItem compareItem;

		public AttributeCompareItem(CompareItem compareItem) {
			this.compareItem = compareItem;
		}

		public CompareItem getCompareItem() {
			return this.compareItem;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((compareItem.getGuid() == null) ? 0 : compareItem.getGuid().hashCode());
			result = prime * result + ((compareItem.getName() == null) ? 0 : compareItem.getName().hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			AttributeCompareItem other = (AttributeCompareItem) obj;
			if (compareItem.getGuid() == null) {
				if (other.getCompareItem().getGuid() != null)
					return false;
			} else if (!compareItem.getGuid().equals(other.getCompareItem().getGuid()))
				return false;
			if (compareItem.getName() == null) {
				if (other.getCompareItem().getName() != null)
					return false;
			} else if (!compareItem.getName().equals(other.getCompareItem().getName()))
				return false;
			return true;
		}

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
		jaxbMarshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
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
		 * =========================================================================
		 * Process child packages.
		 * =========================================================================
		 */
		List<PackageType> baselineChildPackages = baselineModel.getPackages();
		List<PackageType> targetChildPackages = targetModel.getPackages();

		for (PackageType targetChildPackage : targetChildPackages) {
			theRootPackage.getCompareItem().add(parsePackage(targetChildPackage));
		}

		/**
		 * Process any packages deleted from the baseline.
		 */
		for (PackageType baselineChildPackage : baselineChildPackages) {
			if (preProcessor.getBaselineDeletedPackagesGUIDs().containsKey(baselineChildPackage.getKey())) {
				theRootPackage.getCompareItem().add(parsePackage(baselineChildPackage));
			}
		}

		/**
		 * =========================================================================
		 * Process child classes.
		 * =========================================================================
		 */
		List<ClassType> baselineChildClasses = baselineModel.getClasses();
		List<ClassType> targetChildClasses = targetModel.getClasses();

		for (ClassType targetChildClass : targetChildClasses) {
			PackageType targetParentPackage = preProcessor.getTargetPackagesGUIDs()
					.get(targetChildClass.getParentPackageGUID());
			theRootPackage.getCompareItem().add(parseClass(targetChildClass, targetParentPackage));
		}

		/**
		 * Process any classes deleted from the baseline.
		 */
		for (ClassType baselineChildClass : baselineChildClasses) {
			if (preProcessor.getBaselineDeletedClassesGUIDs().containsKey(baselineChildClass.getKey())) {
				PackageType baselineParentPackage = preProcessor.getBaselinePackagesGUIDs()
						.get(baselineChildClass.getParentPackageGUID());
				theRootPackage.getCompareItem().add(parseClass(baselineChildClass, baselineParentPackage));
			}
		}

		CompareResults compareResults = new CompareResults(theRootPackage, hasChanges);
		return compareResults;
	}

	private CompareItem parsePackage(PackageType aPackage) {
		CompareItem thePackage = null;

		// Very first thing we do is to first determine if the package passed in is a
		// deleted package...
		if (preProcessor.getBaselineDeletedPackagesGUIDs().containsKey(aPackage.getKey())) {
			PackageProperties deletedPackageProperties = new PackageProperties(aPackage, null);
			Properties properties = deletedPackageProperties.getProperties();

			thePackage = new CompareItem(properties, aPackage.getName(), "", aPackage.getKey(),
					Status.BaselineOnly.toString());

			for (PackageType childPackage : aPackage.getPackages()) {
				// Special process for a deleted parent package -- i.e. we only included those
				// child packages and child classes that also were deleted (and not those that
				// have been moved)...
				if (preProcessor.getBaselineDeletedPackagesGUIDs().containsKey(childPackage.getKey())) {
					thePackage.getCompareItem().add(parsePackage(childPackage));
				}
			}

			for (ClassType childClass : aPackage.getClasses()) {
				// Special process for a deleted child classes -- i.e. we only included those
				// child classes that also were deleted (and not those that have been moved)...
				if (preProcessor.getBaselineDeletedClassesGUIDs().containsKey(childClass.getKey())) {
					thePackage.getCompareItem().add(parseClass(childClass, aPackage));
				}
			}
		} else {
			// Retrieve the baselinePackage in order to process package properties...
			PackageType baselinePackage = (preProcessor.getBaselinePackagesGUIDs().containsKey(aPackage.getKey())
					? preProcessor.getBaselinePackagesGUIDs().get(aPackage.getKey())
					: null);

			PackageProperties packageProperties = new PackageProperties(baselinePackage, aPackage);
			Properties properties = packageProperties.getProperties();

			thePackage = new CompareItem(properties, aPackage.getName(), "Package", aPackage.getKey(),
					packageProperties.getStatus().toString());

			List<PackageType> targetChildPackages = aPackage.getPackages();

			for (PackageType targetChildPackage : targetChildPackages) {
				thePackage.getCompareItem().add(parsePackage(targetChildPackage));
			}

			// Next we search for any child packages of this target package that were
			// deleted. This is determined by scanning all deleted baseline packages who's
			// parent package GUID is that of the target package...
			for (PackageType deletedChildPackage : preProcessor.getBaselineDeletedPackagesGUIDs().values()) {
				// Special process for a deleted parent package -- i.e. we only included those
				// child packages and child classes that also were deleted (and not those that
				// have been moved)...
				if (aPackage.getKey().equals(deletedChildPackage.getParentPackageGUID())) {
					thePackage.getCompareItem().add(parsePackage(deletedChildPackage));
				}
			}

			for (ClassType targetChildClass : aPackage.getClasses()) {
				thePackage.getCompareItem().add(parseClass(targetChildClass, aPackage));
			}

			for (ClassType baselineDeletedClass : preProcessor.getBaselineDeletedClassesGUIDs().values()) {
				// Special process for a deleted child classes -- i.e. we only included those
				// child classes that also were deleted (and not those that have been moved)...
				if (aPackage.getKey().equals(baselineDeletedClass.getParentPackageGUID())) {
					PackageType baselineParentPackage = preProcessor.getBaselinePackagesGUIDs().get(aPackage.getKey());
					thePackage.getCompareItem().add(parseClass(baselineDeletedClass, baselineParentPackage));
				}
			}
		}

		return thePackage;
	}

	private CompareItem parseClass(ClassType aClass, PackageType classParentPackage) {
		CompareItem theClass = null;

		// Very first thing we do is to first determine if the package passed in is a
		// deleted package...
		if (preProcessor.getBaselineDeletedClassesGUIDs().containsKey(aClass.getKey())) {
			ClassProperties deletedClassProperties = new ClassProperties(aClass, classParentPackage, null, null);
			Properties properties = deletedClassProperties.getProperties();

			theClass = new CompareItem(properties, aClass.getName(), "", aClass.getKey(),
					Status.BaselineOnly.toString());

			/** Process the Class's attributes... */
			Set<String> sortedAttributeNames = new TreeSet<String>(new org.cimug.compare.StringComparator());

			List<AttributeType> classAttributes = aClass.getAttributes();
			classAttributes.forEach(attribute -> sortedAttributeNames.add(attribute.getName()));

			for (AttributeType baselineAttribute : classAttributes) {
				theClass.getCompareItem().add(parseAttribute(baselineAttribute, null));
			}

			// Process the Class's links (i.e. associations, generalizations, aggregations,
			// dependencies)...
			theClass.getCompareItem().add(parseLinks(aClass, classParentPackage, null, null));
		} else {
			// Retrieve the baselineClass in order to process class properties...
			ClassType baselineClass = (preProcessor.getBaselineClassesGUIDs().containsKey(aClass.getKey())
					? preProcessor.getBaselineClassesGUIDs().get(aClass.getKey())
					: null);

			PackageType baselineParentPackage = (baselineClass != null
					? preProcessor.getBaselinePackagesGUIDs().get(baselineClass.getKey())
					: null);

			ClassProperties classProperties = new ClassProperties(baselineClass, baselineParentPackage, aClass,
					classParentPackage);
			Properties properties = classProperties.getProperties();

			theClass = new CompareItem(properties, aClass.getName(), "Class", aClass.getKey(),
					classProperties.getStatus().toString());

			Set<AttributeType> sortedAttributeTypes = new TreeSet<AttributeType>(
					new NamedTypeComparator<AttributeType>());

			switch (classProperties.getStatus())
				{
				case BaselineOnly:
					baselineClass.getAttributes().forEach(attribute -> sortedAttributeTypes.add(attribute));

					for (AttributeType baselineAttribute : sortedAttributeTypes) {
						theClass.getCompareItem().add(parseAttribute(baselineAttribute, null));
					}
					break;
				case ModelOnly:
					aClass.getAttributes().forEach(attribute -> sortedAttributeTypes.add(attribute));

					for (AttributeType targetAttribute : sortedAttributeTypes) {
						theClass.getCompareItem().add(parseAttribute(null, targetAttribute));
					}
					break;
				default:
					Set<AttributeCompareItem> sortedCompareItems = new TreeSet<AttributeCompareItem>(
							new Comparator<AttributeCompareItem>() {
								public int compare(AttributeCompareItem item1, AttributeCompareItem item2) {
									if (item1 == item2) {
										return 0;
									}
									if (item1 == null) {
										return -1;
									}
									if (item2 == null) {
										return 1;
									}

									String item1Type = item1.getCompareItem().getType();
									String item2Type = item2.getCompareItem().getType();

									String item1Name = item1.getCompareItem().getName();
									String item2Name = item2.getCompareItem().getName();

									if (item1Name == null && item2Name == null) {
										return 0;
									}
									if (item1Name == null) {
										return -1;
									}
									if (item2Name == null) {
										return 1;
									}

									if (item1Name.equals(item2Name)) {
										if (item1Type == null && item2Type == null) {
											return 0;
										}
										if (item1Type == null) {
											return -1;
										}
										if (item2Type == null) {
											return 1;
										}
										return item1Type.compareTo(item2Type);
									}

									return item1Name.compareTo(item2Name);
								}
							});

					/** We loop through all target attributes... */
					for (AttributeType targetAttribute : aClass.getAttributes()) {
						AttributeType baselineAttribute = baselineClass.getAttributeByGUID(targetAttribute.getKey());
						sortedCompareItems
								.add(new AttributeCompareItem(parseAttribute(baselineAttribute, targetAttribute)));
					}

					/** We now identify deleted attributes... */
					for (AttributeType baselineAttribute : baselineClass.getAttributes()) {
						// If null then the attribute was deleted and doesn't appear in the target
						// class...
						if (aClass.getAttributeByGUID(baselineAttribute.getKey()) == null) {
							sortedCompareItems.add(new AttributeCompareItem(parseAttribute(baselineAttribute, null)));
						}
					}

					/**
					 * Finally, we add the final sorted compare items to the list in sorted order
					 */
					for (AttributeCompareItem item : sortedCompareItems) {
						theClass.getCompareItem().add(item.getCompareItem());
					}
					break;
				}

			// Process the Class's links (i.e. associations, generalizations,
			// aggregations)...
			theClass.getCompareItem().add(parseLinks(baselineClass, baselineParentPackage, aClass, classParentPackage));
		}

		return theClass;
	}

	private CompareItem parseAttribute(AttributeType baselineAttribute, AttributeType targetAttribute) {

		CompareItem theAttribute;

		AttributeProperties propsProcessor = new AttributeProperties(baselineAttribute, targetAttribute);
		Properties properties = propsProcessor.getProperties();

		// It is assumed that both attributes will never be both null...
		if (baselineAttribute == null) {
			// Only model attribute exists...
			theAttribute = new CompareItem(properties, new ArrayList<CompareItem>(), targetAttribute.getName(),
					"Attribute", targetAttribute.getTheValue("ea_guid"), Status.ModelOnly.toString());
		} else if (targetAttribute == null) {
			// Only baseline attribute exists.
			// NOTE: This corresponds to a deleted attribute and therefore the type
			// parameter is specified
			// as an empty string (i.e. "")...
			theAttribute = new CompareItem(properties, new ArrayList<CompareItem>(), baselineAttribute.getName(), "",
					baselineAttribute.getTheValue("ea_guid"), Status.BaselineOnly.toString());
		} else {
			// Both attributes exist...
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

		if (baselineClass == null) {
			// Only model class exists...

			for (GeneralizationType targetGeneralization : targetParentPackage.getGeneralizations()) {
				GeneralizationProperties propsProcessor = new GeneralizationProperties(null, targetGeneralization);
				Properties properties = propsProcessor.getProperties();

				CompareItem generalizationCompareItem = new CompareItem(properties, "Generalization", "",
						targetGeneralization.getKey(), propsProcessor.getStatus().toString());

				/** Source end... */
				SourceGeneralizationEndProperties sourcePropsProcessor = new SourceGeneralizationEndProperties(null,
						targetGeneralization);
				Properties sourceEndProperties = sourcePropsProcessor.getProperties();

				CompareItem sourceCompareItem = new CompareItem(sourceEndProperties,
						"Source: (" + targetGeneralization.getName() + ")", "Src",
						"Src-" + targetGeneralization.getKey(), sourcePropsProcessor.getStatus().toString());
				generalizationCompareItem.getCompareItem().add(sourceCompareItem);

				/** Destination end... */
				DestinationGeneralizationEndProperties destinationPropsProcessor = new DestinationGeneralizationEndProperties(
						null, targetGeneralization);
				Properties destinationEndProperties = destinationPropsProcessor.getProperties();

				CompareItem destinationCompareItem = new CompareItem(destinationEndProperties,
						"Target: (" + targetGeneralization.getName() + ")", "Dst",
						"Dst-" + targetGeneralization.getKey(), destinationPropsProcessor.getStatus().toString());
				generalizationCompareItem.getCompareItem().add(destinationCompareItem);

				// Finally we add the new generalization compare item to the links
				links.getCompareItem().add(generalizationCompareItem);
			}

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

				String sourceGUID = generateSourceGUID(targetClass, targetAssociation, targetSourceAssocationEnd);

				CompareItem sourceEndCompareItem = new CompareItem(sourceEndProperties,
						"Source: (" + targetAssociation.getName() + ")", "Src", sourceGUID,
						Status.ModelOnly.toString());

				associationCompareItem.getCompareItem().add(sourceEndCompareItem);

				/**
				 * Create 'Dst' end of the association...
				 */
				AssociationEndType targetDestinationAssocationEnd = targetAssociation.getDestinationAssocationEnd();
				DestinationAssociationEndProperties destinationPropsProcessor = new DestinationAssociationEndProperties(
						null, null, targetDestinationAssocationEnd, targetAssociation);
				Properties destinationEndProperties = destinationPropsProcessor.getProperties();

				String destinationGUID = generateDestinationGUID(targetClass, targetAssociation,
						targetDestinationAssocationEnd);

				CompareItem destinationEndCompareItem = new CompareItem(destinationEndProperties,
						"Target: (" + targetAssociation.getName() + ")", "Dst", destinationGUID,
						Status.ModelOnly.toString());
				associationCompareItem.getCompareItem().add(destinationEndCompareItem);

				// Finally we add the new association compare item to the links
				links.getCompareItem().add(associationCompareItem);
			}
		} else if (targetClass == null) {
			// Only baseline class exists...so we know the class has been deleted.
			// In this case

			for (GeneralizationType baselineGeneralization : baselineParentPackage.getGeneralizations()) {
				GeneralizationProperties propsProcessor = new GeneralizationProperties(baselineGeneralization, null);
				Properties properties = propsProcessor.getProperties();

				CompareItem generalizationCompareItem = new CompareItem(properties, "Generalization", "",
						baselineGeneralization.getKey(), propsProcessor.getStatus().toString());

				/** Source end... */
				SourceGeneralizationEndProperties sourcePropsProcessor = new SourceGeneralizationEndProperties(
						baselineGeneralization, null);
				Properties sourceEndProperties = sourcePropsProcessor.getProperties();

				CompareItem sourceCompareItem = new CompareItem(sourceEndProperties,
						"Source: (" + baselineGeneralization.getTheValue("ea_sourceName") + ")", "Src",
						"Src-" + baselineGeneralization.getKey(), sourcePropsProcessor.getStatus().toString());
				generalizationCompareItem.getCompareItem().add(sourceCompareItem);

				/** Destination end... */
				DestinationGeneralizationEndProperties destinationPropsProcessor = new DestinationGeneralizationEndProperties(
						null, baselineGeneralization);
				Properties destinationEndProperties = destinationPropsProcessor.getProperties();

				CompareItem destinationCompareItem = new CompareItem(destinationEndProperties,
						"Target: (" + baselineGeneralization.getTheValue("ea_targetName") + ")", "Dst",
						"Dst-" + baselineGeneralization.getKey(), destinationPropsProcessor.getStatus().toString());
				generalizationCompareItem.getCompareItem().add(destinationCompareItem);

				// Finally we add the new generalization compare item to the links
				links.getCompareItem().add(generalizationCompareItem);
			}

			for (AssociationType baselineAssociation : baselineParentPackage.getAssociations()) {
				AssociationProperties propsProcessor = new AssociationProperties(baselineAssociation, null);
				Properties properties = propsProcessor.getProperties();

				CompareItem associationCompareItem = new CompareItem(properties, "Association", "",
						baselineAssociation.getKey(), propsProcessor.getStatus().toString());

				/**
				 * Create 'Src' end of the association...
				 */
				AssociationEndType baselineSourceAssocationEnd = baselineAssociation.getSourceAssocationEnd();
				SourceAssociationEndProperties sourcePropsProcessor = new SourceAssociationEndProperties(
						baselineSourceAssocationEnd, baselineAssociation, null, null);
				Properties sourceEndProperties = sourcePropsProcessor.getProperties();

				String sourceGUID = generateSourceGUID(baselineClass, baselineAssociation, baselineSourceAssocationEnd);

				CompareItem sourceEndCompareItem = new CompareItem(sourceEndProperties,
						"Source: (" + baselineSourceAssocationEnd.getName() + ")", "Src", sourceGUID,
						Status.BaselineOnly.toString());

				associationCompareItem.getCompareItem().add(sourceEndCompareItem);

				/**
				 * Create 'Dst' end of the association...
				 */
				AssociationEndType baselineDestinationAssocationEnd = baselineAssociation.getDestinationAssocationEnd();
				DestinationAssociationEndProperties destinationPropsProcessor = new DestinationAssociationEndProperties(
						baselineDestinationAssocationEnd, baselineAssociation, null, null);
				Properties destinationEndProperties = destinationPropsProcessor.getProperties();

				String destinationGUID = generateDestinationGUID(baselineClass, baselineAssociation,
						baselineDestinationAssocationEnd);

				CompareItem destinationEndCompareItem = new CompareItem(destinationEndProperties,
						"Target: (" + baselineDestinationAssocationEnd.getName() + ")", "Dst", destinationGUID,
						Status.BaselineOnly.toString());
				associationCompareItem.getCompareItem().add(destinationEndCompareItem);

				// Finally we add the new association compare item to the links
				links.getCompareItem().add(associationCompareItem);
			}
		} else {
			// Both classes exist...
			// NOTE: For "root" classes there is NO parent package so we must ensure it is
			// != null
			/**
			 * if (targetParentPackage != null) { for (GeneralizationType
			 * targetGeneralization : targetParentPackage.getGeneralizations()) { //
			 * Retrieve the baselineGeneralization in order to process generalization //
			 * properties... GeneralizationType baselineGeneralization =
			 * (preProcessor.getBaselineGeneralizationsGUIDs()
			 * .containsKey(targetGeneralization.getKey()) ?
			 * preProcessor.getBaselineGeneralizationsGUIDs().get(targetGeneralization.getKey())
			 * : null);
			 * 
			 * GeneralizationProperties propsProcessor = new
			 * GeneralizationProperties(baselineGeneralization, targetGeneralization);
			 * Properties properties = propsProcessor.getProperties();
			 * 
			 * CompareItem generalizationCompareItem = new CompareItem(properties,
			 * "Generalization", "", targetGeneralization.getKey(),
			 * propsProcessor.getStatus().toString());
			 * 
			 * SourceGeneralizationEndProperties sourcePropsProcessor = new
			 * SourceGeneralizationEndProperties(null, targetGeneralization); Properties
			 * sourceEndProperties = sourcePropsProcessor.getProperties();
			 * 
			 * CompareItem sourceCompareItem = new CompareItem(sourceEndProperties, "Source:
			 * (" + targetGeneralization.getName() + ")", "Src", "Src-" +
			 * targetGeneralization.getKey(), sourcePropsProcessor.getStatus().toString());
			 * generalizationCompareItem.getCompareItem().add(sourceCompareItem);
			 * 
			 * DestinationGeneralizationEndProperties destinationPropsProcessor = new
			 * DestinationGeneralizationEndProperties( baselineGeneralization,
			 * targetGeneralization); Properties destinationEndProperties =
			 * destinationPropsProcessor.getProperties();
			 * 
			 * CompareItem destinationCompareItem = new
			 * CompareItem(destinationEndProperties, "Target: (" +
			 * targetGeneralization.getName() + ")", "Dst", "Dst-" +
			 * targetGeneralization.getKey(),
			 * destinationPropsProcessor.getStatus().toString());
			 * generalizationCompareItem.getCompareItem().add(destinationCompareItem);
			 * 
			 * links.getCompareItem().add(generalizationCompareItem); }
			 * 
			 * for (AssociationType targetAssociation :
			 * targetParentPackage.getAssociations()) { // Retrieve the baselineAssociations
			 * in order to process association // properties... AssociationType
			 * baselineAssociation = (preProcessor.getBaselineAssociationsGUIDs()
			 * .containsKey(targetAssociation.getKey()) ?
			 * preProcessor.getBaselineAssociationsGUIDs().get(targetAssociation.getKey()) :
			 * null);
			 * 
			 * AssociationProperties propsProcessor = new
			 * AssociationProperties(baselineAssociation, targetAssociation); Properties
			 * properties = propsProcessor.getProperties();
			 * 
			 * CompareItem associationCompareItem = new CompareItem(properties,
			 * "Association", "", targetAssociation.getKey(),
			 * propsProcessor.getStatus().toString());
			 * 
			 * AssociationEndType targetSourceAssocationEnd =
			 * targetAssociation.getSourceAssocationEnd(); AssociationEndType
			 * baselineSourceAssocationEnd = (baselineAssociation != null ?
			 * baselineAssociation.getSourceAssocationEnd() : null);
			 * 
			 * SourceAssociationEndProperties sourcePropsProcessor = new
			 * SourceAssociationEndProperties( baselineSourceAssocationEnd,
			 * baselineAssociation, targetSourceAssocationEnd, targetAssociation);
			 * Properties sourceEndProperties = sourcePropsProcessor.getProperties();
			 * 
			 * String sourceGUID = generateSourceGUID(targetClass, targetAssociation,
			 * targetSourceAssocationEnd);
			 * 
			 * CompareItem sourceEndCompareItem = new CompareItem(sourceEndProperties,
			 * "Source: (" + targetAssociation.getName() + ")", "Src", sourceGUID,
			 * Status.ModelOnly.toString());
			 * 
			 * associationCompareItem.getCompareItem().add(sourceEndCompareItem);
			 * 
			 * AssociationEndType targetDestinationAssocationEnd =
			 * targetAssociation.getDestinationAssocationEnd(); AssociationEndType
			 * baselineDestinationAssocationEnd = (baselineAssociation != null ?
			 * baselineAssociation.getDestinationAssocationEnd() : null);
			 * 
			 * DestinationAssociationEndProperties destinationPropsProcessor = new
			 * DestinationAssociationEndProperties( baselineDestinationAssocationEnd,
			 * baselineAssociation, targetDestinationAssocationEnd, targetAssociation);
			 * Properties destinationEndProperties =
			 * destinationPropsProcessor.getProperties();
			 * 
			 * String destinationGUID = generateDestinationGUID(targetClass,
			 * targetAssociation, targetDestinationAssocationEnd);
			 * 
			 * CompareItem destinationEndCompareItem = new
			 * CompareItem(destinationEndProperties, "Target: (" +
			 * targetAssociation.getName() + ")", "Dst", destinationGUID,
			 * Status.ModelOnly.toString());
			 * associationCompareItem.getCompareItem().add(destinationEndCompareItem);
			 * 
			 * links.getCompareItem().add(associationCompareItem); }
			 * 
			 * }
			 */
		}

		return links;
	}

	/**
	 * Format of the Source CompareItem GUID is: 'Src-' + GUID of the class +
	 * 'Links' + GUID of the source AssociationEnd + GUID of the Association
	 * 
	 * @param sourceClass
	 * @param sourceLink
	 * @param sourceAssocationEnd
	 * @return
	 */
	private String generateSourceGUID(ClassType sourceClass, KeyIdentifier sourceLink,
			AssociationEndType sourceAssocationEnd) {
		return "Src-" + sourceClass.getKey() + "Links" + DiffUtils.convertXmiIdToEAGUID(sourceAssocationEnd.getType())
				+ sourceLink.getKey();
	}

	/**
	 * Format of the Destination CompareItem GUID is: 'Dst-' + GUID of the class +
	 * 'Links' + GUID of the target AssociationEnd + GUID of the Association
	 * 
	 * @param destinationClass
	 * @param destinationLink
	 * @param destinationAssocationEnd
	 * @return
	 */
	private String generateDestinationGUID(ClassType destinationClass, KeyIdentifier destinationLink,
			AssociationEndType destinationAssocationEnd) {
		return "Dst-" + destinationClass.getKey() + "Links"
				+ DiffUtils.convertXmiIdToEAGUID(destinationAssocationEnd.getType()) + destinationLink.getKey();
	}

}
