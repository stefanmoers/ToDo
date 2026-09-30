package de.ultimatetodo.user.domain;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Locale;
import java.util.regex.Pattern;

/** Normalisierung und Prüfung von Benutzereingaben, die für Frontend, Backend und Datenbank gleich gelten. */
public final class UserInput {

	private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

	private UserInput() {
	}

	public static String normalizeEmail(String email) {
		return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
	}

	public static boolean isValidEmail(String normalizedEmail) {
		return normalizedEmail.length() <= 254 && EMAIL.matcher(normalizedEmail).matches();
	}

	public static String normalizeName(String name) {
		return name == null ? "" : name.strip();
	}

	public static boolean isValidName(String strippedName) {
		int length = strippedName.codePointCount(0, strippedName.length());
		return length >= 1 && length <= 100;
	}

	public static boolean isValidTimeZone(String zone) {
		if (zone == null || zone.isBlank() || !zone.contains("/") && !zone.equals("UTC")) {
			return false;
		}
		try {
			ZoneId.of(zone);
			return true;
		}
		catch (DateTimeException e) {
			return false;
		}
	}

}
