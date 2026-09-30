package de.ultimatetodo.project.application;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Schmale öffentliche Schnittstelle des Projekt-Moduls für andere Module (docs/ARCHITECTURE.md): kein
 * direkter Zugriff auf {@code ProjectRepository}/{@code MembershipRepository} von außerhalb.
 */
public interface ProjectAccess {

	/** Nur der Eigentümer, nicht ein einfaches Mitglied (Projektverwaltung bleibt beim Eigentümer). */
	boolean isOwner(UUID projectId, UUID userId);

	/** Eigentümer oder aktuelles Mitglied (US-060 bis US-062): Grundlage für Lese-/Bearbeitungsrechte. */
	boolean isAccessible(UUID projectId, UUID userId);

	/** Alle Projekte, für die {@code userId} Eigentümer oder Mitglied ist. */
	Set<UUID> accessibleProjectIds(UUID userId);

	/** Aktueller Name, ungeprüft auf Zugriff (US-081: Momentaufnahme für die Aktivitätshistorie). */
	Optional<String> nameOf(UUID projectId);

}
