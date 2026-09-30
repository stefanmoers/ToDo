package de.ultimatetodo.task.application;

import de.ultimatetodo.common.ApiError.FieldError;
import de.ultimatetodo.common.ApiException;
import de.ultimatetodo.common.Paging;
import de.ultimatetodo.project.application.ProjectAccess;
import de.ultimatetodo.tag.application.TagAccess;
import de.ultimatetodo.tag.application.TagSummary;
import de.ultimatetodo.task.domain.ActivityType;
import de.ultimatetodo.task.domain.DueDate;
import de.ultimatetodo.task.domain.Task;
import de.ultimatetodo.task.domain.TaskChangeKind;
import de.ultimatetodo.task.domain.TaskPriority;
import de.ultimatetodo.task.domain.TaskStatus;
import de.ultimatetodo.task.domain.TaskTag;
import de.ultimatetodo.task.persistence.TaskRepository;
import de.ultimatetodo.task.persistence.TaskSpecifications;
import de.ultimatetodo.task.persistence.TaskTagRepository;
import de.ultimatetodo.user.application.UserAccess;
import de.ultimatetodo.user.application.UserSummary;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Anwendungsfälle des Aufgabenmoduls (US-030 bis US-037, US-043, US-050 bis US-052, US-062, US-110, US-120,
 * US-121 M1-Teil).
 */
@Service
public class TaskService {

	private static final String CONFLICT_MESSAGE = "Die Aufgabe wurde zwischenzeitlich geändert. Bitte lade sie neu.";
	private static final String TAGS_INVALID_MESSAGE = "Diese Tags existieren nicht oder gehören dir nicht.";
	private static final Set<TaskStatus> NOT_DONE = Set.of(TaskStatus.OPEN, TaskStatus.IN_PROGRESS);
	private static final Set<TaskPriority> HIGH_OR_URGENT = Set.of(TaskPriority.HIGH, TaskPriority.URGENT);

	private final TaskRepository tasks;
	private final TaskTagRepository taskTags;
	private final TaskDraftValidator validator;
	private final TaskListQueryValidator listQueryValidator;
	private final ProjectAccess projectAccess;
	private final TagAccess tagAccess;
	private final UserAccess userAccess;
	private final ActivityRecorder activityRecorder;
	private final Clock clock;

	public TaskService(TaskRepository tasks, TaskTagRepository taskTags, TaskDraftValidator validator,
			TaskListQueryValidator listQueryValidator, ProjectAccess projectAccess, TagAccess tagAccess,
			UserAccess userAccess, ActivityRecorder activityRecorder, Clock clock) {
		this.tasks = tasks;
		this.taskTags = taskTags;
		this.validator = validator;
		this.listQueryValidator = listQueryValidator;
		this.projectAccess = projectAccess;
		this.tagAccess = tagAccess;
		this.userAccess = userAccess;
		this.activityRecorder = activityRecorder;
		this.clock = clock;
	}

	@Transactional
	public Task create(UUID userId, TaskDraft draft) {
		ValidTaskValues v = validator.validate(draft, true);
		requireAccessibleProjectIfSet(v.projectId(), userId);
		requireValidAssignee(v.projectId(), v.assigneeId(), userId);
		Instant now = now();
		Task task = Task.create(userId, v.projectId(), v.assigneeId(), v.title(), v.description(), v.priority(), v.due(),
				now);
		tasks.saveAndFlush(task);
		activityRecorder.record(task.getId(), userId, ActivityType.CREATED, null, null, now);
		return task;
	}

	@Transactional(readOnly = true)
	public Task get(UUID userId, UUID taskId) {
		return loadAccessible(userId, taskId);
	}

	/** US-050 (Suche), US-051 (Filter) und US-052 (Sortierung): eine Aufgabenliste mit beliebig kombinierten Kriterien. */
	@Transactional(readOnly = true)
	public Page<Task> list(UUID userId, TaskListQuery query) {
		TaskListQueryValidator.Result parsed = listQueryValidator.validate(query);
		Pageable pageable = Paging.of(query.page(), query.size(), Sort.unsorted());
		TaskListFilter filter = new TaskListFilter(query.q(), parsed.statuses(), parsed.priorities(),
				toSet(query.projectId()), toSet(query.tagId()), toSet(query.assigneeId()), Boolean.TRUE.equals(query.overdue()),
				query.dueDateFrom(), query.dueDateTo());
		Specification<Task> spec = buildFilterSpecification(userId, filter)
			.and(TaskSpecifications.orderedBy(parsed.sortField(), parsed.ascending()));
		return tasks.findAll(spec, pageable);
	}

	/** US-110: fünf Zähler mit denselben Rechte- und Zeitregeln wie die Listen, für die persönliche Übersicht. */
	@Transactional(readOnly = true)
	public TaskSummaryCounts summary(UUID userId, LocalDate today) {
		Specification<Task> accessible = TaskSpecifications.accessibleTo(userId, accessibleProjectIds(userId));
		long open = tasks.count(accessible.and(TaskSpecifications.hasStatus(NOT_DONE)));
		long dueToday = tasks
			.count(accessible.and(TaskSpecifications.hasStatus(NOT_DONE)).and(TaskSpecifications.dueDateBetween(today, today)));
		long overdue = tasks.count(accessible.and(TaskSpecifications.isOverdue(now())));
		long done = tasks.count(accessible.and(TaskSpecifications.hasStatus(Set.of(TaskStatus.DONE))));
		long highPriorityOpen = tasks
			.count(accessible.and(TaskSpecifications.hasStatus(NOT_DONE)).and(TaskSpecifications.hasPriority(HIGH_OR_URGENT)));
		return new TaskSummaryCounts(open, dueToday, overdue, done, highPriorityOpen);
	}

	/** Kombiniert nur tatsächlich angeforderte Filterausschnitte mit UND; {@code null} bedeutet „keine Einschränkung". */
	private Specification<Task> buildFilterSpecification(UUID userId, TaskListFilter filter) {
		List<Specification<Task>> parts = new ArrayList<>();
		parts.add(TaskSpecifications.accessibleTo(userId, accessibleProjectIds(userId)));
		addIfPresent(parts, TaskSpecifications.hasSearchText(filter.searchText()));
		addIfPresent(parts, TaskSpecifications.hasStatus(filter.statuses()));
		addIfPresent(parts, TaskSpecifications.hasPriority(filter.priorities()));
		addIfPresent(parts, TaskSpecifications.hasProject(filter.projectIds()));
		addIfPresent(parts, TaskSpecifications.hasTag(filter.tagIds()));
		addIfPresent(parts, TaskSpecifications.hasAssignee(filter.assigneeIds()));
		if (filter.overdueOnly()) {
			parts.add(TaskSpecifications.isOverdue(now()));
		}
		addIfPresent(parts, TaskSpecifications.dueDateBetween(filter.dueDateFrom(), filter.dueDateTo()));
		Specification<Task> combined = parts.get(0);
		for (int i = 1; i < parts.size(); i++) {
			combined = combined.and(parts.get(i));
		}
		return combined;
	}

	private static void addIfPresent(List<Specification<Task>> parts, Specification<Task> spec) {
		if (spec != null) {
			parts.add(spec);
		}
	}

	private static Set<UUID> toSet(List<UUID> values) {
		return values == null ? Set.of() : Set.copyOf(values);
	}

	/** Ersetzt die bearbeitbaren Felder. 409, wenn {@code expectedVersion} nicht mehr der aktuellen entspricht. */
	@Transactional
	public Task update(UUID userId, UUID taskId, TaskDraft draft, Long expectedVersion) {
		requireVersion(expectedVersion);
		ValidTaskValues v = validator.validate(draft, false);
		Task task = loadAccessible(userId, taskId);
		checkVersion(task, expectedVersion);
		requireCanChangeProject(task, userId, v.projectId());
		requireAccessibleProjectIfSet(v.projectId(), userId);
		requireValidAssignee(v.projectId(), v.assigneeId(), task.getCreatorId());
		String oldTitle = task.getTitle();
		TaskStatus oldStatus = task.getStatus();
		TaskPriority oldPriority = task.getPriority();
		UUID oldAssigneeId = task.getAssigneeId();
		UUID oldProjectId = task.getProjectId();
		DueDate oldDue = task.getDue();
		Instant now = now();
		Set<TaskChangeKind> changes = task.update(v.projectId(), v.assigneeId(), v.title(), v.description(), v.status(),
				v.priority(), v.due(), now);
		if (!changes.isEmpty()) {
			flushOrConflict();
			recordChangeActivities(task, userId, changes, oldTitle, oldStatus, oldPriority, oldAssigneeId, oldProjectId,
					oldDue, now);
		}
		return task;
	}

	/**
	 * US-081: für jedes AC-relevante geänderte Feld ein eigener, verständlicher Historien-Eintrag. Mehrere
	 * Felder ändern sich in derselben Anfrage mit demselben {@code now}; ein wachsender Versatz von je einer
	 * Mikrosekunde (der kleinsten von PostgreSQL gespeicherten Einheit, siehe {@code now()}) macht die
	 * Reihenfolge dieser Einträge deterministisch (sonst entscheidet die zufällige ID über die Reihenfolge
	 * bei gleichem Zeitstempel, siehe {@code ActivityRepository}-Sortierung).
	 */
	private void recordChangeActivities(Task task, UUID actorId, Set<TaskChangeKind> changes, String oldTitle,
			TaskStatus oldStatus, TaskPriority oldPriority, UUID oldAssigneeId, UUID oldProjectId, DueDate oldDue,
			Instant now) {
		int seq = 0;
		if (changes.contains(TaskChangeKind.TITLE)) {
			activityRecorder.record(task.getId(), actorId, ActivityType.TITLE_CHANGED, oldTitle, task.getTitle(),
					now.plusNanos(1000L * seq++));
		}
		if (changes.contains(TaskChangeKind.STATUS)) {
			activityRecorder.record(task.getId(), actorId, ActivityType.STATUS_CHANGED, oldStatus.name(),
					task.getStatus().name(), now.plusNanos(1000L * seq++));
		}
		if (changes.contains(TaskChangeKind.PRIORITY)) {
			activityRecorder.record(task.getId(), actorId, ActivityType.PRIORITY_CHANGED, oldPriority.name(),
					task.getPriority().name(), now.plusNanos(1000L * seq++));
		}
		if (changes.contains(TaskChangeKind.ASSIGNEE)) {
			activityRecorder.record(task.getId(), actorId, ActivityType.ASSIGNEE_CHANGED, userNameOrNull(oldAssigneeId),
					userNameOrNull(task.getAssigneeId()), now.plusNanos(1000L * seq++));
		}
		if (changes.contains(TaskChangeKind.DUE)) {
			String oldValue = oldDue == null ? null : oldDue.date().toString();
			DueDate newDue = task.getDue();
			String newValue = newDue == null ? null : newDue.date().toString();
			activityRecorder.record(task.getId(), actorId, ActivityType.DUE_CHANGED, oldValue, newValue,
					now.plusNanos(1000L * seq++));
		}
		if (changes.contains(TaskChangeKind.PROJECT)) {
			activityRecorder.record(task.getId(), actorId, ActivityType.PROJECT_CHANGED, projectNameOrNull(oldProjectId),
					projectNameOrNull(task.getProjectId()), now.plusNanos(1000L * seq++));
		}
	}

	private String userNameOrNull(UUID userId) {
		if (userId == null) {
			return null;
		}
		UserSummary summary = userAccess.summarize(Set.of(userId)).get(userId);
		return summary == null ? null : summary.displayName();
	}

	private String projectNameOrNull(UUID projectId) {
		return projectId == null ? null : projectAccess.nameOf(projectId).orElse(null);
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
			Instant now = now();
			if (task.complete(now)) {
				flushOrConflict();
				activityRecorder.record(task.getId(), userId, ActivityType.COMPLETED, null, null, now);
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
			Instant now = now();
			if (task.reopen(now)) {
				flushOrConflict();
				activityRecorder.record(task.getId(), userId, ActivityType.REOPENED, null, null, now);
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

	// Paketsichtbar statt privat: SubtaskService (US-070/071) prüft Rechte über dieselbe Hauptaufgaben-Prüfung
	// (docs/DOMAIN_MODEL.md: "Rechte werden über die Hauptaufgabe geprüft"), ohne sie zu duplizieren.
	Task loadAccessible(UUID userId, UUID taskId) {
		return tasks.findAccessibleById(taskId, userId, accessibleProjectIds(userId))
			.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TASK_NOT_FOUND", "Die Aufgabe wurde nicht gefunden."));
	}

	private Set<UUID> accessibleProjectIds(UUID userId) {
		return projectAccess.accessibleProjectIds(userId);
	}

	/** US-040ff/US-060: Eine Aufgabe darf nur einem Projekt zugeordnet werden, das der Handelnde selbst zugänglich ist. */
	private void requireAccessibleProjectIfSet(UUID projectId, UUID userId) {
		if (projectId != null && !projectAccess.isAccessible(projectId, userId)) {
			throw ApiException.validation(List
				.of(new FieldError("projectId", "INVALID", "Dieses Projekt existiert nicht oder du hast keinen Zugriff.")));
		}
	}

	/**
	 * US-042/US-062 (docs/DOMAIN_MODEL.md): eine bestehende Aufgabe darf nur ihr Ersteller oder der Eigentümer
	 * ihres aktuellen Projekts einem anderen Projekt zuordnen (auch das Entfernen aus einem Projekt) — ein
	 * einfaches Mitglied, das die Aufgabe sonst bearbeiten darf, darf sie nicht verschieben.
	 */
	private void requireCanChangeProject(Task task, UUID userId, UUID newProjectId) {
		if (Objects.equals(task.getProjectId(), newProjectId)) {
			return;
		}
		boolean isCreator = task.getCreatorId().equals(userId);
		boolean isCurrentProjectOwner = task.getProjectId() != null && projectAccess.isOwner(task.getProjectId(), userId);
		if (!isCreator && !isCurrentProjectOwner) {
			throw ApiException.validation(List.of(new FieldError("projectId", "FORBIDDEN",
					"Nur der Ersteller oder der Eigentümer des aktuellen Projekts darf die Aufgabe einem anderen Projekt zuordnen.")));
		}
	}

	/**
	 * US-062 (docs/DOMAIN_MODEL.md): ohne Projekt ist nur der Ersteller selbst als Bearbeiter erlaubt; mit
	 * Projekt nur ein aktuelles Mitglied oder der Eigentümer dieses Projekts.
	 */
	private void requireValidAssignee(UUID projectId, UUID assigneeId, UUID creatorId) {
		if (assigneeId == null) {
			return;
		}
		if (projectId == null) {
			if (!assigneeId.equals(creatorId)) {
				throw ApiException.validation(List
					.of(new FieldError("assigneeId", "INVALID", "Ohne Projekt kann nur der Ersteller selbst zugewiesen werden.")));
			}
			return;
		}
		if (!projectAccess.isAccessible(projectId, assigneeId)) {
			throw ApiException.validation(List.of(
					new FieldError("assigneeId", "INVALID", "Dieser Bearbeiter ist kein Mitglied oder Eigentümer dieses Projekts.")));
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
