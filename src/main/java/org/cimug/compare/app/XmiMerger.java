package org.cimug.compare.app;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 * Merges several EA "UML 1.3 (XMI 1.1)" exports, one per root node of an EA
 * project, into a single XMI file that the comparison reads like any other
 * export (issue #88).
 *
 * <p>
 * EA exports a root node as {@code XMI.content › UML:Model name="EA Model"
 * xmi.id="MX_EAID_<root>" › UML:Namespace.ownedElement › (EARootClass, the root
 * UML:Package, model-level DataTypes ...)}, followed by the diagrams and any
 * stubs at the {@code XMI.content} level and the {@code XMI.extensions}. The
 * merged file keeps that shape: one {@code EA Model} whose owned elements are
 * those of every input in order, then every input's diagrams, then every input's
 * extensions. The {@code EA Model}'s name and xmi.id, the header and
 * {@code XMI.difference} come from the first input, so a merged project whose
 * first root is the same package as a single-root project's root gets the same
 * model id.
 * </p>
 *
 * <ul>
 * <li>{@code EARootClass} is kept once (from the first input).</li>
 * <li>EA numbers anonymous ids {@code eaxmiid0, eaxmiid1, ...} per export file
 * (model-level DataTypes and references to them). From the second input on they
 * are renamed {@code eaxmiid<k>_<n>} so they stay unique.</li>
 * <li>An {@code EAStub} (EA's placeholder for an element outside the exported
 * package) is dropped when another input defines that element; other stubs are
 * kept once.</li>
 * <li>An xmi.id defined by more than one input is reported, since the
 * comparison expects each element once.</li>
 * </ul>
 *
 * The inputs are streamed, so memory does not grow with model size. The output
 * is UTF-8; elements are re-serialised with newlines and tabs in attribute values
 * kept as character references.
 */
public final class XmiMerger {

	private static final String UML_NS = "omg.org/UML1.3";
	private static final Pattern EAXMIID = Pattern.compile("eaxmiid\\d+");
	private static final String EA_ROOT_CLASS = "EARootClass";
	private static final String EA_STUB = "EAStub";
	private static final String XMI_ID = "xmi.id";

	/** What a merge found; see {@link #merge(List, File)}. */
	public static final class Result {
		/** Ids defined by more than one input (first few, with the input names). */
		public final Map<String, List<String>> duplicateIds;
		/** Number of ids defined by more than one input. */
		public final int duplicateCount;
		/** Stubs dropped because another input defines the element. */
		public final int stubsResolved;

		Result(Map<String, List<String>> duplicateIds, int duplicateCount, int stubsResolved) {
			this.duplicateIds = duplicateIds;
			this.duplicateCount = duplicateCount;
			this.stubsResolved = stubsResolved;
		}
	}

	private XmiMerger() {
	}

	/**
	 * Merges {@code inputs} (at least one) into {@code output}. The inputs are
	 * left in place.
	 */
	public static Result merge(List<File> inputs, File output) throws IOException, XMLStreamException {
		if (inputs == null || inputs.isEmpty())
			throw new IllegalArgumentException("No XMI files to merge");

		File workDir = output.getAbsoluteFile().getParentFile();
		List<File> temps = new ArrayList<File>();
		try {
			List<Part> parts = new ArrayList<Part>();
			for (int k = 0; k < inputs.size(); k++) {
				Part part = new Part(inputs.get(k), k, workDir);
				temps.add(part.owned);
				temps.add(part.content);
				temps.add(part.extensions);
				part.read();
				parts.add(part);
			}

			// Which input defines each id (stubs excluded).
			Map<String, List<String>> definedBy = new HashMap<String, List<String>>();
			for (Part p : parts)
				for (String id : p.definedIds) {
					List<String> l = definedBy.get(id);
					if (l == null)
						definedBy.put(id, l = new ArrayList<String>(1));
					l.add(p.source.getName());
				}
			Map<String, List<String>> duplicates = new LinkedHashMap<String, List<String>>();
			int duplicateCount = 0;
			for (Map.Entry<String, List<String>> e : definedBy.entrySet())
				if (e.getValue().size() > 1) {
					duplicateCount++;
					if (duplicates.size() < 10)
						duplicates.put(e.getKey(), e.getValue());
				}

			int stubsResolved = 0;
			Set<String> stubsWritten = new HashSet<String>();
			Part first = parts.get(0);

			try (OutputStream os = new BufferedOutputStream(new FileOutputStream(output))) {
				Writer w = new OutputStreamWriter(os, StandardCharsets.UTF_8);
				w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
				w.write(first.xmiStartTag);
				w.write("\n\t");
				w.write(first.headerXml);
				w.write("\n\t<XMI.content>\n\t\t<UML:Model");
				writeAttributes(w, first.modelAttributes);
				w.write(">\n\t\t\t<UML:Namespace.ownedElement>");
				w.flush();
				for (Part p : parts)
					append(p.owned, os);
				w.write("\n\t\t\t</UML:Namespace.ownedElement>\n\t\t</UML:Model>");
				w.flush();
				for (Part p : parts)
					append(p.content, os);
				for (Part p : parts)
					for (Stub s : p.stubs) {
						if (definedBy.containsKey(s.id)) {
							stubsResolved++;
						} else if (stubsWritten.add(s.id)) {
							w.write("\n\t\t");
							w.write(s.xml);
						}
					}
				w.write("\n\t</XMI.content>");
				if (first.differenceXml != null) {
					w.write("\n\t");
					w.write(first.differenceXml);
				}
				w.write("\n\t<XMI.extensions");
				writeAttributes(w, first.extensionsAttributes);
				w.write(">");
				w.flush();
				for (Part p : parts)
					append(p.extensions, os);
				w.write("\n\t</XMI.extensions>\n</XMI>\n");
				w.flush();
			}
			return new Result(duplicates, duplicateCount, stubsResolved);
		} finally {
			for (File t : temps)
				if (t != null)
					t.delete();
		}
	}

	private static void writeAttributes(Writer w, Map<String, String> attributes) throws IOException {
		for (Map.Entry<String, String> a : attributes.entrySet())
			w.write(" " + a.getKey() + "=\"" + escape(a.getValue()) + "\"");
	}

	static String escape(String s) {
		StringBuilder b = new StringBuilder(s.length());
		for (char c : s.toCharArray()) {
			switch (c) {
			case '&': b.append("&amp;"); break;
			case '<': b.append("&lt;"); break;
			case '>': b.append("&gt;"); break;
			case '"': b.append("&quot;"); break;
			// Kept as character references: written literally, a parser would
			// normalise them to spaces (EA notes end with "&#xA;").
			case '\n': b.append("&#xA;"); break;
			case '\r': b.append("&#xD;"); break;
			case '\t': b.append("&#x9;"); break;
			default: b.append(c);
			}
		}
		return b.toString();
	}

	private static void append(File fragment, OutputStream os) throws IOException {
		Files.copy(fragment.toPath(), os);
	}

	private static String qname(QName n) {
		return n.getPrefix() == null || n.getPrefix().isEmpty() ? n.getLocalPart()
				: n.getPrefix() + ":" + n.getLocalPart();
	}

	private static final class Stub {
		final String id;
		final String xml;

		Stub(String id, String xml) {
			this.id = id;
			this.xml = xml;
		}
	}

	/** One input, read once and split into fragment files. */
	private static final class Part {
		final File source;
		final int index;
		final File owned, content, extensions;
		final Set<String> definedIds = new HashSet<String>();
		final List<Stub> stubs = new ArrayList<Stub>();
		String xmiStartTag = "<XMI xmi.version=\"1.1\" xmlns:UML=\"" + UML_NS + "\">";
		String headerXml = "";
		String differenceXml;
		final Map<String, String> modelAttributes = new LinkedHashMap<String, String>();
		final Map<String, String> extensionsAttributes = new LinkedHashMap<String, String>();

		private final XMLEventFactory events = XMLEventFactory.newInstance();

		Part(File source, int index, File workDir) throws IOException {
			this.source = source;
			this.index = index;
			this.owned = File.createTempFile("xmi-merge-owned-", ".part", workDir);
			this.content = File.createTempFile("xmi-merge-content-", ".part", workDir);
			this.extensions = File.createTempFile("xmi-merge-ext-", ".part", workDir);
		}

		void read() throws IOException, XMLStreamException {
			XMLInputFactory inf = XMLInputFactory.newInstance();
			inf.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
			inf.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);

			try (InputStream in = new BufferedInputStream(new FileInputStream(source));
					OutputStream ownedOut = new BufferedOutputStream(new FileOutputStream(owned));
					OutputStream contentOut = new BufferedOutputStream(new FileOutputStream(content));
					OutputStream extOut = new BufferedOutputStream(new FileOutputStream(extensions))) {

				XMLEventReader r = inf.createXMLEventReader(in);
				Writer ownedW = new OutputStreamWriter(ownedOut, StandardCharsets.UTF_8);
				Writer contentW = new OutputStreamWriter(contentOut, StandardCharsets.UTF_8);
				Writer extW = new OutputStreamWriter(extOut, StandardCharsets.UTF_8);

				// Element path: 1 XMI, 2 XMI.content / XMI.header / ..., 3 UML:Model / diagrams,
				// 4 UML:Namespace.ownedElement, 5 owned elements.
				List<String> path = new ArrayList<String>();
				Writer target = null; // writer for the subtree being copied
				int targetDepth = 0; // depth of the subtree root being copied
				boolean skipping = false; // inside an EARootClass from a later input
				int skipDepth = 0;
				boolean inStub = false;
				String stubId = null;
				StringBuilder captured = null; // header / difference / stub text
				int captureDepth = 0;
				String captureKind = null;

				while (r.hasNext()) {
					XMLEvent e = r.nextEvent();

					if (e.isStartElement()) {
						StartElement se = rename(e.asStartElement());
						String name = se.getName().getLocalPart();
						path.add(name);
						int depth = path.size();

						String id = attr(se, XMI_ID);
						if (id != null && !inStub && !EA_STUB.equals(name) && !skipping
								&& !(depth == 5 && EA_ROOT_CLASS.equals(attr(se, "name"))))
							definedIds.add(id);

						if (skipping) {
							continue;
						}
						if (captured != null) {
							appendStart(captured, se);
							continue;
						}
						if (target != null) {
							target.write(startTag(se));
							continue;
						}

						if (depth == 1) {
							xmiStartTag = startTag(se);
						} else if (depth == 2 && ("XMI.header".equals(name) || "XMI.difference".equals(name))) {
							captured = new StringBuilder();
							captureDepth = depth;
							captureKind = name;
							appendStart(captured, se);
						} else if (depth == 2 && "XMI.extensions".equals(name)) {
							copyAttributes(se, extensionsAttributes);
						} else if (depth == 3 && "XMI.content".equals(path.get(1)) && "Model".equals(name)) {
							copyAttributes(se, modelAttributes);
						} else if (depth == 3 && "XMI.content".equals(path.get(1)) && EA_STUB.equals(name)) {
							inStub = true;
							stubId = attr(se, XMI_ID);
							captured = new StringBuilder();
							captureDepth = depth;
							captureKind = EA_STUB;
							appendStart(captured, se);
						} else if (depth == 3 && "XMI.content".equals(path.get(1))) {
							target = contentW;
							targetDepth = depth;
							contentW.write("\n\t\t");
							contentW.write(startTag(se));
						} else if (depth == 3 && "XMI.extensions".equals(path.get(1))) {
							target = extW;
							targetDepth = depth;
							extW.write("\n\t\t");
							extW.write(startTag(se));
						} else if (depth == 5 && "Namespace.ownedElement".equals(path.get(3))
								&& "Model".equals(path.get(2))) {
							if (index > 0 && EA_ROOT_CLASS.equals(attr(se, "name"))) {
								skipping = true;
								skipDepth = depth;
							} else {
								target = ownedW;
								targetDepth = depth;
								ownedW.write("\n\t\t\t\t");
								ownedW.write(startTag(se));
							}
						}
					} else if (e.isEndElement()) {
						int depth = path.size();
						path.remove(path.size() - 1);
						if (skipping) {
							if (depth == skipDepth)
								skipping = false;
							continue;
						}
						if (captured != null) {
							captured.append("</").append(qname(e.asEndElement().getName())).append('>');
							if (depth == captureDepth) {
								if ("XMI.header".equals(captureKind))
									headerXml = captured.toString();
								else if ("XMI.difference".equals(captureKind))
									differenceXml = captured.toString();
								else {
									stubs.add(new Stub(stubId, captured.toString()));
									inStub = false;
								}
								captured = null;
							}
							continue;
						}
						if (target != null) {
							target.write("</" + qname(e.asEndElement().getName()) + ">");
							if (depth == targetDepth)
								target = null;
						}
					} else if (e.isCharacters()) {
						if (skipping)
							continue;
						if (captured != null)
							captured.append(escapeText(e.asCharacters().getData()));
						else if (target != null)
							target.write(escapeText(e.asCharacters().getData()));
					} else if (target != null && !skipping && captured == null
							&& e.getEventType() == XMLEvent.COMMENT) {
						target.write("<!--" + ((javax.xml.stream.events.Comment) e).getText() + "-->");
					}
				}
				r.close();
				ownedW.flush();
				contentW.flush();
				extW.flush();
			}
		}

		/** Renames anonymous eaxmiid ids from the second input on. */
		private StartElement rename(StartElement se) {
			if (index == 0)
				return se;
			boolean changed = false;
			List<Attribute> attrs = new ArrayList<Attribute>();
			for (Iterator<?> it = se.getAttributes(); it.hasNext();) {
				Attribute a = (Attribute) it.next();
				String v = a.getValue();
				if (EAXMIID.matcher(v).matches()) {
					attrs.add(events.createAttribute(a.getName(), "eaxmiid" + index + "_" + v.substring(7)));
					changed = true;
				} else {
					attrs.add(a);
				}
			}
			if (!changed)
				return se;
			QName n = se.getName();
			return events.createStartElement(n.getPrefix(), n.getNamespaceURI(), n.getLocalPart(), attrs.iterator(),
					se.getNamespaces());
		}

		private static String attr(StartElement se, String name) {
			Attribute a = se.getAttributeByName(new QName(name));
			return a == null ? null : a.getValue();
		}

		private static void copyAttributes(StartElement se, Map<String, String> into) {
			into.clear();
			for (Iterator<?> it = se.getAttributes(); it.hasNext();) {
				Attribute a = (Attribute) it.next();
				into.put(qname(a.getName()), a.getValue());
			}
		}

		private static String startTag(StartElement se) {
			StringBuilder b = new StringBuilder();
			appendStart(b, se);
			return b.toString();
		}

		private static void appendStart(StringBuilder b, StartElement se) {
			b.append('<').append(qname(se.getName()));
			for (Iterator<?> it = se.getNamespaces(); it.hasNext();) {
				javax.xml.stream.events.Namespace ns = (javax.xml.stream.events.Namespace) it.next();
				String p = ns.getPrefix();
				b.append(p == null || p.isEmpty() ? " xmlns" : " xmlns:" + p).append("=\"")
						.append(escape(ns.getNamespaceURI())).append('"');
			}
			for (Iterator<?> it = se.getAttributes(); it.hasNext();) {
				Attribute a = (Attribute) it.next();
				b.append(' ').append(qname(a.getName())).append("=\"").append(escape(a.getValue())).append('"');
			}
			b.append('>');
		}

		private static String escapeText(String s) {
			return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\r", "&#xD;");
		}
	}
}
