package de.ultimatetodo.tag.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Ein Tag markiert Aufgaben unabhängig von ihrer Projektansicht (US-043). Gehört entweder persönlich einem
 * Benutzer oder einem Projekt — genau eines von beiden (docs/DOMAIN_MODEL.md). Keine Version: Umbenennen/Löschen
 * verlangt nur Zugriffsrecht, keinen Konfliktnachweis (anders als Aufgabe/Projekt nicht Teil der Backlog-Kriterien).
 */
@Entity
@Table(name = "tag")
public class Tag {

	@Id
	private UUID id;

	@Column(name = "owner_id")
	private UUID ownerId;

	@Column(name = "project_id")
	private UUID projectId;

	@Column(nullable = false)
	private String name;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected Tag() {
	}

	public static Tag personal(UUID ownerId, String name, Instant now) {
		Tag t = new Tag();
		t.id = UUID.randomUUID();
		t.ownerId = ownerId;
		t.name = name;
		t.createdAt = now;
		return t;
	}

	public static Tag forProject(UUID projectId, String name, Instant now) {
		Tag t = new Tag();
		t.id = UUID.randomUUID();
		t.projectId = projectId;
		t.name = name;
		t.createdAt = now;
		return t;
	}

	/** Liefert {@code true}, wenn sich dadurch tatsächlich etwas geändert hat. */
	public boolean rename(String name) {
		if (this.name.equals(name)) {
			return false;
		}
		this.name = name;
		return true;
	}

	public boolean isPersonal() { return ownerId != null; }

	public UUID getId() { return id; }

	public UUID getOwnerId() { return ownerId; }

	public UUID getProjectId() { return projectId; }

	public String getName() { return name; }

	public Instant getCreatedAt() { return createdAt; }

}
