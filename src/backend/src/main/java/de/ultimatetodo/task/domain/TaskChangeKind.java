package de.ultimatetodo.task.domain;

/** Welche Felder eine {@link Task#update} tatsächlich geändert hat (US-081: Grundlage der Aktivitätshistorie). */
public enum TaskChangeKind {
	TITLE, DESCRIPTION, STATUS, PRIORITY, DUE, ASSIGNEE, PROJECT
}
