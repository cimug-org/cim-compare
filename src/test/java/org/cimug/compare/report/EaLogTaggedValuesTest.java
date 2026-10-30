package org.cimug.compare.report;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.cimug.compare.logs.EACompareLog;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * #169: tagged values as EA's own Compare Utility logs them (Log To XML, EA 17), for
 * EA logs given as input (Option #3). The fixture is such a log with its names made
 * neutral: the compared package, whose baseuri changed, a class that gained a tag, and an
 * association (listed under both its classes) that gained one. EA leaves those
 * elements Identical; the report counts them as changed.
 */
public class EaLogTaggedValuesTest {

	private static final XPath XP = XPathFactory.newInstance().newXPath();

	private static Document parse(InputStream in) throws Exception {
		DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
		f.setIgnoringElementContentWhitespace(false);
		DocumentBuilder b = f.newDocumentBuilder();
		Document d = b.parse(in);
		strip(d.getDocumentElement());
		return d;
	}

	/** Removes whitespace-only text, so formatting does not matter. */
	private static void strip(Node n) {
		for (Node c = n.getFirstChild(); c != null;) {
			Node next = c.getNextSibling();
			if (c.getNodeType() == Node.TEXT_NODE && c.getTextContent().trim().isEmpty())
				n.removeChild(c);
			else if (c.getNodeType() == Node.ELEMENT_NODE)
				strip(c);
			c = next;
		}
	}

	private InputStream fixture() {
		return getClass().getClassLoader().getResourceAsStream("report/ea-log-tagged-values.xml");
	}

	@Test
	public void eaTaggedValueGroupsAreReadAndWrittenBackUnchanged() throws Exception {
		JAXBContext ctx = JAXBContext.newInstance(EACompareLog.class);
		EACompareLog log = (EACompareLog) ctx.createUnmarshaller().unmarshal(fixture());
		Marshaller m = ctx.createMarshaller();
		m.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		m.marshal(log, out);
		Document original = parse(fixture());
		Document written = parse(new java.io.ByteArrayInputStream(out.toByteArray()));
		assertTrue("the same document after JAXB", original.getDocumentElement().isEqualNode(written.getDocumentElement()));
		assertEquals(4.0, XP.evaluate("count(//CompareItem[@type='Tagged Values'])", written, javax.xml.xpath.XPathConstants.NUMBER));
	}

	@Test
	public void eaTaggedValuesAreReported() throws Exception {
		Document log = parse(fixture());
		Document enriched = new ReportPreProcessor(false, null).process(log);
		// #188: the compared package itself (ExtVendor, whose baseuri changed) is the log's root; the
		// report starts there, so its own tag change and the classes directly in it are shown
		assertEquals("ExtVendor (full model)", XP.evaluate("/ComparisonReport/@scope", enriched));
		assertEquals("ExtVendor", XP.evaluate("/ComparisonReport/Package/@name", enriched));
		assertEquals("changed", XP.evaluate("/ComparisonReport/Package/@status", enriched));
		assertEquals("http://example.org/test/vendor-schema-cim16#",
				XP.evaluate("/ComparisonReport/Package/Tags/Tag[@name='baseuri']/@model", enriched));
		assertEquals("1", XP.evaluate("count(/ComparisonReport/Package/Class[@name='AccuracyClass'])", enriched));
		// the class that gained a tag, and the association under each of its classes
		assertEquals("changed", XP.evaluate("//Class[@name='ExtVendorSynchronousMachine']/@status", enriched));
		assertEquals("added", XP.evaluate("//Class[@name='ExtVendorSynchronousMachine']/Tags/Tag/@status", enriched));
		// the association is listed under both its classes, AccuracyClass being directly in the compared package
		assertEquals("2", XP.evaluate("count(//Link[@kind='Association'][Tags/Tag[@status='added']])", enriched));
		assertEquals("changed", XP.evaluate("//Link[@kind='Association']/@status", enriched));
		assertEquals("https://example.com/Extensions#",
				XP.evaluate("(//Link[@kind='Association']/Tags/Tag/@model)[1]", enriched));
		// a "Tagged Values" group is never taken for an attribute
		assertEquals("0", XP.evaluate("count(//Attribute[@name='Tagged Values'])", enriched));
	}
}
