package de.ultimatetodo.tag.domain;

/** Normalisierung und Prüfung von Tag-Eingaben, die für Frontend, Backend und Datenbank gleich gelten. */
public final class TagInput {

	public static final int MAX_NAME = 50;

	private TagInput() {
	}

	public static String normalizeName(String name) {
		return name == null ? "" : name.strip();
	}

	public static boolean isValidName(String strippedName) {
		int length = strippedName.codePointCount(0, strippedName.length());
		return length >= 1 && length <= MAX_NAME;
	}

}
