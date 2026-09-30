package de.ultimatetodo.task.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Fälligkeit einer Aufgabe (US-037). Lokale Angaben (Datum, Uhrzeit, IANA-Zone, bei doppelter Uhrzeit der gewählte
 * Offset) sind maßgeblich; {@link #deadline()} ist der daraus abgeleitete Zeitpunkt für Überfälligkeit und Sortierung.
 * {@code time}/{@code offset} sind {@code null}, wenn nur ein Datum ohne Uhrzeit gesetzt ist.
 */
public record DueDate(LocalDate date, LocalTime time, ZoneId zone, ZoneOffset offset) {

	/**
	 * Löst eine Fälligkeitsangabe auf. {@code zone} muss gesetzt sein, sobald {@code date} gesetzt ist
	 * (wird von der Anwendungsschicht als Pflichtfeld geprüft, bevor diese Methode aufgerufen wird).
	 * Liefert {@code null}, wenn keine Fälligkeit angegeben ist.
	 */
	public static DueDate resolve(LocalDate date, LocalTime time, ZoneId zone, ZoneOffset requestedOffset) {
		if (date == null) {
			if (time != null) {
				throw new InvalidDueDateException(InvalidDueDateException.Problem.TIME_REQUIRES_DATE);
			}
			if (requestedOffset != null) {
				throw new InvalidDueDateException(InvalidDueDateException.Problem.OFFSET_WITHOUT_TIME);
			}
			return null;
		}
		if (time == null) {
			if (requestedOffset != null) {
				throw new InvalidDueDateException(InvalidDueDateException.Problem.OFFSET_WITHOUT_TIME);
			}
			return new DueDate(date, null, zone, null);
		}
		// Sommerzeitwechsel: eine lokale Uhrzeit kann gar nicht (Frühjahrslücke) oder zweimal (Herbstüberlappung) vorkommen.
		List<ZoneOffset> valid = zone.getRules().getValidOffsets(LocalDateTime.of(date, time));
		if (valid.isEmpty()) {
			throw new InvalidDueDateException(InvalidDueDateException.Problem.TIME_NONEXISTENT);
		}
		ZoneOffset resolvedOffset;
		if (valid.size() > 1) {
			if (requestedOffset == null) {
				throw new InvalidDueDateException(InvalidDueDateException.Problem.OFFSET_REQUIRED, valid);
			}
			if (!valid.contains(requestedOffset)) {
				throw new InvalidDueDateException(InvalidDueDateException.Problem.OFFSET_INVALID, valid);
			}
			resolvedOffset = requestedOffset;
		}
		else {
			resolvedOffset = valid.get(0);
			if (requestedOffset != null && !requestedOffset.equals(resolvedOffset)) {
				throw new InvalidDueDateException(InvalidDueDateException.Problem.OFFSET_INVALID, valid);
			}
		}
		return new DueDate(date, time, zone, resolvedOffset);
	}

	/** Zeitpunkt, ab dem die Aufgabe überfällig ist. Ohne Uhrzeit: Beginn des Folgetags in der gespeicherten Zone. */
	public Instant deadline() {
		if (time == null) {
			return date.plusDays(1).atStartOfDay(zone).toInstant();
		}
		return OffsetDateTime.of(date, time, offset).toInstant();
	}

	/** Nur mit Uhrzeit vorhanden: der exakte Zeitpunkt der Fälligkeit; {@code null} bei einem reinen Datum. */
	public Instant exactInstant() {
		return time == null ? null : deadline();
	}

}
