package org.cimug.compare;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.cimug.compare.logs.Property;
import org.cimug.compare.uml1_3.ModelElementTaggedValue;
import org.cimug.compare.uml1_3.PackageType;
import org.cimug.compare.uml1_3.TaggedValueType;
import org.junit.Test;

/** #147: a package's parent package, as the comparison records it. */
public class PackagePropertiesTest {

	private static PackageType pkg(String name, String parentId) {
		PackageType p = new PackageType();
		p.setName(name);
		p.setXmiId("EAPK_8FF85768_B48A_4be5_9113_0DD28B347634");
		List<TaggedValueType> tags = new ArrayList<TaggedValueType>();
		tags.add(new TaggedValueType(parentId, "parent", parentId, null, null));
		tags.add(new TaggedValueType("Proposed", "status", "Proposed", null, null));
		p.setModelElementTaggedValue(new ModelElementTaggedValue(tags));
		return p;
	}

	private static Property parent(PackageProperties p) {
		for (Property x : p.getProperties().getProperty())
			if ("ParentPackage".equals(x.getName()))
				return x;
		return null;
	}

	private static final String PART303 = "EAPK_097BFA4A_0000_0000_0000_000000000001";
	private static final String METADATA = "EAPK_500389E9_0000_0000_0000_000000000002";

	@Test
	public void aMovedPackageRecordsWhereItWas() {
		// GenericDataSet, CIM 18v13 -> 18v14: from Grid::Part303 to Grid::InfGrid::Metadata
		PackageProperties p = new PackageProperties(pkg("GenericDataSet", PART303), pkg("GenericDataSet", METADATA),
				"Part303", "Metadata");
		assertEquals(Status.Moved, p.getStatus());
		Property parent = parent(p);
		assertNotNull(parent);
		assertEquals("Part303", parent.getBaseline());
		assertEquals("Metadata", parent.getModel());
		assertEquals("Changed", parent.getStatus());
	}

	@Test
	public void aPackageWhoseParentWasOnlyRenamedIsUnchanged() {
		// the same parent package, renamed from InfPart303 to Metadata
		PackageProperties p = new PackageProperties(pkg("GenericDataSet", METADATA), pkg("GenericDataSet", METADATA),
				"InfPart303", "Metadata");
		assertEquals(Status.Identical, p.getStatus());
		assertEquals("Identical", parent(p).getStatus());
		assertEquals("InfPart303", parent(p).getBaseline());
		assertEquals("Metadata", parent(p).getModel());
	}

	@Test
	public void aRemovedPackageRecordsItsParent() {
		PackageProperties p = new PackageProperties(pkg("GenericDataSet", PART303), null, "Part303", null);
		assertEquals("Part303", parent(p).getBaseline());
		assertEquals("Baseline only", parent(p).getStatus());
	}
}
