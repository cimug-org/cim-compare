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
				if (aPackage.getKey().equals(key)) {
					return aPackage;
				}
			}
		}
		return null;
	}

	default PackageType getPackageByGUID(String guid) {
		if (getNamespaceOwnedElement() != null) {
			for (PackageType aPackage : getNamespaceOwnedElement().getPackages()) {
				if (aPackage.getKey().equals(guid)) {
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
				if (aPackage.getKey().equals(guid)) {
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
				if (aClass.getKey().equals(key)) {
					return aClass;
				}
			}
		}
		return null;
	}
	
	default PackageType getClassByGUID(String guid) {
		if (getNamespaceOwnedElement() != null) {
			for (PackageType aPackage : getNamespaceOwnedElement().getPackages()) {
				if (aPackage.getKey().equals(guid)) {
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

	default GeneralizationType getGeneralization(String guid) {
		if (getNamespaceOwnedElement() != null) {
			for (GeneralizationType aGeneralization : getNamespaceOwnedElement().getGeneralizations()) {
				if (aGeneralization.getXmiId().equals(guid)) {
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
