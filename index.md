## cim-compare Release Log

### Release 1.2.0 [10-Jan-2022]
Minor release of the **cim-compare** project.  This release includes:

- Support for Enterprise Architect .eap files as direct input into report generation. This eliminates the intermediate step of manually exporting baseline and destination XMI files from EA in order to generate reports.
- New **--export-diagrams**, **--include-diagrams** and **--image-type** command line options. When **--export-diagrams** is specified **cim-compare** will export diagrams along with the XMI as part of processing the .eap files. Consult the README file for details on the usage of these new options.
- A new **--zip** command line option. When specified the generated report is packaged into a single ZIP archive. This was added to simplify distribution of reports when diagrams are included.
- Diagram images displayed as part of comparison reports are now auto-sized to better scale relative to the size of the browser window.
- Introduced support for indicating when Classes and Attributes have been 'deprecated' in the model the 'deprecated' stereotype. The indicators are now carried through and indicated where applicable.
- Based on end user feedback the 'Links' section of the comparison report has been reworked to better communicate changes to Associations, Generalizations, etc.
- Numerous other minor bug fixes and CIM comparison report format and layout updates.

### Release 1.1.0 [09-Feb-2021]
Minor release of the **cim-compare** project.  This release includes:

- By default **cim-compare** generates comparison reports that include packages, classes, diagrams, attributes, etc. that are "Identical" between baseline and target models. Such packages and classes are included as they are useful for reference purposes within reports. A new **--minimal** command line option has been introduced in this release.  When specified **cim-compare** will exclude all "Identical" references and report only the packages, classes, class attributes, associations, generalizations, and UML diagrams that have had actual changes. This feature is useful when it is necessary to perform detailed analysis of only the most concise set of changes between models.
- A key enhancement in this release is support for UML diagrams comparisons in comparison reports. Refer to the "Enterprise Architect XMI Export Procedures" section of **cim-compare**'s README.md file for further information.  

### Release 1.0.1 [31-Jan-2021]
Patch release of the **cim-compare** project.  This release includes:

- Better command line XMI export version validation and end-user error output. **cim-compare** will now provide more detail on what specifically was invalid for a baseline or target XMI input file that was not exported by an Enterprise Architect exporter currently support by **cim-compare**. For example the "Unisys/Rose Format" option must not be checked for an XMI export within Enterprise Architect. This is detected and reported to the end-user with graceful termination of execution.
- Fixed numerous defects reported around the Links section of the comparison reports.
- Additional improvements to the HTML output generated so that changes are more clearly communicated.

### Release 1.0.0 [17-Apr-2020]
Initial release of the **cim-compare** project.  This release includes:

- Command line input of Enterprise Architect CompareLog (XML) files.
- Command line input of  **UML 1.3 (XMI 1.1)** format of Enterprise Architect exports of the CIM.  This allows an end-user to bypass having to use Enterprise Architect to first generate and export a model comparison log. It also aligns this release with the XMI version supported by EA's Compare Utility.
- Generation of standalone HTML comparison reports viewable in most standard browsers (e.g. Chrome, Firefox, Edge, etc.)
- Given the size of comparison reports the initial release includes the ability to auto-locate and expand on a specific CIM class via a search by class name.
- Model changes represented using color-oriented visualization within reports.
