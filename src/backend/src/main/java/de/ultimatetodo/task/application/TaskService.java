package de.ultimatetodo.task.application;

import de.ultimatetodo.common.ApiError.FieldError;
import de.ultimatetodo.common.ApiException;
import de.ultimatetodo.project.application.ProjectAccess;
import de.ultimatetodo.tag.application.TagAccess;
import de.ultimatetodo.tag.application.TagSummary;
import de.ultimatetodo.task.domain.Task;
import de.ultimatetodo.task.domain.TaskStatus;
import de.ultimatetodo.task.domain.TaskTag;
import de.ultimatetodo.task.persistence.TaskRepository;
import de.ultimatetodo.task.persistence.TaskTagRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Anwendungsfälle des Aufgabenmoduls (US-030 bis US-037, US-043, US-120, US-121 M1-Teil). */
@Service
public class TaskService {

	private static final String CONFLICT_MESSAGE = "Die Aufgabe wurde zwischenzeitlich geändert. Bitte lade sie neu.";
	private static final String TAGS_INVALID_MESSAGE = "Diese Tags existieren nicht oder gehören dir nicht.";

	private final TaskRepository tasks;
	private final TaskTagRepository taskTags;
	private final TaskDraftValidator validator;
	private final ProjectAccess projectAccess;
	private final TagAccess tagAccess;
	private final Clock clock;

	public TaskService(TaskRepository tasks, TaskTagRepository taskTags, TaskDraftValidator validator,
			ProjectAccess projectAccess, TagAccess tagAccess, Clock clock) {
		this.tasks = tasks;
		this.taskTags = taskTags;
		this.validator = validator;
		this.projectAccess = projectAccess;
		this.tagAccess = tagAccess;
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

	/** US-043: „Tags sind filterbar“. */
	@Transactional(readOnly = true)
	public Page<Task> listByTag(UUID userId, UUID tagId, Pageable pageable) {
		return tasks.findAccessibleByTag(userId, ownedProjectIds(userId), tagId, pageable);
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

	/** Liefert die Tags mehrerer Aufgaben in zwei Abfragen statt einer je Aufgabe (Liste, US-043). */
	@Transactional(readOnly = true)
	public Map<UUID, List<TagSummary>> tagsFor(Collection<UUID> taskIds) {
		if (taskIds.isEmpty()) {
			return Map.of();
		}
		List<TaskTag> links = taskTags.findByTaskIdIn(taskIds);
		if (links.isEmpty()) {
			return Map.of();
		}
		Set<UUID> allTagIds = links.stream().map(TaskTag::getTagId).collect(Collectors.toSet());
		Map<UUID, TagSummary> byTagId = tagAccess.summarize(allTagIds).stream()
			.collect(Collectors.toMap(TagSummary::id, s -> s));
		Map<UUID, List<TagSummary>> result = new HashMap<>();
		for (TaskTag link : links) {
			TagSummary summary = byTagId.get(link.getTagId());
			if (summary != null) {
				result.computeIfAbsent(link.getTaskId(), k -> new ArrayList<>()).add(summary);
			}
		}
		return result;
	}

	/** Ersetzt die Tag-Zuordnung vollständig (US-043); ändert weder Version noch `updatedAt` der Aufgabe. */
	@Transactional
	public List<TagSummary> setTags(UUID userId, UUID taskId, List<String> rawTagIds) {
		Task task = loadAccessible(userId, taskId);
		Set<UUID> tagIds = new HashSet<>();
		for (String raw : rawTagIds) {
			try {
				tagIds.add(UUID.fromString(raw));
			}
			catch (IllegalArgumentException e) {
				throw ApiException.validation(List.of(new FieldError("tagIds", "INVALID", TAGS_INVALID_MESSAGE)));
			}
		}
		if (!tagAccess.areAccessible(tagIds, userId)) {
			throw ApiException.validation(List.of(new FieldError("tagIds", "INVALID", TAGS_INVALID_MESSAGE)));
		}
		taskTags.deleteByTaskId(task.getId());
		Instant now = now();
		taskTags.saveAll(tagIds.stream().map(tagId -> TaskTag.of(task.getId(), tagId, now)).toList());
		return tagAccess.summarize(tagIds);
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
