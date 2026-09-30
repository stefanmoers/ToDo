package de.ultimatetodo.project.application;

import de.ultimatetodo.common.ApiError.FieldError;
import de.ultimatetodo.common.ApiException;
import de.ultimatetodo.project.domain.Membership;
import de.ultimatetodo.project.domain.Project;
import de.ultimatetodo.project.persistence.MembershipRepository;
import de.ultimatetodo.project.persistence.ProjectRepository;
import de.ultimatetodo.tag.application.ProjectTagSupport;
import de.ultimatetodo.task.application.TaskProjectSupport;
import de.ultimatetodo.user.application.UserAccess;
import de.ultimatetodo.user.application.UserSummary;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Anwendungsfälle des Projekt-Moduls (US-040 bis US-042, US-060, US-061). Implementiert {@link ProjectAccess},
 * die schmale Schnittstelle, über die andere Module Projekt-Eigentümerschaft/-Mitgliedschaft prüfen
 * (docs/ARCHITECTURE.md). Lesen (anzeigen, auflisten) ist Eigentümern und Mitgliedern gleichermaßen erlaubt;
 * Umbenennen, Löschen und Mitgliederverwaltung bleiben dem Eigentümer vorbehalten (docs/DOMAIN_MODEL.md).
 */
@Service
public class ProjectService implements ProjectAccess {

	private static final String CONFLICT_MESSAGE = "Das Projekt wurde zwischenzeitlich geändert. Bitte lade es neu.";
	private static final String OWNER_ONLY_MESSAGE = "Nur der Eigentümer darf diese Aktion ausführen.";

	private final ProjectRepository projects;
	private final MembershipRepository memberships;
	private final ProjectDraftValidator validator;
	private final TaskProjectSupport taskProjectSupport;
	private final ProjectTagSupport projectTagSupport;
	private final UserAccess userAccess;
	private final Clock clock;

	public ProjectService(ProjectRepository projects, MembershipRepository memberships, ProjectDraftValidator validator,
			TaskProjectSupport taskProjectSupport, ProjectTagSupport projectTagSupport, UserAccess userAccess, Clock clock) {
		this.projects = projects;
		this.memberships = memberships;
		this.validator = validator;
		this.taskProjectSupport = taskProjectSupport;
		this.projectTagSupport = projectTagSupport;
		this.userAccess = userAccess;
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
		Project p = loadOwned(userId, projectId);
		checkVersion(p, expectedVersion);
		if (p.update(v.name(), v.description(), now())) {
			flushOrConflict();
		}
		return p;
	}

	/**
	 * Löscht das Projekt endgültig. Seine Aufgaben bleiben erhalten und werden zu persönlichen Aufgaben ihres
	 * jeweiligen Erstellers (US-042); Projekt-Tags und Zuweisungen werden entfernt — alles in derselben
	 * Transaktion (US-121). Mitgliedschaften verschwinden per Fremdschlüssel-Kaskade mit der Projektzeile.
	 */
	@Transactional
	public void delete(UUID userId, UUID projectId, Long expectedVersion) {
		requireVersion(expectedVersion);
		Project p = loadOwned(userId, projectId);
		checkVersion(p, expectedVersion);
		projectTagSupport.deleteAllForProject(projectId);
		taskProjectSupport.detachFromProject(projectId);
		projects.delete(p);
		flushOrConflict();
	}

	/** US-060: sichtbar für Eigentümer und aktuelle Mitglieder, chronologisch nach Aufnahmezeitpunkt. */
	@Transactional(readOnly = true)
	public List<ProjectMember> listMembers(UUID userId, UUID projectId) {
		loadAccessible(userId, projectId);
		List<Membership> rows = memberships.findByProjectIdOrderByCreatedAtAscIdAsc(projectId);
		Map<UUID, UserSummary> byId = userAccess.summarize(rows.stream().map(Membership::getUserId).toList());
		return rows.stream().map(m -> {
			UserSummary user = byId.get(m.getUserId());
			return user == null ? null : new ProjectMember(user, m.getCreatedAt());
		}).filter(pm -> pm != null).toList();
	}

	/**
	 * US-060: nur der Eigentümer nimmt über die exakte Login-E-Mail einen aktiven Benutzer auf. Wiederholte
	 * Aufnahme dupliziert nichts (weder Anwendungs- noch Datenbankebene über den Unique-Constraint).
	 */
	@Transactional
	public ProjectMember addMember(UUID userId, UUID projectId, String rawEmail) {
		Project p = loadOwned(userId, projectId);
		UserSummary candidate = userAccess.findByEmail(rawEmail)
			.orElseThrow(() -> ApiException
				.validation(List.of(new FieldError("email", "NOT_FOUND", "Kein Konto mit dieser E-Mail-Adresse gefunden."))));
		if (!candidate.active()) {
			throw ApiException.validation(List.of(new FieldError("email", "INACTIVE", "Dieses Konto ist deaktiviert.")));
		}
		if (candidate.id().equals(p.getOwnerId())) {
			throw ApiException
				.validation(List.of(new FieldError("email", "IS_OWNER", "Der Eigentümer ist bereits automatisch Mitglied.")));
		}
		Instant addedAt = memberships.findByProjectIdAndUserId(projectId, candidate.id()).map(Membership::getCreatedAt)
			.orElseGet(() -> addMembershipRow(projectId, candidate.id()));
		return new ProjectMember(candidate, addedAt);
	}

	private Instant addMembershipRow(UUID projectId, UUID candidateId) {
		Instant now = now();
		try {
			memberships.saveAndFlush(Membership.of(projectId, candidateId, now));
			return now;
		}
		catch (DataIntegrityViolationException e) {
			// Rennen mit einer parallelen Aufnahme: die Mitgliedschaft existiert jetzt bereits, das gewünschte Ergebnis.
			return memberships.findByProjectIdAndUserId(projectId, candidateId).map(Membership::getCreatedAt).orElse(now);
		}
	}

	/**
	 * US-061: entfernt eine Mitgliedschaft; der Eigentümer kann darüber nicht entfernt werden, da er keine
	 * eigene Mitgliedschaftszeile hat. Zuweisungen dieses Benutzers in diesem Projekt werden in derselben
	 * Transaktion bereinigt; Kommentare und Aktivitäten bleiben erhalten.
	 */
	@Transactional
	public void removeMember(UUID userId, UUID projectId, UUID memberUserId) {
		Project p = loadOwned(userId, projectId);
		if (memberUserId.equals(p.getOwnerId())) {
			throw ApiException
				.validation(List.of(new FieldError("userId", "IS_OWNER", "Der Eigentümer kann nicht entfernt werden.")));
		}
		Membership m = memberships.findByProjectIdAndUserId(projectId, memberUserId)
			.orElseThrow(
					() -> new ApiException(HttpStatus.NOT_FOUND, "MEMBERSHIP_NOT_FOUND", "Diese Mitgliedschaft wurde nicht gefunden."));
		memberships.delete(m);
		taskProjectSupport.unassignUser(projectId, memberUserId);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean isOwner(UUID projectId, UUID userId) {
		if (projectId == null) {
			return false;
		}
		return projects.findById(projectId).map(p -> p.getOwnerId().equals(userId)).orElse(false);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean isAccessible(UUID projectId, UUID userId) {
		if (projectId == null) {
			return false;
		}
		return projects.findAccessibleById(projectId, userId).isPresent();
	}

	@Override
	@Transactional(readOnly = true)
	public Set<UUID> accessibleProjectIds(UUID userId) {
		return projects.findAccessibleIds(userId);
	}

	/** Eigentümer oder Mitglied; für Umbenennen/Löschen/Mitgliederverwaltung siehe {@link #loadOwned}. */
	private Project loadAccessible(UUID userId, UUID projectId) {
		return projects.findAccessibleById(projectId, userId)
			.orElseThrow(
					() -> new ApiException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "Das Projekt wurde nicht gefunden."));
	}

	/** Wie {@link #loadAccessible}, verlangt zusätzlich Eigentümerschaft (403 statt 404 für ein bekanntes Mitglied). */
	private Project loadOwned(UUID userId, UUID projectId) {
		Project p = loadAccessible(userId, projectId);
		if (!p.getOwnerId().equals(userId)) {
			throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", OWNER_ONLY_MESSAGE);
		}
		return p;
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
