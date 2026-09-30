package de.ultimatetodo.tag.application;

import java.util.UUID;

/**
 * Schmale öffentliche Schnittstelle des Tag-Moduls für das Projekt-Modul (docs/ARCHITECTURE.md): kein
 * direkter Zugriff auf {@code TagRepository} von außerhalb.
 */
public interface ProjectTagSupport {

	/** Entfernt alle Tags dieses Projekts (US-042); die Fremdschlüsselprüfung auf {@code tag.project_id} ist sonst RESTRICT. */
	void deleteAllForProject(UUID projectId);

}
