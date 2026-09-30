package de.ultimatetodo.project.api;

import de.ultimatetodo.common.PageResponse;
import de.ultimatetodo.common.Paging;
import de.ultimatetodo.project.api.ProjectDtos.AddMemberRequest;
import de.ultimatetodo.project.api.ProjectDtos.CreateProjectRequest;
import de.ultimatetodo.project.api.ProjectDtos.MemberResponse;
import de.ultimatetodo.project.api.ProjectDtos.ProjectResponse;
import de.ultimatetodo.project.api.ProjectDtos.UpdateProjectRequest;
import de.ultimatetodo.project.application.ProjectDraft;
import de.ultimatetodo.project.application.ProjectMember;
import de.ultimatetodo.project.application.ProjectService;
import de.ultimatetodo.project.domain.Project;
import de.ultimatetodo.user.application.AppUserDetails;
import de.ultimatetodo.user.application.UserAccess;
import de.ultimatetodo.user.application.UserSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
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
 * Projekte (US-040 bis US-042) und ihre Mitgliedschaft (US-060, US-061). Genau ein Eigentümer je Projekt;
 * siehe {@link ProjectService}.
 */
@RestController
@RequestMapping("/api/v1/projects")
@Tag(name = "Projekte", description = "Projekte des angemeldeten Benutzers")
@SecurityRequirement(name = "session")
class ProjectController {

	private final ProjectService projectService;
	private final UserAccess userAccess;

	ProjectController(ProjectService projectService, UserAccess userAccess) {
		this.projectService = projectService;
		this.userAccess = userAccess;
	}

	@Operation(summary = "Projekt anlegen")
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	ResponseEntity<ProjectResponse> create(@AuthenticationPrincipal AppUserDetails principal,
			@RequestBody CreateProjectRequest request) {
		Project p = projectService.create(principal.id(), new ProjectDraft(request.name(), request.description()));
		return ResponseEntity.created(URI.create("/api/v1/projects/" + p.getId())).body(toResponse(p));
	}

	@Operation(summary = "Eigene und geteilte Projekte auflisten")
	@GetMapping
	PageResponse<ProjectResponse> list(@AuthenticationPrincipal AppUserDetails principal,
			@RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
		Pageable pageable = Paging.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
		Page<Project> result = projectService.list(principal.id(), pageable);
		List<Project> projectList = result.getContent();
		Map<UUID, UserSummary> owners = userAccess.summarize(projectList.stream().map(Project::getOwnerId).toList());
		return PageResponse.of(result, p -> toResponse(p, owners));
	}

	@Operation(summary = "Einzelnes Projekt ansehen")
	@GetMapping("/{id}")
	ProjectResponse get(@AuthenticationPrincipal AppUserDetails principal, @PathVariable UUID id) {
		return toResponse(projectService.get(principal.id(), id));
	}

	@Operation(summary = "Projekt bearbeiten", description = "Optimistisches Sperren: 409, wenn version veraltet ist.")
	@PutMapping("/{id}")
	ProjectResponse update(@AuthenticationPrincipal AppUserDetails principal, @PathVariable UUID id,
			@RequestBody UpdateProjectRequest request) {
		Project p = projectService.update(principal.id(), id, new ProjectDraft(request.name(), request.description()),
				request.version());
		return toResponse(p);
	}

	@Operation(summary = "Projekt endgültig löschen",
			description = "Aufgaben bleiben erhalten und werden zu persönlichen Aufgaben ihres Erstellers.")
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@AuthenticationPrincipal AppUserDetails principal, @PathVariable UUID id,
			@RequestParam(required = false) Long version) {
		projectService.delete(principal.id(), id, version);
	}

	@Operation(summary = "Mitglieder auflisten (US-060)",
			description = "Für Eigentümer und aktuelle Mitglieder gleichermaßen sichtbar.")
	@GetMapping("/{id}/members")
	List<MemberResponse> listMembers(@AuthenticationPrincipal AppUserDetails principal, @PathVariable UUID id) {
		return projectService.listMembers(principal.id(), id).stream().map(this::toResponse).toList();
	}

	@Operation(summary = "Mitglied aufnehmen (US-060)",
			description = "Nur der Eigentümer; über die exakte Login-E-Mail eines aktiven Kontos, keine durchsuchbare Benutzerliste.")
	@PostMapping("/{id}/members")
	@ResponseStatus(HttpStatus.CREATED)
	MemberResponse addMember(@AuthenticationPrincipal AppUserDetails principal, @PathVariable UUID id,
			@RequestBody AddMemberRequest request) {
		return toResponse(projectService.addMember(principal.id(), id, request.email()));
	}

	@Operation(summary = "Mitglied entfernen (US-061)",
			description = "Nur der Eigentümer; entfernt zugleich dessen Zuweisungen in diesem Projekt.")
	@DeleteMapping("/{id}/members/{userId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void removeMember(@AuthenticationPrincipal AppUserDetails principal, @PathVariable UUID id,
			@PathVariable UUID userId) {
		projectService.removeMember(principal.id(), id, userId);
	}

	private MemberResponse toResponse(ProjectMember member) {
		return MemberResponse.of(member.user(), member.addedAt());
	}

	private ProjectResponse toResponse(Project p) {
		return toResponse(p, userAccess.summarize(List.of(p.getOwnerId())));
	}

	private static ProjectResponse toResponse(Project p, Map<UUID, UserSummary> owners) {
		UserSummary owner = owners.get(p.getOwnerId());
		return ProjectResponse.of(p, owner == null ? null : owner.displayName());
	}

}
