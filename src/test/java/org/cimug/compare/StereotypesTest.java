package org.cimug.compare;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;

import org.cimug.compare.uml1_3.TaggedValueType;
import org.junit.Test;

public class StereotypesTest {

	private static TaggedValueType tv(String tag, String value) {
		TaggedValueType t = new TaggedValueType();
		t.setTag(tag);
		t.setTheValue(value);
		return t;
	}

	private static final String XREF = "$XREFPROP=$XID={A}$XID;$NAM=CustomProperties$NAM;$TYP=element property$TYP;$VIS=Public$VIS;$PAR=0$PAR;$DES=@PROP=@NAME=isID@ENDNAME;@ENDPROP;$DES;$ENDXREF;"
			+ "$XREFPROP=$XID={B}$XID;$NAM=Stereotypes$NAM;$TYP=element property$TYP;$VIS=Public$VIS;$PAR=0$PAR;"
			+ "$DES=@STEREO;Name=ShadowExtension;FQName=X::ShadowExtension;@ENDSTEREO;@STEREO;Name=util;GUID={C};@ENDSTEREO;$DES;$CLT={D}$CLT;$SUP=&lt;none&gt;$SUP;$ENDXREF;";

	@Test
	public void readsTheWholeListInOrder() {
		assertEquals("ShadowExtension, util",
				Stereotypes.of(Arrays.asList(tv("stereotype", "ShadowExtension"), tv("$ea_xref_property", XREF))));
	}

	@Test
	public void fallsBackToTheSingleStereotype() {
		assertEquals("enum", Stereotypes.of(Arrays.asList(tv("stereotype", "enum"))));
		assertEquals("enum", Stereotypes.of(Arrays.asList(tv("$ea_xref_property", "$XREFPROP=$NAM=CustomProperties$NAM;$ENDXREF;")), "enum"));
		assertNull(Stereotypes.of(null));
	}

	@Test
	public void splitsAList() {
		assertEquals(Arrays.asList("ShadowExtension", "util"), Stereotypes.split("ShadowExtension, util"));
	}
}
