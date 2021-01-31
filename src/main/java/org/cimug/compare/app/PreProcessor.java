package org.cimug.compare.app;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.cimug.compare.DiffUtils;
import org.cimug.compare.uml1_3.AssociationType;
import org.cimug.compare.uml1_3.ClassType;
import org.cimug.compare.uml1_3.GeneralizationType;
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

		/**
		 * Using the unique set of class GUIDs derived from both the baseline and target
		 * models we determine what level of GUID "commonality" exists between the two
		 * models. This is done using a simple percentage-based calculation whereby if
		 * 10% or more of the class GUIDs from the baseline model appear in the target
		 * model, then we set the keyType to "GUID-based". Otherwise, we assume that a
		 * non-GUID based approach will be needed and that we cannot rely on any
		 * consistency to exist between the GUIDs of the packages/classes in the
		 * baseline verses those in the target model...
		 */
		double packagePercentage = 1.0 - ((double) packageGUIDsInBoth.size()
				/ (double) (baselinePackagesGUIDs.size() + targetPackagesGUIDs.size()));

		double classPercentage = 1.0 - ((double) classGUIDsInBoth.size()
				/ (double) (baselineClassesGUIDs.size() + targetClassesGUIDs.size()));

		double generalizationsPercentage = 1.0 - ((double) generalizationGUIDsInBoth.size()
				/ (double) (baselineGeneralizationsGUIDs.size() + targetGeneralizationsGUIDs.size()));

		double associationsPercentage = 1.0 - ((double) associationGUIDsInBoth.size()
				/ (double) (baselineAssociationsGUIDs.size() + targetAssociationsGUIDs.size()));

		System.out.println();
		System.out.println("====================== STATISTICAL OVERVIEW ======================");
		System.out.println(
				"   Total unique baseline package GUIDs:                       " + baselinePackagesGUIDs.size());
		System.out
				.println("   Total unique target package GUIDs:                         " + targetPackagesGUIDs.size());
		System.out
				.println("   Total count of ALL package GUIDS across models:            " + packageGUIDsInBoth.size());
		System.out.println("   Percentage of package GUIDs common to both models:         "
				+ String.format("%.2f", (packagePercentage * 100.0)) + " %");
		System.out.println("");
		System.out.println(
				"   Total unique baseline class GUIDs:                         " + baselineClassesGUIDs.size());
		System.out
				.println("   Total unique target class GUIDs:                           " + targetClassesGUIDs.size());
		System.out.println("   Total count of ALL class GUIDS across models:              " + classGUIDsInBoth.size());
		System.out.println("   Percentage of class GUIDs common to both models:           "
				+ String.format("%.2f", (classPercentage * 100.0)) + " %");
		System.out.println("");
		System.out.println(
				"   Total unique baseline generalization GUIDs:                " + baselineGeneralizationsGUIDs.size());
		System.out.println(
				"   Total unique target generalization GUIDs:                  " + targetGeneralizationsGUIDs.size());
		System.out.println(
				"   Total count of ALL generalization GUIDS across models:     " + generalizationGUIDsInBoth.size());
		System.out.println("   Percentage of generalization GUIDs common to both models:  "
				+ String.format("%.2f", (generalizationsPercentage * 100.0)) + " %");
		System.out.println("");
		System.out.println(
				"   Total unique baseline association GUIDs:                   " + baselineAssociationsGUIDs.size());
		System.out.println(
				"   Total unique target association GUIDs:                     " + targetAssociationsGUIDs.size());
		System.out.println(
				"   Total count of ALL association GUIDS across models:        " + associationGUIDsInBoth.size());
		System.out.println("   Percentage of association GUIDs common to both models:     "
				+ String.format("%.2f", (associationsPercentage * 100.0)) + " %");
		System.out.println("==================================================================");

		postInitialization();
	}

	private void initialize( //
			Model model, //
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
				processPackage( //
						aPackage, //
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
				processClass( //
						aClass, //
						classes, //
						allClasses, //
						classKeyFunction);
			}
		}

	}

	private void processPackage( //
			PackageType thePackage, //
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

		String key = packageKeyFunction.getKey(thePackage);

		packages.put(key, thePackage);
		allPackages.add(key);

		NamespaceOwnedElementType ownedElement = thePackage.getNamespaceOwnedElement();

		if (ownedElement != null) {
			for (PackageType aPackage : ownedElement.getPackages()) {
				processPackage( //
						aPackage, //
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
						&& (targetTaggedValues != null && targetTaggedValues.getTaggedValue("parent") != null)) {
					String baselineParentPackageGUID = baselineTaggedValues.getTaggedValue("parent").getTheValue();
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
						&& (targetTaggedValues != null && targetTaggedValues.getTaggedValue("package") != null)) {
					String baselineParentPackageGUID = baselineTaggedValues.getTaggedValue("package").getTheValue();
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
				// We now check to determine if the package that the generalization was in
				// "Moved"
				GeneralizationType baselineGeneralization = baselineGeneralizationsGUIDs.get(guid);
				GeneralizationType targetGeneralization = targetGeneralizationsGUIDs.get(guid);

				// If either the subtype or supertype have changed we know that the
				// generalization was moved...
				if (!baselineGeneralization.getSubtype().equals(targetGeneralization.getSubtype())
						|| !baselineGeneralization.getSupertype().equals(targetGeneralization.getSupertype())) {
					baselineMovedGeneralizationsGUIDs.put(baselineGeneralization.getXmiId(), baselineGeneralization);
					targetMovedGeneralizationsGUIDs.put(targetGeneralization.getXmiId(), targetGeneralization);
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
				AssociationType targetAssociation = targetAssociationsGUIDs.get(guid);

				// If either the subtype or supertype have changed we know that the
				// generalization was moved...
				if (!baselineAssociation.getSourceAssociationEnd().getType().equals(targetAssociation.getSourceAssociationEnd().getType())
						|| !baselineAssociation.getDestinationAssociationEnd().getType().equals(targetAssociation.getDestinationAssociationEnd().getType())) {
					baselineMovedAssociationsGUIDs.put(baselineAssociation.getXmiId(), baselineAssociation);
					targetMovedAssociationsGUIDs.put(targetAssociation.getXmiId(), targetAssociation);
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

	public Map<String, GeneralizationType> getBaselineDeletedGeneralizationsGUIDs() {
		return baselineDeletedGeneralizationsGUIDs;
	}

	public Map<String, AssociationType> getBaselineDeletedAssociationsGUIDs() {
		return baselineDeletedAssociationsGUIDs;
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

}
