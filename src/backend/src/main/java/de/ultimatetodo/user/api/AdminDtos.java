package de.ultimatetodo.user.api;

import de.ultimatetodo.user.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/** Ein-/Ausgabemodelle der Konten-Administration (US-014). Enthält nie Passwort oder Hash. */
public final class AdminDtos {

	private AdminDtos() {
	}

	@Schema(description = "Ein Konto aus Administrator-Sicht. Verrät keine Aufgaben- oder Projektinhalte.")
	public record AdminUserResponse(UUID id, String email, String displayName, String status, String role,
			Instant createdAt) {

		public static AdminUserResponse of(User u) {
			return new AdminUserResponse(u.getId(), u.getEmail(), u.getDisplayName(), u.getStatus().name(),
					u.getRole().name(), u.getCreatedAt());
		}

	}

}
