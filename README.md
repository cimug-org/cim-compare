cim-compare
===========

>   Command line tool for generating CIM model comparison reports.

Project Overview
================

The CIM Model Comparison Report utility allows for generation of model
comparison reports between two points in time. Enterprise Architect (Corporate
Edition and above) has the capability to perform model comparisons; however, the
feature is not inherently useful as results are only exportable as XML files
that are not directly human consumable.

This Java command-line utility generates a self-contained HTML comparison report
of all changes between a “baseline” and “target” model. The report is viewable
in most standard browsers such as Firefox, Chrome, IE, etc. and provides the
ability to search for specific CIM classes within the report.

![](media/d894cccda3a7fb8859e998cbdcbbab8c.png)

The reports use colors to visually indicate changes to packages, classes,
attributes and associations. New items are shown in green, deleted items are in
red, items that have moved location within the model appear as yellow, blue
represents items that were changed in some way, and finally violet is reserved
for items that are unchanged.

![](media/a0e57c0c99a1473a3aa6595f664497d1.png)

Finally, when CIM standards are published (e.g. IEC 61970-301, IEC 61968-11 or
IEC 62325-301) it is often desirable to include an informational report of all
model-related changes since the prior publication of the standard. The
comparison reports produced by this utility can potentially be used to meet such
requirements.

Command Line Usage
------------------

There are two possible options for command-line usage for the utility to produce
an HTML CIM model comparison report.

### Option \#1: EA Model Comparison Logs

The first option is by specifying an **EA model comparison log** file. This
particular usage takes the following form:

| **Example EA model comparison file usage:**                                                                                     |
|---------------------------------------------------------------------------------------------------------------------------------|
| java -mx1024m -jar cim-compare.jar \<comparison-results-xml-file\> [\<output-directory-or-html-file\>] [--\<iec-package-name\>] |

*Parameters*:

**\<comparison-results-xml-file\>** (**Required**): The model comparison file.
When not specified as an absolute file path the location of the file is assumed
to be the directory the utility is being executed in.

**[\<output-directory-or-html-file\>] (Optional)**: An output directory or an
output HTML file. In the case where a directory is specified for this parameter
it must exist or execution will terminate with an appropriate error. When an
HTML file is provided it may be specified as an absolute file path or as simply
the name of the HTML file to be generated. If not specified then the application
will generate an HTML file whose name is derived from the two input files.

**[--\<iec-package-name\>] (Optional):** The root package to compare and
generate the comparison report on. Appearing at the end of the command-line, the
package must exist in both the baseline and target models (e.g. --IEC61970,
--IEC61968, --IEC62325, --TC57CIM, etc.) and be specified with two leading
dashes (-). Note that when no IEC package name is specified that the report is
generated from the root package in the models.

| **Command Line Examples:**                                                                                |
|-----------------------------------------------------------------------------------------------------------|
| java -jar cim-compare.jar "C:\\CIM XMI exports\\CIM15v33_CIM16v26a_EA_Comparison_Report.xml" "C:_Reports" |
| java -jar cim-compare.jar CIM15v33_CIM16v26a_EA_Comparison_Report.xml "C:_Reports"                        |
| java -jar cim-compare.jar CIM15v33_CIM16v26a_EA_Comparison_Report.xml "C:_Reports" --IEC61968             |
| java -jar cim-compare.jar CIM15v33_CIM16v26a_EA_Comparison_Report.xml ComparisonReport_15v33_16v26a.html  |
| java -jar cim-compare.jar CIM15v33_CIM16v26a_EA_Comparison_Report.xml                                     |
| java -jar cim-compare.jar CIM15v33_CIM16v26a_EA_Comparison_Report.xml --IEC61970                          |

### Option \#2: XMI Baseline and Target Models

The second usage option allows one to bypass Enterprise Architect and directly
specify two XMI 1.1 compliant files representing the **baseline** and **target**
models respectively. In this scenario the command-line usage takes the following
form:

| **Example EA model baseline and target model input files usage:**                                                                                     |
|-------------------------------------------------------------------------------------------------------------------------------------------------------|
| java -mx1024m -jar cim-compare.jar \<baseline-model-xmi-file\> \<target-model-xmi-file\> [\<output-directory-or-html-file\>] [--\<iec-package-name\>] |

*Parameters*:

**\<baseline-model-xmi-file\>** (**Required**): An XMI 1.1 compliant baseline
model file exported from EA. When not specified as an absolute file path the
location of the file is assumed to be the directory the utility is being
executed from.

**\<target-model-xmi-file\>** (**Required**): An XMI 1.1 compliant target model
file exported from EA. When not specified as an absolute file path the location
of the file is assumed to be the directory the utility is being executed from.

**[\<output-directory-or-html-file\>] (Optional)**: An output directory or an
output HTML file. In the case where a directory is specified for this parameter
it must exist or execution will terminate with an appropriate error. When an
HTML file is provided it may be specified as an absolute file path or as simply
the name of the HTML file to be generated. If not specified then the application
will generate an HTML file whose name is derived from the two input files.

**[--\<iec-package-name\>] (Optional):** The root package within the models from
which to start the comparison report from. Appearing at the end of the
command-line, when specified the package must exist in both the baseline and
target models (e.g. --IEC61970, --IEC61968, --IEC62325, --TC57CIM, etc.) and be
specified with two leading dashes (-). Note that when no IEC package name is
specified that the report is generated from the root package in the models.

| **Command Line Examples:**                                                                               |
|----------------------------------------------------------------------------------------------------------|
| java -jar cim-compare.jar "C:\\XMI exports15v33.xmi" "C:\\XMI exports16v26a.xmi" "C:\\"                  |
| java -jar cim-compare.jar CIM15v33.xmi CIM16v26a.xmi "C:\\"                                              |
| java -jar cim-compare.jar CIM15v33.xmi CIM16v26a.xmi "C:15v33_CIM16v26a_ComparisonReport.html"           |
| java -jar cim-compare.jar CIM15v33.xmi CIM16v26a.xmi CIM15v33_CIM16v26a_ComparisonReport.html            |
| java -jar cim-compare.jar CIM15v33.xmi CIM16v26a.xmi CIM15v33_CIM16v26a_ComparisonReport.html --IEC62325 |

Java Technical Requirements
---------------------------

The utility consists of a self-contained executable jar file executable from the
command line with no additional dependencies.

As of Java 9 and forward the JAXB binding APIs are no longer packaged with the
JRE and therefore the jar that is shipped contains these libraries. The utility
has been tested and confirmed to run successfully using Java 1.8 and Java 9
through 12.

Given the large file sizes consumed and produced by the utility, **OutOfMemory**
exceptions occur if either the default heap size is used or too little heap is
allocated on the command line. The following guidelines should be followed:

-   For a 32-bit JRE it is recommended to specify a max heap size of at least 1G
    (i.e. 1024m) resources permitting. With a 1G heap size the execution will
    typically run a little slower. Note that the maximum theoretical heap limit
    for a 32-bit JVM is 4G. However, due to various additional constraints such
    as available swap, kernel address space usage, memory fragmentation, and VM
    overhead, in practice the limit can be much lower. On most modern 32-bit
    Windows systems the maximum heap size will range from 1.4G to 1.6G.

| **Example x86 32-bit command-line parameters:**                                               |
|-----------------------------------------------------------------------------------------------|
| java -mx1024m -jar **cim-compare.jar** iec61970cim16v26a.xmi iec61970cim17v16.xmi D:\\reports |

-   For 64 bit JREs it is recommended that the maximum heap size be increased to
    at least 2G (i.e. 2048m) if available. It has been observed that 2G is
    usually sufficient to handle the largest CIM files one might encounter.

| **Example x64 command-line parameters:**                                                      |
|-----------------------------------------------------------------------------------------------|
| java -mx2048m -jar **cim-compare.jar** iec61970cim16v26a.xmi iec61970cim17v16.xmi D:\\reports |

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Release History
---------------

-   1.0.0

    -   Initial Release

-   0.0.1

    -   Work in progress

License
-------

Distributed under the MIT license. See `LICENSE` for more information.

<https://github.com/CIMug-org/cim-compare>
