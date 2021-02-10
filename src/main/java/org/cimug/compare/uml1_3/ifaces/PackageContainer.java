/**
 * 
 */
package org.cimug.compare.uml1_3.ifaces;

import java.util.ArrayList;
import java.util.List;

import org.cimug.compare.uml1_3.AssociationType;
import org.cimug.compare.uml1_3.ClassType;
import org.cimug.compare.uml1_3.GeneralizationType;
import org.cimug.compare.uml1_3.NamespaceOwnedElementType;
import org.cimug.compare.uml1_3.PackageType;

/**
 * @author tviegut
 *
 */
public interface PackageContainer {

	NamespaceOwnedElementType getNamespaceOwnedElement();

	default List<PackageType> getPackages() {
		if (getNamespaceOwnedElement() != null) {
			return getNamespaceOwnedElement().getPackages();
		}
		return new ArrayList<PackageType>();
	}

	default PackageType getPackage(String key) {
		if (getNamespaceOwnedElement() != null) {
			for (PackageType aPackage : getNamespaceOwnedElement().getPackages()) {
				if (aPackage.getGUID().equals(key)) {
					return aPackage;
				}
			}
		}
		return null;
	}

	default PackageType getPackageByGUID(String guid) {
		if (getNamespaceOwnedElement() != null) {
			for (PackageType aPackage : getNamespaceOwnedElement().getPackages()) {
				if (aPackage.getGUID().equals(guid)) {
					return aPackage;
				}
			}
		}
		return null;
	}

	/**
	 * Determines if the PackageContainer has an immediate child package with the
	 * specified GUID.
	 * 
	 * @param guid
	 * @return
	 */
	default boolean hasPackage(String guid) {
		if (getNamespaceOwnedElement() != null) {
			for (PackageType aPackage : getNamespaceOwnedElement().getPackages()) {
				if (aPackage.getGUID().equals(guid)) {
					return true;
				}
			}
		}
		return false;
	}

	default List<ClassType> getClasses() {
		if (getNamespaceOwnedElement() != null) {
			return getNamespaceOwnedElement().getClasses();
		}
		return new ArrayList<ClassType>();
	}

	default ClassType getClass(String key) {
		if (getNamespaceOwnedElement() != null) {
			for (ClassType aClass : getNamespaceOwnedElement().getClasses()) {
				if (aClass.getGUID().equals(key)) {
					return aClass;
				}
			}
		}
		return null;
	}

	default PackageType getClassByGUID(String guid) {
		if (getNamespaceOwnedElement() != null) {
			for (PackageType aPackage : getNamespaceOwnedElement().getPackages()) {
				if (aPackage.getGUID().equals(guid)) {
					return aPackage;
				}
			}
		}
		return null;
	}

	default List<AssociationType> getAssociations() {
		if (getNamespaceOwnedElement() != null) {
			return getNamespaceOwnedElement().getAssociations();
		}
		return new ArrayList<AssociationType>();
	}

	default AssociationType getAssociation(String guid) {
		if (getNamespaceOwnedElement() != null) {
			for (AssociationType anAssociation : getNamespaceOwnedElement().getAssociations()) {
				if (anAssociation.getXmiId().equals(guid)) {
					return anAssociation;
				}
			}
		}
		return null;
	}

	/**
	 * Retrieve all associations for a specific class. The class's unique xmiId
	 * identifier is passed and all associations associated with the class (whether
	 * the class is the source or target) are returned.
	 * 
	 * @param classXmiId
	 * @return The list of all associations that the specified class is a part of.
	 */
	default List<AssociationType> getAssociations(String classXmiId) {
		List<AssociationType> associations = new ArrayList<AssociationType>();
		if (getNamespaceOwnedElement() != null) {
			for (AssociationType anAssociation : getNamespaceOwnedElement().getAssociations()) {
				// Note that the call to getType() returns the xmiId of the class that the
				// association end is for...
				if (anAssociation.getSourceAssociationEnd().getType().equals(classXmiId)
						|| anAssociation.getDestinationAssociationEnd().getType().equals(classXmiId)) {
					associations.add(anAssociation);
				}
			}
		}
		return associations;
	}

	default AssociationType getAssociation(String source, String target) {
		if (getNamespaceOwnedElement() != null) {
			for (AssociationType anAssociation : getNamespaceOwnedElement().getAssociations()) {
				String sourceName = anAssociation.getTaggedValue("ea_sourceName").getTheValue();
				String targetName = anAssociation.getTaggedValue("ea_targetName").getTheValue();
				if (sourceName.equals(source) && targetName.equals(source)) {
					return anAssociation;
				}
			}
		}
		return null;
	}

	default List<GeneralizationType> getGeneralizations() {
		if (getNamespaceOwnedElement() != null) {
			return getNamespaceOwnedElement().getGeneralizations();
		}
		return new ArrayList<GeneralizationType>();
	}

	/**
	 * Return all generalizations associated with a specific class. The class's
	 * unique xmiId identifier is passed and all generalizations associated with the
	 * class (whether the class is the subtype or supertype) are returned.
	 * 
	 * @param classXmiId
	 * @return The list of all generalizations that the specified class is a part
	 *         of.
	 */
	default List<GeneralizationType> getGeneralizations(String classXmiId) {
		List<GeneralizationType> generalizations = new ArrayList<GeneralizationType>();
		if (getNamespaceOwnedElement() != null) {
			for (GeneralizationType aGeneralization : getNamespaceOwnedElement().getGeneralizations()) {
				if (aGeneralization.getSubtype().equals(classXmiId)
						|| aGeneralization.getSupertype().equals(classXmiId)) {
					generalizations.add(aGeneralization);
				}
			}
		}
		return generalizations;
	}

	default GeneralizationType getGeneralization(String generalizationXmiId) {
		if (getNamespaceOwnedElement() != null) {
			for (GeneralizationType aGeneralization : getNamespaceOwnedElement().getGeneralizations()) {
				if (aGeneralization.getXmiId().equals(generalizationXmiId)) {
					return aGeneralization;
				}
			}
		}
		return null;
	}

	default GeneralizationType getGeneralization(String source, String target) {
		if (getNamespaceOwnedElement() != null) {
			for (GeneralizationType aGeneralization : getNamespaceOwnedElement().getGeneralizations()) {
				String sourceName = aGeneralization.getTaggedValue("ea_sourceName").getTheValue();
				String targetName = aGeneralization.getTaggedValue("ea_targetName").getTheValue();
				if (sourceName.equals(source) && targetName.equals(source)) {
					return aGeneralization;
				}
			}
		}
		return null;
	}

}
