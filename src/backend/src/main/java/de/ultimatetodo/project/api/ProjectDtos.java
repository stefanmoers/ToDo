package de.ultimatetodo.project.api;

import de.ultimatetodo.project.domain.Project;
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

	@Schema(description = "Ein Projekt. Vorerst genau ein Eigentümer, keine Mitgliedschaft (folgt in M2).")
	public record ProjectResponse(UUID id, String name, String description, UUID ownerId, Instant createdAt,
			Instant updatedAt, long version) {

		public static ProjectResponse of(Project p) {
			return new ProjectResponse(p.getId(), p.getName(), p.getDescription(), p.getOwnerId(), p.getCreatedAt(),
					p.getUpdatedAt(), p.getVersion());
		}

	}

}
