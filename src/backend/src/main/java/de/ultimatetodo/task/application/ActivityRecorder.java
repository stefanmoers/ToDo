package de.ultimatetodo.task.application;

import de.ultimatetodo.task.domain.Activity;
import de.ultimatetodo.task.domain.ActivityType;
import de.ultimatetodo.task.persistence.ActivityRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Schreibt Einträge der Aktivitätshistorie (US-081). Bewusst ohne eigene Rechteprüfung und ohne Abhängigkeit
 * auf {@link TaskService}: wird ausschließlich von Anwendungsfällen aufgerufen, die die Berechtigung für die
 * betroffene Aufgabe bereits selbst geprüft haben (z. B. {@code TaskService}, {@code CommentService}) — eine
 * Rückabhängigkeit hierher würde einen zirkulären Spring-Bean-Zyklus erzeugen.
 */
@Component
class ActivityRecorder {

	private final ActivityRepository activities;

	ActivityRecorder(ActivityRepository activities) {
		this.activities = activities;
	}

	void record(UUID taskId, UUID actorId, ActivityType type, String fromValue, String toValue, Instant now) {
		activities.save(Activity.of(taskId, actorId, type, fromValue, toValue, now));
	}

}
