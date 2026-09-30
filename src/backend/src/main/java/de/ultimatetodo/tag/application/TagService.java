package de.ultimatetodo.tag.application;

import de.ultimatetodo.common.ApiError.FieldError;
import de.ultimatetodo.common.ApiException;
import de.ultimatetodo.project.application.ProjectAccess;
import de.ultimatetodo.tag.domain.Tag;
import de.ultimatetodo.tag.persistence.TagRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Anwendungsfälle des Tag-Moduls (US-043). Implementiert {@link TagAccess}, die schmale Schnittstelle, über die
 * das Aufgaben-Modul Tag-Zugehörigkeit prüft und Namen anzeigt (docs/ARCHITECTURE.md).
 */
@Service
public class TagService implements TagAccess {

	private static final String CONFLICT_MESSAGE = "Ein Tag mit diesem Namen existiert in diesem Geltungsbereich bereits.";

	private final TagRepository tags;
	private final TagDraftValidator validator;
	private final ProjectAccess projectAccess;
	private final Clock clock;

	public TagService(TagRepository tags, TagDraftValidator validator, ProjectAccess projectAccess, Clock clock) {
		this.tags = tags;
		this.validator = validator;
		this.projectAccess = projectAccess;
		this.clock = clock;
	}

	@Transactional
	public Tag create(UUID userId, TagDraft draft) {
		ValidTagValues v = validator.validate(draft);
		Tag tag;
		if (v.projectId() != null) {
			if (!projectAccess.isOwner(v.projectId(), userId)) {
				throw ApiException.validation(
						List.of(new FieldError("projectId", "INVALID", "Dieses Projekt existiert nicht oder gehört dir nicht.")));
			}
			tag = Tag.forProject(v.projectId(), v.name(), now());
		}
		else {
			tag = Tag.personal(userId, v.name(), now());
		}
		try {
			return tags.saveAndFlush(tag);
		}
		catch (DataIntegrityViolationException e) {
			throw new ApiException(HttpStatus.CONFLICT, "TAG_ALREADY_EXISTS", CONFLICT_MESSAGE,
					List.of(new FieldError("name", "TAKEN", CONFLICT_MESSAGE)), null);
		}
	}

	@Transactional(readOnly = true)
	public List<Tag> list(UUID userId) {
		return tags.findAccessible(userId, accessibleProjectIds(userId));
	}

	@Transactional
	public Tag rename(UUID userId, UUID tagId, String rawName) {
		ValidTagValues v = validator.validate(new TagDraft(rawName, null));
		Tag tag = loadAccessible(userId, tagId);
		if (tag.rename(v.name())) {
			try {
				tags.flush();
			}
			catch (DataIntegrityViolationException e) {
				throw new ApiException(HttpStatus.CONFLICT, "TAG_ALREADY_EXISTS", CONFLICT_MESSAGE,
						List.of(new FieldError("name", "TAKEN", CONFLICT_MESSAGE)), null);
			}
		}
		return tag;
	}

	/** Entfernt nur den Tag; bestehende Aufgaben bleiben erhalten (nur ihre Zuordnung zu diesem Tag verschwindet). */
	@Transactional
	public void delete(UUID userId, UUID tagId) {
		Tag tag = loadAccessible(userId, tagId);
		tags.delete(tag);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean areAccessible(Collection<UUID> tagIds, UUID userId) {
		Set<UUID> distinct = new HashSet<>(tagIds);
		if (distinct.isEmpty()) {
			return true;
		}
		return tags.countAccessible(distinct, userId, accessibleProjectIds(userId)) == distinct.size();
	}

	@Override
	@Transactional(readOnly = true)
	public List<TagSummary> summarize(Collection<UUID> tagIds) {
		return tags.findAllById(tagIds).stream().map(t -> new TagSummary(t.getId(), t.getName())).toList();
	}

	private Tag loadAccessible(UUID userId, UUID tagId) {
		return tags.findAccessibleById(tagId, userId, accessibleProjectIds(userId))
			.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TAG_NOT_FOUND", "Der Tag wurde nicht gefunden."));
	}

	private Set<UUID> accessibleProjectIds(UUID userId) {
		return projectAccess.accessibleProjectIds(userId);
	}

	private Instant now() {
		return clock.instant().truncatedTo(ChronoUnit.MICROS);
	}

}
