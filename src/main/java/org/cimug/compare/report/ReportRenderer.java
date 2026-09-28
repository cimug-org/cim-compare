package org.cimug.compare.report;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;

/**
 * Stage 3 of cim-compare: comparison XML → enriched XML (written beside the
 * input, for debugging and for other tooling) → HTML report.
 *
 * The HTML layout lives in {@code report/report.xslt} (XSLT 3.0, run on
 * Saxon-HE). The stylesheet and script that make the report interactive live
 * in {@code report/report.css} and {@code report/report.js}; they are read
 * from the classpath here and passed to the stylesheet, which inlines them so
 * the generated report is a single self-contained file that needs no network
 * access.
 */
public class ReportRenderer {

	public static final String ENRICHED_SUFFIX = "-enriched.xml";

	private static final String XSLT = "report/report.xslt";
	private static final String CSS = "report/report.css";
	private static final String JS = "report/report.js";

	private final boolean full;
	private final String packageFilter;
	private final boolean includeDiagrams;
	private final String imageType;

	public ReportRenderer(boolean full, String packageFilter, boolean includeDiagrams, String imageType) {
		this.full = full;
		this.packageFilter = packageFilter;
		this.includeDiagrams = includeDiagrams;
		this.imageType = imageType == null ? "jpg" : imageType;
	}

	/**
	 * @param comparisonXml
	 *            the comparison XML produced by the diff (or an EA compare log)
	 * @param html
	 *            the report to write, or null for stdout
	 * @return the enriched XML file that was written
	 */
	public File render(File comparisonXml, File html) throws Exception {
		return render(parse(comparisonXml), comparisonXml, html);
	}

	/**
	 * Variant taking an already parsed comparison document.
	 *
	 * @param comparisonXml
	 *            the file the document came from (used to name the enriched XML)
	 */
	public File render(Document in, File comparisonXml, File html) throws Exception {
		Document enriched = new ReportPreProcessor(full, packageFilter).process(in);
		in = null; // let the (large) input go

		File enrichedFile = enrichedFileFor(comparisonXml, html);
		writeXml(enriched, enrichedFile);

		Transformer t = newTransformer();
		t.setParameter("css", readResource(CSS));
		t.setParameter("js", readResource(JS));
		t.setParameter("include-diagrams", Boolean.toString(includeDiagrams));
		t.setParameter("image-type", imageType);

		Writer w = null;
		try {
			w = html == null ? new OutputStreamWriter(System.out, StandardCharsets.UTF_8)
					: new BufferedWriter(new OutputStreamWriter(new FileOutputStream(html), StandardCharsets.UTF_8), 1 << 16);
			t.transform(new DOMSource(enriched), new StreamResult(w));
			w.flush();
		} finally {
			if (w != null && html != null)
				w.close();
		}
		return enrichedFile;
	}

	public static File enrichedFileFor(File comparisonXml, File html) {
		File dir = html != null && html.getParentFile() != null ? html.getParentFile() : comparisonXml.getAbsoluteFile().getParentFile();
		String base = comparisonXml.getName().replaceAll("(?i)\\.xml$", "");
		return new File(dir, base + ENRICHED_SUFFIX);
	}

	private static Document parse(File xml) throws Exception {
		DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
		f.setNamespaceAware(false);
		f.setExpandEntityReferences(false);
		return f.newDocumentBuilder().parse(xml);
	}

	private static void writeXml(Document doc, File file) throws TransformerException {
		Transformer t = TransformerFactory.newInstance().newTransformer();
		t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		t.setOutputProperty(OutputKeys.INDENT, "no");
		t.transform(new DOMSource(doc), new StreamResult(file));
	}

	/**
	 * Saxon-HE is required for XSLT 3.0. It is addressed through the standard
	 * JAXP API by class name so that this code compiles without Saxon on the
	 * compile classpath.
	 */
	private static Transformer newTransformer() throws TransformerConfigurationException, IOException {
		TransformerFactory f;
		try {
			f = TransformerFactory.newInstance("net.sf.saxon.TransformerFactoryImpl", ReportRenderer.class.getClassLoader());
		} catch (Exception e) {
			throw new TransformerConfigurationException(
					"Saxon-HE is required to generate the report (net.sf.saxon.TransformerFactoryImpl not found on the classpath)", e);
		}
		InputStream xslt = ReportRenderer.class.getClassLoader().getResourceAsStream(XSLT);
		if (xslt == null)
			throw new IOException("Report stylesheet not found on classpath: " + XSLT);
		return f.newTransformer(new StreamSource(xslt));
	}

	private static String readResource(String name) throws IOException {
		InputStream in = ReportRenderer.class.getClassLoader().getResourceAsStream(name);
		if (in == null)
			throw new IOException("Report resource not found on classpath: " + name);
		try {
			java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
			byte[] buf = new byte[8192];
			int n;
			while ((n = in.read(buf)) > 0)
				bos.write(buf, 0, n);
			return new String(bos.toByteArray(), StandardCharsets.UTF_8);
		} finally {
			in.close();
		}
	}
}
