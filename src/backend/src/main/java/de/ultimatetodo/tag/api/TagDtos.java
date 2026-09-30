package de.ultimatetodo.tag.api;

import de.ultimatetodo.tag.application.TagView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/** Ein-/Ausgabemodelle der Tag-API. Entities verlassen die API nie direkt. */
public final class TagDtos {

	private TagDtos() {
	}

	public record CreateTagRequest(
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 50) String name,
			@Schema(description = "Nur ein eigenes Projekt; ohne Angabe ist der Tag persönlich") String projectId) {
	}

	public record UpdateTagRequest(
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 50) String name) {
	}

	@Schema(description = "Ein Tag; gehört entweder persönlich dem Benutzer oder einem Projekt. Bei Projekt-Tags nennen "
			+ "projectName und projectOwnerDisplayName die Herkunft, damit gleichnamige Tags unterscheidbar sind.")
	public record TagResponse(UUID id, String name,
			@Schema(allowableValues = { "PERSONAL", "PROJECT" }) String scope, UUID projectId,
			@Schema(description = "Nur bei Projekt-Tags: aktueller Name des Projekts") String projectName,
			@Schema(description = "Nur bei Projekt-Tags: Eigentümer des Projekts") UUID projectOwnerId,
			@Schema(description = "Nur bei Projekt-Tags: Anzeigename des Projekteigentümers") String projectOwnerDisplayName,
			Instant createdAt) {

		public static TagResponse of(TagView v) {
			var t = v.tag();
			return new TagResponse(t.getId(), t.getName(), t.isPersonal() ? "PERSONAL" : "PROJECT", t.getProjectId(),
					v.projectName(), v.projectOwnerId(), v.projectOwnerDisplayName(), t.getCreatedAt());
		}

	}

}
