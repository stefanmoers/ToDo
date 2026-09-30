package de.ultimatetodo.tag.api;

import de.ultimatetodo.tag.api.TagDtos.CreateTagRequest;
import de.ultimatetodo.tag.api.TagDtos.TagResponse;
import de.ultimatetodo.tag.api.TagDtos.UpdateTagRequest;
import de.ultimatetodo.tag.application.TagDraft;
import de.ultimatetodo.tag.application.TagService;
import de.ultimatetodo.tag.domain.Tag;
import de.ultimatetodo.user.application.AppUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.net.URI;
import java.util.List;
import java.util.UUID;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Tags (US-043). Persönlich oder je einem eigenen Projekt zugeordnet; siehe {@link TagService}. */
@RestController
@RequestMapping("/api/v1/tags")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Tags",
		description = "Persönliche und projektbezogene Tags des angemeldeten Benutzers")
@SecurityRequirement(name = "session")
class TagController {

	private final TagService tagService;

	TagController(TagService tagService) {
		this.tagService = tagService;
	}

	@Operation(summary = "Tag anlegen")
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	ResponseEntity<TagResponse> create(@AuthenticationPrincipal AppUserDetails principal,
			@RequestBody CreateTagRequest request) {
		Tag tag = tagService.create(principal.id(), new TagDraft(request.name(), request.projectId()));
		return ResponseEntity.created(URI.create("/api/v1/tags/" + tag.getId())).body(TagResponse.of(tagService.describe(tag)));
	}

	@Operation(summary = "Eigene Tags auflisten", description = "Persönliche Tags, Tags eigener Projekte und Tags von Projekten, in denen der Benutzer Mitglied ist, "
				+ "ohne Paginierung. Projekt-Tags nennen Projekt und Eigentümer (US-181). Sortiert nach Name, dann persönlich "
				+ "vor Projekt, dann nach Projektname.")
	@GetMapping
	List<TagResponse> list(@AuthenticationPrincipal AppUserDetails principal) {
		return tagService.listWithOrigin(principal.id()).stream().map(TagResponse::of).toList();
	}

	@Operation(summary = "Tag umbenennen")
	@PutMapping("/{id}")
	TagResponse rename(@AuthenticationPrincipal AppUserDetails principal, @PathVariable UUID id,
			@RequestBody UpdateTagRequest request) {
		return TagResponse.of(tagService.describe(tagService.rename(principal.id(), id, request.name())));
	}

	@Operation(summary = "Tag löschen", description = "Entfernt nur den Tag; zugeordnete Aufgaben bleiben erhalten.")
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@AuthenticationPrincipal AppUserDetails principal, @PathVariable UUID id) {
		tagService.delete(principal.id(), id);
	}

}
