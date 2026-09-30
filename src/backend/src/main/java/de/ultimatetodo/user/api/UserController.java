package de.ultimatetodo.user.api;

import de.ultimatetodo.user.api.Dtos.ChangePasswordRequest;
import de.ultimatetodo.user.api.Dtos.UpdateProfileRequest;
import de.ultimatetodo.user.api.Dtos.UserResponse;
import de.ultimatetodo.user.application.AppUserDetails;
import de.ultimatetodo.user.application.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Eigenes Profil. Es gibt bewusst keine Endpunkte, die fremde Konten lesen oder ändern. */
@RestController
@RequestMapping("/api/v1/users/me")
class UserController {

	private final UserService users;
	private final Duration sessionTimeout;

	UserController(UserService users, @Value("${server.servlet.session.timeout}") Duration sessionTimeout) {
		this.users = users;
		this.sessionTimeout = sessionTimeout;
	}

	@GetMapping
	UserResponse get(@AuthenticationPrincipal AppUserDetails principal) {
		return UserResponse.of(users.get(principal.id()), sessionTimeout.toSeconds());
	}

	/** Nur Anzeigename und Zeitzone sind änderbar; E-Mail, Rolle und Status nicht. */
	@PutMapping
	UserResponse update(@AuthenticationPrincipal AppUserDetails principal, @RequestBody UpdateProfileRequest request) {
		return UserResponse.of(users.updateProfile(principal.id(), request.displayName(), request.timeZone()),
				sessionTimeout.toSeconds());
	}

	/** Danach sind alle Sitzungen ungültig; auch diese wird beendet und eine neue Anmeldung verlangt. */
	@PostMapping("/password")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void changePassword(@AuthenticationPrincipal AppUserDetails principal, @RequestBody ChangePasswordRequest request,
			HttpServletRequest http, HttpServletResponse response) {
		users.changePassword(principal.id(), request.currentPassword(), request.newPassword());
		HttpSession session = http.getSession(false);
		if (session != null) {
			session.invalidate();
		}
		SecurityContextHolder.clearContext();
	}

}
