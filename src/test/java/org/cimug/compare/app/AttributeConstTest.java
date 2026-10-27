package org.cimug.compare.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathFactory;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Document;

/**
 * End to end from two XMI exports (#122): an attribute's Const is EA's
 * {@code changeable="frozen"}, not whether it has an initial value. The exports
 * are made from {@code multiroot/rootB-Extensions.xmi} by changing its
 * attribute {@code ratingMVA}.
 */
public class AttributeConstTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private static final Charset CP1252 = Charset.forName("windows-1252");
	private static final String OPEN = "<UML:Attribute name=\"ratingMVA\" changeable=\"none\"";

	private File export(String name, String changeable, String initialValue) throws Exception {
		File src = new File(getClass().getClassLoader().getResource("multiroot/rootB-Extensions.xmi").toURI());
		String xmi = new String(Files.readAllBytes(src.toPath()), CP1252);
		assertTrue(xmi.contains(OPEN));
		int i = xmi.indexOf(OPEN);
		int body = xmi.indexOf('>', i) + 1;
		String open = xmi.substring(i, body).replace("changeable=\"none\"", "changeable=\"" + changeable + "\"");
		String init = initialValue == null ? ""
				: "<UML:Attribute.initialValue><UML:Expression body=\"" + initialValue + "\" /></UML:Attribute.initialValue>";
		xmi = xmi.substring(0, i) + open + init + xmi.substring(body);
		File f = tmp.newFile(name);
		Files.write(f.toPath(), xmi.getBytes(CP1252));
		return f;
	}

	private Document compare(File baseline, File destination) throws Exception {
		File xml = new File(tmp.getRoot(), baseline.getName() + "-" + destination.getName() + ".xml");
		DiffXMLGenerator.main(new String[] { baseline.getPath(), destination.getPath(), xml.getPath(), "jpg" });
		return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xml);
	}

	private static String prop(Document d, String name, String what) throws Exception {
		return XPathFactory.newInstance().newXPath().evaluate(
				"//CompareItem[@name='ratingMVA']/Properties/Property[@name='" + name + "']/@" + what, d);
	}

	@Test
	public void anInitialValueAloneDoesNotMakeAnAttributeConst() throws Exception {
		Document d = compare(export("b1.xmi", "none", null), export("d1.xmi", "none", "3"));
		assertEquals("Changed", prop(d, "Default", "status"));
		assertEquals("3", prop(d, "Default", "model"));
		assertEquals("Identical", prop(d, "Const", "status"));
		assertEquals("false", prop(d, "Const", "model"));
	}

	@Test
	public void aFrozenAttributeIsConstWithOrWithoutAnInitialValue() throws Exception {
		Document d = compare(export("b2.xmi", "none", null), export("d2.xmi", "frozen", null));
		assertEquals("Changed", prop(d, "Const", "status"));
		assertEquals("false", prop(d, "Const", "baseline"));
		assertEquals("true", prop(d, "Const", "model"));
		assertEquals("Identical", prop(d, "Default", "status"));
		Document e = compare(export("b3.xmi", "frozen", "1"), export("d3.xmi", "frozen", "1"));
		assertEquals("Identical", prop(e, "Const", "status"));
		assertEquals("true", prop(e, "Const", "model"));
	}
}
