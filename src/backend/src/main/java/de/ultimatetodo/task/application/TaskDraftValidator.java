package de.ultimatetodo.task.application;

import de.ultimatetodo.common.ApiError.FieldError;
import de.ultimatetodo.common.ApiException;
import de.ultimatetodo.task.domain.DueDate;
import de.ultimatetodo.task.domain.InvalidDueDateException;
import de.ultimatetodo.task.domain.TaskInput;
import de.ultimatetodo.task.domain.TaskPriority;
import de.ultimatetodo.task.domain.TaskStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Sammelt alle Feldfehler einer Aufgaben-Eingabe (US-030, US-036, US-037), statt beim ersten Fehler abzubrechen.
 * Beim Anlegen ({@code isCreate=true}) ist die Priorität optional (Standard NORMAL) und ein mitgesendeter Status
 * wird ignoriert (immer OPEN); beim Bearbeiten sind Status und Priorität Pflicht.
 */
@Component
public class TaskDraftValidator {

	public ValidTaskValues validate(TaskDraft draft, boolean isCreate) {
		List<FieldError> errors = new ArrayList<>();

		UUID projectId = null;
		if (draft.projectId() != null && !draft.projectId().isBlank()) {
			try {
				projectId = UUID.fromString(draft.projectId());
			}
			catch (IllegalArgumentException e) {
				errors.add(new FieldError("projectId", "INVALID", "Dieses Projekt existiert nicht oder gehört dir nicht."));
			}
		}

		String title = TaskInput.normalizeTitle(draft.title());
		if (!TaskInput.isValidTitle(title)) {
			errors.add(new FieldError("title", "INVALID", "Der Titel muss 1 bis 200 Zeichen lang sein."));
		}

		String description = TaskInput.normalizeDescription(draft.description());
		if (!TaskInput.isValidDescription(description)) {
			errors.add(
					new FieldError("description", "TOO_LONG", "Die Beschreibung darf höchstens 10.000 Zeichen lang sein."));
		}

		TaskPriority priority = TaskPriority.NORMAL;
		if (draft.priority() != null && !draft.priority().isBlank()) {
			Optional<TaskPriority> parsed = TaskPriority.parse(draft.priority());
			if (parsed.isEmpty()) {
				errors.add(
						new FieldError("priority", "INVALID", "Ungültige Priorität. Erlaubt sind LOW, NORMAL, HIGH, URGENT."));
			}
			else {
				priority = parsed.get();
			}
		}
		else if (!isCreate) {
			errors.add(new FieldError("priority", "REQUIRED", "Bitte gib eine Priorität an."));
		}

		TaskStatus status = TaskStatus.OPEN;
		if (!isCreate) {
			if (draft.status() == null || draft.status().isBlank()) {
				errors.add(new FieldError("status", "REQUIRED", "Bitte gib einen Status an."));
			}
			else {
				Optional<TaskStatus> parsed = TaskStatus.parse(draft.status());
				if (parsed.isEmpty()) {
					errors.add(
							new FieldError("status", "INVALID", "Ungültiger Status. Erlaubt sind OPEN, IN_PROGRESS, DONE."));
				}
				else {
					status = parsed.get();
				}
			}
		}

		DueDate due = validateDue(draft, errors);

		if (!errors.isEmpty()) {
			throw ApiException.validation(errors);
		}
		return new ValidTaskValues(projectId, title, description, status, priority, due);
	}

	private DueDate validateDue(TaskDraft draft, List<FieldError> errors) {
		boolean hasDateInput = draft.dueDate() != null && !draft.dueDate().isBlank();
		LocalDate date = null;
		if (hasDateInput) {
			Optional<LocalDate> parsed = TaskInput.parseDate(draft.dueDate());
			if (parsed.isEmpty()) {
				errors.add(new FieldError("dueDate", "INVALID", "Ungültiges Datum."));
				return null;
			}
			date = parsed.get();
		}

		boolean hasTimeInput = draft.dueTime() != null && !draft.dueTime().isBlank();
		LocalTime time = null;
		if (hasTimeInput) {
			Optional<LocalTime> parsed = TaskInput.parseTime(draft.dueTime());
			if (parsed.isEmpty()) {
				errors.add(new FieldError("dueTime", "INVALID", "Ungültige Uhrzeit. Format: HH:mm."));
				return null;
			}
			time = parsed.get();
		}

		ZoneId zone = null;
		if (hasDateInput) {
			if (draft.dueTimeZone() == null || draft.dueTimeZone().isBlank()) {
				errors.add(new FieldError("dueTimeZone", "REQUIRED", "Bitte gib eine Zeitzone für die Fälligkeit an."));
				return null;
			}
			Optional<ZoneId> parsed = TaskInput.parseZone(draft.dueTimeZone());
			if (parsed.isEmpty()) {
				errors.add(new FieldError("dueTimeZone", "INVALID", "Ungültige Zeitzone, zum Beispiel Europe/Berlin."));
				return null;
			}
			zone = parsed.get();
		}

		ZoneOffset offset = null;
		if (draft.dueOffset() != null && !draft.dueOffset().isBlank()) {
			Optional<ZoneOffset> parsed = TaskInput.parseOffset(draft.dueOffset());
			if (parsed.isEmpty()) {
				errors.add(new FieldError("dueOffset", "INVALID", "Ungültiger Zeitzonen-Offset, zum Beispiel +02:00."));
				return null;
			}
			offset = parsed.get();
		}

		try {
			return DueDate.resolve(date, time, zone, offset);
		}
		catch (InvalidDueDateException e) {
			errors.add(toFieldError(e));
			return null;
		}
	}

	private FieldError toFieldError(InvalidDueDateException e) {
		String offsets = e.validOffsets().stream().map(ZoneOffset::getId).reduce((a, b) -> a + " oder " + b).orElse("");
		return switch (e.problem()) {
			case TIME_REQUIRES_DATE ->
				new FieldError("dueTime", "REQUIRES_DATE", "Eine Uhrzeit ist nur zusammen mit einem Datum möglich.");
			case OFFSET_WITHOUT_TIME -> new FieldError("dueOffset", "WITHOUT_TIME",
					"Ein Zeitzonen-Offset ist nur zusammen mit einer Uhrzeit möglich.");
			case TIME_NONEXISTENT -> new FieldError("dueTime", "NONEXISTENT",
					"Diese Uhrzeit gibt es am gewählten Tag wegen der Zeitumstellung nicht.");
			case OFFSET_REQUIRED -> new FieldError("dueOffset", "REQUIRED",
					"Diese Uhrzeit kommt wegen der Zeitumstellung zweimal vor. Bitte wähle " + offsets + ".");
			case OFFSET_INVALID -> new FieldError("dueOffset", "INVALID",
					"Ungültiger Zeitzonen-Offset für diesen Zeitpunkt. Gültig ist " + offsets + ".");
		};
	}

}
