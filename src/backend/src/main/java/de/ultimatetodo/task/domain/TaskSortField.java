package de.ultimatetodo.task.domain;

import java.util.Optional;

/** Erlaubte Sortierfelder der Aufgabenliste (US-052). Nicht erlaubte Werte werden abgewiesen, nicht ignoriert. */
public enum TaskSortField {

	CREATED_AT("createdAt"), UPDATED_AT("updatedAt"), DUE("due"), PRIORITY("priority"), TITLE("title");

	private final String wireName;

	TaskSortField(String wireName) {
		this.wireName = wireName;
	}

	public String wireName() { return wireName; }

	public static Optional<TaskSortField> parse(String raw) {
		if (raw == null) {
			return Optional.empty();
		}
		for (TaskSortField field : values()) {
			if (field.wireName.equals(raw)) {
				return Optional.of(field);
			}
		}
		return Optional.empty();
	}

}
