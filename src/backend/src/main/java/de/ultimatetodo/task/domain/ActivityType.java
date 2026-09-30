package de.ultimatetodo.task.domain;

/** Art eines Aktivitätshistorie-Eintrags (US-081). */
public enum ActivityType {
	CREATED, TITLE_CHANGED, STATUS_CHANGED, PRIORITY_CHANGED, ASSIGNEE_CHANGED, DUE_CHANGED, PROJECT_CHANGED,
	COMPLETED, REOPENED, COMMENTED
}
