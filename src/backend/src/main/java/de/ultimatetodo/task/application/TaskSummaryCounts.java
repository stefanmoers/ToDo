package de.ultimatetodo.task.application;

/** Fünf Zähler der persönlichen Übersicht (US-110), mit denselben Rechte- und Zeitregeln wie die Listen. */
public record TaskSummaryCounts(long open, long dueToday, long overdue, long done, long highPriorityOpen) {
}
