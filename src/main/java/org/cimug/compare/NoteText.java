package org.cimug.compare;

/**
 * The text of a tagged value as it is compared and reported (#200).
 *
 * Notes and role notes ({@code documentation} and {@code description} tags)
 * keep their line breaks, as EA's Notes pane shows them: Windows and old Mac
 * line endings are made Unix ones and trailing white space is dropped, so that
 * a difference in line endings alone is not reported as a change. Every other
 * tagged value has its line breaks removed, as before.
 */
public final class NoteText {

	private NoteText() {
	}

	/** True for the tags that hold an element's notes or an association end's role note. */
	public static boolean isNoteTag(String tag) {
		return "documentation".equals(tag) || "description".equals(tag);
	}

	/** The value of tag {@code tag} as it is compared and reported. */
	public static String value(String raw, String tag) {
		if (raw == null)
			return null;
		if (isNoteTag(tag))
			return raw.replace("\r\n", "\n").replace('\r', '\n').replaceAll("\\s+$", "");
		return raw.replaceAll("\n", "");
	}
}
