
Background to this project:

Currently, EA only supports XMI 1.1 for its comparison functionality and  release 1.x of this tool supports the same.  This is so that it 
can process either EA comparison results files (XML) directly into HTML reports or two XMI 1.1 files in which case generates the intermediary
EA comparison file which is then processed into HTML comparison report.

Some quick background should it be necessary to revisit this project in order to update or regenerate the JAXB classes for future versions of XMI.  
An official XMI_1.1.xsd corresponding to version 1.1 of the XMI format was not available after extensive research to locate it.  The ideal is to
utilize an representative XSD schema for the purposes of generating JAXB POJOs for use in marshalling/unmarshalling XMI instance data files. The 
same applies for the Enterprise Architect CompareLog JAXB classes for this project. 

Consequently, a variety of open source and online tools for inferring an XSD Schema based on a set of XML instance data files as input were investigated. 
The goal was to be able to reverse engineer an XSD schema for XMI 1.1. that would result in a "Venetian Blinds" style of XSD (and not Salami Slice, 
Russian Doll, or Garden of Eden). This style of XSD caters particularly well for generating JAXB POJOs derived from XSD global complex types and 
with minimal anonymous classes. (See: https://www.ibm.com/developerworks/library/x-schemascope/)

Ultimately, the Apache XMLBeans open source project was the only toolset that was able to generate the desired XSDs in the format needed for the EA 
XMI 1.1 exports that are ship with releases of the CIM.  To generate the XMI JAXB POJOs, release version 3.1.0 of XMLBeans was leveraged using the
following command-line options:

java -Xmx1024m -classpath D:\xmlbeans-3.1.0\lib\xmlbeans-3.1.0.jar;D:\xmlbeans-3.1.0\lib\xmlbeans-3.1.0\resolver.jar org.apache.xmlbeans.impl.inst2xsd.Inst2Xsd -design vb -simple-content-types string -enumerations never iec61970cim17v10_iec61968cim12v10_iec62325cim03v02-ea-xmi11.xml iec61970cim15v33_iec61968cim11v13_iec62325cim01v07-ea-xmi11.xml


Several things that should be noted within this command line:

1. A larger maximum heap size for Java is specified (-Xmx1024m) and was necessary in order to be able to process the larger XMI files without OutOfMemory errors.
2. The standard extensions of the XMI 1.1 export files (.xmi) needed to be manually renamed from .xmi to .xml for the XMLBeans to work correctly.
3. Both the "-design vb" and "-simple-content-types string" command line options are required in order to produce the desired XSD style previously mentioned.

The result of executing this from the command line is two new XSD schemas (schema0.xsd and schema1.xsd).  schema0.xsd was renamed to UML_1.3.xsd and schema1.xsd 
to XMI_1.1.xsd.  Additionally, an import statement had to be added to the XMI_1.1.xsd file in order to properly reference the UML_1.3.xsd. Finally, the XSDs were 
added to the src/main/resources/schema folder for use within the Maven build for the "clean generate-sources" goal.  This generates the necessary POJOs needed for 
the project.  Note that additionally, a jaxb-bindings.xjb file was necessary for the JAXB plugin to address customization to address a number of code generation 
issues at runtime.  For example, the name of a "value" XSD attribute had to be renamed to "theValue" and a duplicate DiagramElementType java class was also being 
generated that required custom bindings to fix build time errors.  The following is the resulting JAXB bindings file that was added to the src/main/resources/schema 
directory:

<?xml version="1.0" encoding="UTF-8"?>
<bindings xmlns="http://java.sun.com/xml/ns/jaxb"
		  xmlns:xjc="http://java.sun.com/xml/ns/jaxb/xjc"
          xmlns:xsi="http://www.w3.org/2000/10/XMLSchema-instance"
          xmlns:xs="http://www.w3.org/2001/XMLSchema"
          version="1.0">      
	<globalBindings>  
		<!-- By default JAXB generates Java POJOs with a suffix of "Type".  By specifying the simple global binding this is removed. -->           
    	<xjc:simple/>
  	</globalBindings>
    <bindings schemaLocation="XMI_1.1.xsd" version="1.0">
        <schemaBindings>
            <package name="org.cimug.compare.xmi1_1"/>
        </schemaBindings>   
    </bindings>
    <bindings schemaLocation="UML_1.3.xsd" version="1.0">
        <schemaBindings>
        	<package name="org.cimug.compare.uml1_3"/>
	    </schemaBindings>   
        <bindings node="//xs:complexType[@name='TaggedValueType']//xs:attribute[@name='value']">
        	<property name="theValue"/>
        </bindings>
       <bindings node="//xs:complexType[@name='DiagramElementType']">
        	<class name="DiagramElement"/>
        </bindings>
    </bindings>
</bindings>




