package org.cimug.compare;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.StringReader;

import javax.xml.bind.JAXBContext;
import javax.xml.transform.stream.StreamSource;

import org.cimug.compare.logs.Property;
import org.cimug.compare.uml1_3.AttributeType;
import org.junit.Test;

/**
 * An attribute's AllowDuplicates and Scale compare as their defaults (false, 0)
 * when the tag is blank or missing, as EA writes them in .eap projects, so a
 * mixed .eap/.qea comparison reports no false changes (issue #86).
 */
public class AttributePropertiesTest {

	/** An attribute as EA exports it; {@code tags} are extra TaggedValue elements. */
	private static AttributeType attribute(String tags) throws Exception {
		String xml = "<UML:Attribute xmlns:UML=\"omg.org/UML1.3\" name=\"r\" changeable=\"none\" visibility=\"public\""
				+ " ownerScope=\"instance\" targetScope=\"instance\">"
				+ "<UML:StructuralFeature.type><UML:Classifier xmi.idref=\"eaxmiid1\"/></UML:StructuralFeature.type>"
				+ "<UML:ModelElement.taggedValue>"
				+ "<UML:TaggedValue tag=\"type\" value=\"Resistance\"/>"
				+ "<UML:TaggedValue tag=\"lowerBound\" value=\"0\"/>"
				+ "<UML:TaggedValue tag=\"upperBound\" value=\"1\"/>"
				+ "<UML:TaggedValue tag=\"description\" value=\"Positive sequence resistance.\"/>"
				+ tags + "</UML:ModelElement.taggedValue></UML:Attribute>";
		return JAXBContext.newInstance(AttributeType.class).createUnmarshaller()
				.unmarshal(new StreamSource(new StringReader(xml)), AttributeType.class).getValue();
	}

	private static final String QEA = "<UML:TaggedValue tag=\"duplicates\" value=\"0\"/>"
			+ "<UML:TaggedValue tag=\"scale\" value=\"0\"/>";
	private static final String EAP_EMPTY = "<UML:TaggedValue tag=\"duplicates\" value=\"\"/>"
			+ "<UML:TaggedValue tag=\"scale\" value=\"\"/>";
	private static final String EAP_MISSING = "";

	private static Property property(AttributeProperties p, String name) {
		for (Property x : p.getProperties().getProperty())
			if (name.equals(x.getName()))
				return x;
		throw new AssertionError("no property " + name);
	}

	private static void assertDefaults(AttributeProperties p) {
		assertEquals(Status.Identical, p.getStatus());
		assertEquals("false", property(p, "AllowDuplicates").getBaseline());
		assertEquals("false", property(p, "AllowDuplicates").getModel());
		assertEquals("0", property(p, "Scale").getBaseline());
		assertEquals("0", property(p, "Scale").getModel());
	}

	@Test
	public void eapToQeaWithEmptyTagsIsIdentical() throws Exception {
		assertDefaults(new AttributeProperties(attribute(EAP_EMPTY), attribute(QEA)));
	}

	@Test
	public void qeaToEapWithMissingTagsIsIdentical() throws Exception {
		assertDefaults(new AttributeProperties(attribute(QEA), attribute(EAP_MISSING)));
	}

	@Test
	public void realChangesAreStillReported() throws Exception {
		AttributeProperties p = new AttributeProperties(attribute(EAP_EMPTY),
				attribute("<UML:TaggedValue tag=\"duplicates\" value=\"1\"/><UML:TaggedValue tag=\"scale\" value=\"2\"/>"));
		assertEquals(Status.Changed, p.getStatus());
		assertEquals(Status.Changed.toString(), property(p, "AllowDuplicates").getStatus());
		assertEquals("true", property(p, "AllowDuplicates").getModel());
		assertEquals(Status.Changed.toString(), property(p, "Scale").getStatus());
	}

	@Test
	public void addedAttributeHasNoBaselineValues() throws Exception {
		AttributeProperties p = new AttributeProperties(null, attribute(EAP_MISSING));
		assertEquals(Status.ModelOnly, p.getStatus());
		assertNull(property(p, "AllowDuplicates").getBaseline());
	}
}
