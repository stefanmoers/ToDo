package de.ultimatetodo.project.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Ein Projekt bündelt Aufgaben (US-040 bis US-042). Vorerst genau ein Eigentümer und keine Mitgliedschaft;
 * Zusammenarbeit folgt erst mit US-060 (M2).
 */
@Entity
@Table(name = "project")
public class Project {

	@Id
	private UUID id;

	@Column(name = "owner_id", nullable = false, updatable = false)
	private UUID ownerId;

	@Column(nullable = false)
	private String name;

	@Column
	private String description;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	// Wrapper-Typ, nicht primitiv: siehe Kommentar bei Task.version (Task.java) für die Begründung.
	@Version
	@Column(nullable = false)
	private Long version;

	protected Project() {
	}

	public static Project create(UUID ownerId, String name, String description, Instant now) {
		Project p = new Project();
		p.id = UUID.randomUUID();
		p.ownerId = ownerId;
		p.name = name;
		p.description = description;
		p.createdAt = now;
		p.updatedAt = now;
		return p;
	}

	/** Liefert {@code true}, wenn sich dadurch tatsächlich etwas geändert hat. */
	public boolean update(String name, String description, Instant now) {
		boolean changed = false;
		if (!this.name.equals(name)) {
			this.name = name;
			changed = true;
		}
		if (!Objects.equals(this.description, description)) {
			this.description = description;
			changed = true;
		}
		if (changed) {
			this.updatedAt = now;
		}
		return changed;
	}

	public UUID getId() { return id; }

	public UUID getOwnerId() { return ownerId; }

	public String getName() { return name; }

	public String getDescription() { return description; }

	public Instant getCreatedAt() { return createdAt; }

	public Instant getUpdatedAt() { return updatedAt; }

	public Long getVersion() { return version; }

}
