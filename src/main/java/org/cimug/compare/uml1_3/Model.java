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
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.cimug.compare.uml1_3.ifaces.GUIDIdentifier;
import org.cimug.compare.uml1_3.ifaces.NamedType;
import org.cimug.compare.uml1_3.ifaces.PackageContainer;

/**
 * <p>
 * Java class for ModelType complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within
 * this class.
 * 
 * <pre>
 * &lt;complexType name="ModelType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="Namespace.ownedElement" type="{omg.org/UML1.3}Namespace.ownedElementType"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="xmi.id" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ModelType", propOrder = { "namespaceOwnedElement" })
@XmlRootElement(name = "Model")
public class Model implements PackageContainer, NamedType, GUIDIdentifier {

	@XmlElement(name = "Namespace.ownedElement", required = true)
	protected NamespaceOwnedElementType namespaceOwnedElement;
	@XmlAttribute(name = "name")
	protected String name;
	@XmlAttribute(name = "xmi.id")
	protected String xmiId;

	public Model(NamespaceOwnedElementType namespaceOwnedElement, String name, String xmiId) {
		super();
		this.namespaceOwnedElement = namespaceOwnedElement;
		this.name = name;
		this.xmiId = xmiId;
	}

	public Model() {
		super();
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

	@Override
	public String getGUID() {
		return getXmiId();
	}

	@Override
	public String toString() {
		String str = "Model [\nname = " + name + ", \nxmiId = " + xmiId + ", \npackages = ";
		for (PackageType aPackage : namespaceOwnedElement.getPackages()) {
			str += "\n" + aPackage;
			for (PackageType childPackage : aPackage.getPackages()) {
				str += "\n\t" + aPackage;
				for (PackageType aChildPackage : childPackage.getPackages()) {
					str += "\n\t\t" + aChildPackage;
				}
			}
		}
		str += "]";
		return str;
	}

}