package de.ultimatetodo.task.application;

import java.util.UUID;

/**
 * Schmale öffentliche Schnittstelle des Aufgaben-Moduls für das Projekt-Modul (docs/ARCHITECTURE.md):
 * kein direkter Zugriff auf {@code TaskRepository} von außerhalb.
 */
public interface TaskProjectSupport {

	/**
	 * Löst alle Aufgaben von diesem Projekt (project_id wird {@code null}); sie werden dadurch zu persönlichen
	 * Aufgaben ihres jeweiligen Erstellers (US-042). Die Aufgaben selbst bleiben erhalten.
	 */
	void detachFromProject(UUID projectId);

}
