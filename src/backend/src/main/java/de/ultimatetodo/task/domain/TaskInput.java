package de.ultimatetodo.task.domain;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Optional;
import java.util.regex.Pattern;

/** Normalisierung und Prüfung von Aufgaben-Eingaben, die für Frontend, Backend und Datenbank gleich gelten. */
public final class TaskInput {

	public static final int MAX_TITLE = 200;
	public static final int MAX_DESCRIPTION = 10_000;

	private static final Pattern TIME = Pattern.compile("^([01]\\d|2[0-3]):[0-5]\\d$");
	private static final Pattern OFFSET = Pattern.compile("^[+-]\\d{2}:\\d{2}$");
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE
		.withResolverStyle(ResolverStyle.STRICT);

	private TaskInput() {
	}

	public static String normalizeTitle(String title) {
		return title == null ? "" : title.strip();
	}

	public static boolean isValidTitle(String strippedTitle) {
		int length = strippedTitle.codePointCount(0, strippedTitle.length());
		return length >= 1 && length <= MAX_TITLE;
	}

	/** Leere/blanke Beschreibung wird zu {@code null}; sonst bleibt sie unverändert (Formatierung bleibt erhalten). */
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

	public static Optional<LocalDate> parseDate(String raw) {
		if (raw == null || raw.isBlank()) {
			return Optional.empty();
		}
		try {
			return Optional.of(LocalDate.parse(raw, DATE_FORMAT));
		}
		catch (DateTimeParseException e) {
			return Optional.empty();
		}
	}

	public static Optional<LocalTime> parseTime(String raw) {
		if (raw == null || raw.isBlank()) {
			return Optional.empty();
		}
		if (!TIME.matcher(raw).matches()) {
			return Optional.empty();
		}
		try {
			return Optional.of(LocalTime.parse(raw));
		}
		catch (DateTimeParseException e) {
			return Optional.empty();
		}
	}

	public static Optional<ZoneId> parseZone(String raw) {
		if (raw == null || raw.isBlank() || !raw.contains("/") && !raw.equals("UTC")) {
			return Optional.empty();
		}
		try {
			return Optional.of(ZoneId.of(raw));
		}
		catch (DateTimeException e) {
			return Optional.empty();
		}
	}

	public static Optional<ZoneOffset> parseOffset(String raw) {
		if (raw == null || raw.isBlank() || !OFFSET.matcher(raw).matches()) {
			return Optional.empty();
		}
		try {
			return Optional.of(ZoneOffset.of(raw));
		}
		catch (DateTimeException e) {
			return Optional.empty();
		}
	}

}
