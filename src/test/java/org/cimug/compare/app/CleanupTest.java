package org.cimug.compare.app;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * {@code --zip --cleanup} deletes only what the run created, never the user's
 * input (issue #53). Runs the command line with an EA compare log as input,
 * which needs no Enterprise Architect installation.
 */
public class CleanupTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private File compareLog() throws Exception {
		File xml = tmp.newFile("my-ea-compare-log.xml");
		InputStream in = getClass().getClassLoader().getResourceAsStream("report/mini-comparison.xml");
		try {
			Files.copy(in, xml.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} finally {
			in.close();
		}
		return xml;
	}

	@Test
	public void cleanupKeepsTheInputCompareLog() throws Exception {
		File xml = compareLog();
		File dir = xml.getParentFile();

		CIMModelComparisonGenerator.main(new String[] { xml.getAbsolutePath(), "--zip", "--cleanup" });

		assertTrue("the input compare log is kept", xml.isFile());
		assertTrue("ZIP written", new File(dir, "my-ea-compare-log.zip").isFile());
		assertFalse("report removed (it is in the ZIP)", new File(dir, "my-ea-compare-log.html").exists());
		assertFalse("enriched XML removed", new File(dir, "my-ea-compare-log-enriched.xml").exists());
	}

	@Test
	public void withoutCleanupEverythingIsKept() throws Exception {
		File xml = compareLog();
		File dir = xml.getParentFile();

		CIMModelComparisonGenerator.main(new String[] { xml.getAbsolutePath(), "--zip" });

		assertTrue(xml.isFile());
		assertTrue(new File(dir, "my-ea-compare-log.zip").isFile());
		assertTrue(new File(dir, "my-ea-compare-log.html").isFile());
		assertTrue(new File(dir, "my-ea-compare-log-enriched.xml").isFile());
	}
}
