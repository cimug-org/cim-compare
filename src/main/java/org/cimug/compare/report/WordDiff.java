package org.cimug.compare.report;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Word-level text comparison used to produce "redline" markup for changed text
 * properties: descriptions (Notes, RoleNote). Names are not redlined.
 *
 * The result is a sequence of DOM nodes: plain text for unchanged runs,
 * {@code <del>} elements for text only in the baseline and {@code <ins>}
 * elements for text only in the destination. The XSLT copies these nodes
 * straight into the HTML.
 *
 * Two heuristics keep the output readable:
 * <ul>
 * <li>When the two texts are mostly different (token similarity below
 * {@link #REWRITE_THRESHOLD}) the whole baseline is struck and the whole
 * destination inserted, rather than an interleaved word diff.</li>
 * <li>Very long inputs fall back to the same whole-replacement form instead of
 * building a large LCS table.</li>
 * </ul>
 */
public final class WordDiff {

	/** Below this similarity ratio (0..1) a change is rendered as a rewrite. */
	public static final double REWRITE_THRESHOLD = 0.5;

	/** Guard on the LCS table size (tokens of a * tokens of b). */
	private static final long MAX_TABLE_CELLS = 4_000_000L;

	private WordDiff() {
	}

	/**
	 * Appends redline nodes for baseline → destination to {@code parent}.
	 *
	 * @return the similarity ratio that was computed (1.0 when identical).
	 */
	public static double appendRedline(Document doc, Element parent, String baseline, String destination) {
		String a = baseline == null ? "" : baseline;
		String b = destination == null ? "" : destination;
		if (a.equals(b)) {
			parent.appendChild(doc.createTextNode(a));
			return 1.0;
		}
		if (a.isEmpty()) {
			parent.appendChild(ins(doc, b));
			return 0.0;
		}
		if (b.isEmpty()) {
			parent.appendChild(del(doc, a));
			return 0.0;
		}
		List<String> ta = tokenize(a);
		List<String> tb = tokenize(b);
		int n = ta.size(), m = tb.size();
		if ((long) n * (long) m > MAX_TABLE_CELLS) {
			parent.appendChild(del(doc, a));
			parent.appendChild(doc.createTextNode(" "));
			parent.appendChild(ins(doc, b));
			return 0.0;
		}
		// LCS length table
		int[][] lcs = new int[n + 1][m + 1];
		for (int i = n - 1; i >= 0; i--) {
			String x = ta.get(i);
			for (int j = m - 1; j >= 0; j--) {
				if (x.equals(tb.get(j)))
					lcs[i][j] = lcs[i + 1][j + 1] + 1;
				else
					lcs[i][j] = Math.max(lcs[i + 1][j], lcs[i][j + 1]);
			}
		}
		int wordsA = countWords(ta), wordsB = countWords(tb);
		int matchedWords = 0;
		{ // count matched non-whitespace tokens along one LCS path
			int i = 0, j = 0;
			while (i < n && j < m) {
				if (ta.get(i).equals(tb.get(j))) {
					if (!ta.get(i).trim().isEmpty())
						matchedWords++;
					i++;
					j++;
				} else if (lcs[i + 1][j] >= lcs[i][j + 1]) {
					i++;
				} else {
					j++;
				}
			}
		}
		double ratio = (wordsA + wordsB) == 0 ? 1.0 : (2.0 * matchedWords) / (wordsA + wordsB);
		if (ratio < REWRITE_THRESHOLD && Math.min(wordsA, wordsB) > 3) {
			parent.appendChild(del(doc, a));
			parent.appendChild(doc.createTextNode(" "));
			parent.appendChild(ins(doc, b));
			return ratio;
		}
		// Walk the table again, emitting runs.
		StringBuilder eq = new StringBuilder(), delRun = new StringBuilder(), insRun = new StringBuilder();
		int i = 0, j = 0;
		while (i < n || j < m) {
			if (i < n && j < m && ta.get(i).equals(tb.get(j))) {
				flushChange(doc, parent, delRun, insRun);
				eq.append(ta.get(i));
				i++;
				j++;
			} else if (j < m && (i >= n || lcs[i][j + 1] >= lcs[i + 1][j])) {
				flushEqual(doc, parent, eq);
				insRun.append(tb.get(j));
				j++;
			} else {
				flushEqual(doc, parent, eq);
				delRun.append(ta.get(i));
				i++;
			}
		}
		flushEqual(doc, parent, eq);
		flushChange(doc, parent, delRun, insRun);
		return ratio;
	}

	private static void flushEqual(Document doc, Element parent, StringBuilder eq) {
		if (eq.length() > 0) {
			parent.appendChild(doc.createTextNode(eq.toString()));
			eq.setLength(0);
		}
	}

	private static void flushChange(Document doc, Element parent, StringBuilder delRun, StringBuilder insRun) {
		if (delRun.length() > 0) {
			parent.appendChild(del(doc, delRun.toString()));
			delRun.setLength(0);
		}
		if (insRun.length() > 0) {
			parent.appendChild(ins(doc, insRun.toString()));
			insRun.setLength(0);
		}
	}

	private static Element del(Document doc, String s) {
		Element e = doc.createElement("del");
		e.setTextContent(s);
		return e;
	}

	private static Element ins(Document doc, String s) {
		Element e = doc.createElement("ins");
		e.setTextContent(s);
		return e;
	}

	private static int countWords(List<String> toks) {
		int c = 0;
		for (String t : toks)
			if (!t.trim().isEmpty())
				c++;
		return c;
	}

	/**
	 * Splits text into runs of whitespace, runs of word characters, and single
	 * punctuation characters, so that the concatenation of the tokens is the
	 * original text.
	 */
	static List<String> tokenize(String s) {
		List<String> out = new ArrayList<String>();
		int i = 0, len = s.length();
		while (i < len) {
			char c = s.charAt(i);
			int start = i;
			if (Character.isWhitespace(c)) {
				while (i < len && Character.isWhitespace(s.charAt(i)))
					i++;
			} else if (Character.isLetterOrDigit(c) || c == '_') {
				while (i < len && (Character.isLetterOrDigit(s.charAt(i)) || s.charAt(i) == '_'))
					i++;
			} else {
				i++;
			}
			out.add(s.substring(start, i));
		}
		return out;
	}
}
