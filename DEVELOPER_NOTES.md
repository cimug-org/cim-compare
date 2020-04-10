
## Developer Notes

These notes provide brief background that may be useful for future reference.  In particular, if the need arises for support of a later version of the XMI format the information which follows should be useful.  Right now Enterprise Architect only supports **XMI 1.1** for its comparison functionality and **cim-compare 1.0.0** does likewise.

After a search to locate one, an official **XMI_1.1.xsd** for XMI 1.1 was not available that could be used to generate JAXB classes for the needed inputs for processing. The same applied for the EA **CompareLog** XNL input file format.

A variety of open source and online tools for inferring XSD Schemas based on XML instance files were investigated.  We wanted the tool to be able to support reverse engineering XSD schemas in the "Venetian Blinds" design style (and not Salami Slice, Russian Doll, or Garden of Eden). This style caters particularly well for generating JAXB POJOs derived from XSD global complex types and with minimal anonymous classes. For more information check out the **"Basic Design Patterns"** section of the article [Schema scope: Primer and best practices](https://www.ibm.com/developerworks/library/x-schemascope/)

The outcome was the use of release 3.1.0 of the Apache XMLBeans open source project.  The tool generated the desired XSDs using a variety of CIM exports in the **"UML 1.3/XMI 1.1"** format type. An example of the command line invocation used to generate the XSDs used for this project:

```
java -Xmx2048m -classpath D:\xmlbeans-3.1.0\lib\xmlbeans-3.1.0.jar;D:\xmlbeans-3.1.0\lib\xmlbeans-3.1.0\resolver.jar org.apache.xmlbeans.impl.inst2xsd.Inst2Xsd -design vb -simple-content-types string -enumerations never iec61970cim17v10_iec61968cim12v10_iec62325cim03v02-ea-xmi11.xml iec61970cim15v33_iec61968cim11v13_iec62325cim01v07-ea-xmi11.xml
```

> For details on XMLBeans's **inst2xsd** (Instance to Schema Tool) visit: [Generate XML schema from XML instance files.](https://xmlbeans.apache.org/docs/3.0.0/guide/tools.html#inst2xsd)

Several things to note in the above command line example:

1. A larger max Java heap size is specified (```-Xmx1024m``` or ```-Xmx2048m``` depending on whether x86 or x64 JREs are used) and was necessary in order to be able
   to process the larger XMI files and eliminate **OutOfMemory** errors.
2. The standard extensions on the **XMI 1.1** instance data files needed to be renamed from **.xmi** to **.xml** for XMLBeans to execute.
3. Both the ```-design vb``` and ```-simple-content-types string``` command line options are required in order to produce the desired XSD style previously mentioned.

The result of executing this from the command line is two new XSD schemas (**schema0.xsd** and **schema1.xsd**).  **schema0.xsd** was renamed to **UML_1.3.xsd** and **schema1.xsd** to **XMI_1.1.xsd**.  Additionally, an import statement had to be added to the **XMI_1.1.xsd** file in order to properly reference the **UML_1.3.xsd**. Finally, the XSDs were  added to the ```src/main/resources/``` schema folder for use within the Maven build for the "clean generate-sources" goal.  This generates the necessary POJOs needed for the project.  Note that additionally, a **jaxb-bindings.xjb** file was necessary for the JAXB plugin config customizations in order to address a few runtime code generation issues.  For example, the name of a "value" XSD attribute had to be renamed to "theValue" and a duplicate **DiagramElementType** java class was also being  generated that required custom bindings to fix build time errors.  The following is the resulting JAXB bindings file that was added to the ```src/main/resources/schema
directory```:

``` XML
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
```
