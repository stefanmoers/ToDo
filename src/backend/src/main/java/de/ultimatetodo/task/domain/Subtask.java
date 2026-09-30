package de.ultimatetodo.task.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Unteraufgabe (US-070/071): eine einfache Checkliste innerhalb einer Aufgabe, keine weitere vollständige Aufgabe.
 * Genau eine Ebene, keine rekursiven Unteraufgaben (docs/DOMAIN_MODEL.md). Rechte richten sich nach der
 * Hauptaufgabe; {@link de.ultimatetodo.task.application.SubtaskService} prüft sie über {@code TaskService}.
 */
@Entity
@Table(name = "subtask")
public class Subtask {

	@Id
	private UUID id;

	@Column(name = "task_id", nullable = false, updatable = false)
	private UUID taskId;

	@Column(nullable = false)
	private String title;

	@Column(nullable = false)
	private boolean done;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Subtask() {
	}

	public static Subtask create(UUID taskId, String title, int sortOrder, Instant now) {
		Subtask s = new Subtask();
		s.id = UUID.randomUUID();
		s.taskId = taskId;
		s.title = title;
		s.done = false;
		s.sortOrder = sortOrder;
		s.createdAt = now;
		s.updatedAt = now;
		return s;
	}

	/** Liefert {@code true}, wenn sich der Titel dadurch tatsächlich geändert hat. */
	public boolean rename(String title, Instant now) {
		if (this.title.equals(title)) {
			return false;
		}
		this.title = title;
		this.updatedAt = now;
		return true;
	}

	/** Setzt den Erledigt-Zustand explizit (nicht nur umschaltend), rückgängig änderbar (US-071). */
	public boolean setDone(boolean done, Instant now) {
		if (this.done == done) {
			return false;
		}
		this.done = done;
		this.updatedAt = now;
		return true;
	}

	public void setSortOrder(int sortOrder, Instant now) {
		this.sortOrder = sortOrder;
		this.updatedAt = now;
	}

	public UUID getId() { return id; }

	public UUID getTaskId() { return taskId; }

	public String getTitle() { return title; }

	public boolean isDone() { return done; }

	public int getSortOrder() { return sortOrder; }

	public Instant getCreatedAt() { return createdAt; }

	public Instant getUpdatedAt() { return updatedAt; }

}
