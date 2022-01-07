
## Developer Notes

The background provided here may be useful in the future should the need arise to support a later version of the XMI format.  Currently, Enterprise Architect only supports **XMI 1.1** for its comparison utility and, correspondingly **cim-compare** as well.

After investigation, an official **XMI_1.1.xsd** for **XMI 1.1** was unavailable for generating JAXB objects for the needed inputs to **cim-compare**. The same applied for the EA **CompareLog** XML input file format.

A variety of open source and online tools for inferring XSD Schemas based on XML instance files were investigated.  We wanted the tool to be able to support reverse engineering XSD schemas in the "Venetian Blinds" design style (and not Salami Slice, Russian Doll, or Garden of Eden). This style caters particularly well for generating JAXB POJOs derived from XSD global complex types and with minimal anonymous classes. For more information check out the "Basic Design Patterns" section of the article [Schema scope: Primer and best practices](https://www.ibm.com/developerworks/library/x-schemascope/)

The outcome was the use of release 3.1.0 of the [Apache XMLBeans](https://xmlbeans.apache.org/) open source project.  The tool generated the desired XSDs using a variety of CIM model export files in the **"UML 1.3/XMI 1.1"** format and exported from Enterprise Architect. The following is an example of the command line invocation used to generate XSDs for XMI 1.1:

```
java -Xmx2048m -classpath D:\xmlbeans-3.1.0\lib\xmlbeans-3.1.0.jar;D:\xmlbeans-3.1.0\lib\xmlbeans-3.1.0\resolver.jar org.apache.xmlbeans.impl.inst2xsd.Inst2Xsd -design vb -simple-content-types string -enumerations never iec61970cim17v10_iec61968cim12v10_iec62325cim03v02-ea-xmi11.xml iec61970cim15v33_iec61968cim11v13_iec62325cim01v07-ea-xmi11.xml
```

> For details on XMLBeans's **inst2xsd** (Instance to Schema Tool) visit: [Generate XML schema from XML instance files.](https://xmlbeans.apache.org/docs/3.0.0/guide/tools.html#inst2xsd)

Several things to note in the above command line example:

1. A larger max Java heap size was specified (```-Xmx1024m``` or ```-Xmx2048m``` depending on whether x86 or x64 JREs are used) and was necessary in order to be able to process the larger XMI files and eliminate **OutOfMemory** errors.
2. The standard extensions on **XMI 1.1** instance data files needed to be renamed from **.xmi** to **.xml** for XMLBeans to execute correctly.
3. Both the ```-design vb``` and ```-simple-content-types string``` command line options were required in order to produce the desired XSD style previously mentioned.

This resulted in two new XSD schemas (schema0.xsd and schema1.xsd) with **schema0.xsd** renamed to **UML_1.3.xsd** and **schema1.xsd** to **XMI_1.1.xsd**. An import statement had to be added to the **XMI_1.1.xsd** to properly reference the **UML_1.3.xsd**.  The renamed XSDs were added to the ```src/main/resources/schema``` folder for access by Maven during a ```clean generate-sources``` to generate the JAXB POJOs.  Finally, a **jaxb-bindings.xjb** bindings file had to be added to the ```src/main/resources/schema``` directory to address a couple of runtime code generation issues.  For example, for an XSD attribute named "value" the bindings file needed to provide configuration to rename the attribute to "theValue".

The following **jaxb-bindings.xjb** file was added to the ```src/main/resources/schema```
directory:

``` XML
<?xml version="1.0" encoding="UTF-8"?>
<bindings xmlns="http://java.sun.com/xml/ns/jaxb"
		  xmlns:xjc="http://java.sun.com/xml/ns/jaxb/xjc"
          xmlns:xsi="http://www.w3.org/2000/10/XMLSchema-instance"
          xmlns:xs="http://www.w3.org/2001/XMLSchema"
          version="1.0">      
	<globalBindings>  
		<!-- By default JAXB generates Java POJOs with a suffix of "Type".  
    By specifying the simple global binding this is removed. -->           
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
```
