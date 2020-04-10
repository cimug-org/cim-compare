/**
 * 
 */
package org.cimug.compare.uml1_3.ifaces;

import org.cimug.compare.DiffUtils;

public interface PackagedElement extends ModelElementTaggedValueContainer {

	String getParentPackageGUIDTagName();

	String getParentPackageNameTagName();

	default String getParentPackageGUID() {
		if ((getModelElementTaggedValue() != null) && (getModelElementTaggedValue().getTaggedValue(getParentPackageGUIDTagName()) != null)) {
			return DiffUtils.convertXmiIdToEAGUID(
					getModelElementTaggedValue().getTaggedValue(getParentPackageGUIDTagName()).getTheValue());
		}
		return null;
	}

	default String getParentPackageName() {
		if ((getModelElementTaggedValue() != null) && (getModelElementTaggedValue().getTaggedValue(getParentPackageNameTagName()) != null)) {
			return getModelElementTaggedValue().getTaggedValue(getParentPackageNameTagName()).getTheValue();
		}
		return null;
	}

}
