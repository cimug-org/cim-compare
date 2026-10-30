# Review guidance for cim-compare

cim-compare compares two CIM UML models (EA project files, XMI 1.1 exports,
or EA compare logs) and writes a self-contained HTML report.

When reviewing a pull request, check:

- **Tests.** A fix or feature comes with a JUnit test that fails without it.
  Flag a code change in `src/main` with no matching change in `src/test`.
- **Java level.** Source and target are Java 8 (`pom.xml`): no `var`,
  records, text blocks, `List.of` or other APIs newer than Java 8.
- **Report assets** (`src/main/resources/report/`): `report.js` is inlined
  into the HTML, so a `//` comment must never be followed by code on the
  same line; the report must work offline (no external URLs, fonts or
  scripts). `report.xslt` is XSLT 3.0 run by Saxon-HE.
- **Comparison XML.** Changes to the comparison XML keep EA's Compare Log
  shape (`CompareItem`, `Properties`, `Property`), so EA's own logs and
  cim-compare's stay interchangeable.
- **Names.** No real company, utility or ISO names in code, test fixtures or
  documentation; fixtures use neutral names (for example ExtVendor,
  example.com).
- **Documentation.** README text is a how-to for the current release:
  present tense, no "now"/"also" change wording. Version numbers are written
  in full (2.4.0, not 2.4).
- **Release PRs** (`release-x.y.z` into `master`) bring together changes
  that were already reviewed; focus on anything not in an earlier PR.

Keep comments to real problems: correctness, a missing test, a broken
convention above. Skip style preferences the code base does not already
follow.
