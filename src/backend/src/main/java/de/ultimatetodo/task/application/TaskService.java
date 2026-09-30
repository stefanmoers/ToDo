package de.ultimatetodo.task.application;

import de.ultimatetodo.common.ApiError.FieldError;
import de.ultimatetodo.common.ApiException;
import de.ultimatetodo.project.application.ProjectAccess;
import de.ultimatetodo.task.domain.Task;
import de.ultimatetodo.task.domain.TaskStatus;
import de.ultimatetodo.task.persistence.TaskRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Anwendungsfälle des Aufgabenmoduls (US-030 bis US-037, US-120, US-121 M1-Teil). */
@Service
public class TaskService {

	private static final String CONFLICT_MESSAGE = "Die Aufgabe wurde zwischenzeitlich geändert. Bitte lade sie neu.";

	private final TaskRepository tasks;
	private final TaskDraftValidator validator;
	private final ProjectAccess projectAccess;
	private final Clock clock;

	public TaskService(TaskRepository tasks, TaskDraftValidator validator, ProjectAccess projectAccess, Clock clock) {
		this.tasks = tasks;
		this.validator = validator;
		this.projectAccess = projectAccess;
		this.clock = clock;
	}

	@Transactional
	public Task create(UUID userId, TaskDraft draft) {
		ValidTaskValues v = validator.validate(draft, true);
		requireOwnedProjectIfSet(v.projectId(), userId);
		Task task = Task.create(userId, v.projectId(), v.title(), v.description(), v.priority(), v.due(), now());
		return tasks.saveAndFlush(task);
	}

	@Transactional(readOnly = true)
	public Task get(UUID userId, UUID taskId) {
		return loadAccessible(userId, taskId);
	}

	@Transactional(readOnly = true)
	public Page<Task> list(UUID userId, Pageable pageable) {
		return tasks.findAccessible(userId, ownedProjectIds(userId), pageable);
	}

	/** Ersetzt die bearbeitbaren Felder. 409, wenn {@code expectedVersion} nicht mehr der aktuellen entspricht. */
	@Transactional
	public Task update(UUID userId, UUID taskId, TaskDraft draft, Long expectedVersion) {
		requireVersion(expectedVersion);
		ValidTaskValues v = validator.validate(draft, false);
		requireOwnedProjectIfSet(v.projectId(), userId);
		Task task = loadAccessible(userId, taskId);
		checkVersion(task, expectedVersion);
		if (task.update(v.projectId(), v.title(), v.description(), v.status(), v.priority(), v.due(), now())) {
			flushOrConflict();
		}
		return task;
	}

	/** Löscht endgültig. Abhängige Daten (Unteraufgaben, Kommentare, …) folgen ab M2 transaktional per CASCADE. */
	@Transactional
	public void delete(UUID userId, UUID taskId, Long expectedVersion) {
		requireVersion(expectedVersion);
		Task task = loadAccessible(userId, taskId);
		checkVersion(task, expectedVersion);
		tasks.delete(task);
		flushOrConflict();
	}

	/**
	 * Idempotent (Arbeitsannahme, docs/DOMAIN_MODEL.md): Ist die Aufgabe bereits erledigt, wird die Version nicht
	 * geprüft; ein erneuter Aufruf mit veralteter Version ändert nichts und liefert den aktuellen Stand.
	 */
	@Transactional
	public Task complete(UUID userId, UUID taskId, Long expectedVersion) {
		requireVersion(expectedVersion);
		Task task = loadAccessible(userId, taskId);
		if (task.getStatus() != TaskStatus.DONE) {
			checkVersion(task, expectedVersion);
			if (task.complete(now())) {
				flushOrConflict();
			}
		}
		return task;
	}

	/** Wirkungslos (und ohne Versionsprüfung) auf einer nicht erledigten Aufgabe. */
	@Transactional
	public Task reopen(UUID userId, UUID taskId, Long expectedVersion) {
		requireVersion(expectedVersion);
		Task task = loadAccessible(userId, taskId);
		if (task.getStatus() == TaskStatus.DONE) {
			checkVersion(task, expectedVersion);
			if (task.reopen(now())) {
				flushOrConflict();
			}
		}
		return task;
	}

	private Task loadAccessible(UUID userId, UUID taskId) {
		return tasks.findAccessibleById(taskId, userId, ownedProjectIds(userId))
			.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TASK_NOT_FOUND", "Die Aufgabe wurde nicht gefunden."));
	}

	private Set<UUID> ownedProjectIds(UUID userId) {
		return projectAccess.ownedProjectIds(userId);
	}

	/** US-040ff: Eine Aufgabe darf nur einem Projekt zugeordnet werden, das der Handelnde selbst besitzt. */
	private void requireOwnedProjectIfSet(UUID projectId, UUID userId) {
		if (projectId != null && !projectAccess.isOwner(projectId, userId)) {
			throw ApiException.validation(List
				.of(new FieldError("projectId", "INVALID", "Dieses Projekt existiert nicht oder gehört dir nicht.")));
		}
	}

	private void requireVersion(Long expectedVersion) {
		if (expectedVersion == null) {
			throw ApiException
				.validation(List.of(new FieldError("version", "REQUIRED", "Bitte gib die erwartete Version an.")));
		}
	}

	private void checkVersion(Task task, long expectedVersion) {
		if (task.getVersion() != expectedVersion) {
			throw new ApiException(HttpStatus.CONFLICT, "VERSION_CONFLICT", CONFLICT_MESSAGE);
		}
	}

	/** Erzwingt den Konflikt-Check auch dann, wenn er erst beim Commit zwischen Laden und Schreiben auffällt. */
	private void flushOrConflict() {
		try {
			tasks.flush();
		}
		catch (ObjectOptimisticLockingFailureException e) {
			throw new ApiException(HttpStatus.CONFLICT, "VERSION_CONFLICT", CONFLICT_MESSAGE);
		}
	}

	private Instant now() {
		// PostgreSQL speichert Mikrosekunden; ohne Kürzung würde createdAt sich beim nächsten Lesen minimal unterscheiden.
		return clock.instant().truncatedTo(ChronoUnit.MICROS);
	}

}
