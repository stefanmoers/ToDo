package de.ultimatetodo.task.application;

/** Rohe, ungeprüfte Eingabe aus der API. Löst die Abhängigkeit von {@code application} zu {@code api}. */
public record TaskDraft(String projectId, String title, String description, String status, String priority,
		String dueDate, String dueTime, String dueTimeZone, String dueOffset) {
}
