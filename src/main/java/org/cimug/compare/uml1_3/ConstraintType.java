//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.3.0 
// See <a href="https://javaee.github.io/jaxb-v2/">https://javaee.github.io/jaxb-v2/</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2020.03.25 at 06:13:11 PM CDT 
//

package org.cimug.compare.uml1_3;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import org.cimug.compare.uml1_3.ifaces.NamedType;

/**
 * <p>
 * Java class for ConstraintType complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within
 * this class.
 * 
 * <pre>
 * &lt;complexType name="ConstraintType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="ModelElement.taggedValue" type="{omg.org/UML1.3}ModelElement.taggedValueType"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ConstraintType", propOrder = { "modelElementTaggedValue" })
public class ConstraintType implements NamedType {

	@XmlElement(name = "ModelElement.taggedValue", required = true)
	protected ModelElementTaggedValue modelElementTaggedValue;
	@XmlAttribute(name = "name")
	protected String name;

	/**
	 * Gets the value of the modelElementTaggedValue property.
	 * 
	 * @return possible object is {@link ModelElementTaggedValue }
	 * 
	 */
	public ModelElementTaggedValue getModelElementTaggedValue() {
		return modelElementTaggedValue;
	}

	/**
	 * Sets the value of the modelElementTaggedValue property.
	 * 
	 * @param value
	 *            allowed object is {@link ModelElementTaggedValue }
	 * 
	 */
	public void setModelElementTaggedValue(ModelElementTaggedValue value) {
		this.modelElementTaggedValue = value;
	}

	/**
	 * Gets the value of the name property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the value of the name property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	public void setName(String value) {
		this.name = value;
	}

}
