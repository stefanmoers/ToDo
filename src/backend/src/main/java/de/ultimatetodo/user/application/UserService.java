package de.ultimatetodo.user.application;

import de.ultimatetodo.common.ApiError.FieldError;
import de.ultimatetodo.common.ApiException;
import de.ultimatetodo.common.RateLimitProperties;
import de.ultimatetodo.common.RateLimiter;
import de.ultimatetodo.user.domain.PasswordPolicy;
import de.ultimatetodo.user.domain.User;
import de.ultimatetodo.user.domain.UserInput;
import de.ultimatetodo.user.persistence.UserRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Anwendungsfälle des Benutzermoduls: registrieren, Profil lesen/ändern, Passwort ändern. */
@Service
public class UserService {

	private static final String MSG_PASSWORD =
			"Das Passwort muss zwischen " + PasswordPolicy.MIN_LENGTH + " und " + PasswordPolicy.MAX_LENGTH + " Zeichen lang sein.";

	private final UserRepository users;
	private final PasswordEncoder encoder;
	private final Clock clock;
	private final RateLimiter limiter;
	private final RateLimitProperties limits;

	public UserService(UserRepository users, PasswordEncoder encoder, Clock clock, RateLimiter limiter,
			RateLimitProperties limits) {
		this.users = users;
		this.encoder = encoder;
		this.clock = clock;
		this.limiter = limiter;
		this.limits = limits;
	}

	public User register(String rawEmail, String rawName, String password, String timeZone, String clientAddress) {
		limiter.hit("register:" + clientAddress, limits.registerAttempts(), limits.registerWindow());
		String email = UserInput.normalizeEmail(rawEmail);
		String name = UserInput.normalizeName(rawName);
		List<FieldError> errors = new ArrayList<>();
		if (!UserInput.isValidEmail(email)) {
			errors.add(new FieldError("email", "INVALID", "Bitte gib eine gültige E-Mail-Adresse an (höchstens 254 Zeichen)."));
		}
		if (!UserInput.isValidName(name)) {
			errors.add(new FieldError("displayName", "INVALID", "Der Anzeigename muss 1 bis 100 Zeichen lang sein."));
		}
		if (!PasswordPolicy.isValid(password)) {
			errors.add(new FieldError("password", "INVALID", MSG_PASSWORD));
		}
		if (!UserInput.isValidTimeZone(timeZone)) {
			errors.add(new FieldError("timeZone", "INVALID", "Bitte wähle eine gültige Zeitzone, zum Beispiel Europe/Berlin."));
		}
		if (!errors.isEmpty()) {
			throw ApiException.validation(errors);
		}
		User user = User.register(email, name, encoder.encode(password), timeZone, clock.instant());
		try {
			return users.saveAndFlush(user);
		}
		catch (DataIntegrityViolationException e) {
			// Unique-Constraint greift auch bei parallelen Anfragen.
			throw new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED", "Diese E-Mail-Adresse ist bereits registriert.",
					List.of(new FieldError("email", "TAKEN", "Diese E-Mail-Adresse ist bereits registriert.")), null);
		}
	}

	@Transactional(readOnly = true)
	public User get(UUID id) {
		return users.findById(id).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Bitte melde dich an."));
	}

	@Transactional
	public User updateProfile(UUID id, String rawName, String timeZone) {
		String name = UserInput.normalizeName(rawName);
		List<FieldError> errors = new ArrayList<>();
		if (!UserInput.isValidName(name)) {
			errors.add(new FieldError("displayName", "INVALID", "Der Anzeigename muss 1 bis 100 Zeichen lang sein."));
		}
		if (!UserInput.isValidTimeZone(timeZone)) {
			errors.add(new FieldError("timeZone", "INVALID", "Bitte wähle eine gültige Zeitzone, zum Beispiel Europe/Berlin."));
		}
		if (!errors.isEmpty()) {
			throw ApiException.validation(errors);
		}
		User user = get(id);
		// Bestehende Fälligkeiten behalten ihre eigene gespeicherte Zeitzone; hier wird nur die Kontozeitzone geändert.
		user.updateProfile(name, timeZone, clock.instant());
		return user;
	}

	/** Ändert das Passwort und erhöht die Sitzungsgeneration: alle bisherigen Sitzungen werden ungültig. */
	@Transactional
	public void changePassword(UUID id, String currentPassword, String newPassword) {
		limiter.hit("pwchange:" + id, limits.passwordChangeAttempts(), limits.passwordChangeWindow());
		User user = get(id);
		List<FieldError> errors = new ArrayList<>();
		if (currentPassword == null || !encoder.matches(currentPassword, user.getPasswordHash())) {
			errors.add(new FieldError("currentPassword", "WRONG", "Das bisherige Passwort ist nicht korrekt."));
		}
		if (!PasswordPolicy.isValid(newPassword)) {
			errors.add(new FieldError("newPassword", "INVALID", MSG_PASSWORD));
		}
		if (!errors.isEmpty()) {
			throw ApiException.validation(errors);
		}
		user.changePassword(encoder.encode(newPassword), clock.instant());
	}

}
