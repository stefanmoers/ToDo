package de.ultimatetodo.task.application;

import de.ultimatetodo.task.persistence.TaskRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementiert {@link TaskProjectSupport} losgelöst von {@link TaskService}: {@link ProjectService} löscht
 * ein Projekt in derselben Transaktion, die diese Klasse aufruft; hinge diese Schnittstelle an {@code TaskService}
 * (das selbst {@code ProjectAccess} aus dem Projekt-Modul benötigt), entstünde ein zyklischer Bean-Zugriff.
 */
@Component
class TaskProjectMaintenance implements TaskProjectSupport {

	private final TaskRepository tasks;

	TaskProjectMaintenance(TaskRepository tasks) {
		this.tasks = tasks;
	}

	@Override
	@Transactional
	public void detachFromProject(UUID projectId) {
		tasks.detachFromProject(projectId);
	}

	@Override
	@Transactional
	public void unassignUser(UUID projectId, UUID userId) {
		tasks.unassignUser(projectId, userId);
	}

}
