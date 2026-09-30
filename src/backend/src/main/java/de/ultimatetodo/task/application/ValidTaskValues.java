package de.ultimatetodo.task.application;

import de.ultimatetodo.task.domain.DueDate;
import de.ultimatetodo.task.domain.TaskPriority;
import de.ultimatetodo.task.domain.TaskStatus;
import java.util.UUID;

/** Geprüfte, normalisierte Werte einer Aufgaben-Eingabe. {@code projectId}/{@code assigneeId} sind nur formal
 * geprüft (gültige UUID); Eigentümerschaft/Zuweisbarkeit prüft {@code TaskService} über {@code ProjectAccess}. */
public record ValidTaskValues(UUID projectId, UUID assigneeId, String title, String description, TaskStatus status,
		TaskPriority priority, DueDate due) {
}
