package de.ultimatetodo.tag.api;

import de.ultimatetodo.tag.domain.Tag;
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

	@Schema(description = "Ein Tag; gehört entweder persönlich dem Benutzer oder einem eigenen Projekt.")
	public record TagResponse(UUID id, String name,
			@Schema(allowableValues = { "PERSONAL", "PROJECT" }) String scope, UUID projectId, Instant createdAt) {

		public static TagResponse of(Tag t) {
			return new TagResponse(t.getId(), t.getName(), t.isPersonal() ? "PERSONAL" : "PROJECT", t.getProjectId(),
					t.getCreatedAt());
		}

	}

}
