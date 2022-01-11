package org.cimug.compare.app;

import java.io.File;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.JAXBIntrospector;
import javax.xml.bind.Unmarshaller;

import org.cimug.compare.xmi1_1.XMI;

class DiffXMLGenerator {

	public static void main(String[] args) {
		if (args.length != 3) {
			System.err.println("Usage: java " + DiffXMLGenerator.class.getSimpleName()
					+ " <baseline-model-file> <target-model-file> <output-comparison-file>");
			System.err.println();
			System.exit(1);
		}

		File baselineXmiFile = new File(args[0]);
		File targetXmiFile = new File(args[1]);
		File outputFile = new File(args[2]);

		try {
			XMI baselineXmi = loadCimModel(baselineXmiFile);
			XMI targetXmi = loadCimModel(targetXmiFile);

			validateExporterVersion(baselineXmiFile, targetXmiFile, baselineXmi, targetXmi);

			DiffReportGenerator generator = new GUIDBasedDiffReportGeneratorImpl(baselineXmi.getXMIContent(),
					targetXmi.getXMIContent(), outputFile);

			generator.processDiffReport();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Currently only "UML 1.3 (XMI 1.1)" XMI exports (with "Unisys/Rose Format"
	 * unchecked) are supported for cim-compare comparison reports. This equates to
	 * an exporter version number of "2.5".
	 * 
	 * This method ensures that the XMI input files are compliant to the versions
	 * currently supported by cim-compare.
	 * 
	 * @param baselineXmiFile
	 * @param targetXmiFile
	 * @param baselineExporterVersion
	 * @param targetExporterVersion
	 */
	private static void validateExporterVersion(File baselineXmiFile, File targetXmiFile, XMI baselineXmi,
			XMI targetXmi) {

		String baselineExporterVersion = baselineXmi.getXMIHeader().getXMIDocumentation().getXMIExporterVersion();
		String targetExporterVersion = targetXmi.getXMIHeader().getXMIDocumentation().getXMIExporterVersion();

		if (!baselineExporterVersion.equals("2.5") || !targetExporterVersion.equals("2.5")) {

			System.err.println(
					"Currently only \"UML 1.3 (XMI 1.1)\" XMI exports (with \"Unisys/Rose Format\" unchecked) are supported for cim-compare comparison reports.");

			if (!baselineExporterVersion.equals("2.5")) {
				if (baselineExporterVersion.equals("4.1RR")) {
					System.err.println(" - The baseline XMI file [" + baselineXmiFile.getName()
							+ "] is a \"UML 1.3 (XMI 1.1)\" compliant export file but was exported with \"Unisys/Rose Format\" checked.");
				} else {
					System.err.println(" - The baseline XMI file [" + baselineXmiFile.getName()
							+ "] is not a \"UML 1.3 (XMI 1.1)\" compliant export file.");
				}
			}

			if (!targetExporterVersion.equals("2.5")) {
				if (targetExporterVersion.equals("4.1RR")) {
					System.err.println(" - The target XMI file [" + targetXmiFile.getName()
							+ "] is a \"UML 1.3 (XMI 1.1)\" compliant export file but was exported with \"Unisys/Rose Format\" checked.");
				} else {
					System.err.println(" - The target XMI file [" + targetXmiFile.getName()
							+ "] is not a \"UML 1.3 (XMI 1.1)\" compliant export file.");
				}
			}

			System.err.println();
			System.exit(1);
		}
	}

	private static XMI loadCimModel(File xmiFile) throws JAXBException {
		JAXBContext jaxbContext = JAXBContext.newInstance(XMI.class);
		Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		return (XMI) JAXBIntrospector.getValue(jaxbUnmarshaller.unmarshal(xmiFile));
	}

}
