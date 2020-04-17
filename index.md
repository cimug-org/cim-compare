## cim-compare Release Log

### Release 1.0.0 [17-Apr-2020]
Initial release of the **cim-compare** project.  This release supports:

- Command line input of Enterprise Architect CompareLog (XML) files.
- Command line input of **XMI 1.1** format of Enterprise Architect exports of the CIM.  This allows one to bypass having to use Enterprise Architect to first generate and export a model comparison log. It also aligns this release with the XMI version supported by EA's Compare Utility.
- Generation of standalone HTML comparison reports viewable in most standard browsers (e.g. Chrome, Firefox, Edge, etc.)
- Given the size of comparison reports the initial release includes the ability to auto-locate and expand on a specific CIM class via a search by class name.
- Model changes represented using color-oriented visualization within reports.
