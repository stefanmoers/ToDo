package de.ultimatetodo.user.api;

import de.ultimatetodo.common.PageResponse;
import de.ultimatetodo.common.Paging;
import de.ultimatetodo.user.api.AdminDtos.AdminUserResponse;
import de.ultimatetodo.user.application.UserAdminService;
import de.ultimatetodo.user.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * US-014: Konten auflisten, deaktivieren, reaktivieren. Nur die Rolle ADMIN erreicht diese Pfade
 * (SecurityConfig erzwingt {@code /api/v1/admin/**} serverseitig, unabhängig von jeder UI-Ausblendung).
 * Administration gewährt keinen Zugriff auf Aufgaben- oder Projektinhalte; die Antworten enthalten
 * ausschließlich Kontodaten.
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@Tag(name = "Konten-Administration", description = "Nur für Konten mit Rolle ADMIN erreichbar")
@SecurityRequirement(name = "session")
class AdminController {

	private final UserAdminService admin;

	AdminController(UserAdminService admin) {
		this.admin = admin;
	}

	@Operation(summary = "Konten auflisten (US-014)")
	@GetMapping
	PageResponse<AdminUserResponse> list(@RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size) {
		Page<User> result = admin.list(Paging.of(page, size, Sort.by(Sort.Direction.ASC, "email")));
		return PageResponse.of(result, AdminUserResponse::of);
	}

	@Operation(summary = "Konto deaktivieren (US-014)",
			description = "Beendet dessen Sitzungen spätestens bei der nächsten Anfrage; Daten bleiben bestehen. "
					+ "409, wenn dies das letzte aktive Administratorkonto wäre.")
	@PostMapping("/{id}/deactivate")
	AdminUserResponse deactivate(@PathVariable UUID id) {
		return AdminUserResponse.of(admin.deactivate(id));
	}

	@Operation(summary = "Konto reaktivieren (US-014)")
	@PostMapping("/{id}/reactivate")
	AdminUserResponse reactivate(@PathVariable UUID id) {
		return AdminUserResponse.of(admin.reactivate(id));
	}

}
