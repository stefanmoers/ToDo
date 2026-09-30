package de.ultimatetodo.project.application;

import java.util.Set;
import java.util.UUID;

/**
 * Schmale öffentliche Schnittstelle des Projekt-Moduls für andere Module (docs/ARCHITECTURE.md): kein
 * direkter Zugriff auf {@code ProjectRepository} von außerhalb.
 */
public interface ProjectAccess {

	boolean isOwner(UUID projectId, UUID userId);

	Set<UUID> ownedProjectIds(UUID userId);

}
