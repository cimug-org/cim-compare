//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.3.0 
// See <a href="https://javaee.github.io/jaxb-v2/">https://javaee.github.io/jaxb-v2/</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2020.03.25 at 06:13:11 PM CDT 
//

package org.cimug.compare.uml1_3;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import org.cimug.compare.uml1_3.ClassifierType;

/**
 * <p>
 * Java class for StructuralFeature.typeType complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within
 * this class.
 * 
 * <pre>
 * &lt;complexType name="StructuralFeature.typeType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="Classifier" type="{omg.org/UML1.3}ClassifierType"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "StructuralFeature.typeType", propOrder = { "classifier" })
public class StructuralFeatureTypeType {

	@XmlElement(name = "Classifier", required = true)
	protected ClassifierType classifier;

	public StructuralFeatureTypeType(ClassifierType classifier) {
		super();
		this.classifier = classifier;
	}

	public StructuralFeatureTypeType() {
		super();
	}

	/**
	 * Gets the value of the classifier property.
	 * 
	 * @return possible object is {@link ClassifierType }
	 * 
	 */
	public ClassifierType getClassifier() {
		return classifier;
	}

	/**
	 * Sets the value of the classifier property.
	 * 
	 * @param value
	 *            allowed object is {@link ClassifierType }
	 * 
	 */
	public void setClassifier(ClassifierType value) {
		this.classifier = value;
	}

}
