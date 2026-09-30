package de.ultimatetodo.project.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Mitgliedschaft eines Benutzers in einem Projekt (US-060, US-061). Der Eigentümer ist davon unabhängig
 * immer zugriffsberechtigt und hat keine eigene Zeile hier. Eindeutig pro (projectId, userId) über einen
 * Unique-Constraint in der Datenbank (V6__membership.sql); wiederholte Aufnahme dupliziert nichts.
 */
@Entity
@Table(name = "membership")
public class Membership {

	@Id
	private UUID id;

	@Column(name = "project_id", nullable = false, updatable = false)
	private UUID projectId;

	@Column(name = "user_id", nullable = false, updatable = false)
	private UUID userId;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected Membership() {
	}

	public static Membership of(UUID projectId, UUID userId, Instant now) {
		Membership m = new Membership();
		m.id = UUID.randomUUID();
		m.projectId = projectId;
		m.userId = userId;
		m.createdAt = now;
		return m;
	}

	public UUID getId() { return id; }

	public UUID getProjectId() { return projectId; }

	public UUID getUserId() { return userId; }

	public Instant getCreatedAt() { return createdAt; }

}
