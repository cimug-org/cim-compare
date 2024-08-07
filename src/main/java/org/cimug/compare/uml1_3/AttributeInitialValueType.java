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

/**
 * <p>
 * Java class for Attribute.initialValueType complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within
 * this class.
 * 
 * <pre>
 * &lt;complexType name="Attribute.initialValueType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="Expression" type="{omg.org/UML1.3}ExpressionType"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Attribute.initialValueType", propOrder = { "expression" })
public class AttributeInitialValueType {

	@XmlElement(name = "Expression", required = true)
	protected ExpressionType expression;

	public AttributeInitialValueType(ExpressionType expression) {
		super();
		this.expression = expression;
	}

	public AttributeInitialValueType() {
		super();
	}
	
	/**
	 * Gets the value of the expression property.
	 * 
	 * @return possible object is {@link ExpressionType }
	 * 
	 */
	public ExpressionType getExpression() {
		return expression;
	}

	/**
	 * Sets the value of the expression property.
	 * 
	 * @param value
	 *            allowed object is {@link ExpressionType }
	 * 
	 */
	public void setExpression(ExpressionType value) {
		this.expression = value;
	}

}
