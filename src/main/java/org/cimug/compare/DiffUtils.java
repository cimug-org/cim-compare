/**
 * 
 */
package org.cimug.compare;

/**
 * @author tviegut
 *
 */
public final class DiffUtils {

	private static final String ROOT_MODEL_XMIID_PREFIX = "MX_";
	private static final String EAID_GUID_PREFIX = "EAID";
	private static final String EAPK_GUID_PREFIX = "EAPK";
	private static final int GUID_LENGTH = 36;

	private DiffUtils() {
	}

	public static String convertRootModelXmiIdToEAGUID(String xmiId) {
		if (xmiId != null && !"".equals(xmiId)) {
			return xmiId.replace(ROOT_MODEL_XMIID_PREFIX, "");
		}
		return xmiId;
	}

	public static String convertXmiIdToEAGUID(String xmiId) {
		String guid = "";
		if (xmiId != null && !"".equals(xmiId)) {
			if (xmiId.contains(EAID_GUID_PREFIX)) {
				int index = xmiId.indexOf(EAID_GUID_PREFIX) + EAID_GUID_PREFIX.length() + 1;
				guid = xmiId.substring(index, index + GUID_LENGTH);
			} else if (xmiId.contains(EAPK_GUID_PREFIX)) {
				int index = xmiId.indexOf(EAPK_GUID_PREFIX) + EAPK_GUID_PREFIX.length() + 1;
				guid = xmiId.substring(index, index + GUID_LENGTH);
			} else {
				guid = xmiId.substring(0, 36);
			}
		}
		return "{" + guid.replaceAll("_", "-") + "}";
	}

	public static void main(String[] args) {
		System.out.println(convertRootModelXmiIdToEAGUID("MX_EAID_6952FF0A_7BCE_4b37_88F7_399320BB07D0"));
	}
}
