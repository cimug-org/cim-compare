package org.cimug.compare;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.cimug.compare.logs.CompareItem;
import org.cimug.compare.logs.Properties;
import org.cimug.compare.logs.Property;
import org.junit.Test;

/** #145: the changes a diagram shows, on the comparison's own items. */
public class DrawnChangesTest {

	private static CompareItem item(String type, String name, String guid, String status, Property... props) {
		Properties p = new Properties();
		for (Property x : props)
			p.getProperty().add(x);
		return new CompareItem(p, name, type, guid, status);
	}

	private static Property same(String name, String value) {
		return new Property(name, value, value, "Identical");
	}

	private static Property changed(String name, String baseline, String model) {
		return new Property(name, baseline, model, "Changed");
	}

	private static final String G = "{00000000-0000-0000-0000-0000000000A1}";

	@Test
	public void aClassShowsANameOrAnAttributeChangeButNotADescriptionChange() {
		CompareItem renamed = item("Class", "Breaker", G, "Changed", changed("Name", "Breaker", "Breaker2"));
		assertTrue(DrawnChanges.classShows(renamed));

		CompareItem described = item("Class", "Breaker", G, "Changed", changed("Notes", "old", "new"), same("Name", "Breaker"));
		assertFalse(DrawnChanges.classShows(described));

		CompareItem attr = item("Class", "Breaker", G, "Identical", same("Name", "Breaker"));
		attr.getCompareItem().add(item("Attribute", "inTransitTime", "{x}", "Changed", changed("Type", "Seconds", "Float")));
		assertTrue(DrawnChanges.classShows(attr));

		CompareItem added = item("Class", "Breaker", G, "Model only");
		assertFalse("an added class adds itself to the diagram", DrawnChanges.classShows(added));
	}

	@Test
	public void classesAreFoundAnywhereInTheComparison() {
		CompareItem root = item("Package", "Grid", "{p}", "Identical");
		CompareItem pkg = item("Package", "Wires", "{q}", "Identical");
		root.getCompareItem().add(pkg);
		pkg.getCompareItem().add(item("Class", "Breaker", G, "Changed", changed("Stereotype", "", "deprecated")));
		assertEquals(java.util.Collections.singleton("EAID_00000000_0000_0000_0000_0000000000A1"), DrawnChanges.classes(root));
	}

	@Test
	public void aConnectorSaysWhatChanged() {
		CompareItem link = item("", "Association", G, "Changed");
		CompareItem src = item("Src", "Source: (Breaker)", "Src-" + G, "Changed", changed("Cardinality", "0..*", "1..*"),
				same("Role", "Breakers"));
		link.getCompareItem().add(src);
		assertEquals("multiplicity 0..* → 1..*", DrawnChanges.connectorShows(link));

		CompareItem two = item("", "Association", G, "Changed", changed("Stereotype", "", "informative"));
		two.getCompareItem().add(item("Dst", "Target: (Switch)", "Dst-" + G, "Changed", changed("Role", "", "Switch")));
		assertEquals("stereotype, role name", DrawnChanges.connectorShows(two));

		CompareItem notes = item("", "Association", G, "Changed", changed("Notes", "a", "b"));
		assertNull("notes are not drawn", DrawnChanges.connectorShows(notes));
		assertNull("an added association adds its connector",
				DrawnChanges.connectorShows(item("", "Association", G, "Model only", changed("Name", null, "x"))));

		CompareItem root = item("Package", "Grid", "{p}", "Identical");
		CompareItem links = item("Links", "Links", "{l}", "Changed");
		root.getCompareItem().add(links);
		links.getCompareItem().add(link);
		links.getCompareItem().add(item("", "Generalization", "{g}", "Changed", changed("Name", "a", "b")));
		assertEquals(java.util.Collections.singletonMap("EAID_00000000_0000_0000_0000_0000000000A1",
				"multiplicity 0..* → 1..*"), DrawnChanges.connectors(root));
	}

	@Test
	public void guidsBecomeEaids() {
		assertEquals("EAID_6CA575E7_020B_40d8_B841_027BF8B51BE9", DrawnChanges.eaid("{6CA575E7-020B-40d8-B841-027BF8B51BE9}"));
		assertEquals("EAID_X", DrawnChanges.eaid("EAID_X"));
	}
}
