package org.cimug.compare;

import java.io.InputStream;
import java.util.Properties;

/**
 * The cim-compare release, taken from the pom at build time.
 *
 * <p>
 * {@code cim-compare-version.properties} is the one resource Maven filters, so
 * its {@code version} is the pom's {@code project.version}. The report header,
 * its release link and the usage text all read it from here, so none of them
 * has to be edited by hand for a release (#87).
 * </p>
 */
public final class Version {

	/** The pom version, e.g. {@code 2.0.2}; {@code dev} when the build did not filter it. */
	public static final String VERSION = load();

	/** The GitHub release page for {@link #VERSION}. */
	public static final String RELEASE_URL = "https://github.com/cimug-org/cim-compare/releases/tag/" + VERSION;

	private static final String RESOURCE = "/cim-compare-version.properties";

	private Version() {
	}

	private static String load() {
		try (InputStream in = Version.class.getResourceAsStream(RESOURCE)) {
			if (in != null) {
				Properties p = new Properties();
				p.load(in);
				String v = p.getProperty("version", "").trim();
				if (!v.isEmpty() && !v.startsWith("${"))
					return v;
			}
		} catch (Exception e) {
			// fall through
		}
		return "dev";
	}
}
