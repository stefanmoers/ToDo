package de.ultimatetodo.user.domain;

/**
 * Passwortregeln (docs/SECURITY.md): 15–128 Unicode-Codepoints, keine Zeichenklassen-Pflicht,
 * kein Trimmen, keine Normalisierung.
 */
public final class PasswordPolicy {

	public static final int MIN_LENGTH = 15;
	public static final int MAX_LENGTH = 128;

	private PasswordPolicy() {
	}

	public static boolean isValid(String password) {
		if (password == null) {
			return false;
		}
		int length = password.codePointCount(0, password.length());
		return length >= MIN_LENGTH && length <= MAX_LENGTH;
	}

}
