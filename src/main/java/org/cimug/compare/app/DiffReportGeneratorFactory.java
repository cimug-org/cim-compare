package org.cimug.compare.app;

import java.io.File;

import javax.xml.bind.JAXBException;

import org.cimug.compare.uml1_3.KeyTypeEnum;
import org.cimug.compare.uml1_3.Model;

final class DiffReportGeneratorFactory {

	public static DiffReportGenerator createInstance(Model baselineModel, Model targetModel, File outputFile) {
		PreProcessor preProcessor = new PreProcessor(baselineModel, targetModel);

		DiffReportGenerator generator;
		try {
			if (KeyTypeEnum.GUID.equals(preProcessor.getKeyType())) {
				generator = new GUIDBasedDiffReportGeneratorImpl(baselineModel, targetModel, outputFile);
			} else {
				generator = new NameBasedDiffReportGeneratorImpl(baselineModel, targetModel, outputFile);
			}
		} catch (JAXBException e) {
			throw new RuntimeException(e);
		}
		
		return generator;
	}

}
