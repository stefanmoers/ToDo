package de.ultimatetodo.project.api;

import de.ultimatetodo.project.domain.Project;
import de.ultimatetodo.user.application.UserSummary;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/** Ein-/Ausgabemodelle der Projekt-API. Entities verlassen die API nie direkt. */
public final class ProjectDtos {

	private ProjectDtos() {
	}

	public record CreateProjectRequest(
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 100,
					example = "Steuererklärung 2026") String name,
			@Schema(maxLength = 10000) String description) {
	}

	public record UpdateProjectRequest(
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 100) String name,
			@Schema(maxLength = 10000) String description,
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") Long version) {
	}

	@Schema(description = "Ein Projekt. Eigentümer und Mitglieder (US-060) haben Zugriff auf seine Aufgaben; "
			+ "Projekt selbst umbenennen/löschen/Mitglieder verwalten bleibt dem Eigentümer vorbehalten.")
	public record ProjectResponse(UUID id, String name, String description, UUID ownerId, String ownerDisplayName,
			Instant createdAt, Instant updatedAt, long version) {

		public static ProjectResponse of(Project p, String ownerDisplayName) {
			return new ProjectResponse(p.getId(), p.getName(), p.getDescription(), p.getOwnerId(), ownerDisplayName,
					p.getCreatedAt(), p.getUpdatedAt(), p.getVersion());
		}

	}

	@Schema(description = "Ein aktives oder inaktives Projektmitglied (US-060, US-061).")
	public record MemberResponse(UUID userId, String displayName, Instant addedAt) {

		public static MemberResponse of(UserSummary user, Instant addedAt) {
			return new MemberResponse(user.id(), user.displayName(), addedAt);
		}

	}

	@Schema(description = "Nimmt ein Mitglied über die exakte Login-E-Mail auf (US-060); keine durchsuchbare Benutzerliste.")
	public record AddMemberRequest(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String email) {
	}

}
