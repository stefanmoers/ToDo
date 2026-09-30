package de.ultimatetodo.task.api;

import de.ultimatetodo.tag.application.TagSummary;
import de.ultimatetodo.task.domain.DueDate;
import de.ultimatetodo.task.domain.Task;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * Ein-/Ausgabemodelle der Aufgaben-API. Datum/Uhrzeit/Zone werden als Strings übertragen (nicht als typisierte
 * Felder), damit ungültige Werte als Feldfehler statt als nicht zuordenbarer 400-Fehler ankommen (Muster wie
 * {@code de.ultimatetodo.user.api.Dtos.RegisterRequest}).
 */
public final class TaskDtos {

	private TaskDtos() {
	}

	public record CreateTaskRequest(
			@Schema(description = "Nur ein eigenes Projekt; ohne Angabe ist die Aufgabe persönlich") String projectId,
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 200,
					example = "Steuererklärung abgeben") String title,
			@Schema(maxLength = 10000) String description,
			@Schema(allowableValues = { "LOW", "NORMAL", "HIGH", "URGENT" }, defaultValue = "NORMAL") String priority,
			@Schema(format = "date", example = "2026-10-25") String dueDate,
			@Schema(pattern = "^([01]\\d|2[0-3]):[0-5]\\d$", description = "Nur zusammen mit dueDate") String dueTime,
			@Schema(example = "Europe/Berlin", description = "IANA-Zeitzone; Pflicht, wenn dueDate gesetzt ist") String dueTimeZone,
			@Schema(pattern = "^[+-]\\d{2}:\\d{2}$",
					description = "Nur nötig, wenn die Uhrzeit wegen Zeitumstellung doppelt vorkommt") String dueOffset) {
	}

	public record UpdateTaskRequest(
			@Schema(description = "Nur ein eigenes Projekt; null entfernt die Zuordnung") String projectId,
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 200) String title,
			@Schema(maxLength = 10000) String description,
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED,
					allowableValues = { "OPEN", "IN_PROGRESS", "DONE" }) String status,
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED,
					allowableValues = { "LOW", "NORMAL", "HIGH", "URGENT" }) String priority,
			@Schema(format = "date") String dueDate,
			@Schema(pattern = "^([01]\\d|2[0-3]):[0-5]\\d$") String dueTime,
			@Schema(example = "Europe/Berlin") String dueTimeZone,
			@Schema(pattern = "^[+-]\\d{2}:\\d{2}$") String dueOffset,
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") Long version) {
	}

	@Schema(description = "Version der Aufgabe, wie zuletzt gelesen; Grundlage der Konflikterkennung (US-120).")
	public record VersionRequest(@Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") Long version) {
	}

	@Schema(description = "Die vollständige gewünschte Tag-Zuordnung einer Aufgabe (US-043); ersetzt die bisherige.")
	public record UpdateTaskTagsRequest(List<String> tagIds) {
	}

	@Schema(description = "Eine Aufgabe. Felder ohne Wert (z. B. keine Beschreibung, keine Fälligkeit, kein Projekt) fehlen in der Antwort.")
	public record TaskResponse(UUID id, UUID projectId, String title, String description, String status,
			String priority, String dueDate, String dueTime, String dueTimeZone, String dueOffset,
			@Schema(description = "Nur bei Uhrzeit: exakter Fälligkeitszeitpunkt in UTC") Instant dueAt,
			@Schema(description = "true ab dem Fälligkeitszeitpunkt, solange die Aufgabe nicht erledigt ist") boolean overdue,
			UUID creatorId, Instant createdAt, Instant updatedAt, Instant completedAt, long version,
			List<TagSummary> tags) {

		public static TaskResponse of(Task t, Instant now, List<TagSummary> tags) {
			DueDate due = t.getDue();
			String dueDate = due == null ? null : due.date().toString();
			String dueTime = due == null || due.time() == null ? null : due.time().toString();
			String dueTimeZone = due == null ? null : due.zone().getId();
			String dueOffset = due == null || due.time() == null ? null : formatOffset(due.offset());
			Instant dueAt = due == null ? null : due.exactInstant();
			return new TaskResponse(t.getId(), t.getProjectId(), t.getTitle(), t.getDescription(), t.getStatus().name(),
					t.getPriority().name(), dueDate, dueTime, dueTimeZone, dueOffset, dueAt, t.isOverdue(now),
					t.getCreatorId(), t.getCreatedAt(), t.getUpdatedAt(), t.getCompletedAt(), t.getVersion(), tags);
		}

		private static String formatOffset(ZoneOffset offset) {
			int total = offset.getTotalSeconds();
			String sign = total < 0 ? "-" : "+";
			int abs = Math.abs(total);
			return "%s%02d:%02d".formatted(sign, abs / 3600, (abs % 3600) / 60);
		}

	}

}
