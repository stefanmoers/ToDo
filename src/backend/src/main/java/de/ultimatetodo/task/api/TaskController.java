package de.ultimatetodo.task.api;

import de.ultimatetodo.common.PageResponse;
import de.ultimatetodo.tag.application.TagSummary;
import de.ultimatetodo.task.api.TaskDtos.CreateTaskRequest;
import de.ultimatetodo.task.api.TaskDtos.TaskResponse;
import de.ultimatetodo.task.api.TaskDtos.TaskSummaryResponse;
import de.ultimatetodo.task.api.TaskDtos.UpdateTaskRequest;
import de.ultimatetodo.task.api.TaskDtos.UpdateTaskTagsRequest;
import de.ultimatetodo.task.api.TaskDtos.VersionRequest;
import de.ultimatetodo.task.application.TaskDraft;
import de.ultimatetodo.task.application.TaskListQuery;
import de.ultimatetodo.task.application.TaskService;
import de.ultimatetodo.task.domain.Task;
import de.ultimatetodo.user.application.AppUserDetails;
import de.ultimatetodo.user.application.UserAccess;
import de.ultimatetodo.user.application.UserSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Persönliche und geteilte Aufgaben (US-030 bis US-037, US-043, US-050 bis US-052, US-062, US-110, US-120).
 * Jeder Zugriff ist auf zugängliche Aufgaben beschränkt ({@link TaskService} filtert serverseitig nach
 * Ersteller-, Projekt-Eigentümerschaft oder -Mitgliedschaft).
 */
@RestController
@RequestMapping("/api/v1/tasks")
@Tag(name = "Aufgaben", description = "Persönliche und geteilte Aufgaben des angemeldeten Benutzers")
@SecurityRequirement(name = "session")
class TaskController {

	private final TaskService taskService;
	private final UserAccess userAccess;
	private final Clock clock;

	TaskController(TaskService taskService, UserAccess userAccess, Clock clock) {
		this.taskService = taskService;
		this.userAccess = userAccess;
		this.clock = clock;
	}

	@Operation(summary = "Aufgabe anlegen")
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	ResponseEntity<TaskResponse> create(@AuthenticationPrincipal AppUserDetails principal,
			@RequestBody CreateTaskRequest request) {
		Task task = taskService.create(principal.id(), toDraft(request));
		return ResponseEntity.created(URI.create("/api/v1/tasks/" + task.getId())).body(toResponse(task));
	}

	@Operation(summary = "Eigene und geteilte Aufgaben auflisten", description = "Suche (q, Titel/Beschreibung, ohne "
			+ "Beachtung der Großschreibung), Filter (status, priority, projectId, tagId, assigneeId je mehrfach "
			+ "möglich und mit ODER verknüpft; unterschiedliche Filterarten mit UND; overdue; dueDateFrom/dueDateTo "
			+ "vergleichen das rohe Fälligkeitsdatum) und Sortierung (sortBy: createdAt, updatedAt, due, priority, "
			+ "title; sortDirection: asc, desc; Standard createdAt/desc, stabil nach id; Aufgaben ohne Fälligkeit "
			+ "stehen bei sortBy=due immer zuletzt) gemäß US-050 bis US-052.")
	@GetMapping
	PageResponse<TaskResponse> list(@AuthenticationPrincipal AppUserDetails principal,
			@RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size,
			@RequestParam(required = false) String q, @RequestParam(required = false) List<String> status,
			@RequestParam(required = false) List<String> priority, @RequestParam(required = false) List<UUID> projectId,
			@RequestParam(required = false) List<UUID> tagId, @RequestParam(required = false) List<UUID> assigneeId,
			@RequestParam(required = false) Boolean overdue, @RequestParam(required = false) LocalDate dueDateFrom,
			@RequestParam(required = false) LocalDate dueDateTo, @RequestParam(required = false) String sortBy,
			@RequestParam(required = false) String sortDirection) {
		TaskListQuery query = new TaskListQuery(q, status, priority, projectId, tagId, assigneeId, overdue, dueDateFrom,
				dueDateTo, sortBy, sortDirection, page, size);
		Page<Task> result = taskService.list(principal.id(), query);
		List<Task> taskList = result.getContent();
		Map<UUID, List<TagSummary>> tags = taskService.tagsFor(taskList.stream().map(Task::getId).toList());
		Map<UUID, UserSummary> users = resolveUsers(taskList);
		Instant now = clock.instant();
		return PageResponse.of(result, t -> TaskResponse.of(t, now, tags.getOrDefault(t.getId(), List.of()), users));
	}

	@Operation(summary = "Persönliche Übersicht (US-110)", description = "Fünf Zähler mit denselben Rechte- und "
			+ "Zeitregeln wie die Listen. `today` ist Pflicht: das Aufgaben-Modul kennt die Kontozeitzone nicht "
			+ "(Modulgrenze, docs/ARCHITECTURE.md); der Client löst „heute“ in seiner Kontozeitzone auf.")
	@GetMapping("/summary")
	TaskSummaryResponse summary(@AuthenticationPrincipal AppUserDetails principal,
			@Parameter(required = true) @RequestParam LocalDate today) {
		return TaskSummaryResponse.of(taskService.summary(principal.id(), today));
	}

	@Operation(summary = "Einzelne Aufgabe ansehen")
	@GetMapping("/{id}")
	TaskResponse get(@AuthenticationPrincipal AppUserDetails principal, @PathVariable UUID id) {
		return toResponse(taskService.get(principal.id(), id));
	}

	@Operation(summary = "Aufgabe bearbeiten", description = "Optimistisches Sperren: 409, wenn version veraltet ist.")
	@PutMapping("/{id}")
	TaskResponse update(@AuthenticationPrincipal AppUserDetails principal, @PathVariable UUID id,
			@RequestBody UpdateTaskRequest request) {
		Task task = taskService.update(principal.id(), id, toDraft(request), request.version());
		return toResponse(task);
	}

	@Operation(summary = "Aufgabe als erledigt markieren", description = "Idempotent: ist die Aufgabe bereits erledigt, ändert sich nichts.")
	@PostMapping("/{id}/complete")
	TaskResponse complete(@AuthenticationPrincipal AppUserDetails principal, @PathVariable UUID id,
			@RequestBody VersionRequest request) {
		return toResponse(taskService.complete(principal.id(), id, request.version()));
	}

	@Operation(summary = "Aufgabe wieder öffnen")
	@PostMapping("/{id}/reopen")
	TaskResponse reopen(@AuthenticationPrincipal AppUserDetails principal, @PathVariable UUID id,
			@RequestBody VersionRequest request) {
		return toResponse(taskService.reopen(principal.id(), id, request.version()));
	}

	@Operation(summary = "Aufgabe endgültig löschen")
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@AuthenticationPrincipal AppUserDetails principal, @PathVariable UUID id,
			@RequestParam(required = false) Long version) {
		taskService.delete(principal.id(), id, version);
	}

	@Operation(summary = "Tag-Zuordnung einer Aufgabe ersetzen",
			description = "Nur eigene oder in einem eigenen Projekt zugängliche Tags; ändert keine Version.")
	@PutMapping("/{id}/tags")
	List<TagSummary> setTags(@AuthenticationPrincipal AppUserDetails principal, @PathVariable UUID id,
			@RequestBody UpdateTaskTagsRequest request) {
		List<String> tagIds = request.tagIds() == null ? List.of() : request.tagIds();
		return taskService.setTags(principal.id(), id, tagIds);
	}

	private TaskResponse toResponse(Task task) {
		List<TagSummary> tags = taskService.tagsFor(List.of(task.getId())).getOrDefault(task.getId(), List.of());
		return TaskResponse.of(task, clock.instant(), tags, resolveUsers(List.of(task)));
	}

	/** Ersteller und Bearbeiter in einer Stapelabfrage auflösen, statt je Aufgabe einzeln (US-062). */
	private Map<UUID, UserSummary> resolveUsers(List<Task> taskList) {
		Set<UUID> ids = new HashSet<>();
		for (Task t : taskList) {
			ids.add(t.getCreatorId());
			if (t.getAssigneeId() != null) {
				ids.add(t.getAssigneeId());
			}
		}
		return userAccess.summarize(ids);
	}

	private static TaskDraft toDraft(CreateTaskRequest r) {
		return new TaskDraft(r.projectId(), r.assigneeId(), r.title(), r.description(), null, r.priority(), r.dueDate(),
				r.dueTime(), r.dueTimeZone(), r.dueOffset());
	}

	private static TaskDraft toDraft(UpdateTaskRequest r) {
		return new TaskDraft(r.projectId(), r.assigneeId(), r.title(), r.description(), r.status(), r.priority(),
				r.dueDate(), r.dueTime(), r.dueTimeZone(), r.dueOffset());
	}

}
