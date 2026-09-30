package de.ultimatetodo.project.domain;

/** Normalisierung und Prüfung von Projekt-Eingaben, die für Frontend, Backend und Datenbank gleich gelten. */
public final class ProjectInput {

	public static final int MAX_NAME = 100;
	public static final int MAX_DESCRIPTION = 10_000;

	private ProjectInput() {
	}

	public static String normalizeName(String name) {
		return name == null ? "" : name.strip();
	}

	public static boolean isValidName(String strippedName) {
		int length = strippedName.codePointCount(0, strippedName.length());
		return length >= 1 && length <= MAX_NAME;
	}

	/** Leere/blanke Beschreibung wird zu {@code null}; sonst bleibt sie unverändert. */
	public static String normalizeDescription(String description) {
		return description == null || description.isBlank() ? null : description;
	}

	public static boolean isValidDescription(String normalizedDescription) {
		if (normalizedDescription == null) {
			return true;
		}
		int length = normalizedDescription.codePointCount(0, normalizedDescription.length());
		return length >= 1 && length <= MAX_DESCRIPTION;
	}

}
