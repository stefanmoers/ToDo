package de.ultimatetodo.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Security-Fehler im selben JSON-Format wie alle API-Fehler statt HTML-Seiten (ADR-005). */
@Component
public class JsonSecurityHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

	private final JsonMapper mapper;

	public JsonSecurityHandlers(JsonMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
			throws IOException {
		write(response, ApiError.of(401, "UNAUTHENTICATED", "Bitte melde dich an."));
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
			throws IOException {
		if (ex instanceof CsrfException) {
			write(response, ApiError.of(403, "CSRF_INVALID",
					"Die Sicherheitsprüfung ist fehlgeschlagen. Bitte lade die Seite neu und versuche es erneut."));
		}
		else {
			write(response, ApiError.of(403, "FORBIDDEN", "Diese Aktion ist nicht erlaubt."));
		}
	}

	private void write(HttpServletResponse response, ApiError error) throws IOException {
		response.setStatus(error.status());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(mapper.writeValueAsString(error));
	}

}
