package de.ultimatetodo.task.application;

import de.ultimatetodo.task.domain.DueDate;
import de.ultimatetodo.task.domain.TaskPriority;
import de.ultimatetodo.task.domain.TaskStatus;

/** Geprüfte, normalisierte Werte einer Aufgaben-Eingabe. */
public record ValidTaskValues(String title, String description, TaskStatus status, TaskPriority priority,
		DueDate due) {
}
