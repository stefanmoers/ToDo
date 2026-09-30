package de.ultimatetodo.user.api;

import de.ultimatetodo.user.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/** Ein-/Ausgabemodelle der Benutzer-API. Entities verlassen die API nie direkt. */
public final class Dtos {

	private Dtos() {
	}

	public record RegisterRequest(String email, String displayName, String password, String timeZone) {
	}

	public record LoginRequest(String email, String password) {
	}

	public record UpdateProfileRequest(String displayName, String timeZone) {
	}

	public record ChangePasswordRequest(String currentPassword, String newPassword) {
	}

	@Schema(description = "Token für schreibende Anfragen; im Header mit dem angegebenen Namen zurücksenden")
	public record CsrfResponse(String headerName, String token) {
	}

	@Schema(description = "Eigenes Konto. Enthält weder Passwort noch Hash.")
	public record UserResponse(UUID id, String email, String displayName, String timeZone, String role,
			Instant createdAt) {

		public static UserResponse of(User u) {
			return new UserResponse(u.getId(), u.getEmail(), u.getDisplayName(), u.getTimeZone(), u.getRole().name(),
					u.getCreatedAt());
		}

	}

}
