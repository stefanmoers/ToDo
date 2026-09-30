package de.ultimatetodo.task.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Kommentar an einer Aufgabe (US-080). Gleiche Leseberechtigung wie die Aufgabe; der Inhalt wird als reiner
 * Text behandelt (kein HTML-Rendering). Bearbeiten/Löschen ist zunächst nicht vorgesehen (docs/DOMAIN_MODEL.md).
 */
@Entity
@Table(name = "comment")
public class Comment {

	@Id
	private UUID id;

	@Column(name = "task_id", nullable = false, updatable = false)
	private UUID taskId;

	@Column(name = "author_id", nullable = false, updatable = false)
	private UUID authorId;

	@Column(nullable = false, updatable = false)
	private String content;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected Comment() {
	}

	public static Comment create(UUID taskId, UUID authorId, String content, Instant now) {
		Comment c = new Comment();
		c.id = UUID.randomUUID();
		c.taskId = taskId;
		c.authorId = authorId;
		c.content = content;
		c.createdAt = now;
		return c;
	}

	public UUID getId() { return id; }

	public UUID getTaskId() { return taskId; }

	public UUID getAuthorId() { return authorId; }

	public String getContent() { return content; }

	public Instant getCreatedAt() { return createdAt; }

}
