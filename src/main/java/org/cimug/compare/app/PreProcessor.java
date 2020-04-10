package org.cimug.compare.app;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.cimug.compare.DiffUtils;
import org.cimug.compare.uml1_3.AssociationType;
import org.cimug.compare.uml1_3.AttributeType;
import org.cimug.compare.uml1_3.ClassType;
import org.cimug.compare.uml1_3.GeneralizationType;
import org.cimug.compare.uml1_3.KeyType;
import org.cimug.compare.uml1_3.KeyTypeEnum;
import org.cimug.compare.uml1_3.Model;
import org.cimug.compare.uml1_3.ModelElementTaggedValue;
import org.cimug.compare.uml1_3.NamespaceOwnedElementType;
import org.cimug.compare.uml1_3.PackageType;

class PreProcessor {

	/** Maps to support GUID-based processing */
	private Map<String, PackageType> baselinePackagesGUIDs = new HashMap<String, PackageType>();
	private Map<String, PackageType> baselineDeletedPackagesGUIDs = new HashMap<String, PackageType>();
	private Map<String, PackageType> baselineMovedPackagesGUIDs = new HashMap<String, PackageType>();
	private Map<String, ClassType> baselineClassesGUIDs = new HashMap<String, ClassType>();
	private Map<String, ClassType> baselineDeletedClassesGUIDs = new HashMap<String, ClassType>();
	private Map<String, ClassType> baselineMovedClassesGUIDs = new HashMap<String, ClassType>();
	private Map<String, GeneralizationType> baselineGeneralizationsGUIDs = new HashMap<String, GeneralizationType>();
	private Map<String, GeneralizationType> baselineDeletedGeneralizationsGUIDs = new HashMap<String, GeneralizationType>();
	private Map<String, GeneralizationType> baselineMovedGeneralizationsGUIDs = new HashMap<String, GeneralizationType>();
	private Map<String, AssociationType> baselineAssociationsGUIDs = new HashMap<String, AssociationType>();
	private Map<String, AssociationType> baselineDeletedAssociationsGUIDs = new HashMap<String, AssociationType>();
	private Map<String, AssociationType> baselineMovedAssociationsGUIDs = new HashMap<String, AssociationType>();

	private Map<String, PackageType> targetPackagesGUIDs = new HashMap<String, PackageType>();
	private Map<String, PackageType> targetNewPackagesGUIDs = new HashMap<String, PackageType>();
	private Map<String, PackageType> targetMovedPackagesGUIDs = new HashMap<String, PackageType>();
	private Map<String, ClassType> targetClassesGUIDs = new HashMap<String, ClassType>();
	private Map<String, ClassType> targetNewClassesGUIDs = new HashMap<String, ClassType>();
	private Map<String, ClassType> targetMovedClassesGUIDs = new HashMap<String, ClassType>();
	private Map<String, GeneralizationType> targetGeneralizationsGUIDs = new HashMap<String, GeneralizationType>();
	private Map<String, GeneralizationType> targetNewGeneralizationsGUIDs = new HashMap<String, GeneralizationType>();
	private Map<String, GeneralizationType> targetMovedGeneralizationsGUIDs = new HashMap<String, GeneralizationType>();
	private Map<String, AssociationType> targetAssociationsGUIDs = new HashMap<String, AssociationType>();
	private Map<String, AssociationType> targetNewAssociationsGUIDs = new HashMap<String, AssociationType>();
	private Map<String, AssociationType> targetMovedAssociationsGUIDs = new HashMap<String, AssociationType>();

	private Set<String> packageGUIDsInBoth = new HashSet<String>();
	private Set<String> classGUIDsInBoth = new HashSet<String>();
	private Set<String> generalizationGUIDsInBoth = new HashSet<String>();
	private Set<String> associationGUIDsInBoth = new HashSet<String>();

	/** Maps to support Names-based processing */
	private Map<String, PackageType> baselinePackages = new HashMap<String, PackageType>();
	private Map<String, PackageType> baselineDeletedPackages = new HashMap<String, PackageType>();
	private Map<String, PackageType> baselineMovedPackages = new HashMap<String, PackageType>();
	private Map<String, ClassType> baselineClasses = new HashMap<String, ClassType>();
	private Map<String, ClassType> baselineDeletedClasses = new HashMap<String, ClassType>();
	private Map<String, ClassType> baselineMovedClasses = new HashMap<String, ClassType>();
	private Map<String, GeneralizationType> baselineGeneralizations = new HashMap<String, GeneralizationType>();
	private Map<String, GeneralizationType> baselineDeletedGeneralizations = new HashMap<String, GeneralizationType>();
	private Map<String, GeneralizationType> baselineMovedGeneralizations = new HashMap<String, GeneralizationType>();
	private Map<String, AssociationType> baselineAssociations = new HashMap<String, AssociationType>();
	private Map<String, AssociationType> baselineDeletedAssociations = new HashMap<String, AssociationType>();
	private Map<String, AssociationType> baselineMovedAssociations = new HashMap<String, AssociationType>();

	private Map<String, PackageType> targetPackages = new HashMap<String, PackageType>();
	private Map<String, PackageType> targetNewPackages = new HashMap<String, PackageType>();
	private Map<String, PackageType> targetMovedPackages = new HashMap<String, PackageType>();
	private Map<String, ClassType> targetClasses = new HashMap<String, ClassType>();
	private Map<String, ClassType> targetNewClasses = new HashMap<String, ClassType>();
	private Map<String, ClassType> targetMovedClasses = new HashMap<String, ClassType>();
	private Map<String, GeneralizationType> targetGeneralizations = new HashMap<String, GeneralizationType>();
	private Map<String, GeneralizationType> targetNewGeneralizations = new HashMap<String, GeneralizationType>();
	private Map<String, GeneralizationType> targetMovedGeneralizations = new HashMap<String, GeneralizationType>();
	private Map<String, AssociationType> targetAssociations = new HashMap<String, AssociationType>();
	private Map<String, AssociationType> targetNewAssociations = new HashMap<String, AssociationType>();
	private Map<String, AssociationType> targetMovedAssociations = new HashMap<String, AssociationType>();

	private Set<String> packagesInBoth = new HashSet<String>();
	private Set<String> classesInBoth = new HashSet<String>();
	private Set<String> generalizationsInBoth = new HashSet<String>();
	private Set<String> associationsInBoth = new HashSet<String>();

	private KeyTypeEnum keyType;

	@FunctionalInterface
	public interface Key<T> {
		String getKey(T element);
	}

	public PreProcessor(Model baselineModel, Model targetModel) {

		initialize(baselineModel, //
				baselinePackagesGUIDs, //
				packageGUIDsInBoth, //
				baselineClassesGUIDs, //
				classGUIDsInBoth, //
				baselineGeneralizationsGUIDs, //
				generalizationGUIDsInBoth, //
				baselineAssociationsGUIDs, //
				associationGUIDsInBoth, //
				aPackage -> DiffUtils.convertXmiIdToEAGUID(aPackage.getXmiId()), //
				aClass -> DiffUtils.convertXmiIdToEAGUID(aClass.getXmiId()), //
				aGeneralization -> DiffUtils.convertXmiIdToEAGUID(aGeneralization.getXmiId()), //
				anAssociation -> DiffUtils.convertXmiIdToEAGUID(anAssociation.getXmiId()));

		initialize(targetModel, //
				targetPackagesGUIDs, //
				packageGUIDsInBoth, //
				targetClassesGUIDs, //
				classGUIDsInBoth, //
				targetGeneralizationsGUIDs, //
				generalizationGUIDsInBoth, //
				targetAssociationsGUIDs, //
				associationGUIDsInBoth, //
				aPackage -> DiffUtils.convertXmiIdToEAGUID(aPackage.getXmiId()), //
				aClass -> DiffUtils.convertXmiIdToEAGUID(aClass.getXmiId()), //
				aGeneralization -> DiffUtils.convertXmiIdToEAGUID(aGeneralization.getXmiId()), //
				anAssociation -> DiffUtils.convertXmiIdToEAGUID(anAssociation.getXmiId()));

		double packagePercentage = 1.0 - ((double) packageGUIDsInBoth.size()
				/ (double) (baselinePackagesGUIDs.size() + targetPackagesGUIDs.size()));
		
		/**
		 * Using the unique set of class GUIDs derived from both the baseline and target
		 * models we determine what level of GUID "commonality" exists between the two
		 * models. This is done using a simple percentage-based calculation whereby if
		 * 10% or more of the class GUIDs from the baseline model appear in the target model,
		 * then we set the keyType to "GUID-based". Otherwise, we assume that a non-GUID
		 * based approach will be needed and that we cannot rely on any consistency to
		 * exist between the GUIDs of the packages/classes in the baseline verses those
		 * in the target model...
		 */
		double classPercentage = 1.0 - ((double) classGUIDsInBoth.size()
				/ (double) (baselineClassesGUIDs.size() + targetClassesGUIDs.size()));

		keyType = (classPercentage >= .10 ? KeyTypeEnum.GUID : KeyTypeEnum.NAME);
		KeyType.KEY_TYPE = keyType;

		System.out.println();
		System.out.println("====================== STATISTICAL OVERVIEW ======================");
		System.out.println("   Total unique baseline package GUIDs:                " + baselinePackagesGUIDs.size());
		System.out.println("   Total unique target package GUIDs:                  " + targetPackagesGUIDs.size());
		System.out.println("   Total count of ALL package GUIDS across models:     " + packageGUIDsInBoth.size());
		System.out.println("   Percentage of package GUIDs common to both models:  " + String.format("%.2f", (packagePercentage * 100.0))  + " %");
		System.out.println("");
		System.out.println("   Total unique baseline class GUIDs:                  " + baselineClassesGUIDs.size());
		System.out.println("   Total unique target class GUIDs:                    " + targetClassesGUIDs.size());
		System.out.println("   Total count of ALL class GUIDS across models:       " + classGUIDsInBoth.size());
		System.out.println();
		System.out.println("   Percentage of class GUIDs common to both models:    " + String.format("%.2f", (classPercentage * 100.0))  + " %");
		System.out.println("==================================================================");

		if (KeyTypeEnum.NAME.equals(keyType)) {
			initialize(baselineModel, //
					baselinePackages, //
					packagesInBoth, //
					baselineClasses, //
					classesInBoth, //
					baselineGeneralizations, //
					generalizationsInBoth, //
					baselineAssociations, //
					associationsInBoth, //
					aPackage -> DiffUtils.convertXmiIdToEAGUID(aPackage.getName()), //
					aClass -> DiffUtils.convertXmiIdToEAGUID(aClass.getName()), //
					aGeneralization -> DiffUtils.convertXmiIdToEAGUID(aGeneralization.getName()), //
					anAssociation -> DiffUtils.convertXmiIdToEAGUID(anAssociation.getName()));

			initialize(targetModel, //
					targetPackages, //
					packagesInBoth, //
					targetClasses, //
					classesInBoth, //
					targetGeneralizations, //
					generalizationsInBoth, //
					targetAssociations, //
					associationsInBoth, //
					aPackage -> DiffUtils.convertXmiIdToEAGUID(aPackage.getName()), //
					aClass -> DiffUtils.convertXmiIdToEAGUID(aClass.getName()), //
					aGeneralization -> DiffUtils.convertXmiIdToEAGUID(aGeneralization.getName()), //
					anAssociation -> DiffUtils.convertXmiIdToEAGUID(anAssociation.getName()));
		}

		postInitialization();
	}

	private void initialize(Model model, //
			Map<String, PackageType> packages, //
			Set<String> allPackages, //
			Map<String, ClassType> classes, //
			Set<String> allClasses, //
			Map<String, GeneralizationType> generalizations, //
			Set<String> allGeneralizations, //
			Map<String, AssociationType> associations, //
			Set<String> allAssocations, //
			Key<PackageType> packageKeyFunction, //
			Key<ClassType> classKeyFunction, //
			Key<GeneralizationType> generalizationKeyFunction, //
			Key<AssociationType> assocationKeyFunction) {

		NamespaceOwnedElementType ownedElement = model.getNamespaceOwnedElement();

		if (ownedElement != null) {
			for (PackageType aPackage : ownedElement.getPackages()) {
				processPackage(aPackage, packages, //
						allPackages, //
						classes, //
						allClasses, //
						generalizations, //
						allGeneralizations, //
						associations, //
						allAssocations, //
						packageKeyFunction, //
						classKeyFunction, //
						generalizationKeyFunction, //
						assocationKeyFunction);
			}

			for (ClassType aClass : ownedElement.getClasses()) {
				processClass(aClass, classes, allClasses, classKeyFunction);
			}
		}

	}

	private void processPackage(PackageType thePackage, Map<String, PackageType> packages, //
			Set<String> allPackages, //
			Map<String, ClassType> classes, //
			Set<String> allClasses, //
			Map<String, GeneralizationType> generalizations, //
			Set<String> allGeneralizations, //
			Map<String, AssociationType> associations, //
			Set<String> allAssocations, //
			Key<PackageType> packageKeyFunction, //
			Key<ClassType> classKeyFunction, //
			Key<GeneralizationType> generalizationKeyFunction, //
			Key<AssociationType> assocationKeyFunction) {

		String key = packageKeyFunction.getKey(thePackage);

		packages.put(key, thePackage);
		allPackages.add(key);

		NamespaceOwnedElementType ownedElement = thePackage.getNamespaceOwnedElement();

		if (ownedElement != null) {
			for (PackageType aPackage : ownedElement.getPackages()) {
				processPackage(aPackage, //
						packages, //
						allPackages, //
						classes, //
						allClasses, //
						generalizations, //
						allGeneralizations, //
						associations, //
						allAssocations, //
						packageKeyFunction, //
						classKeyFunction, //
						generalizationKeyFunction, //
						assocationKeyFunction);
			}

			for (ClassType aClass : ownedElement.getClasses()) {
				processClass(aClass, classes, allClasses, classKeyFunction);
			}

			for (GeneralizationType aGeneralization : ownedElement.getGeneralizations()) {
				processGeneralization(aGeneralization, generalizations, allGeneralizations, generalizationKeyFunction);
			}

			for (AssociationType anAssociation : ownedElement.getAssociations()) {
				processAssociation(anAssociation, associations, allAssocations, assocationKeyFunction);
			}

		}

	}

	private void processClass(ClassType aClass, Map<String, ClassType> classMappings, Set<String> allClassMappings,
			Key<ClassType> classKey) {
		String key = classKey.getKey(aClass);
		classMappings.put(key, aClass);
		allClassMappings.add(key);
	}

	private void processGeneralization(GeneralizationType aGeneralization,
			Map<String, GeneralizationType> generalizationMappings, Set<String> allGeneralizationMappings,
			Key<GeneralizationType> generalizationKey) {
		String key = generalizationKey.getKey(aGeneralization);
		generalizationMappings.put(key, aGeneralization);
		allGeneralizationMappings.add(key);
	}

	private void processAssociation(AssociationType anAssociation, Map<String, AssociationType> associationMappings,
			Set<String> allAssociationMappings, Key<AssociationType> associationKey) {
		String key = associationKey.getKey(anAssociation);
		associationMappings.put(key, anAssociation);
		allAssociationMappings.add(key);
	}

	/**
	 * Here we perform post initialization processing and build out the mappings for
	 * deletions and additions.
	 */
	private void postInitialization() {

		switch (keyType)
			{
			case GUID:

				/**
				 * ==================================================================================
				 * We iterate through all packages contained across the baseline and target
				 * models to determine into which category they fall: "New", "Moved" or
				 * "Deleted"
				 * ==================================================================================
				 */
				for (String guid : baselinePackagesGUIDs.keySet()) {
					// First, check if the package was "Deleted"
					if (!targetPackagesGUIDs.containsKey(guid)) {
						baselineDeletedPackagesGUIDs.put(guid, baselinePackagesGUIDs.get(guid));
					} else {
						// We now check to determine if the package was "Moved"
						PackageType baselinePackage = baselinePackagesGUIDs.get(guid);
						ModelElementTaggedValue baselineTaggedValues = baselinePackage.getModelElementTaggedValue();

						PackageType targetPackage = targetPackagesGUIDs.get(guid);
						ModelElementTaggedValue targetTaggedValues = targetPackage.getModelElementTaggedValue();

						if ((baselineTaggedValues != null && baselineTaggedValues.getTaggedValue("parent") != null)
								&& (targetTaggedValues != null
										&& targetTaggedValues.getTaggedValue("parent") != null)) {
							String baselineParentPackageGUID = baselineTaggedValues.getTaggedValue("parent")
									.getTheValue();
							String targetParentPackageGUID = targetTaggedValues.getTaggedValue("parent").getTheValue();

							if (!baselineParentPackageGUID.equals(targetParentPackageGUID)) {
								// Package was moved...
								baselineMovedPackagesGUIDs.put(guid, baselinePackagesGUIDs.get(guid));
								targetMovedPackagesGUIDs.put(guid, targetPackagesGUIDs.get(guid));
							}
						}
					}
				}
				// Identify any "New" (i.e. "Model Only") packages that appear in the target
				// model
				for (String guid : targetPackagesGUIDs.keySet()) {
					if (!baselinePackagesGUIDs.containsKey(guid)) {
						targetNewPackagesGUIDs.put(guid, targetPackagesGUIDs.get(guid));
					}
				}

				/**
				 * ==================================================================================
				 * Now iterate through all classes contained across the baseline and target
				 * models to determine into which category they fall: "New", "Moved" or
				 * "Deleted"
				 * ==================================================================================
				 */
				for (String guid : baselineClassesGUIDs.keySet()) {
					if (!targetClassesGUIDs.containsKey(guid)) {
						baselineDeletedClassesGUIDs.put(guid, baselineClassesGUIDs.get(guid));
					} else {
						// We now check to determine if the package was "Moved"
						ClassType baselineClass = baselineClassesGUIDs.get(guid);
						ModelElementTaggedValue baselineTaggedValues = baselineClass.getModelElementTaggedValue();

						ClassType targetClass = targetClassesGUIDs.get(guid);
						ModelElementTaggedValue targetTaggedValues = targetClass.getModelElementTaggedValue();

						if ((baselineTaggedValues != null && baselineTaggedValues.getTaggedValue("package") != null)
								&& (targetTaggedValues != null
										&& targetTaggedValues.getTaggedValue("package") != null)) {
							String baselineParentPackageGUID = baselineTaggedValues.getTaggedValue("package")
									.getTheValue();
							String targetParentPackageGUID = targetTaggedValues.getTaggedValue("package").getTheValue();

							if (!baselineParentPackageGUID.equals(targetParentPackageGUID)) {
								// Package was moved...
								baselineMovedClassesGUIDs.put(guid, baselineClassesGUIDs.get(guid));
								targetMovedClassesGUIDs.put(guid, targetClassesGUIDs.get(guid));
							}
						}
					}
				}
				//
				// Identify any "New" (i.e. "Model Only") classes that appear in the target
				// model
				for (String guid : targetClassesGUIDs.keySet()) {
					if (!baselineClassesGUIDs.containsKey(guid)) {
						targetNewClassesGUIDs.put(guid, targetClassesGUIDs.get(guid));
					}
				}

				/**
				 * ==================================================================================
				 * Now iterate through all generalizations contained across the baseline and
				 * target models to determine into which category they fall: "New", "Moved" or
				 * "Deleted"
				 * ==================================================================================
				 */
				for (String guid : baselineGeneralizationsGUIDs.keySet()) {
					if (!targetGeneralizationsGUIDs.containsKey(guid)) {
						baselineDeletedGeneralizationsGUIDs.put(guid, baselineGeneralizationsGUIDs.get(guid));
					} else {
						// We now check to determine if the package was "Moved"
						GeneralizationType baselineGeneralization = baselineGeneralizationsGUIDs.get(guid);
						ModelElementTaggedValue baselineTaggedValues = baselineGeneralization
								.getModelElementTaggedValue();

						GeneralizationType targetGeneralization = targetGeneralizationsGUIDs.get(guid);
						ModelElementTaggedValue targetTaggedValues = targetGeneralization.getModelElementTaggedValue();

						if ((baselineTaggedValues != null && baselineTaggedValues.getTaggedValue("package") != null)
								&& (targetTaggedValues != null
										&& targetTaggedValues.getTaggedValue("package") != null)) {
							String baselineParentPackageGUID = baselineTaggedValues.getTaggedValue("package")
									.getTheValue();
							String targetParentPackageGUID = targetTaggedValues.getTaggedValue("package").getTheValue();

							if (!baselineParentPackageGUID.equals(targetParentPackageGUID)) {
								// Package was moved...
								baselineMovedGeneralizationsGUIDs.put(guid, baselineGeneralizationsGUIDs.get(guid));
								targetMovedGeneralizationsGUIDs.put(guid, targetGeneralizationsGUIDs.get(guid));
							}
						}
					}
				}
				//
				// Identify any "New" (i.e. "Model Only") generalizations that appear in the
				// target
				// model
				for (String guid : targetGeneralizationsGUIDs.keySet()) {
					if (!baselineGeneralizationsGUIDs.containsKey(guid)) {
						targetNewGeneralizationsGUIDs.put(guid, targetGeneralizationsGUIDs.get(guid));
					}
				}

				/**
				 * ==================================================================================
				 * Now iterate through all associations contained across the baseline and target
				 * models to determine into which category they fall: "New", "Moved" or
				 * "Deleted"
				 * ==================================================================================
				 */
				for (String guid : baselineAssociationsGUIDs.keySet()) {
					if (!targetAssociationsGUIDs.containsKey(guid)) {
						baselineDeletedAssociationsGUIDs.put(guid, baselineAssociationsGUIDs.get(guid));
					} else {
						// We now check to determine if the package was "Moved"
						AssociationType baselineAssociation = baselineAssociationsGUIDs.get(guid);
						ModelElementTaggedValue baselineTaggedValues = baselineAssociation.getModelElementTaggedValue();

						AssociationType targetAssociation = targetAssociationsGUIDs.get(guid);
						ModelElementTaggedValue targetTaggedValues = targetAssociation.getModelElementTaggedValue();

						if ((baselineTaggedValues != null && baselineTaggedValues.getTaggedValue("package") != null)
								&& (targetTaggedValues != null
										&& targetTaggedValues.getTaggedValue("package") != null)) {
							String baselineParentPackageGUID = baselineTaggedValues.getTaggedValue("package")
									.getTheValue();
							String targetParentPackageGUID = targetTaggedValues.getTaggedValue("package").getTheValue();

							if (!baselineParentPackageGUID.equals(targetParentPackageGUID)) {
								// Package was moved...
								baselineMovedAssociationsGUIDs.put(guid, baselineAssociationsGUIDs.get(guid));
								targetMovedAssociationsGUIDs.put(guid, targetAssociationsGUIDs.get(guid));
							}
						}
					}
				}
				//
				// Identify any "New" (i.e. "Model Only") classes that appear in the target
				// model
				for (String guid : targetAssociationsGUIDs.keySet()) {
					if (!baselineAssociationsGUIDs.containsKey(guid)) {
						targetNewAssociationsGUIDs.put(guid, targetAssociationsGUIDs.get(guid));
					}
				}

				/**
				 * Final step is to clean up the primary mappings to remove those
				 * packages/classes/generalizations/associations identified as either deleted,
				 * moved, etc...
				 */

				/** Clean up Package mappings... */
				for (String guid : baselineDeletedPackagesGUIDs.keySet()) {
					packageGUIDsInBoth.remove(guid);
				}
				//
				for (String guid : targetNewPackagesGUIDs.keySet()) {
					targetPackagesGUIDs.remove(guid);
					packageGUIDsInBoth.remove(guid);
				}

				/** Clean up Class mappings... */
				for (String guid : baselineDeletedClassesGUIDs.keySet()) {
					baselineClassesGUIDs.remove(guid);
					classGUIDsInBoth.remove(guid);
				}
				//
				for (String guid : targetNewClassesGUIDs.keySet()) {
					classGUIDsInBoth.remove(guid);
				}

				/** Clean up Generalization mappings... */
				for (String guid : baselineDeletedGeneralizationsGUIDs.keySet()) {
					baselineGeneralizationsGUIDs.remove(guid);
					generalizationGUIDsInBoth.remove(guid);
				}
				//
				for (String guid : targetNewGeneralizationsGUIDs.keySet()) {
					generalizationGUIDsInBoth.remove(guid);
				}

				/** Clean up Association mappings... */
				for (String guid : baselineDeletedAssociationsGUIDs.keySet()) {
					baselineAssociationsGUIDs.remove(guid);
					associationGUIDsInBoth.remove(guid);
				}
				//
				for (String guid : targetNewAssociationsGUIDs.keySet()) {
					associationGUIDsInBoth.remove(guid);
				}
				break;
			case NAME:
				for (String packageName : baselinePackages.keySet()) {
					if (targetPackages.containsKey(packageName)) {

						/**
						 * This IF covers the scenario whereby a package may have been renamed and
						 * "Moved".
						 */
						PackageType baselinePackage = baselinePackages.get(packageName);
						ModelElementTaggedValue baselineTaggedValues = baselinePackage.getModelElementTaggedValue();

						PackageType targetPackage = targetPackages.get(packageName);
						ModelElementTaggedValue targetTaggedValues = targetPackage.getModelElementTaggedValue();

						if ((baselineTaggedValues != null && baselineTaggedValues.getTaggedValue("parent") != null)
								&& (targetTaggedValues != null
										&& targetTaggedValues.getTaggedValue("parent") != null)) {

							String baselineParentPackageGUID = DiffUtils
									.convertXmiIdToEAGUID(baselineTaggedValues.getTaggedValue("parent").getTheValue());

							String targetParentPackageGUID = DiffUtils
									.convertXmiIdToEAGUID(targetTaggedValues.getTaggedValue("parent").getTheValue());

							// Note...that this one MUST be pulled from the BASELINE_PACKAGES_GUIDS as the
							// value is a GUID
							PackageType baselineParentPackage = baselinePackagesGUIDs.get(baselineParentPackageGUID);
							PackageType targetParentPackage = targetPackagesGUIDs.get(targetParentPackageGUID);

							if (baselineParentPackage != null && targetParentPackage != null
									&& !baselineParentPackage.getName().equals(targetParentPackage)) {
								// The package that contains this package has a different name meaning that we
								// will assume the package was either "Moved" into a different package or that
								// it is deleted from the baseline and a new package (with the same name) has
								// been added to the target model.
								//
								// To determine this we call the comparePackageContents() method to see if the
								// contents of the package consists of a fairly close representation (>= 25%) of
								// the same named packages/classes.
								if (comparePackageContents(baselinePackage, targetPackage)) {
									// Package contents appear to be close so we assume the package was renamed and
									// "Moved".
									baselineMovedPackages.put(packageName, baselinePackages.get(packageName));
									targetMovedPackages.put(packageName, targetPackages.get(packageName));
								} else {
									// Though the packages are named the same contents appear to be close so we
									// assume the package is "Moved"
									baselineDeletedPackages.put(packageName, baselinePackages.get(packageName));
									targetNewPackages.put(packageName, targetPackages.get(packageName));
								}
							}
						}
					} else {
						/**
						 * This else covers the scenario whereby a package may have been renamed and
						 * "Moved". Should be RARE.
						 */
						PackageType baselinePackage = baselinePackages.get(packageName);
						for (String targetPackageName : targetPackages.keySet()) {
							PackageType targetPackage = targetPackages.get(targetPackageName);
							if (comparePackageContents(baselinePackage, targetPackage)) {
								baselineMovedPackages.put(packageName, baselinePackages.get(packageName));
								// Now break out of the for loop. No need to progress further as we found a
								// renamed "match".
								break;
							}
						}
					}
				}
				//
				for (String packageName : targetPackages.keySet()) {
					if (!baselinePackages.containsKey(packageName)) {
						targetNewPackages.put(packageName, targetPackages.get(packageName));
					}
				}
				//
				for (String className : baselineClasses.keySet()) {
					if (targetClasses.containsKey(className)) {

						/**
						 * This IF covers the scenario whereby a class may have been renamed and
						 * "Moved".
						 */
						ClassType baselineClass = baselineClasses.get(className);
						ModelElementTaggedValue baselineTaggedValues = baselineClass.getModelElementTaggedValue();

						ClassType targetClass = targetClasses.get(className);
						ModelElementTaggedValue targetTaggedValues = targetClass.getModelElementTaggedValue();

						if ((baselineTaggedValues != null
								&& baselineTaggedValues.getTaggedValue("package_name") != null)
								&& (targetTaggedValues != null
										&& targetTaggedValues.getTaggedValue("package_name") != null)) {

							String baselineParentPackageName = baselineTaggedValues.getTaggedValue("package_name")
									.getTheValue();
							String targetParentPackageName = targetTaggedValues.getTaggedValue("package_name")
									.getTheValue();

							if (!baselineParentPackageName.equals(targetParentPackageName)) {
								// The package that contains this class has a different name meaning that we
								// will assume the class was either "Moved" into a different package or that
								// it was deleted from the baseline and a new class (with the same name) was
								// added to the target model.
								//
								// To determine this a call to the compareClassContents() method to see if the
								// contents of the class consists of a fairly close representation (>= 25%) of
								// the same named attributes.
								if (compareClassContents(baselineClass, targetClass)) {
									// Package contents appear to be close so we assume the package was renamed and
									// "Moved".
									baselineMovedClasses.put(className, baselineClasses.get(className));
									targetMovedClasses.put(className, targetClasses.get(className));
								} else {
									// Though the packages are named the same contents appear to be close so we
									// assume the package is "Moved"
									baselineDeletedClasses.put(className, baselineClasses.get(className));
									targetNewClasses.put(className, targetClasses.get(className));
								}
							}
						}
					} else {
						/**
						 * This else covers the scenario whereby a class may have been renamed and
						 * "Moved". Should be RARE.
						 */
						ClassType baselineClass = baselineClasses.get(className);
						for (String targetClassName : targetClasses.keySet()) {
							ClassType targetClass = targetClasses.get(targetClassName);
							if (compareClassContents(baselineClass, targetClass)) {
								baselineMovedClasses.put(className, baselineClasses.get(className));
								// Now break out of the for loop. No need to progress further as we found a
								// renamed "match".
								break;
							}
						}
					}
				}
				//
				for (String className : targetClasses.keySet()) {
					if (!baselineClasses.containsKey(className)) {
						targetNewClasses.put(className, targetClasses.get(className));
					}
				}

				/**
				 * Finally step is to clean up the primary mappings to remove those
				 * packages/classes identified as either deleted, moved, etc.
				 */

				/** Clean up Package mappings... */
				for (String guid : baselineDeletedPackages.keySet()) {
					packagesInBoth.remove(guid);
				}
				//
				for (String guid : targetNewPackages.keySet()) {
					targetPackages.remove(guid);
					packagesInBoth.remove(guid);
				}

				/** Clean up Class mappings... */
				for (String guid : baselineDeletedClasses.keySet()) {
					baselineClasses.remove(guid);
					classGUIDsInBoth.remove(guid);
				}
				//
				for (String guid : targetNewClasses.keySet()) {
					classesInBoth.remove(guid);
				}

				/** Clean up Generalization mappings... */
				for (String guid : baselineDeletedGeneralizations.keySet()) {
					baselineGeneralizations.remove(guid);
					generalizationsInBoth.remove(guid);
				}
				//
				for (String guid : targetNewGeneralizations.keySet()) {
					generalizationsInBoth.remove(guid);
				}

				/** Clean up Association mappings... */
				for (String guid : baselineDeletedAssociations.keySet()) {
					baselineAssociations.remove(guid);
					associationsInBoth.remove(guid);
				}
				//
				for (String guid : targetNewAssociations.keySet()) {
					associationsInBoth.remove(guid);
				}
				break;
			}
	}

	/**
	 * Method that compares the package contents of the two packages passed in. If
	 * the packages have the same GUID then it is guaranteed to be the same package
	 * and we simply return true. If not we instead test the package contents to see
	 * if >= 25% of the package's classes have matching names. If so we assume the
	 * packages are the "same". The 25% is an really "rough" arbitrarily chosen
	 * number intended to approximate what could be deemed as "similar" package
	 * contents.
	 * 
	 * @param baselinePackage
	 * @param targetPackage
	 * @return
	 */
	private boolean comparePackageContents(PackageType baselinePackage, PackageType targetPackage) {
		if (baselinePackage.getXmiId().equals(targetPackage.getXmiId())) {
			return true;
		}

		/**
		 * First, compare the similarities in packages contained within the parent
		 * packages passed in.
		 */
		int baselinePackagePackagesCount = (baselinePackage.getNamespaceOwnedElement() != null
				? baselinePackage.getNamespaceOwnedElement().getPackages().size()
				: 0);
		int targetPackagePackagesCount = (targetPackage.getNamespaceOwnedElement() != null
				? targetPackage.getNamespaceOwnedElement().getPackages().size()
				: 0);

		PackageType outerPackage;
		PackageType innerPackage;

		boolean hasComparablePackages = false;

		// If either parent package has no child packages contents then we are going to
		// assume
		// they are not the same package.
		if (baselinePackagePackagesCount != 0 && targetPackagePackagesCount != 0) {
			if (baselinePackagePackagesCount >= targetPackagePackagesCount) {
				outerPackage = baselinePackage;
				innerPackage = targetPackage;
			} else {
				outerPackage = targetPackage;
				innerPackage = baselinePackage;
			}

			int matchingPackagesCount = 0;

			for (PackageType anOuterPackage : outerPackage.getNamespaceOwnedElement().getPackages()) {
				for (PackageType anInnerPackage : innerPackage.getNamespaceOwnedElement().getPackages()) {
					if (anOuterPackage.getName().equals(anInnerPackage.getName())) {
						matchingPackagesCount += 1;
					}
				}
			}

			hasComparablePackages = (((double) matchingPackagesCount
					/ (double) outerPackage.getNamespaceOwnedElement().getPackages().size()) >= .25 ? true : false);
		}

		/**
		 * Second, compare the similarities in classes contained within the parent
		 * packages passed in.
		 */
		int baselinePackageClassesCount = (baselinePackage.getNamespaceOwnedElement() != null
				? baselinePackage.getNamespaceOwnedElement().getClasses().size()
				: 0);
		int targetPackageClassesCount = (targetPackage.getNamespaceOwnedElement() != null
				? targetPackage.getNamespaceOwnedElement().getClasses().size()
				: 0);

		boolean hasComparableClasses = false;

		// If either parent package has no class contents then we are going to assume
		// they are not the same package.
		if (baselinePackageClassesCount != 0 && targetPackageClassesCount != 0) {
			if (baselinePackageClassesCount >= targetPackageClassesCount) {
				outerPackage = baselinePackage;
				innerPackage = targetPackage;
			} else {
				outerPackage = targetPackage;
				innerPackage = baselinePackage;
			}

			int matchingClassesCount = 0;
			for (ClassType outerClass : outerPackage.getNamespaceOwnedElement().getClasses()) {
				for (ClassType innerClass : innerPackage.getNamespaceOwnedElement().getClasses()) {
					if (outerClass.getName().equals(innerClass.getName())) {
						matchingClassesCount += 1;
					}
				}
			}

			hasComparableClasses = (((double) matchingClassesCount
					/ (double) outerPackage.getNamespaceOwnedElement().getClasses().size()) >= .25 ? true : false);
		}

		return (hasComparablePackages || hasComparableClasses);
	}

	/**
	 * Method that compares the class contents of the two classes passed in. If the
	 * classes have the same GUID then it is guaranteed to be the same class and we
	 * simply return true. If not we instead test the class's contents to see if >=
	 * 25% of the classes attributes match. If so we assume the classes are the
	 * "same". The 25% is a really "rough" arbitrarily chosen percentage intended to
	 * approximate what could be deemed as "similar" class contents.
	 * 
	 * @param baselineClass
	 * @param targetClass
	 * @return
	 */
	private boolean compareClassContents(ClassType baselineClass, ClassType targetClass) {
		if (baselineClass.getXmiId().equals(targetClass.getXmiId())) {
			return true;
		}

		// If either has no attribute contents then we are going to assume they are not
		// the same class.
		if (baselineClass.getAttributes().size() == 0 || targetClass.getAttributes().size() == 0) {
			return false;
		}

		ClassType outerClass;
		ClassType innerClass;

		if (baselineClass.getAttributes().size() >= targetClass.getAttributes().size()) {
			outerClass = baselineClass;
			innerClass = targetClass;
		} else {
			outerClass = targetClass;
			innerClass = baselineClass;
		}

		int matchingAttributesCount = 0;

		for (AttributeType outerAttribute : outerClass.getAttributes()) {
			for (AttributeType innerAttribute : innerClass.getAttributes()) {
				if (outerAttribute.getName().equals(innerAttribute.getName())) {
					matchingAttributesCount += 1;
				}
			}
		}

		return (((double) matchingAttributesCount / (double) outerClass.getAttributes().size()) >= .25 ? true : false);
	}

	public Map<String, GeneralizationType> getBaselineGeneralizationsGUIDs() {
		return baselineGeneralizationsGUIDs;
	}

	public Map<String, AssociationType> getBaselineAssociationsGUIDs() {
		return baselineAssociationsGUIDs;
	}

	public Map<String, PackageType> getBaselinePackagesGUIDs() {
		return baselinePackagesGUIDs;
	}

	public Map<String, PackageType> getBaselineDeletedPackagesGUIDs() {
		return baselineDeletedPackagesGUIDs;
	}

	public Map<String, PackageType> getBaselineMovedPackagesGUIDs() {
		return baselineMovedPackagesGUIDs;
	}

	public Map<String, ClassType> getBaselineClassesGUIDs() {
		return baselineClassesGUIDs;
	}

	public Map<String, ClassType> getBaselineDeletedClassesGUIDs() {
		return baselineDeletedClassesGUIDs;
	}

	public Map<String, ClassType> getBaselineMovedClassesGUIDs() {
		return baselineMovedClassesGUIDs;
	}

	public Map<String, PackageType> getTargetPackagesGUIDs() {
		return targetPackagesGUIDs;
	}

	public Map<String, PackageType> getTargetNewPackagesGUIDs() {
		return targetNewPackagesGUIDs;
	}

	public Map<String, PackageType> getTargetMovedPackagesGUIDs() {
		return targetMovedPackagesGUIDs;
	}

	public Map<String, ClassType> getTargetClassesGUIDs() {
		return targetClassesGUIDs;
	}

	public Map<String, ClassType> getTargetNewClassesGUIDs() {
		return targetNewClassesGUIDs;
	}

	public Map<String, ClassType> getTargedMovedClassesGUIDs() {
		return targetMovedClassesGUIDs;
	}

	/**
	 * Method to retrieve the Set of GUIDs for ALL packages that exist in BOTH
	 * baseline and target models.
	 * 
	 * @return
	 */
	public Set<String> getPackageGUIDsInBoth() {
		return packageGUIDsInBoth;
	}

	/**
	 * Method to retrieve the Set of GUIDs for ALL class that exist in BOTH baseline
	 * and target models.
	 * 
	 * @return
	 */
	public Set<String> getClassGUIDsInBoth() {
		return classGUIDsInBoth;
	}

	public Map<String, PackageType> getBaselinePackages() {
		return baselinePackages;
	}

	public Map<String, PackageType> getBaselineDeletedPackages() {
		return baselineDeletedPackages;
	}

	public Map<String, PackageType> getBaselineMovedPackages() {
		return baselineMovedPackages;
	}

	public Map<String, ClassType> getBaselineClasses() {
		return baselineClasses;
	}

	public Map<String, ClassType> getBaselineDeletedClasses() {
		return baselineDeletedClasses;
	}

	public Map<String, ClassType> getBaselineMovedClasses() {
		return baselineMovedClasses;
	}

	public Map<String, PackageType> getTargetPackages() {
		return targetPackages;
	}

	public Map<String, PackageType> getTargetNewPackages() {
		return targetNewPackages;
	}

	public Map<String, PackageType> getTargetMovedPackages() {
		return targetMovedPackages;
	}

	public Map<String, ClassType> getTargetClasses() {
		return targetClasses;
	}

	public Map<String, ClassType> getTargetNewClasses() {
		return targetNewClasses;
	}

	public Map<String, ClassType> getTargetMovedClasses() {
		return targetMovedClasses;
	}

	public Set<String> getPackagesInBoth() {
		return packagesInBoth;
	}

	public Set<String> getClassesInBoth() {
		return classesInBoth;
	}

	public KeyTypeEnum getKeyType() {
		return keyType;
	}
}
