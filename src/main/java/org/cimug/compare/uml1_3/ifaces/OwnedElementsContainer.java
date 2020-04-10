/**
 * 
 */
package org.cimug.compare.uml1_3.ifaces;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBElement;

import org.cimug.compare.uml1_3.AssociationRoleType;
import org.cimug.compare.uml1_3.AssociationType;
import org.cimug.compare.uml1_3.ClassType;
import org.cimug.compare.uml1_3.ClassifierRoleType;
import org.cimug.compare.uml1_3.CollaborationType;
import org.cimug.compare.uml1_3.CommentType;
import org.cimug.compare.uml1_3.DataTypeType;
import org.cimug.compare.uml1_3.DependencyType;
import org.cimug.compare.uml1_3.GeneralizationType;
import org.cimug.compare.uml1_3.PackageType;

/**
 * @author tviegut
 *
 */
public interface OwnedElementsContainer {

	List<Serializable> getContent();

	default List<ClassifierRoleType> getClassifierRoles() {
		List<ClassifierRoleType> classifierRoles = new ArrayList<ClassifierRoleType>();
		for (Serializable item : getContent()) {
			if (item instanceof ClassifierRoleType) {
				classifierRoles.add((ClassifierRoleType) item);
			} else if (item instanceof JAXBElement && ((JAXBElement) item).getValue() instanceof ClassifierRoleType) {
				classifierRoles.add((ClassifierRoleType) ((JAXBElement) item).getValue());
			}
		}
		return classifierRoles;
	}

	default List<CollaborationType> getCollaborations() {
		List<CollaborationType> collaborations = new ArrayList<CollaborationType>();
		for (Serializable item : getContent()) {
			if (item instanceof CollaborationType) {
				collaborations.add((CollaborationType) item);
			} else if (item instanceof JAXBElement && ((JAXBElement) item).getValue() instanceof CollaborationType) {
				collaborations.add((CollaborationType) ((JAXBElement) item).getValue());
			}
		}
		return collaborations;
	}

	default List<AssociationRoleType> getAssociationRoles() {
		List<AssociationRoleType> associationRoles = new ArrayList<AssociationRoleType>();
		for (Serializable item : getContent()) {
			if (item instanceof AssociationRoleType) {
				associationRoles.add((AssociationRoleType) item);
			} else if (item instanceof JAXBElement && ((JAXBElement) item).getValue() instanceof AssociationRoleType) {
				associationRoles.add((AssociationRoleType) ((JAXBElement) item).getValue());
			}
		}
		return associationRoles;
	}

	default List<PackageType> getPackages() {
		List<PackageType> packages = new ArrayList<PackageType>();
		for (Serializable item : getContent()) {
			if (item instanceof PackageType) {
				packages.add((PackageType) item);
			} else if (item instanceof JAXBElement && ((JAXBElement) item).getValue() instanceof PackageType) {
				packages.add((PackageType) ((JAXBElement) item).getValue());
			}
		}
		return packages;
	}

	default List<ClassType> getClasses() {
		List<ClassType> classes = new ArrayList<ClassType>();
		for (Serializable item : getContent()) {
			if (item instanceof ClassType) {
				classes.add((ClassType) item);
			} else if (item instanceof JAXBElement && ((JAXBElement) item).getValue() instanceof ClassType) {
				classes.add((ClassType) ((JAXBElement) item).getValue());
			}
		}
		return classes;
	}

	default List<DataTypeType> getDataTypes() {
		List<DataTypeType> dateTypes = new ArrayList<DataTypeType>();
		for (Serializable item : getContent()) {
			if (item instanceof DataTypeType) {
				dateTypes.add((DataTypeType) item);
			} else if (item instanceof JAXBElement && ((JAXBElement) item).getValue() instanceof DataTypeType) {
				dateTypes.add((DataTypeType) ((JAXBElement) item).getValue());
			}
		}
		return dateTypes;
	}

	default List<GeneralizationType> getGeneralizations() {
		List<GeneralizationType> generalizations = new ArrayList<GeneralizationType>();
		for (Serializable item : getContent()) {
			if (item instanceof GeneralizationType) {
				generalizations.add((GeneralizationType) item);
			} else if (item instanceof JAXBElement && ((JAXBElement) item).getValue() instanceof GeneralizationType) {
				generalizations.add((GeneralizationType) ((JAXBElement) item).getValue());
			}
		}
		return generalizations;
	}

	default List<AssociationType> getAssociations() {
		List<AssociationType> associations = new ArrayList<AssociationType>();
		for (Serializable item : getContent()) {
			if (item instanceof AssociationType) {
				associations.add((AssociationType) item);
			} else if (item instanceof JAXBElement && ((JAXBElement) item).getValue() instanceof AssociationType) {
				associations.add((AssociationType) ((JAXBElement) item).getValue());
			}
		}
		return associations;
	}

	default List<DependencyType> getDependencies() {
		List<DependencyType> dependencies = new ArrayList<DependencyType>();
		for (Serializable item : getContent()) {
			if (item instanceof DependencyType) {
				dependencies.add((DependencyType) item);
			} else if (item instanceof JAXBElement && ((JAXBElement) item).getValue() instanceof DependencyType) {
				dependencies.add((DependencyType) ((JAXBElement) item).getValue());
			}
		}
		return dependencies;
	}

	default List<CommentType> getComments() {
		List<CommentType> comments = new ArrayList<CommentType>();
		for (Serializable item : getContent()) {
			if (item instanceof CommentType) {
				comments.add((CommentType) item);
			} else if (item instanceof JAXBElement && ((JAXBElement) item).getValue() instanceof CommentType) {
				comments.add((CommentType) ((JAXBElement) item).getValue());
			}
		}
		return comments;
	}

}
