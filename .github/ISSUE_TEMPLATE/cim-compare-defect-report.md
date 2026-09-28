---
name: cim-compare Defect Report
about: Create a report to help us improve cim-compare
title: "[DEFECT] "
labels: bug
assignees: ''

---

> [!CAUTION]
> **Do not use this template to report a security vulnerability.**
> Filing a vulnerability here discloses it publicly, to everyone, before a fix
> exists. Report it privately instead: [open a security advisory](https://github.com/cimug-org/cim-compare/security/advisories/new).

### Describe the defect:

A clear and concise description of what the defect is. Include what you were trying to do, the input you used (two `.eap`/`.qea` project files, two XMI files, or an EA compare log) and the full command line with its options (e.g. `java -Xmx4G -Djava.library.path="C:\cim-compare\ea16" -jar cim-compare-2.0.0.jar CIM17v40.qea CIM18v16.qea --package=Grid --include-diagrams --zip`).

### cim-compare release:

The exact release of cim-compare you were using (e.g. `cim-compare-2.0.0.jar`), and how it was run:
- Java vendor and version, and whether it is 32-bit or 64-bit (the output of `java -version`)
- when `.eap`/`.qea` project files are the input: the Enterprise Architect version and which `SSJavaCOM.dll` / `SSJavaCOM64.dll` folder was on `-Djava.library.path`

### CIM model versions:

The baseline and destination models compared. For example:
- iec61970cim17v40_iec61968cim13v13b_iec62325cim03v17b_CIM100.1.1.1.qea → CIM_Grid18v16_Enterprise14v00_Market04v14.qea
- iec61970cim16v33c_iec61968cim12v08_iec62325cim03v01a.xmi → ...
- or the name of the EA compare log, if that was the input

### To Reproduce:

Depending on the nature of the defect, it can help to give the steps that produce it. For example:
1. Run `java -jar cim-compare-2.0.0.jar ...`
2. Open the report and expand '...'
3. See '...'

**Expected behavior**
A clear and concise description of what you expected to happen.

**Screenshots**
For a problem in the report, a screenshot with the problem marked. For an error, the console output or stack trace in a code block (the first 20 lines or so are usually enough).

**Additional context**
Anything else that helps: whether it also happens with or without `--package`, with a different image type, or with an earlier release; a link to a report that shows it.
