package de.ultimatetodo.common;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;
import org.springframework.context.annotation.Configuration;

/** Beschreibt Session-Cookie und CSRF-Header in der generierten OpenAPI-Dokumentation (ADR-005). */
@Configuration
@OpenAPIDefinition(info = @Info(title = "UltimateTodo API", version = "v1"))
@SecuritySchemes({
		@SecurityScheme(name = "session", type = SecuritySchemeType.APIKEY, in = SecuritySchemeIn.COOKIE,
				paramName = "ULTIMATETODO_SESSION", description = "Serverseitige Sitzung nach erfolgreichem Login"),
		@SecurityScheme(name = "csrf", type = SecuritySchemeType.APIKEY, in = SecuritySchemeIn.HEADER,
				paramName = "X-CSRF-TOKEN", description = "Token aus GET /api/v1/auth/csrf; für alle schreibenden Anfragen") })
class OpenApiConfig {
}
