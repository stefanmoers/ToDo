package de.ultimatetodo.task.api;

import de.ultimatetodo.task.domain.Activity;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/** Ausgabemodell der Aktivitätshistorie-API (US-081). */
public final class ActivityDtos {

	private ActivityDtos() {
	}

	@Schema(description = "Ein Eintrag der Aktivitätshistorie; nicht durch Benutzer editierbar. fromValue/toValue "
			+ "sind bereits anzeigefertiger Text (z. B. Titel, Status-/Prioritätsname, Bearbeiter-/Projektname zum "
			+ "Zeitpunkt der Änderung) und fehlen, wenn die Art keine Werte hat (z. B. Erledigen, Kommentar).")
	public record ActivityResponse(UUID id,
			@Schema(allowableValues = { "CREATED", "TITLE_CHANGED", "STATUS_CHANGED", "PRIORITY_CHANGED",
					"ASSIGNEE_CHANGED", "DUE_CHANGED", "PROJECT_CHANGED", "COMPLETED", "REOPENED",
					"COMMENTED" }) String type,
			String fromValue, String toValue, UUID actorId, String actorDisplayName, Instant createdAt) {

		public static ActivityResponse of(Activity a, String actorDisplayName) {
			return new ActivityResponse(a.getId(), a.getType().name(), a.getFromValue(), a.getToValue(), a.getActorId(),
					actorDisplayName, a.getCreatedAt());
		}

	}

}
