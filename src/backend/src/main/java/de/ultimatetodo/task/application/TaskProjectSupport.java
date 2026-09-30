package de.ultimatetodo.task.application;

import java.util.UUID;

/**
 * Schmale öffentliche Schnittstelle des Aufgaben-Moduls für das Projekt-Modul (docs/ARCHITECTURE.md):
 * kein direkter Zugriff auf {@code TaskRepository} von außerhalb.
 */
public interface TaskProjectSupport {

	/**
	 * Löst alle Aufgaben von diesem Projekt (project_id wird {@code null}) und entfernt dabei ihre Zuweisung
	 * (US-042/US-062: eine Zuweisung ohne das gemeinsame Projekt ergibt keinen Sinn mehr). Die Aufgaben selbst
	 * werden dadurch zu persönlichen Aufgaben ihres jeweiligen Erstellers und bleiben erhalten.
	 */
	void detachFromProject(UUID projectId);

	/** US-061: entfernt die Zuweisung dieses Benutzers auf allen Aufgaben dieses Projekts (Mitgliedschaft endet). */
	void unassignUser(UUID projectId, UUID userId);

}
