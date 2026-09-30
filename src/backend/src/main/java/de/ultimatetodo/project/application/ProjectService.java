package de.ultimatetodo.project.application;

import de.ultimatetodo.common.ApiError.FieldError;
import de.ultimatetodo.common.ApiException;
import de.ultimatetodo.project.domain.Project;
import de.ultimatetodo.project.persistence.ProjectRepository;
import de.ultimatetodo.task.application.TaskProjectSupport;
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

/**
 * Anwendungsfälle des Projekt-Moduls (US-040 bis US-042). Implementiert {@link ProjectAccess}, die schmale
 * Schnittstelle, über die das Aufgaben-Modul Projekt-Eigentümerschaft prüft (docs/ARCHITECTURE.md).
 */
@Service
public class ProjectService implements ProjectAccess {

	private static final String CONFLICT_MESSAGE = "Das Projekt wurde zwischenzeitlich geändert. Bitte lade es neu.";

	private final ProjectRepository projects;
	private final ProjectDraftValidator validator;
	private final TaskProjectSupport taskProjectSupport;
	private final Clock clock;

	public ProjectService(ProjectRepository projects, ProjectDraftValidator validator,
			TaskProjectSupport taskProjectSupport, Clock clock) {
		this.projects = projects;
		this.validator = validator;
		this.taskProjectSupport = taskProjectSupport;
		this.clock = clock;
	}

	@Transactional
	public Project create(UUID ownerId, ProjectDraft draft) {
		ValidProjectValues v = validator.validate(draft);
		Project p = Project.create(ownerId, v.name(), v.description(), now());
		return projects.saveAndFlush(p);
	}

	@Transactional(readOnly = true)
	public Project get(UUID userId, UUID projectId) {
		return loadAccessible(userId, projectId);
	}

	@Transactional(readOnly = true)
	public Page<Project> list(UUID userId, Pageable pageable) {
		return projects.findAccessible(userId, pageable);
	}

	@Transactional
	public Project update(UUID userId, UUID projectId, ProjectDraft draft, Long expectedVersion) {
		requireVersion(expectedVersion);
		ValidProjectValues v = validator.validate(draft);
		Project p = loadAccessible(userId, projectId);
		checkVersion(p, expectedVersion);
		if (p.update(v.name(), v.description(), now())) {
			flushOrConflict();
		}
		return p;
	}

	/**
	 * Löscht das Projekt endgültig. Seine Aufgaben bleiben erhalten und werden zu persönlichen Aufgaben ihres
	 * jeweiligen Erstellers (US-042) — beides in derselben Transaktion (US-121).
	 */
	@Transactional
	public void delete(UUID userId, UUID projectId, Long expectedVersion) {
		requireVersion(expectedVersion);
		Project p = loadAccessible(userId, projectId);
		checkVersion(p, expectedVersion);
		taskProjectSupport.detachFromProject(projectId);
		projects.delete(p);
		flushOrConflict();
	}

	@Override
	@Transactional(readOnly = true)
	public boolean isOwner(UUID projectId, UUID userId) {
		if (projectId == null) {
			return false;
		}
		return projects.findAccessibleById(projectId, userId).isPresent();
	}

	@Override
	@Transactional(readOnly = true)
	public Set<UUID> ownedProjectIds(UUID userId) {
		return projects.findIdsByOwner(userId);
	}

	private Project loadAccessible(UUID userId, UUID projectId) {
		return projects.findAccessibleById(projectId, userId)
			.orElseThrow(
					() -> new ApiException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "Das Projekt wurde nicht gefunden."));
	}

	private void requireVersion(Long expectedVersion) {
		if (expectedVersion == null) {
			throw ApiException
				.validation(List.of(new FieldError("version", "REQUIRED", "Bitte gib die erwartete Version an.")));
		}
	}

	private void checkVersion(Project p, long expectedVersion) {
		if (p.getVersion() != expectedVersion) {
			throw new ApiException(HttpStatus.CONFLICT, "VERSION_CONFLICT", CONFLICT_MESSAGE);
		}
	}

	private void flushOrConflict() {
		try {
			projects.flush();
		}
		catch (ObjectOptimisticLockingFailureException e) {
			throw new ApiException(HttpStatus.CONFLICT, "VERSION_CONFLICT", CONFLICT_MESSAGE);
		}
	}

	private Instant now() {
		return clock.instant().truncatedTo(ChronoUnit.MICROS);
	}

}
