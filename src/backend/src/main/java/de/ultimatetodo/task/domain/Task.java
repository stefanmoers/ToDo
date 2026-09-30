package de.ultimatetodo.task.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Persönliche Aufgabe (US-030 bis US-037). Ohne Projekt gehört sie ausschließlich ihrem Ersteller
 * (docs/DOMAIN_MODEL.md). Bewusst keine {@code @ManyToOne}-Beziehung auf {@code User}: das Modul {@code task}
 * kennt vom Modul {@code user} nur die rohe ID (docs/ARCHITECTURE.md, Modulgrenzen).
 */
@Entity
@Table(name = "task")
public class Task {

	@Id
	private UUID id;

	@Column(name = "creator_id", nullable = false, updatable = false)
	private UUID creatorId;

	@Column(name = "project_id")
	private UUID projectId;

	// Nur der Ersteller (dann gleich creatorId) oder niemand, solange projectId null ist (docs/DOMAIN_MODEL.md).
	@Column(name = "assignee_id")
	private UUID assigneeId;

	@Column(nullable = false)
	private String title;

	@Column
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TaskStatus status;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TaskPriority priority;

	@Column(name = "due_date")
	private LocalDate dueDate;

	@Column(name = "due_time")
	private LocalTime dueTime;

	@Column(name = "due_time_zone")
	private String dueTimeZone;

	@Column(name = "due_offset_seconds")
	private Integer dueOffsetSeconds;

	@Column(name = "due_deadline")
	private Instant dueDeadline;

	@Column(name = "completed_at")
	private Instant completedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	// Wrapper-Typ (nicht primitiv): null markiert eine neue, noch nicht gespeicherte Aufgabe,
	// damit Hibernate persist() statt merge() verwendet (analog zum Befund bei User.version).
	@Version
	@Column(nullable = false)
	private Long version;

	protected Task() {
	}

	public static Task create(UUID creatorId, UUID projectId, UUID assigneeId, String title, String description,
			TaskPriority priority, DueDate due, Instant now) {
		Task t = new Task();
		t.id = UUID.randomUUID();
		t.creatorId = creatorId;
		t.projectId = projectId;
		t.assigneeId = assigneeId;
		t.title = title;
		t.description = description;
		t.status = TaskStatus.OPEN;
		t.priority = priority;
		t.applyDue(due);
		t.createdAt = now;
		t.updatedAt = now;
		return t;
	}

	/**
	 * Ersetzt alle bearbeitbaren Felder. Liefert die tatsächlich geänderten Felder (US-081: Grundlage der
	 * Aktivitätshistorie); ein leeres Ergebnis bedeutet, dass sich nichts geändert hat.
	 */
	public Set<TaskChangeKind> update(UUID projectId, UUID assigneeId, String title, String description,
			TaskStatus status, TaskPriority priority, DueDate due, Instant now) {
		Set<TaskChangeKind> changes = EnumSet.noneOf(TaskChangeKind.class);
		if (!Objects.equals(this.projectId, projectId)) {
			this.projectId = projectId;
			changes.add(TaskChangeKind.PROJECT);
		}
		if (!Objects.equals(this.assigneeId, assigneeId)) {
			this.assigneeId = assigneeId;
			changes.add(TaskChangeKind.ASSIGNEE);
		}
		if (!this.title.equals(title)) {
			this.title = title;
			changes.add(TaskChangeKind.TITLE);
		}
		if (!Objects.equals(this.description, description)) {
			this.description = description;
			changes.add(TaskChangeKind.DESCRIPTION);
		}
		if (this.priority != priority) {
			this.priority = priority;
			changes.add(TaskChangeKind.PRIORITY);
		}
		if (!dueEquals(due)) {
			applyDue(due);
			changes.add(TaskChangeKind.DUE);
		}
		if (this.status != status) {
			changeStatus(status, now);
			changes.add(TaskChangeKind.STATUS);
		}
		if (!changes.isEmpty()) {
			this.updatedAt = now;
		}
		return changes;
	}

	/** Idempotent: Ist die Aufgabe bereits erledigt, ändert sich nichts (Arbeitsannahme, docs/DOMAIN_MODEL.md). */
	public boolean complete(Instant now) {
		if (status == TaskStatus.DONE) {
			return false;
		}
		changeStatus(TaskStatus.DONE, now);
		this.updatedAt = now;
		return true;
	}

	/** Wirkungslos, wenn die Aufgabe nicht erledigt ist. */
	public boolean reopen(Instant now) {
		if (status != TaskStatus.DONE) {
			return false;
		}
		changeStatus(TaskStatus.OPEN, now);
		this.updatedAt = now;
		return true;
	}

	public boolean isOverdue(Instant now) {
		return status != TaskStatus.DONE && dueDeadline != null && !now.isBefore(dueDeadline);
	}

	private void changeStatus(TaskStatus newStatus, Instant now) {
		if (newStatus == TaskStatus.DONE && this.status != TaskStatus.DONE) {
			this.completedAt = now;
		}
		else if (newStatus != TaskStatus.DONE && this.status == TaskStatus.DONE) {
			this.completedAt = null;
		}
		this.status = newStatus;
	}

	private void applyDue(DueDate due) {
		if (due == null) {
			this.dueDate = null;
			this.dueTime = null;
			this.dueTimeZone = null;
			this.dueOffsetSeconds = null;
			this.dueDeadline = null;
			return;
		}
		this.dueDate = due.date();
		this.dueTime = due.time();
		this.dueTimeZone = due.zone().getId();
		this.dueOffsetSeconds = due.time() == null ? null : due.offset().getTotalSeconds();
		this.dueDeadline = due.deadline();
	}

	private boolean dueEquals(DueDate due) {
		if (due == null) {
			return this.dueDate == null;
		}
		if (!due.date().equals(this.dueDate) || !Objects.equals(due.time(), this.dueTime)
				|| !due.zone().getId().equals(this.dueTimeZone)) {
			return false;
		}
		Integer offsetSeconds = due.time() == null ? null : due.offset().getTotalSeconds();
		return Objects.equals(offsetSeconds, this.dueOffsetSeconds);
	}

	/** Baut das Fälligkeits-Wertobjekt aus den gespeicherten Spalten; {@code null} ohne Fälligkeit. */
	public DueDate getDue() {
		if (dueDate == null) {
			return null;
		}
		ZoneId zone = ZoneId.of(dueTimeZone);
		ZoneOffset offset = dueOffsetSeconds == null ? null : ZoneOffset.ofTotalSeconds(dueOffsetSeconds);
		return new DueDate(dueDate, dueTime, zone, offset);
	}

	public UUID getId() { return id; }

	public UUID getCreatorId() { return creatorId; }

	public UUID getProjectId() { return projectId; }

	public UUID getAssigneeId() { return assigneeId; }

	public String getTitle() { return title; }

	public String getDescription() { return description; }

	public TaskStatus getStatus() { return status; }

	public TaskPriority getPriority() { return priority; }

	public Instant getCompletedAt() { return completedAt; }

	public Instant getCreatedAt() { return createdAt; }

	public Instant getUpdatedAt() { return updatedAt; }

	// Wrapper-Rückgabetyp: Spring Data liest dieses Attribut per Getter, um isNew() zu bestimmen (siehe Feldkommentar
	// bei @Version oben); ein primitiver Rückgabewert würde dabei bei einer neuen, ungespeicherten Aufgabe mit
	// einer NullPointerException beim automatischen Unboxing scheitern.
	public Long getVersion() { return version; }

}
