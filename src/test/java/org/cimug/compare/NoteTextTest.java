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
 * Notes and role notes keep their line breaks; other tagged values do not
 * (issue #200).
 */
public class NoteTextTest {

	@Test
	public void notesKeepLineBreaks() {
		assertEquals("Governor mode (Z).\n1 = droop\n0 = isochronous.",
				NoteText.value("Governor mode (Z).\n1 = droop\n0 = isochronous.", "description"));
		assertEquals("a\nb", NoteText.value("a\nb", "documentation"));
	}

	@Test
	public void lineEndingsAndTrailingBreaksDoNotMatter() {
		assertEquals("a\nb", NoteText.value("a\r\nb", "documentation"));
		assertEquals("a\nb", NoteText.value("a\rb", "documentation"));
		assertEquals("a\nb", NoteText.value("a\nb\n", "documentation"));
		assertEquals("a\nb", NoteText.value("a\nb \r\n", "description"));
	}

	@Test
	public void otherTaggedValuesStillLoseLineBreaks() {
		assertEquals("ab", NoteText.value("a\nb", "type"));
		assertEquals("ab", NoteText.value("a\nb", "stereotype"));
		assertNull(NoteText.value(null, "documentation"));
	}

	/** An attribute as EA exports it, with notes {@code notes} (XML-escaped). */
	private static AttributeType attribute(String notes) throws Exception {
		String xml = "<UML:Attribute xmlns:UML=\"omg.org/UML1.3\" name=\"z\" changeable=\"none\" visibility=\"public\""
				+ " ownerScope=\"instance\" targetScope=\"instance\">"
				+ "<UML:StructuralFeature.type><UML:Classifier xmi.idref=\"eaxmiid1\"/></UML:StructuralFeature.type>"
				+ "<UML:ModelElement.taggedValue>"
				+ "<UML:TaggedValue tag=\"type\" value=\"Integer\"/>"
				+ "<UML:TaggedValue tag=\"lowerBound\" value=\"0\"/>"
				+ "<UML:TaggedValue tag=\"upperBound\" value=\"1\"/>"
				+ "<UML:TaggedValue tag=\"description\" value=\"" + notes + "\"/>"
				+ "<UML:TaggedValue tag=\"duplicates\" value=\"0\"/>"
				+ "<UML:TaggedValue tag=\"scale\" value=\"0\"/>"
				+ "</UML:ModelElement.taggedValue></UML:Attribute>";
		return JAXBContext.newInstance(AttributeType.class).createUnmarshaller()
				.unmarshal(new StreamSource(new StringReader(xml)), AttributeType.class).getValue();
	}

	private static Property notes(AttributeProperties p) {
		for (Property x : p.getProperties().getProperty())
			if ("Notes".equals(x.getName()))
				return x;
		throw new AssertionError("no Notes property");
	}

	@Test
	public void attributeNotesKeepTheirLineBreaks() throws Exception {
		String z = "Governor mode (Z).&#xA;1 = droop&#xA;0 = isochronous.";
		AttributeProperties p = new AttributeProperties(attribute(z), attribute(z));
		assertEquals(Status.Identical, p.getStatus());
		assertEquals("Governor mode (Z).\n1 = droop\n0 = isochronous.", notes(p).getModel());
	}

	@Test
	public void aDifferentLineEndingIsNotAChange() throws Exception {
		AttributeProperties p = new AttributeProperties(attribute("a&#xD;&#xA;b&#xA;"), attribute("a&#xA;b"));
		assertEquals(Status.Identical, p.getStatus());
	}

	@Test
	public void aChangedLineBreakIsAChange() throws Exception {
		AttributeProperties p = new AttributeProperties(attribute("a b"), attribute("a&#xA;b"));
		assertEquals(Status.Changed, p.getStatus());
	}
}
