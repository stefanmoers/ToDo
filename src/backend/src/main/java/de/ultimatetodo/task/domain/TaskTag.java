package de.ultimatetodo.task.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Zuordnung einer Aufgabe zu einem Tag (US-043). Gehört bewusst zum Aufgaben-Modul, nicht zum Tag-Modul: das
 * Tag-Modul kennt nur seine eigenen Tags, nicht deren Verwendung (docs/ARCHITECTURE.md, Modulgrenzen). Löschen
 * einer Aufgabe (FK CASCADE) oder eines Tags (FK CASCADE, im Tag-Modul ausgelöst) entfernt nur diese Zuordnung.
 */
@Entity
@Table(name = "task_tag")
public class TaskTag {

	@Id
	private UUID id;

	@Column(name = "task_id", nullable = false, updatable = false)
	private UUID taskId;

	@Column(name = "tag_id", nullable = false, updatable = false)
	private UUID tagId;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected TaskTag() {
	}

	public static TaskTag of(UUID taskId, UUID tagId, Instant now) {
		TaskTag t = new TaskTag();
		t.id = UUID.randomUUID();
		t.taskId = taskId;
		t.tagId = tagId;
		t.createdAt = now;
		return t;
	}

	public UUID getId() { return id; }

	public UUID getTaskId() { return taskId; }

	public UUID getTagId() { return tagId; }

	public Instant getCreatedAt() { return createdAt; }

}
