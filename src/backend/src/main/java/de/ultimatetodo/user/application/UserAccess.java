package de.ultimatetodo.user.application;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Schmale öffentliche Schnittstelle des Benutzermoduls für andere Module (docs/ARCHITECTURE.md): kein
 * direkter Zugriff auf {@code UserRepository} von außerhalb. Keine frei durchsuchbare Benutzerliste — nur
 * die exakte E-Mail-Adresse oder bereits bekannte IDs lösen einen Treffer aus (US-060).
 */
public interface UserAccess {

	Optional<UserSummary> findByEmail(String email);

	Map<UUID, UserSummary> summarize(Collection<UUID> userIds);

}
