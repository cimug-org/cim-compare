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

import org.cimug.compare.DiffUtils;
import org.cimug.compare.uml1_3.ifaces.GUIDIdentifier;
import org.cimug.compare.uml1_3.ifaces.NamedType;
import org.cimug.compare.uml1_3.ifaces.PackageContainer;
import org.cimug.compare.uml1_3.ifaces.PackagedElement;

/**
 * <p>
 * Java class for PackageType complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within
 * this class.
 * 
 * <pre>
 * &lt;complexType name="PackageType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="ModelElement.taggedValue" type="{omg.org/UML1.3}ModelElement.taggedValueType"/&gt;
 *         &lt;element name="Namespace.ownedElement" type="{omg.org/UML1.3}Namespace.ownedElementType"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="xmi.id" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="isRoot" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="isLeaf" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="isAbstract" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="visibility" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PackageType", propOrder = { "modelElementTaggedValue", "namespaceOwnedElement" })
public class PackageType implements PackagedElement, PackageContainer, NamedType, GUIDIdentifier {

	@XmlElement(name = "ModelElement.taggedValue", required = true)
	protected ModelElementTaggedValue modelElementTaggedValue;
	@XmlElement(name = "Namespace.ownedElement", required = true)
	protected NamespaceOwnedElementType namespaceOwnedElement;
	@XmlAttribute(name = "name")
	protected String name;
	@XmlAttribute(name = "xmi.id")
	protected String xmiId;
	@XmlAttribute(name = "isRoot")
	protected String isRoot;
	@XmlAttribute(name = "isLeaf")
	protected String isLeaf;
	@XmlAttribute(name = "isAbstract")
	protected String isAbstract;
	@XmlAttribute(name = "visibility")
	protected String visibility;

	public PackageType(ModelElementTaggedValue modelElementTaggedValue, NamespaceOwnedElementType namespaceOwnedElement,
			String name, String xmiId, String isRoot, String isLeaf, String isAbstract, String visibility) {
		super();
		this.modelElementTaggedValue = modelElementTaggedValue;
		this.namespaceOwnedElement = namespaceOwnedElement;
		this.name = name;
		this.xmiId = xmiId;
		this.isRoot = isRoot;
		this.isLeaf = isLeaf;
		this.isAbstract = isAbstract;
		this.visibility = visibility;
	}

	public PackageType() {
		super();
	}

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
	 * Gets the value of the namespaceOwnedElement property.
	 * 
	 * @return possible object is {@link NamespaceOwnedElementType }
	 * 
	 */
	public NamespaceOwnedElementType getNamespaceOwnedElement() {
		return namespaceOwnedElement;
	}

	/**
	 * Sets the value of the namespaceOwnedElement property.
	 * 
	 * @param value
	 *            allowed object is {@link NamespaceOwnedElementType }
	 * 
	 */
	public void setNamespaceOwnedElement(NamespaceOwnedElementType value) {
		this.namespaceOwnedElement = value;
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

	@Override
	public String getParentPackageGUIDTagName() {
		return "parent";
	}

	@Override
	public String getParentPackageNameTagName() {
		return "";
	}

	@Override
	public String getGUID() {
		return DiffUtils.convertXmiIdToEAGUID(getXmiId());
	}

	@Override
	public String toString() {
		String str = "Package [name = " + name + ", xmiId = " + xmiId + "]";
		return str;
	}

}
