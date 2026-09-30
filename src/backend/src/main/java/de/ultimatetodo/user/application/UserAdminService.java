package de.ultimatetodo.user.application;

import de.ultimatetodo.common.ApiException;
import de.ultimatetodo.user.domain.User;
import de.ultimatetodo.user.domain.User.Role;
import de.ultimatetodo.user.domain.User.Status;
import de.ultimatetodo.user.persistence.UserRepository;
import java.time.Clock;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-014: Konten auflisten, deaktivieren, reaktivieren. Nur über {@code /api/v1/admin/**} erreichbar, das die
 * Rolle ADMIN bereits auf Sicherheitsfilter-Ebene erzwingt (SecurityConfig) — Administration gewährt bewusst
 * keinen Zugriff auf Aufgaben- oder Projektinhalte, dieser Dienst kennt nur Kontodaten.
 */
@Service
public class UserAdminService {

	private final UserRepository users;
	private final Clock clock;

	public UserAdminService(UserRepository users, Clock clock) {
		this.users = users;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public Page<User> list(Pageable pageable) {
		return users.findAll(pageable);
	}

	/** Liefert 409, wenn dies das letzte aktive Administratorkonto wäre. */
	@Transactional
	public User deactivate(UUID targetId) {
		User target = load(targetId);
		if (target.getRole() == Role.ADMIN && target.isActive() && users.countByRoleAndStatus(Role.ADMIN, Status.ACTIVE) <= 1) {
			throw new ApiException(HttpStatus.CONFLICT, "LAST_ACTIVE_ADMIN",
					"Das letzte aktive Administratorkonto kann nicht deaktiviert werden.");
		}
		target.deactivate(clock.instant());
		return target;
	}

	@Transactional
	public User reactivate(UUID targetId) {
		User target = load(targetId);
		target.reactivate(clock.instant());
		return target;
	}

	private User load(UUID id) {
		return users.findById(id)
			.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Das Konto wurde nicht gefunden."));
	}

}
