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
					+ " <baseline-model-xmi-file> <target-model-xmi-file> <output-comparision-xml-file>");
			System.err.println();
			System.exit(1);
		}

		File baselineXmiFile = new File(args[0]);
		File targetXmiFile = new File(args[1]);
		File outputFile = new File(args[2]);

		try {
			XMI baselineXmi = loadCimModel(baselineXmiFile);
			XMI targetXmi = loadCimModel(targetXmiFile);

			/**
			 * Currently we have not fully implemented the legacy functionality for CIM models prior
			 * to CIM15. So this has been commented out and a GUIDBasedDiffReportGeneratorImpl used 
			 * directly:
			 */

			// DiffReportGenerator generator = DiffReportGeneratorFactory.createInstance(
			// baselineXmi.getXMIContent().getModel(), targetXmi.getXMIContent().getModel(),
			// outputFile);

			DiffReportGenerator generator = new GUIDBasedDiffReportGeneratorImpl(baselineXmi.getXMIContent().getModel(),
					targetXmi.getXMIContent().getModel(), outputFile);

			generator.processDiffReport();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static XMI loadCimModel(File xmiFile) throws JAXBException {
		JAXBContext jaxbContext = JAXBContext.newInstance(XMI.class);
		Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		return (XMI) JAXBIntrospector.getValue(jaxbUnmarshaller.unmarshal(xmiFile));
	}

}
