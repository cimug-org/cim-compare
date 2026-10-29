package org.cimug.compare.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * #143: the comparison XML is written in the UTF-8 it declares, whatever the JVM's
 * default character set. Java 17 and earlier on Windows default to Windows-1252, which
 * wrote characters outside ASCII (here typographic quotes) as bytes that are not UTF-8.
 * The run is made in a separate JVM whose default is forced to Windows-1252.
 */
public class ComparisonXmlEncodingTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private static final Charset CP1252 = Charset.forName("windows-1252");

	@Test
	public void theComparisonXmlIsUtf8UnderAWindows1252Default() throws Exception {
		File src = new File(getClass().getClassLoader().getResource("multiroot/rootB-Extensions.xmi").toURI());
		String xmi = new String(Files.readAllBytes(src.toPath()), CP1252);
		File baseline = tmp.newFile("baseline.xmi"), destination = tmp.newFile("destination.xmi");
		Files.write(baseline.toPath(), xmi.getBytes(CP1252));
		String changed = xmi.replace("An extension line.", "An ‘extension’ line.");
		assertFalse("the fixture has the description to change", changed.equals(xmi));
		Files.write(destination.toPath(), changed.getBytes(CP1252));
		File out = tmp.newFolder("out");

		String java = new File(new File(System.getProperty("java.home"), "bin"), "java").getPath();
		List<String> command = Arrays.asList(java, "-Dfile.encoding=windows-1252", "-cp",
				System.getProperty("java.class.path"), CIMModelComparisonGenerator.class.getName(), baseline.getPath(),
				destination.getPath(), out.getPath());
		Process p = new ProcessBuilder(command).redirectErrorStream(true).start();
		String console = new String(readAll(p.getInputStream()), StandardCharsets.UTF_8);
		assertEquals(console, 0, p.waitFor());

		File[] xmls = out.listFiles((d, n) -> n.startsWith("CIMModelComparison_") && n.endsWith(".xml") && !n.endsWith("-enriched.xml"));
		assertEquals(console, 1, xmls.length);
		File xml = xmls[0];
		byte[] bytes = Files.readAllBytes(xml.toPath());
		String text = utf8(bytes); // fails on a byte that is not UTF-8
		assertTrue(text.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\""));
		assertTrue("the quotes are UTF-8", text.contains("An ‘extension’ line."));
		assertFalse("no parser error is printed", console.contains("[Fatal Error]"));
	}

	private static String utf8(byte[] bytes) throws CharacterCodingException {
		return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
				.onUnmappableCharacter(CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(bytes)).toString();
	}

	private static byte[] readAll(java.io.InputStream in) throws java.io.IOException {
		java.io.ByteArrayOutputStream b = new java.io.ByteArrayOutputStream();
		byte[] buf = new byte[8192];
		for (int n; (n = in.read(buf)) > 0;)
			b.write(buf, 0, n);
		return b.toByteArray();
	}
}
