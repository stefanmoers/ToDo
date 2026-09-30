package de.ultimatetodo.task.api;

import de.ultimatetodo.common.ApiError.FieldError;
import de.ultimatetodo.common.ApiException;
import de.ultimatetodo.common.PageResponse;
import de.ultimatetodo.tag.application.TagSummary;
import de.ultimatetodo.task.api.ActivityDtos.ActivityResponse;
import de.ultimatetodo.task.api.CommentDtos.CommentResponse;
import de.ultimatetodo.task.api.CommentDtos.CreateCommentRequest;
import de.ultimatetodo.task.api.SubtaskDtos.CreateSubtaskRequest;
import de.ultimatetodo.task.api.SubtaskDtos.MoveSubtaskRequest;
import de.ultimatetodo.task.api.SubtaskDtos.RenameSubtaskRequest;
import de.ultimatetodo.task.api.SubtaskDtos.SetSubtaskDoneRequest;
import de.ultimatetodo.task.api.SubtaskDtos.SubtaskResponse;
import de.ultimatetodo.task.api.TaskDtos.CreateTaskRequest;
import de.ultimatetodo.task.api.TaskDtos.TaskResponse;
import de.ultimatetodo.task.api.TaskDtos.TaskSummaryResponse;
import de.ultimatetodo.task.api.TaskDtos.UpdateTaskRequest;
import de.ultimatetodo.task.api.TaskDtos.UpdateTaskTagsRequest;
import de.ultimatetodo.task.api.TaskDtos.VersionRequest;
import de.ultimatetodo.task.application.ActivityQueryService;
import de.ultimatetodo.task.application.CommentService;
import de.ultimatetodo.task.application.SubtaskService;
import de.ultimatetodo.task.application.TaskDraft;
import de.ultimatetodo.task.application.TaskListQuery;
import de.ultimatetodo.task.application.TaskService;
import de.ultimatetodo.task.domain.Activity;
import de.ultimatetodo.task.domain.Comment;
import de.ultimatetodo.task.domain.Subtask;
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
import java.util.stream.Collectors;
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
	private final SubtaskService subtaskService;
	private final CommentService commentService;
	private final ActivityQueryService activityQueryService;
	private final UserAccess userAccess;
	private final Clock clock;

	TaskController(TaskService taskService, SubtaskService subtaskService, CommentService commentService,
			ActivityQueryService activityQueryService, UserAccess userAccess, Clock clock) {
		this.taskService = taskService;
		this.subtaskService = subtaskService;
		this.commentService = commentService;
		this.activityQueryService = activityQueryService;
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
		List<UUID> taskIds = taskList.stream().map(Task::getId).toList();
		Map<UUID, List<TagSummary>> tags = taskService.tagsFor(taskIds);
		Map<UUID, List<Subtask>> subtasks = subtaskService.forTasks(taskIds);
		Map<UUID, UserSummary> users = resolveUsers(taskList);
		Instant now = clock.instant();
		return PageResponse.of(result, t -> TaskResponse.of(t, now, tags.getOrDefault(t.getId(), List.of()), users,
				subtasks.getOrDefault(t.getId(), List.of())));
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

	@Operation(summary = "Unteraufgaben auflisten (US-070)", description = "In stabiler Reihenfolge.")
	@GetMapping("/{id}/subtasks")
	List<SubtaskResponse> listSubtasks(@AuthenticationPrincipal AppUserDetails principal, @PathVariable UUID id) {
		return toSubtaskResponses(subtaskService.list(principal.id(), id));
	}

	@Operation(summary = "Unteraufgabe anlegen (US-070)", description = "Wird ans Ende der Liste angehängt.")
	@PostMapping("/{id}/subtasks")
	@ResponseStatus(HttpStatus.CREATED)
	List<SubtaskResponse> createSubtask(@AuthenticationPrincipal AppUserDetails principal, @PathVariable UUID id,
			@RequestBody CreateSubtaskRequest request) {
		return toSubtaskResponses(subtaskService.create(principal.id(), id, request.title()));
	}

	@Operation(summary = "Unteraufgabe umbenennen (US-070)")
	@PutMapping("/{id}/subtasks/{subtaskId}")
	List<SubtaskResponse> renameSubtask(@AuthenticationPrincipal AppUserDetails principal, @PathVariable UUID id,
			@PathVariable UUID subtaskId, @RequestBody RenameSubtaskRequest request) {
		return toSubtaskResponses(subtaskService.rename(principal.id(), id, subtaskId, request.title()));
	}

	@Operation(summary = "Erledigt-Zustand einer Unteraufgabe setzen (US-071)", description = "Rückgängig änderbar.")
	@PutMapping("/{id}/subtasks/{subtaskId}/done")
	List<SubtaskResponse> setSubtaskDone(@AuthenticationPrincipal AppUserDetails principal, @PathVariable UUID id,
			@PathVariable UUID subtaskId, @RequestBody SetSubtaskDoneRequest request) {
		return toSubtaskResponses(subtaskService.setDone(principal.id(), id, subtaskId, request.done()));
	}

	@Operation(summary = "Unteraufgabe verschieben (US-070)",
			description = "Vertauscht die Position mit dem Vorgänger (UP) oder Nachfolger (DOWN); ohne Ziehbewegung bedienbar.")
	@PostMapping("/{id}/subtasks/{subtaskId}/move")
	List<SubtaskResponse> moveSubtask(@AuthenticationPrincipal AppUserDetails principal, @PathVariable UUID id,
			@PathVariable UUID subtaskId, @RequestBody MoveSubtaskRequest request) {
		return toSubtaskResponses(subtaskService.move(principal.id(), id, subtaskId, toDirection(request.direction())));
	}

	private static boolean toDirection(String direction) {
		if ("UP".equals(direction)) {
			return true;
		}
		if ("DOWN".equals(direction)) {
			return false;
		}
		throw ApiException.validation(
				List.of(new FieldError("direction", "INVALID", "Bitte wähle UP oder DOWN.")));
	}

	@Operation(summary = "Unteraufgabe löschen (US-070)")
	@DeleteMapping("/{id}/subtasks/{subtaskId}")
	List<SubtaskResponse> deleteSubtask(@AuthenticationPrincipal AppUserDetails principal, @PathVariable UUID id,
			@PathVariable UUID subtaskId) {
		return toSubtaskResponses(subtaskService.delete(principal.id(), id, subtaskId));
	}

	private static List<SubtaskResponse> toSubtaskResponses(List<Subtask> subtasks) {
		return subtasks.stream().map(SubtaskResponse::of).toList();
	}

	@Operation(summary = "Kommentare auflisten (US-080)", description = "In chronologischer Reihenfolge.")
	@GetMapping("/{id}/comments")
	List<CommentResponse> listComments(@AuthenticationPrincipal AppUserDetails principal, @PathVariable UUID id) {
		List<Comment> comments = commentService.list(principal.id(), id);
		Map<UUID, UserSummary> authors = userAccess
			.summarize(comments.stream().map(Comment::getAuthorId).collect(Collectors.toSet()));
		return comments.stream().map(c -> CommentResponse.of(c, displayNameOf(authors, c.getAuthorId()))).toList();
	}

	@Operation(summary = "Kommentar anlegen (US-080)", description = "Kommentar und zugehörige Aktivität entstehen atomar.")
	@PostMapping("/{id}/comments")
	@ResponseStatus(HttpStatus.CREATED)
	CommentResponse createComment(@AuthenticationPrincipal AppUserDetails principal, @PathVariable UUID id,
			@RequestBody CreateCommentRequest request) {
		Comment comment = commentService.create(principal.id(), id, request.content());
		UserSummary author = userAccess.summarize(Set.of(principal.id())).get(principal.id());
		return CommentResponse.of(comment, author == null ? null : author.displayName());
	}

	@Operation(summary = "Aktivitätshistorie auflisten (US-081)",
			description = "Chronologisch; nicht durch Benutzer editierbar.")
	@GetMapping("/{id}/activities")
	List<ActivityResponse> listActivities(@AuthenticationPrincipal AppUserDetails principal, @PathVariable UUID id) {
		List<Activity> activities = activityQueryService.list(principal.id(), id);
		Map<UUID, UserSummary> actors = userAccess
			.summarize(activities.stream().map(Activity::getActorId).collect(Collectors.toSet()));
		return activities.stream().map(a -> ActivityResponse.of(a, displayNameOf(actors, a.getActorId()))).toList();
	}

	private static String displayNameOf(Map<UUID, UserSummary> users, UUID id) {
		UserSummary u = users.get(id);
		return u == null ? null : u.displayName();
	}

	// Rechte sind zu diesem Zeitpunkt bereits über taskService.xxx(principal.id(), ...) geprüft; die
	// Unteraufgaben werden hier ungeprüft über die Aufgaben-ID nachgeladen (kein zweiter, überflüssiger Check).
	private TaskResponse toResponse(Task task) {
		List<TagSummary> tags = taskService.tagsFor(List.of(task.getId())).getOrDefault(task.getId(), List.of());
		List<Subtask> subtasks = subtaskService.forTasks(List.of(task.getId())).getOrDefault(task.getId(), List.of());
		return TaskResponse.of(task, clock.instant(), tags, resolveUsers(List.of(task)), subtasks);
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
