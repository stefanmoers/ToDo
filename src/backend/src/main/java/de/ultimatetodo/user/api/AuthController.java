package de.ultimatetodo.user.api;

import de.ultimatetodo.common.ApiException;
import de.ultimatetodo.common.RateLimitProperties;
import de.ultimatetodo.common.RateLimiter;
import de.ultimatetodo.user.api.Dtos.CsrfResponse;
import de.ultimatetodo.user.api.Dtos.LoginRequest;
import de.ultimatetodo.user.api.Dtos.RegisterRequest;
import de.ultimatetodo.user.api.Dtos.UserResponse;
import de.ultimatetodo.user.application.AppUserDetails;
import de.ultimatetodo.user.application.UserService;
import de.ultimatetodo.user.domain.UserInput;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Registrierung, CSRF-Token, Login und aktuelles Konto. Logout ist in der Security-Konfiguration umgesetzt. */
@RestController
@RequestMapping("/api/v1/auth")
class AuthController {

	private static final String GENERIC_LOGIN_ERROR = "E-Mail-Adresse oder Passwort ist nicht korrekt.";

	private final UserService users;
	private final AuthenticationManager authenticationManager;
	private final SecurityContextRepository contextRepository;
	private final SessionAuthenticationStrategy sessionStrategy;
	private final RateLimiter limiter;
	private final RateLimitProperties limits;
	private final Duration sessionTimeout;

	AuthController(UserService users, AuthenticationManager authenticationManager,
			SecurityContextRepository contextRepository, SessionAuthenticationStrategy sessionStrategy,
			RateLimiter limiter, RateLimitProperties limits,
			@Value("${server.servlet.session.timeout}") Duration sessionTimeout) {
		this.users = users;
		this.authenticationManager = authenticationManager;
		this.contextRepository = contextRepository;
		this.sessionStrategy = sessionStrategy;
		this.limiter = limiter;
		this.limits = limits;
		this.sessionTimeout = sessionTimeout;
	}

	/** Liefert den CSRF-Token der aktuellen Sitzung. Nach Login/Logout muss er neu abgerufen werden. */
	@GetMapping("/csrf")
	ResponseEntity<CsrfResponse> csrf(CsrfToken token) {
		return ResponseEntity.ok().cacheControl(CacheControl.noStore())
			.body(new CsrfResponse(token.getHeaderName(), token.getToken()));
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	UserResponse register(@RequestBody RegisterRequest request, HttpServletRequest http) {
		return UserResponse.of(users.register(request.email(), request.displayName(), request.password(),
				request.timeZone(), http.getRemoteAddr()), sessionTimeout.toSeconds());
	}

	@PostMapping("/login")
	UserResponse login(@RequestBody LoginRequest request, HttpServletRequest http, HttpServletResponse response) {
		String email = UserInput.normalizeEmail(request.email());
		// Begrenzung je Adresse und je Konto; nach Ablauf des Fensters sind wieder Versuche möglich (keine Dauersperre).
		limiter.hit("login-ip:" + http.getRemoteAddr(), limits.loginAttempts(), limits.loginWindow());
		limiter.hit("login-account:" + email, limits.loginAttempts(), limits.loginWindow());
		if (email.isEmpty() || request.password() == null || request.password().isEmpty()) {
			throw badCredentials();
		}
		Authentication authentication;
		try {
			authentication = authenticationManager
				.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(email, request.password()));
		}
		catch (AuthenticationException e) {
			// Unbekanntes, inaktives und falsch authentifiziertes Konto sind nach außen nicht unterscheidbar.
			throw badCredentials();
		}
		// Neue Sitzungs-ID und neuer CSRF-Token gegen Session Fixation (docs/SECURITY.md).
		http.getSession(true);
		sessionStrategy.onAuthentication(authentication, http, response);
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
		contextRepository.saveContext(context, http, response);
		limiter.reset("login-account:" + email);
		return UserResponse.of(users.get(((AppUserDetails) authentication.getPrincipal()).id()), sessionTimeout.toSeconds());
	}

	@GetMapping("/me")
	UserResponse me(@AuthenticationPrincipal AppUserDetails principal) {
		return UserResponse.of(users.get(principal.id()), sessionTimeout.toSeconds());
	}

	private static ApiException badCredentials() {
		return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", GENERIC_LOGIN_ERROR);
	}

}
