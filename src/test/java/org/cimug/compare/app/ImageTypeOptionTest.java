package org.cimug.compare.app;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** The --image-type values (#139). */
public class ImageTypeOptionTest {

	@Test
	public void invalidImageTypeMessageListsTheValidValues() {
		// building this list used to run one past the end of the values and crash
		assertEquals("EMF, BMP, GIF, PNG, JPG, SVG", CIMModelComparisonGenerator.validImageTypes());
	}
}
