
## Developer Notes

### Building and Testing

Build with Maven from the project directory:

```
mvn clean package
```

This compiles the code, runs the unit tests and produces the self-contained `target/cim-compare-<version>.jar`. Don't skip the tests (`-DskipTests` or `-Dmaven.test.skip=true`, which is what Eclipse's "Skip Tests" checkbox adds): they take a few seconds and need no Enterprise Architect installation. `mvn test` runs the tests alone.

The tests are in `src/test/java`; Maven runs the classes whose names end in `Test`:

- `WordDiffTest` – the word-level redlining of changed text.
- `DiagramElementDiffTest` – the element and connector differences between two copies of a diagram ([#37](https://github.com/cimug-org/cim-compare/issues/37)).
- `ReportPreProcessorTest` – the preparation of the comparison XML for the report (statuses, counts, links, `--full`, `--package`), over the small hand-written comparison `src/test/resources/report/mini-comparison.xml`. The comment at the top of that file says which case each element in it exercises.
- `ReportRendererTest` – producing the HTML report and the enriched XML.
- `CleanupTest` – `--zip --cleanup` never deletes the user's input files.
- `XmlProcessingLimitsTest` – a large comparison XML is read under Java 24's XML entity size limits once cim-compare has removed them (#65).

`CIMModelComparisonGeneratorUTEST` runs the whole comparison over large XMI files in `src/test/resources`; Maven doesn't run it by default, so run it by hand when changing the XMI comparison.

### Java Versions: XML Limits and Native Access

Recent Java releases added limits and checks that cim-compare's inputs run into, so cim-compare lifts them itself rather than asking users for command-line settings:

- **XML processing limits.** `CIMModelComparisonGenerator.removeXmlProcessingLimits()`, called first in `main`, sets to `0` (no limit) the XPath limits introduced in Java 11 (#21) and the XML entity size limits Java 24 lowered to 100,000 (`jdk.xml.totalEntitySizeLimit`, `jdk.xml.maxGeneralEntitySizeLimit`; #65). It must run before any XML parser, XPath or XSLT processor is created. If a future Java release tightens another `jdk.xml.*` limit, add it there, with a test in `XmlProcessingLimitsTest`. To reproduce Java 24's behaviour on an older Java, run with `-Djdk.xml.totalEntitySizeLimit=100000 -Djdk.xml.maxGeneralEntitySizeLimit=100000`.
- **Native access.** EA's Java API (`eaapi.jar`) loads `SSJavaCOM(64).dll`, which Java 24+ reports as a restricted method (JEP 472). The shade plugin's manifest in `pom.xml` declares `Enable-Native-Access: ALL-UNNAMED` for the executable jar; this can't be set from code. Running from an IDE or with the classes on a plain class path instead of the jar, add `--enable-native-access=ALL-UNNAMED` to the JVM arguments.

### The HTML Report

The report is produced in three steps (`org.cimug.compare.report`):

1. `ReportPreProcessor` turns the comparison XML (EA compare log format) into the *enriched* XML: element kinds and effective statuses resolved, identical items dropped unless `--full`, redlines computed (`WordDiff`), links between classes resolved, and summary counts added. The format is described in the class's Javadoc. It is written beside the report as `<name>-enriched.xml`.
   For diagrams ([#37](https://github.com/cimug-org/cim-compare/issues/37)), the comparison step (`DiagramElementDiff`, called from `GUIDBasedDiffReportGeneratorImpl`) adds `DiagramObject` and `DiagramConnector` items under each changed diagram in the comparison XML. They carry each element's box in the exported image's pixels, as written by EA (`imgL`/`imgT`/`imgR`/`imgB`), and each changed connector with the boxes of the two elements it joins. `ReportPreProcessor` turns them into `Highlight` and `Connector` elements in the enriched XML; an element is marked changed only when its class changed in something its box shows.
2. `ReportRenderer` runs `src/main/resources/report/report.xslt` (XSLT 3.0, run with Saxon-HE) over the enriched XML, then deletes the images of diagrams the report does not show.
3. The stylesheet copies `report.css`, `report.js` and the header logo `logo.svg` into the page, so the report is a single file that needs no network access. `logo.svg` is the cim-compare logo for dark backgrounds, with its internal ids prefixed `ccl-` so they cannot clash with ids in the report. `report.js` places the diagram highlights over the images once they load.

To change the look or behaviour of the report, edit those three files in `src/main/resources/report`; to change what the report knows about the comparison, change `ReportPreProcessor` (and its tests).


### XMI 1.1 and EA Compare Log Schemas

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
