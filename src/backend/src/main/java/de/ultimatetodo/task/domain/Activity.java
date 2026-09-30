package de.ultimatetodo.task.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Ein Eintrag der Aktivitätshistorie einer Aufgabe (US-081). Wird zusammen mit der jeweiligen Fachänderung
 * gespeichert und ist danach nie durch Benutzer editierbar. {@link #fromValue}/{@link #toValue} sind bereits
 * anzeigefertiger, geheimnisfreier Text (z. B. Titel, Status-/Prioritätsname, Bearbeiter-/Projektname zum
 * Zeitpunkt der Änderung) statt roher IDs, damit die Historie auch nach einer späteren Umbenennung oder
 * Entfernung des referenzierten Datensatzes stabil und lesbar bleibt.
 */
@Entity
@Table(name = "activity")
public class Activity {

	@Id
	private UUID id;

	@Column(name = "task_id", nullable = false, updatable = false)
	private UUID taskId;

	@Column(name = "actor_id", nullable = false, updatable = false)
	private UUID actorId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false)
	private ActivityType type;

	@Column(name = "from_value", updatable = false)
	private String fromValue;

	@Column(name = "to_value", updatable = false)
	private String toValue;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected Activity() {
	}

	public static Activity of(UUID taskId, UUID actorId, ActivityType type, String fromValue, String toValue,
			Instant now) {
		Activity a = new Activity();
		a.id = UUID.randomUUID();
		a.taskId = taskId;
		a.actorId = actorId;
		a.type = type;
		a.fromValue = fromValue;
		a.toValue = toValue;
		a.createdAt = now;
		return a;
	}

	public UUID getId() { return id; }

	public UUID getTaskId() { return taskId; }

	public UUID getActorId() { return actorId; }

	public ActivityType getType() { return type; }

	public String getFromValue() { return fromValue; }

	public String getToValue() { return toValue; }

	public Instant getCreatedAt() { return createdAt; }

}
