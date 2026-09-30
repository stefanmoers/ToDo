package de.ultimatetodo.common;

import de.ultimatetodo.user.application.AppUserDetails;
import de.ultimatetodo.user.persistence.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Prüft bei jeder authentifizierten Anfrage, ob das Konto noch aktiv ist und die Sitzung zur aktuellen
 * Sitzungsgeneration gehört. So enden Sitzungen nach Passwortänderung oder Deaktivierung spätestens
 * bei der nächsten geschützten Anfrage (docs/SECURITY.md).
 */
class SessionEpochFilter extends OncePerRequestFilter {

	private final UserRepository users;

	SessionEpochFilter(UserRepository users) {
		this.users = users;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth != null && auth.getPrincipal() instanceof AppUserDetails principal) {
			boolean valid = users.findById(principal.id())
				.map(u -> u.isActive() && u.getAuthEpoch() == principal.authEpoch())
				.orElse(false);
			if (!valid) {
				HttpSession session = request.getSession(false);
				if (session != null) {
					session.invalidate();
				}
				SecurityContextHolder.clearContext();
			}
		}
		chain.doFilter(request, response);
	}

}
