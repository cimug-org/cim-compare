package org.cimug.compare.app;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Simple test to perform a sanity check on the output files for XMI inputs.
 */
public class CIMModelComparisonGeneratorUTEST {

	private static final String RESOURCES_PATH = "src/test/resources/";
	private static final String TARGET_XMI = "cim_17v36a_13v12_03v17a.xmi";
	private static final String BASELINE_XMI = "cim_15v33_11v13_01v07.xmi";
	private static final String REPORT_NAME_XML = "JUNIT_CIM_Model_Comparison_Report.xml";
	private static final String REPORT_NAME_HTML = "JUNIT_CIM_Model_Comparison_Report.html";

	private File xmlOutputFile;
	private File htmlOutputFile;

	@Before
	public void setUp() throws Exception {
		xmlOutputFile = new File(RESOURCES_PATH + REPORT_NAME_XML);
		htmlOutputFile = new File(RESOURCES_PATH + REPORT_NAME_HTML);
	}

	@After
	public void tearDown() throws Exception {
		xmlOutputFile.delete();
		htmlOutputFile.delete();
	}

	@Test
	public void test() {
		CIMModelComparisonGenerator.main(new String[] { RESOURCES_PATH + BASELINE_XMI, RESOURCES_PATH + TARGET_XMI,
				htmlOutputFile.getAbsolutePath() });

		assertTrue("The intermediate XML comparison log file was not generated: " + xmlOutputFile.getAbsolutePath(),
				xmlOutputFile.exists());
		assertTrue("The HTML comparison report was not generated: " + htmlOutputFile.getAbsolutePath(),
				htmlOutputFile.exists());
	}

}
