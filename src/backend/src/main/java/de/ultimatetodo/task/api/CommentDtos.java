package de.ultimatetodo.task.api;

import de.ultimatetodo.task.domain.Comment;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/** Ein-/Ausgabemodelle der Kommentar-API (US-080). */
public final class CommentDtos {

	private CommentDtos() {
	}

	public record CreateCommentRequest(
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 5000) String content) {
	}

	@Schema(description = "Ein Kommentar an einer Aufgabe; der Inhalt wird als reiner Text dargestellt.")
	public record CommentResponse(UUID id, UUID authorId, String authorDisplayName, String content, Instant createdAt) {

		public static CommentResponse of(Comment c, String authorDisplayName) {
			return new CommentResponse(c.getId(), c.getAuthorId(), authorDisplayName, c.getContent(), c.getCreatedAt());
		}

	}

}
