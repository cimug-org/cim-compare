//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.3.0 
// See <a href="https://javaee.github.io/jaxb-v2/">https://javaee.github.io/jaxb-v2/</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2020.03.25 at 06:13:11 PM CDT 
//

package org.cimug.compare.uml1_3;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlMixed;
import javax.xml.bind.annotation.XmlType;

import org.cimug.compare.DiffUtils;
import org.cimug.compare.uml1_3.ifaces.ConnectionContainer;
import org.cimug.compare.uml1_3.ifaces.ContentsContainer;
import org.cimug.compare.uml1_3.ifaces.GUIDIdentifier;

/**
 * <p>
 * Java class for AssociationType complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within
 * this class.
 * 
 * <pre>
 * &lt;complexType name="AssociationType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="ModelElement.stereotype" type="{omg.org/UML1.3}ModelElement.stereotypeType" minOccurs="0"/&gt;
 *         &lt;element name="ModelElement.taggedValue" type="{omg.org/UML1.3}ModelElement.taggedValueType" minOccurs="0"/&gt;
 *         &lt;element name="Classifier.feature" type="{omg.org/UML1.3}Classifier.featureType" minOccurs="0"/&gt;
 *         &lt;element name="Association.connection" type="{omg.org/UML1.3}Association.connectionType" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="xmi.id" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="visibility" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="isRoot" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="isLeaf" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="isAbstract" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AssociationType", propOrder = { "content" })
public class AssociationType
		implements ContentsContainer, ConnectionContainer, GUIDIdentifier, Comparable<AssociationType> {

	@XmlElementRefs({
			@XmlElementRef(name = "ModelElement.stereotype", namespace = "omg.org/UML1.3", type = JAXBElement.class, required = false),
			@XmlElementRef(name = "ModelElement.taggedValue", namespace = "omg.org/UML1.3", type = JAXBElement.class, required = false),
			@XmlElementRef(name = "Classifier.feature", namespace = "omg.org/UML1.3", type = JAXBElement.class, required = false),
			@XmlElementRef(name = "Association.connection", namespace = "omg.org/UML1.3", type = JAXBElement.class, required = false) })
	@XmlMixed
	protected List<Serializable> content;
	@XmlAttribute(name = "xmi.id")
	protected String xmiId;
	@XmlAttribute(name = "visibility")
	protected String visibility;
	@XmlAttribute(name = "isRoot")
	protected String isRoot;
	@XmlAttribute(name = "isLeaf")
	protected String isLeaf;
	@XmlAttribute(name = "isAbstract")
	protected String isAbstract;

	/**
	 * Gets the value of the content property.
	 * 
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot.
	 * Therefore any modification you make to the returned list will be present
	 * inside the JAXB object. This is why there is not a <CODE>set</CODE> method
	 * for the content property.
	 * 
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getContent().add(newItem);
	 * </pre>
	 * 
	 * 
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link JAXBElement
	 * }{@code <}{@link ModelElementStereotypeType }{@code >} {@link JAXBElement
	 * }{@code <}{@link ModelElementTaggedValue }{@code >} {@link JAXBElement
	 * }{@code <}{@link ClassifierFeatureType }{@code >} {@link JAXBElement
	 * }{@code <}{@link AssociationConnectionType }{@code >} {@link String }
	 * 
	 * 
	 */
	public List<Serializable> getContent() {
		if (content == null) {
			content = new ArrayList<Serializable>();
		}
		return this.content;
	}

	/**
	 * Gets the value of the xmiId property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	public String getXmiId() {
		return xmiId;
	}

	/**
	 * Sets the value of the xmiId property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	public void setXmiId(String value) {
		this.xmiId = value;
	}

	/**
	 * Gets the value of the visibility property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	public String getVisibility() {
		return visibility;
	}

	/**
	 * Sets the value of the visibility property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	public void setVisibility(String value) {
		this.visibility = value;
	}

	/**
	 * Gets the value of the isRoot property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	public String getIsRoot() {
		return isRoot;
	}

	/**
	 * Sets the value of the isRoot property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	public void setIsRoot(String value) {
		this.isRoot = value;
	}

	/**
	 * Gets the value of the isLeaf property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	public String getIsLeaf() {
		return isLeaf;
	}

	/**
	 * Sets the value of the isLeaf property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	public void setIsLeaf(String value) {
		this.isLeaf = value;
	}

	/**
	 * Gets the value of the isAbstract property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	public String getIsAbstract() {
		return isAbstract;
	}

	/**
	 * Sets the value of the isAbstract property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	public void setIsAbstract(String value) {
		this.isAbstract = value;
	}

	@Override
	public String getGUID() {
		return DiffUtils.convertXmiIdToEAGUID(getXmiId());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((xmiId == null) ? 0 : xmiId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AssociationType other = (AssociationType) obj;
		if (xmiId == null) {
			if (other.xmiId != null)
				return false;
		} else if (!xmiId.equals(other.xmiId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "AssociationType [xmiId=" + xmiId + ", source=" + getSourceAssociationEnd() + ", target="
				+ getDestinationAssociationEnd() + "]";
	}

	/**
	 * Compares this AssociationType with the specified AssociationType. This method
	 * is provided in preference to individual methods for each of the six boolean
	 * comparison operators ({@literal <}, ==, {@literal >}, {@literal >=}, !=,
	 * {@literal <=}). The suggested idiom for performing these comparisons is:
	 * {@code
	 * (x.compareTo(y)} &lt;<i>op</i>&gt; {@code 0)}, where &lt;<i>op</i>&gt; is one
	 * of the six comparison operators.
	 *
	 * @param otherAssociationType
	 *            AssociationType to which this AssociationType is to be compared.
	 * @return -1, 0 or 1 as this AssociationType is numerically less than, equal
	 *         to, or greater than {@code otherAssociationType}.
	 */
	public int compareTo(AssociationType otherAssociationType) {
		if (otherAssociationType == null) {
			return 1;
		}

		String xmiId = getXmiId();
		String otherXmiId = otherAssociationType.getXmiId();

		if (xmiId == null && otherXmiId == null) {
			return 0;
		}
		if (xmiId == null) {
			return -1;
		}
		if (otherXmiId == null) {
			return 1;
		}
		return xmiId.compareTo(otherXmiId);
	}

}
