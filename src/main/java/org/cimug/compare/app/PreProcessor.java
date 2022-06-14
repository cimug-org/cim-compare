package org.cimug.compare.app;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.cimug.compare.uml1_3.AssociationType;
import org.cimug.compare.uml1_3.ClassType;
import org.cimug.compare.uml1_3.Diagram;
import org.cimug.compare.uml1_3.GeneralizationType;
import org.cimug.compare.uml1_3.ModelElementTaggedValue;
import org.cimug.compare.uml1_3.NamespaceOwnedElementType;
import org.cimug.compare.uml1_3.PackageType;
import org.cimug.compare.xmi1_1.XMIContentType;

public class PreProcessor {

	/** Maps to support XmiId-based processing */
	private Map<String, PackageType> baselinePackagesXmiIds = new HashMap<String, PackageType>();
	private Map<String, PackageType> baselineDeletedPackagesXmiIds = new HashMap<String, PackageType>();
	private Map<String, PackageType> baselineMovedPackagesXmiIds = new HashMap<String, PackageType>();
	private Map<String, ClassType> baselineClassesXmiIds = new HashMap<String, ClassType>();
	private Map<String, ClassType> baselineDeletedClassesXmiIds = new HashMap<String, ClassType>();
	private Map<String, ClassType> baselineMovedClassesXmiIds = new HashMap<String, ClassType>();
	private Map<String, GeneralizationType> baselineGeneralizationsXmiIds = new HashMap<String, GeneralizationType>();
	private Map<String, GeneralizationType> baselineDeletedGeneralizationsXmiIds = new HashMap<String, GeneralizationType>();
	private Map<String, GeneralizationType> baselineMovedGeneralizationsXmiIds = new HashMap<String, GeneralizationType>();
	private Map<String, AssociationType> baselineAssociationsXmiIds = new HashMap<String, AssociationType>();
	private Map<String, AssociationType> baselineDeletedAssociationsXmiIds = new HashMap<String, AssociationType>();
	private Map<String, AssociationType> baselineMovedAssociationsXmiIds = new HashMap<String, AssociationType>();
	private Map<String, Diagram> baselineDiagramsXmiIds = new HashMap<String, Diagram>();
	private Map<String, Diagram> baselineDeletedDiagramsXmiIds = new HashMap<String, Diagram>();
	private Map<String, Diagram> baselineMovedDiagramsXmiIds = new HashMap<String, Diagram>();

	private Map<String, PackageType> targetPackagesXmiIds = new HashMap<String, PackageType>();
	private Map<String, PackageType> targetNewPackagesXmiIds = new HashMap<String, PackageType>();
	private Map<String, PackageType> targetMovedPackagesXmiIds = new HashMap<String, PackageType>();
	private Map<String, ClassType> targetClassesXmiIds = new HashMap<String, ClassType>();
	private Map<String, ClassType> targetNewClassesXmiIds = new HashMap<String, ClassType>();
	private Map<String, ClassType> targetMovedClassesXmiIds = new HashMap<String, ClassType>();
	private Map<String, GeneralizationType> targetGeneralizationsXmiIds = new HashMap<String, GeneralizationType>();
	private Map<String, GeneralizationType> targetNewGeneralizationsXmiIds = new HashMap<String, GeneralizationType>();
	private Map<String, GeneralizationType> targetMovedGeneralizationsXmiIds = new HashMap<String, GeneralizationType>();
	private Map<String, AssociationType> targetAssociationsXmiIds = new HashMap<String, AssociationType>();
	private Map<String, AssociationType> targetNewAssociationsXmiIds = new HashMap<String, AssociationType>();
	private Map<String, AssociationType> targetMovedAssociationsXmiIds = new HashMap<String, AssociationType>();
	private Map<String, Diagram> targetDiagramsXmiIds = new HashMap<String, Diagram>();
	private Map<String, Diagram> targetNewDiagramsXmiIds = new HashMap<String, Diagram>();
	private Map<String, Diagram> targetMovedDiagramsXmiIds = new HashMap<String, Diagram>();

	private Set<String> packageXmiIdsInBoth = new HashSet<String>();
	private Set<String> classXmiIdsInBoth = new HashSet<String>();
	private Set<String> generalizationXmiIdsInBoth = new HashSet<String>();
	private Set<String> associationXmiIdsInBoth = new HashSet<String>();
	private Set<String> diagramXmiIdsInBoth = new HashSet<String>();

	@FunctionalInterface
	public interface Key<T> {
		String getKey(T element);
	}

	public PreProcessor(XMIContentType baselineContentType, XMIContentType targetContentType) {

		initialize(baselineContentType, //
				baselinePackagesXmiIds, //
				packageXmiIdsInBoth, //
				baselineClassesXmiIds, //
				classXmiIdsInBoth, //
				baselineGeneralizationsXmiIds, //
				generalizationXmiIdsInBoth, //
				baselineAssociationsXmiIds, //
				associationXmiIdsInBoth, //
				baselineDiagramsXmiIds, //
				diagramXmiIdsInBoth, //
				aPackage -> aPackage.getXmiId(), //
				aClass -> aClass.getXmiId(), //
				aGeneralization -> aGeneralization.getXmiId(), //
				anAssociation -> anAssociation.getXmiId(), //
				aDiagram -> aDiagram.getXmiId());

		initialize(targetContentType, //
				targetPackagesXmiIds, //
				packageXmiIdsInBoth, //
				targetClassesXmiIds, //
				classXmiIdsInBoth, //
				targetGeneralizationsXmiIds, //
				generalizationXmiIdsInBoth, //
				targetAssociationsXmiIds, //
				associationXmiIdsInBoth, //
				targetDiagramsXmiIds, //
				diagramXmiIdsInBoth, //
				aPackage -> aPackage.getXmiId(), //
				aClass -> aClass.getXmiId(), //
				aGeneralization -> aGeneralization.getXmiId(), //
				anAssociation -> anAssociation.getXmiId(), //
				aDiagram -> aDiagram.getXmiId());

		postInitialization();

		double packagePercentage = 1.0 - ((double) packageXmiIdsInBoth.size()
				/ (double) (baselinePackagesXmiIds.size() + targetPackagesXmiIds.size()));

		double classPercentage = 1.0 - ((double) classXmiIdsInBoth.size()
				/ (double) (baselineClassesXmiIds.size() + targetClassesXmiIds.size()));

		double generalizationsPercentage = 1.0 - ((double) generalizationXmiIdsInBoth.size()
				/ (double) (baselineGeneralizationsXmiIds.size() + targetGeneralizationsXmiIds.size()));

		double associationsPercentage = 1.0 - ((double) associationXmiIdsInBoth.size()
				/ (double) (baselineAssociationsXmiIds.size() + targetAssociationsXmiIds.size()));

		double diagramsPercentage = 1.0 - ((double) diagramXmiIdsInBoth.size()
				/ (double) (baselineDiagramsXmiIds.size() + targetDiagramsXmiIds.size()));

		/*
		System.out.println();
		System.out.println("====================== STATISTICAL OVERVIEW ======================");
		System.out.println(
				"   Total unique baseline package XmiIds:                       " + baselinePackagesXmiIds.size());
		System.out.println(
				"   Total unique target package XmiIds:                         " + targetPackagesXmiIds.size());
		System.out.println(
				"   Total count of ALL package XmiIds across models:            " + packageXmiIdsInBoth.size());
		System.out.println("   Percentage of package XmiIds common to both models:         "
				+ String.format("%.2f", (packagePercentage * 100.0)) + " %");
		System.out.println("");
		System.out.println(
				"   Total unique baseline class XmiIds:                         " + baselineClassesXmiIds.size());
		System.out.println(
				"   Total unique target class XmiIds:                           " + targetClassesXmiIds.size());
		System.out
				.println("   Total count of ALL class XmiIds across models:              " + classXmiIdsInBoth.size());
		System.out.println("   Percentage of class XmiIds common to both models:           "
				+ String.format("%.2f", (classPercentage * 100.0)) + " %");
		System.out.println("");
		System.out.println("   Total unique baseline generalization XmiIds:                "
				+ baselineGeneralizationsXmiIds.size());
		System.out.println(
				"   Total unique target generalization XmiIds:                  " + targetGeneralizationsXmiIds.size());
		System.out.println(
				"   Total count of ALL generalization XmiIds across models:     " + generalizationXmiIdsInBoth.size());
		System.out.println("   Percentage of generalization XmiIds common to both models:  "
				+ String.format("%.2f", (generalizationsPercentage * 100.0)) + " %");
		System.out.println("");
		System.out.println(
				"   Total unique baseline association XmiIds:                   " + baselineAssociationsXmiIds.size());
		System.out.println(
				"   Total unique target association XmiIds:                     " + targetAssociationsXmiIds.size());
		System.out.println(
				"   Total count of ALL association XmiIds across models:        " + associationXmiIdsInBoth.size());
		System.out.println("   Percentage of association XmiIds common to both models:     "
				+ String.format("%.2f", (associationsPercentage * 100.0)) + " %");

		System.out.println("");
		System.out
				.println("   Total unique baseline diagram XmiIds:                   " + baselineDiagramsXmiIds.size());
		System.out.println("   Total unique target diagram XmiIds:                     " + targetDiagramsXmiIds.size());
		System.out.println("   Total count of ALL diagram XmiIds across models:        " + diagramXmiIdsInBoth.size());
		System.out.println("   Percentage of diagram XmiIds common to both models:     "
				+ String.format("%.2f", (diagramsPercentage * 100.0)) + " %");
		System.out.println("==================================================================");
		*/
	}

	private void initialize( //
			XMIContentType contentType, //
			Map<String, PackageType> packages, //
			Set<String> allPackages, //
			Map<String, ClassType> classes, //
			Set<String> allClasses, //
			Map<String, GeneralizationType> generalizations, //
			Set<String> allGeneralizations, //
			Map<String, AssociationType> associations, //
			Set<String> allAssocations, //
			Map<String, Diagram> diagrams, //
			Set<String> allDiagrams, //
			Key<PackageType> packageKeyFunction, //
			Key<ClassType> classKeyFunction, //
			Key<GeneralizationType> generalizationKeyFunction, //
			Key<AssociationType> assocationKeyFunction, //
			Key<Diagram> diagramKeyFunction) {

		NamespaceOwnedElementType ownedElement = contentType.getModel().getNamespaceOwnedElement();

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

		if (contentType.getDiagrams() != null) {
			for (Diagram aDiagram : contentType.getDiagrams()) {
				processDiagram( //
						aDiagram, //
						diagrams, //
						allDiagrams, //
						diagramKeyFunction);
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

	private void processDiagram(Diagram aDiagram, Map<String, Diagram> diagramMappings, Set<String> allDiagramMappings,
			Key<Diagram> diagramKey) {
		String key = diagramKey.getKey(aDiagram);
		diagramMappings.put(key, aDiagram);
		allDiagramMappings.add(key);
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
		for (String xmiId : baselinePackagesXmiIds.keySet()) {
			// First, check if the package was "Deleted"
			if (!targetPackagesXmiIds.containsKey(xmiId)) {
				baselineDeletedPackagesXmiIds.put(xmiId, baselinePackagesXmiIds.get(xmiId));
			} else {
				// We now check to determine if the package was "Moved"
				PackageType baselinePackage = baselinePackagesXmiIds.get(xmiId);
				ModelElementTaggedValue baselineTaggedValues = baselinePackage.getModelElementTaggedValue();

				PackageType targetPackage = targetPackagesXmiIds.get(xmiId);
				ModelElementTaggedValue targetTaggedValues = targetPackage.getModelElementTaggedValue();

				if ((baselineTaggedValues != null && baselineTaggedValues.getTaggedValue("parent") != null)
						&& (targetTaggedValues != null && targetTaggedValues.getTaggedValue("parent") != null)) {
					String baselineParentPackageXmiId = baselineTaggedValues.getTaggedValue("parent").getTheValue();
					String targetParentPackageXmiId = targetTaggedValues.getTaggedValue("parent").getTheValue();

					if (!baselineParentPackageXmiId.equals(targetParentPackageXmiId)) {
						// Package was moved...
						baselineMovedPackagesXmiIds.put(xmiId, baselinePackagesXmiIds.get(xmiId));
						targetMovedPackagesXmiIds.put(xmiId, targetPackagesXmiIds.get(xmiId));
					}
				}
			}
		}
		// Identify any "New" (i.e. "Model Only") packages that appear in the target
		// model
		for (String xmiId : targetPackagesXmiIds.keySet()) {
			if (!baselinePackagesXmiIds.containsKey(xmiId)) {
				targetNewPackagesXmiIds.put(xmiId, targetPackagesXmiIds.get(xmiId));
			}
		}

		/**
		 * ==================================================================================
		 * Now iterate through all classes contained across the baseline and target
		 * models to determine into which category they fall: "New", "Moved" or
		 * "Deleted"
		 * ==================================================================================
		 */
		for (String xmiId : baselineClassesXmiIds.keySet()) {
			if (!targetClassesXmiIds.containsKey(xmiId)) {
				baselineDeletedClassesXmiIds.put(xmiId, baselineClassesXmiIds.get(xmiId));
			} else {
				// We now check to determine if the package was "Moved"
				ClassType baselineClass = baselineClassesXmiIds.get(xmiId);
				ClassType targetClass = targetClassesXmiIds.get(xmiId);

				if (baselineClass.getNamespace() != null && targetClass.getNamespace() != null) {
					String baselineParentPackageXmiId = baselineClass.getNamespace();
					String targetParentPackageXmiId = targetClass.getNamespace();

					if (!baselineParentPackageXmiId.equals(targetParentPackageXmiId)) {
						// Package was moved...
						baselineMovedClassesXmiIds.put(xmiId, baselineClassesXmiIds.get(xmiId));
						targetMovedClassesXmiIds.put(xmiId, targetClassesXmiIds.get(xmiId));
					}
				}
			}
		}
		//
		// Identify any "New" (i.e. "Model Only") classes that appear in the target
		// model
		for (String xmiId : targetClassesXmiIds.keySet()) {
			if (!baselineClassesXmiIds.containsKey(xmiId)) {
				targetNewClassesXmiIds.put(xmiId, targetClassesXmiIds.get(xmiId));
			}
		}

		/**
		 * ==================================================================================
		 * Now iterate through all generalizations contained across the baseline and
		 * target models to determine into which category they fall: "New", "Moved" or
		 * "Deleted"
		 * ==================================================================================
		 */
		for (String xmiId : baselineGeneralizationsXmiIds.keySet()) {
			if (!targetGeneralizationsXmiIds.containsKey(xmiId)) {
				baselineDeletedGeneralizationsXmiIds.put(xmiId, baselineGeneralizationsXmiIds.get(xmiId));
			} else {
				// We now check to determine if the package that the generalization was in
				// "Moved"
				GeneralizationType baselineGeneralization = baselineGeneralizationsXmiIds.get(xmiId);
				GeneralizationType targetGeneralization = targetGeneralizationsXmiIds.get(xmiId);

				// If either the subtype or supertype have changed we know that at least 
				// one of the ends of the generalization was moved to a different class...
				if (!baselineGeneralization.getSubtype().equals(targetGeneralization.getSubtype())
						|| !baselineGeneralization.getSupertype().equals(targetGeneralization.getSupertype())) {
					baselineMovedGeneralizationsXmiIds.put(baselineGeneralization.getXmiId(), baselineGeneralization);
					targetMovedGeneralizationsXmiIds.put(targetGeneralization.getXmiId(), targetGeneralization);
				}
			}
		}
		//
		// Identify any "New" (i.e. "Model Only") generalizations that appear in the
		// target
		// model
		for (String xmiId : targetGeneralizationsXmiIds.keySet()) {
			if (!baselineGeneralizationsXmiIds.containsKey(xmiId)) {
				targetNewGeneralizationsXmiIds.put(xmiId, targetGeneralizationsXmiIds.get(xmiId));
			}
		}

		/**
		 * ==================================================================================
		 * Now iterate through all associations contained across the baseline and target
		 * models to determine into which category they fall: "New", "Moved" or
		 * "Deleted"
		 * ==================================================================================
		 */
		for (String xmiId : baselineAssociationsXmiIds.keySet()) {
			if (!targetAssociationsXmiIds.containsKey(xmiId)) {
				baselineDeletedAssociationsXmiIds.put(xmiId, baselineAssociationsXmiIds.get(xmiId));
			} else {
				// We now check to determine if the package was "Moved"
				AssociationType baselineAssociation = baselineAssociationsXmiIds.get(xmiId);
				AssociationType targetAssociation = targetAssociationsXmiIds.get(xmiId);

				// If either the source or destination have changed we know that the
				// association was moved...
				if (!baselineAssociation.getSourceAssociationEnd().getType()
						.equals(targetAssociation.getSourceAssociationEnd().getType()) 
						|| !baselineAssociation.getDestinationAssociationEnd().getType().equals(targetAssociation.getDestinationAssociationEnd().getType())) {
					baselineMovedAssociationsXmiIds.put(baselineAssociation.getXmiId(), baselineAssociation);
					targetMovedAssociationsXmiIds.put(targetAssociation.getXmiId(), targetAssociation);
				}
			}
		}

		//
		// Identify any "New" (i.e. "Model Only") classes that appear in the target
		// model
		for (String xmiId : targetAssociationsXmiIds.keySet()) {
			if (!baselineAssociationsXmiIds.containsKey(xmiId)) {
				targetNewAssociationsXmiIds.put(xmiId, targetAssociationsXmiIds.get(xmiId));
			}
		}

		/**
		 * ==================================================================================
		 * Now iterate through all diagrams contained across the baseline and target
		 * models to determine into which category they fall: "New", "Moved" or
		 * "Deleted"
		 * ==================================================================================
		 */
		for (String xmiId : baselineDiagramsXmiIds.keySet()) {
			if (!targetDiagramsXmiIds.containsKey(xmiId)) {
				baselineDeletedDiagramsXmiIds.put(xmiId, baselineDiagramsXmiIds.get(xmiId));
			} else {
				// We now check to determine if the diagram was "Moved"
				Diagram baselineDiagram = baselineDiagramsXmiIds.get(xmiId);
				Diagram targetDiagram = targetDiagramsXmiIds.get(xmiId);

				if (baselineDiagram.getOwner() != null && targetDiagram.getOwner() != null) {
					String baselineParentPackageXmiId = baselineDiagram.getOwner();
					String targetParentPackageXmiId = targetDiagram.getOwner();

					if (!baselineParentPackageXmiId.equals(targetParentPackageXmiId)) {
						// Diagram was moved...
						baselineMovedDiagramsXmiIds.put(xmiId, baselineDiagramsXmiIds.get(xmiId));
						targetMovedDiagramsXmiIds.put(xmiId, targetDiagramsXmiIds.get(xmiId));
					}
				}
			}
		}
		//
		// Identify any "New" (i.e. "Model Only") diagrams that appear in the target
		// model
		for (String xmiId : targetDiagramsXmiIds.keySet()) {
			if (!baselineDiagramsXmiIds.containsKey(xmiId)) {
				targetNewDiagramsXmiIds.put(xmiId, targetDiagramsXmiIds.get(xmiId));
			}
		}

		/**
		 * Final step is to clean up the primary mappings to remove those
		 * packages/classes/generalizations/associations/diagrams identified as either
		 * deleted, moved, etc...
		 */

		/** Clean up Package mappings... */
		for (String xmiId : baselineDeletedPackagesXmiIds.keySet()) {
			packageXmiIdsInBoth.remove(xmiId);
		}
		//
		for (String xmiId : targetNewPackagesXmiIds.keySet()) {
			targetPackagesXmiIds.remove(xmiId);
			packageXmiIdsInBoth.remove(xmiId);
		}

		/** Clean up Class mappings... */
		for (String xmiId : baselineDeletedClassesXmiIds.keySet()) {
			baselineClassesXmiIds.remove(xmiId);
			classXmiIdsInBoth.remove(xmiId);
		}
		//
		for (String xmiId : targetNewClassesXmiIds.keySet()) {
			classXmiIdsInBoth.remove(xmiId);
		}

		/** Clean up Generalization mappings... */
		for (String xmiId : baselineDeletedGeneralizationsXmiIds.keySet()) {
			baselineGeneralizationsXmiIds.remove(xmiId);
			generalizationXmiIdsInBoth.remove(xmiId);
		}
		//
		for (String xmiId : targetNewGeneralizationsXmiIds.keySet()) {
			generalizationXmiIdsInBoth.remove(xmiId);
		}

		/** Clean up Association mappings... */
		for (String xmiId : baselineDeletedAssociationsXmiIds.keySet()) {
			baselineAssociationsXmiIds.remove(xmiId);
			associationXmiIdsInBoth.remove(xmiId);
		}
		//
		for (String xmiId : targetNewAssociationsXmiIds.keySet()) {
			associationXmiIdsInBoth.remove(xmiId);
		}

		/** Clean up Diagram mappings... */
		for (String xmiId : baselineDeletedDiagramsXmiIds.keySet()) {
			baselineDiagramsXmiIds.remove(xmiId);
			diagramXmiIdsInBoth.remove(xmiId);
		}
		//
		for (String xmiId : targetNewDiagramsXmiIds.keySet()) {
			diagramXmiIdsInBoth.remove(xmiId);
		}
	}

	public Map<String, GeneralizationType> getTargetGeneralizationsXmiIds() {
		return targetGeneralizationsXmiIds;
	}

	public Map<String, GeneralizationType> getBaselineGeneralizationsXmiIds() {
		return baselineGeneralizationsXmiIds;
	}

	public Map<String, AssociationType> getBaselineAssociationsXmiIds() {
		return baselineAssociationsXmiIds;
	}

	public Map<String, PackageType> getBaselinePackagesXmiIds() {
		return baselinePackagesXmiIds;
	}

	public Map<String, PackageType> getBaselineDeletedPackagesXmiIds() {
		return baselineDeletedPackagesXmiIds;
	}

	public Map<String, PackageType> getBaselineMovedPackagesXmiIds() {
		return baselineMovedPackagesXmiIds;
	}
	
	/**
	 * Convenience method that simply aggregates all added, deleted, and moved packages.
	 * This is needed in certain scenarios. 
	 */
	public Map<String, PackageType> getAllBaselinePackagesXmiIds() {
		Map<String, PackageType> allPackages = new HashMap<String, PackageType>();
		
		baselinePackagesXmiIds.forEach((key, value) -> {
			allPackages.put(key, value);
		});
		
		baselineDeletedPackagesXmiIds.forEach((key, value) -> {
			allPackages.put(key, value);
		});
		
		baselineMovedPackagesXmiIds.forEach((key, value) -> {
			allPackages.put(key, value);
		});
		
		return allPackages;
	}

	public Map<String, ClassType> getBaselineClassesXmiIds() {
		return baselineClassesXmiIds;
	}

	public Map<String, ClassType> getBaselineDeletedClassesXmiIds() {
		return baselineDeletedClassesXmiIds;
	}

	public Map<String, ClassType> getBaselineMovedClassesXmiIds() {
		return baselineMovedClassesXmiIds;
	}

	public Map<String, GeneralizationType> getBaselineDeletedGeneralizationsXmiIds() {
		return baselineDeletedGeneralizationsXmiIds;
	}

	public Map<String, AssociationType> getBaselineDeletedAssociationsXmiIds() {
		return baselineDeletedAssociationsXmiIds;
	}

	public Map<String, GeneralizationType> getBaselineMovedGeneralizationsXmiIds() {
		return baselineMovedGeneralizationsXmiIds;
	}
	
	public Map<String, AssociationType> getBaselineMovedAssociationsXmiIds() {
		return baselineMovedAssociationsXmiIds;
	}

	public Map<String, PackageType> getTargetPackagesXmiIds() {
		return targetPackagesXmiIds;
	}

	public Map<String, PackageType> getTargetNewPackagesXmiIds() {
		return targetNewPackagesXmiIds;
	}

	public Map<String, PackageType> getTargetMovedPackagesXmiIds() {
		return targetMovedPackagesXmiIds;
	}
	
	/**
	 * Convenience method that simply aggregates all added, deleted, and moved packages.
	 * This is needed in certain scenarios. 
	 */
	public Map<String, PackageType> getAllTargetPackagesXmiIds() {
		Map<String, PackageType> allPackages = new HashMap<String, PackageType>();
		
		targetPackagesXmiIds.forEach((key, value) -> {
			allPackages.put(key, value);
		});
		
		targetNewPackagesXmiIds.forEach((key, value) -> {
			allPackages.put(key, value);
		});
		
		targetMovedPackagesXmiIds.forEach((key, value) -> {
			allPackages.put(key, value);
		});
		
		return allPackages;
	}

	public Map<String, ClassType> getTargetClassesXmiIds() {
		return targetClassesXmiIds;
	}

	public Map<String, ClassType> getTargetNewClassesXmiIds() {
		return targetNewClassesXmiIds;
	}

	public Map<String, ClassType> getTargedMovedClassesXmiIds() {
		return targetMovedClassesXmiIds;
	}

	public Map<String, GeneralizationType> getTargetNewGeneralizationsXmiIds() {
		return targetNewGeneralizationsXmiIds;
	}

	public Map<String, GeneralizationType> getTargetMovedGeneralizationsXmiIds() {
		return targetMovedGeneralizationsXmiIds;
	}
	
	public Map<String, AssociationType> getTargetMovedAssociationsXmiIds() {
		return targetMovedAssociationsXmiIds;
	}

	/**
	 * Method to retrieve the Set of XmiIds for ALL packages that exist in BOTH
	 * baseline and target models.
	 * 
	 * @return
	 */
	public Set<String> getPackageXmiIdsInBoth() {
		return packageXmiIdsInBoth;
	}

	/**
	 * Method to retrieve the Set of XmiIds for ALL class that exist in BOTH
	 * baseline and target models.
	 * 
	 * @return
	 */
	public Set<String> getClassXmiIdsInBoth() {
		return classXmiIdsInBoth;
	}

	public Map<String, Diagram> getBaselineDiagramsXmiIds() {
		return baselineDiagramsXmiIds;
	}

	public Map<String, Diagram> getBaselineDeletedDiagramsXmiIds() {
		return baselineDeletedDiagramsXmiIds;
	}

	public Map<String, Diagram> getBaselineMovedDiagramsXmiIds() {
		return baselineMovedDiagramsXmiIds;
	}

	public Map<String, Diagram> getTargetDiagramsXmiIds() {
		return targetDiagramsXmiIds;
	}

	public Map<String, Diagram> getTargetNewDiagramsXmiIds() {
		return targetNewDiagramsXmiIds;
	}

	public Map<String, Diagram> getTargetMovedDiagramsXmiIds() {
		return targetMovedDiagramsXmiIds;
	}

	public Set<String> getDiagramXmiIdsInBoth() {
		return diagramXmiIdsInBoth;
	}

	public Set<GeneralizationType> getAllBaselineGeneralizations(String classXmiId) {
		Set<GeneralizationType> generalizations = new TreeSet<GeneralizationType>();

		for (GeneralizationType generalization : baselineGeneralizationsXmiIds.values()) {
			if (generalization.getSubtype().equals(classXmiId) || generalization.getSupertype().equals(classXmiId)) {
				generalizations.add(generalization);
			}
		}

		for (GeneralizationType generalization : baselineDeletedGeneralizationsXmiIds.values()) {
			if (generalization.getSubtype().equals(classXmiId) || generalization.getSupertype().equals(classXmiId)) {
				generalizations.add(generalization);
			}
		}

		for (GeneralizationType generalization : baselineMovedGeneralizationsXmiIds.values()) {
			if (generalization.getSubtype().equals(classXmiId) || generalization.getSupertype().equals(classXmiId)) {
				generalizations.add(generalization);
			}
		}

		return generalizations;
	}

	public Set<GeneralizationType> getAllTargetGeneralizations(String classXmiId) {
		Set<GeneralizationType> generalizations = new TreeSet<GeneralizationType>();

		for (GeneralizationType generalization : targetGeneralizationsXmiIds.values()) {
			if (generalization.getSubtype().equals(classXmiId) || generalization.getSupertype().equals(classXmiId)) {
				generalizations.add(generalization);
			}
		}

		for (GeneralizationType generalization : targetNewGeneralizationsXmiIds.values()) {
			if (generalization.getSubtype().equals(classXmiId) || generalization.getSupertype().equals(classXmiId)) {
				generalizations.add(generalization);
			}
		}

		for (GeneralizationType generalization : targetMovedGeneralizationsXmiIds.values()) {
			if (generalization.getSubtype().equals(classXmiId) || generalization.getSupertype().equals(classXmiId)) {
				generalizations.add(generalization);
			}
		}

		return generalizations;
	}

	/**
	 * 
	 * Special convenience method that will return ALL generalizations that have a
	 * "source" side class that matches the identifier of the class passed in.
	 * 
	 * @param classXmiId
	 * @return A collection of generalizations.
	 */
	public Set<GeneralizationType> getAllGeneralizations(String classXmiId) {
		Set<GeneralizationType> generalizations = new TreeSet<GeneralizationType>();

		for (GeneralizationType generalization : baselineGeneralizationsXmiIds.values()) {
			if (generalization.getSubtype().equals(classXmiId) || generalization.getSupertype().equals(classXmiId)) {
				generalizations.add(generalization);
			}
		}

		for (GeneralizationType generalization : baselineDeletedGeneralizationsXmiIds.values()) {
			if (generalization.getSubtype().equals(classXmiId) || generalization.getSupertype().equals(classXmiId)) {
				generalizations.add(generalization);
			}
		}

		for (GeneralizationType generalization : baselineMovedGeneralizationsXmiIds.values()) {
			if (generalization.getSubtype().equals(classXmiId) || generalization.getSupertype().equals(classXmiId)) {
				generalizations.add(generalization);
			}
		}

		for (GeneralizationType generalization : targetGeneralizationsXmiIds.values()) {
			if (generalization.getSubtype().equals(classXmiId) || generalization.getSupertype().equals(classXmiId)) {
				generalizations.add(generalization);
			}
		}

		for (GeneralizationType generalization : targetNewGeneralizationsXmiIds.values()) {
			if (generalization.getSubtype().equals(classXmiId) || generalization.getSupertype().equals(classXmiId)) {
				generalizations.add(generalization);
			}
		}

		for (GeneralizationType generalization : targetMovedGeneralizationsXmiIds.values()) {
			if (generalization.getSubtype().equals(classXmiId) || generalization.getSupertype().equals(classXmiId)) {
				generalizations.add(generalization);
			}
		}

		return generalizations;
	}

	public Set<AssociationType> getAllBaselineAssociations(String classXmiId) {
		Set<AssociationType> associations = new TreeSet<AssociationType>();

		for (AssociationType association : baselineAssociationsXmiIds.values()) {
			if (association.getSourceAssociationEnd().getType().equals(classXmiId)
					|| association.getDestinationAssociationEnd().getType().equals(classXmiId)) {
				associations.add(association);
			}
		}

		for (AssociationType association : baselineDeletedAssociationsXmiIds.values()) {
			if (association.getSourceAssociationEnd().getType().equals(classXmiId)
					|| association.getDestinationAssociationEnd().getType().equals(classXmiId)) {
				associations.add(association);
			}
		}

		for (AssociationType association : baselineMovedAssociationsXmiIds.values()) {
			if (association.getSourceAssociationEnd().getType().equals(classXmiId)
					|| association.getDestinationAssociationEnd().getType().equals(classXmiId)) {
				associations.add(association);
			}
		}

		return associations;
	}

	public Set<AssociationType> getAllTargetAssociations(String classXmiId) {
		Set<AssociationType> associations = new TreeSet<AssociationType>();

		for (AssociationType association : targetAssociationsXmiIds.values()) {
			if (association.getSourceAssociationEnd().getType().equals(classXmiId)
					|| association.getDestinationAssociationEnd().getType().equals(classXmiId)) {
				associations.add(association);
			}
		}

		for (AssociationType association : targetNewAssociationsXmiIds.values()) {
			if (association.getSourceAssociationEnd().getType().equals(classXmiId)
					|| association.getDestinationAssociationEnd().getType().equals(classXmiId)) {
				associations.add(association);
			}
		}

		for (AssociationType association : targetMovedAssociationsXmiIds.values()) {
			if (association.getSourceAssociationEnd().getType().equals(classXmiId)
					|| association.getDestinationAssociationEnd().getType().equals(classXmiId)) {
				associations.add(association);
			}
		}

		return associations;
	}

	/**
	 * Special convenience method that will return ALL associations that have a
	 * "source" side class that matches the identifier of the class passed in.
	 * 
	 * @param classXmiId
	 * @return A collection of generalizations.
	 */
	public Set<AssociationType> getAllAssociations(String classXmiId) {
		Set<AssociationType> associations = new TreeSet<AssociationType>();

		for (AssociationType association : baselineAssociationsXmiIds.values()) {
			if (association.getSourceAssociationEnd().getType().equals(classXmiId)
					|| association.getDestinationAssociationEnd().getType().equals(classXmiId)) {
				associations.add(association);
			}
		}

		for (AssociationType association : baselineDeletedAssociationsXmiIds.values()) {
			if (association.getSourceAssociationEnd().getType().equals(classXmiId)
					|| association.getDestinationAssociationEnd().getType().equals(classXmiId)) {
				associations.add(association);
			}
		}

		for (AssociationType association : baselineMovedAssociationsXmiIds.values()) {
			if (association.getSourceAssociationEnd().getType().equals(classXmiId)
					|| association.getDestinationAssociationEnd().getType().equals(classXmiId)) {
				associations.add(association);
			}
		}

		for (AssociationType association : targetAssociationsXmiIds.values()) {
			if (association.getSourceAssociationEnd().getType().equals(classXmiId)
					|| association.getDestinationAssociationEnd().getType().equals(classXmiId)) {
				associations.add(association);
			}
		}

		for (AssociationType association : targetNewAssociationsXmiIds.values()) {
			if (association.getSourceAssociationEnd().getType().equals(classXmiId)
					|| association.getDestinationAssociationEnd().getType().equals(classXmiId)) {
				associations.add(association);
			}
		}

		for (AssociationType association : targetMovedAssociationsXmiIds.values()) {
			if (association.getSourceAssociationEnd().getType().equals(classXmiId)
					|| association.getDestinationAssociationEnd().getType().equals(classXmiId)) {
				associations.add(association);
			}
		}

		return associations;
	}

}
