package org.cimug.compare;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

/** Splitting a --package value into names (issue #94). */
public class PackageNamesTest {

	@Test
	public void oneName() {
		assertEquals(Arrays.asList("Grid"), PackageNames.parse("Grid"));
		assertFalse(PackageNames.isList("Grid"));
	}

	@Test
	public void commaListKeepsOrderAndSpacesInsideNames() {
		assertEquals(Arrays.asList("Grid", "European Standards", "UtilityExtensions"),
				PackageNames.parse(" Grid, European Standards ,UtilityExtensions"));
		assertTrue(PackageNames.isList("Model,UtilityExtensions"));
	}

	@Test
	public void emptyEntriesAndRepeatsAreDropped() {
		assertEquals(Arrays.asList("Model", "UtilityExtensions"), PackageNames.parse("Model,,UtilityExtensions, Model,"));
		assertEquals(Collections.emptyList(), PackageNames.parse(" , "));
		assertEquals(Collections.emptyList(), PackageNames.parse(null));
	}

	@Test
	public void joinForMessages() {
		assertEquals("Model, UtilityExtensions", PackageNames.join(Arrays.asList("Model", "UtilityExtensions")));
	}
}
