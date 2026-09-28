package org.cimug.compare.app;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.cimug.compare.report.ReportRenderer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * #65: Java 24 lowered the default XML entity size limits to 100,000
 * (JDK-8343006). A comparison XML with more escaped characters than that
 * (common in CIM notes) could no longer be read. cim-compare removes the
 * limits at startup, as it does for the XPath limits (#21).
 */
public class XmlProcessingLimitsTest {

	private static final String[] PROPERTIES = { "jdk.xml.totalEntitySizeLimit", "jdk.xml.maxGeneralEntitySizeLimit",
			"jdk.xml.xpathExprGrpLimit", "jdk.xml.xpathExprOpLimit", "jdk.xml.xpathTotalOpLimit" };

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private final Map<String, String> saved = new HashMap<String, String>();

	@Before
	public void saveProperties() {
		for (String p : PROPERTIES)
			saved.put(p, System.getProperty(p));
	}

	@After
	public void restoreProperties() {
		for (String p : PROPERTIES) {
			if (saved.get(p) == null)
				System.clearProperty(p);
			else
				System.setProperty(p, saved.get(p));
		}
	}

	/** The test fixture with a description holding 120,000 escaped characters. */
	private File largeComparison() throws Exception {
		InputStream in = getClass().getClassLoader().getResourceAsStream("report/mini-comparison.xml");
		String xml;
		try (Scanner s = new Scanner(in, "UTF-8")) {
			xml = s.useDelimiter("\\A").next();
		}
		StringBuilder notes = new StringBuilder();
		for (int i = 0; i < 120000; i++)
			notes.append("a &amp; b ");
		String anchor = "model=\"Main classes of Core.\"";
		assertTrue("fixture still has the CoreOverview notes", xml.contains(anchor));
		xml = xml.replace(anchor, "model=\"" + notes + "\"");
		File f = tmp.newFile("comparison-report.xml");
		Files.write(f.toPath(), xml.getBytes(StandardCharsets.UTF_8));
		return f;
	}

	@Test
	public void largeComparisonIsReadOnceTheLimitsAreRemoved() throws Exception {
		File xml = largeComparison();

		// Java 24's defaults
		System.setProperty("jdk.xml.totalEntitySizeLimit", "100000");
		System.setProperty("jdk.xml.maxGeneralEntitySizeLimit", "100000");
		try {
			new ReportRenderer(false, null, false, "jpg").render(xml, new File(tmp.getRoot(), "limited.html"));
			fail("expected the Java 24 entity size limit to stop the report");
		} catch (Exception expected) {
			assertTrue(String.valueOf(expected.getMessage()), String.valueOf(expected.getMessage()).contains("JAXP0001000"));
		}

		CIMModelComparisonGenerator.removeXmlProcessingLimits();
		File html = new File(tmp.getRoot(), "comparison-report.html");
		new ReportRenderer(false, null, false, "jpg").render(xml, html);
		assertTrue("report written", html.isFile());
	}
}
