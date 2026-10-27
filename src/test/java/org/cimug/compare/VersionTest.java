package org.cimug.compare;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

/** #87: the version comes from the pom, so the report and usage text cannot drift from it. */
public class VersionTest {

	@Test
	public void versionIsThePomVersion() throws Exception {
		String pom = new String(Files.readAllBytes(new File("pom.xml").toPath()), StandardCharsets.UTF_8);
		Matcher m = Pattern.compile("<artifactId>cim-compare</artifactId>\\s*<version>([^<]+)</version>").matcher(pom);
		assertTrue("pom version not found", m.find());
		assertEquals(m.group(1).trim(), Version.VERSION);
	}

	@Test
	public void releaseUrlNamesTheVersion() {
		assertEquals("https://github.com/cimug-org/cim-compare/releases/tag/" + Version.VERSION, Version.RELEASE_URL);
	}
}
