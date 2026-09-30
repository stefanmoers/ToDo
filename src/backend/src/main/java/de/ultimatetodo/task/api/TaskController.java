package de.ultimatetodo.task.api;

import de.ultimatetodo.common.PageResponse;
import de.ultimatetodo.common.Paging;
import de.ultimatetodo.tag.application.TagSummary;
import de.ultimatetodo.task.api.TaskDtos.CreateTaskRequest;
import de.ultimatetodo.task.api.TaskDtos.TaskResponse;
import de.ultimatetodo.task.api.TaskDtos.UpdateTaskRequest;
import de.ultimatetodo.task.api.TaskDtos.UpdateTaskTagsRequest;
import de.ultimatetodo.task.api.TaskDtos.VersionRequest;
import de.ultimatetodo.task.application.TaskDraft;
import de.ultimatetodo.task.application.TaskService;
import de.ultimatetodo.task.domain.Task;
import de.ultimatetodo.user.application.AppUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
 * Persönliche Aufgaben (US-030 bis US-037, US-043, US-120). Jeder Zugriff ist auf die eigenen Aufgaben beschränkt
 * ({@link TaskService} filtert serverseitig nach Ersteller/Projekt-Eigentümerschaft).
 */
@RestController
@RequestMapping("/api/v1/tasks")
@Tag(name = "Aufgaben", description = "Persönliche Aufgaben des angemeldeten Benutzers")
@SecurityRequirement(name = "session")
class TaskController {

	private final TaskService taskService;
	private final Clock clock;

	TaskController(TaskService taskService, Clock clock) {
		this.taskService = taskService;
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

	@Operation(summary = "Eigene Aufgaben auflisten",
			description = "Sortierung fest: createdAt absteigend, dann id absteigend (stabil). Mit tagId auf eine "
					+ "Tag-Zuordnung eingeschränkt (US-043). Weitere Sortier- und Filterfelder folgen mit US-051/052.")
	@GetMapping
	PageResponse<TaskResponse> list(@AuthenticationPrincipal AppUserDetails principal,
			@RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size,
			@RequestParam(required = false) UUID tagId) {
		Pageable pageable = Paging.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
		Page<Task> result = tagId == null ? taskService.list(principal.id(), pageable)
				: taskService.listByTag(principal.id(), tagId, pageable);
		Map<UUID, List<TagSummary>> tags = taskService.tagsFor(result.map(Task::getId).toList());
		Instant now = clock.instant();
		return PageResponse.of(result, t -> TaskResponse.of(t, now, tags.getOrDefault(t.getId(), List.of())));
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
		return TaskResponse.of(task, clock.instant(), tags);
	}

	private static TaskDraft toDraft(CreateTaskRequest r) {
		return new TaskDraft(r.projectId(), r.title(), r.description(), null, r.priority(), r.dueDate(), r.dueTime(),
				r.dueTimeZone(), r.dueOffset());
	}

	private static TaskDraft toDraft(UpdateTaskRequest r) {
		return new TaskDraft(r.projectId(), r.title(), r.description(), r.status(), r.priority(), r.dueDate(),
				r.dueTime(), r.dueTimeZone(), r.dueOffset());
	}

}
