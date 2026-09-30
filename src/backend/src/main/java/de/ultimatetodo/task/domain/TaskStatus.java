package de.ultimatetodo.task.domain;

import java.util.Optional;

/** Bearbeitungsstand einer Aufgabe (US-034, US-035). */
public enum TaskStatus {

	OPEN, IN_PROGRESS, DONE;

	/** Erwartet exakt einen der deklarierten Namen; andere Schreibweisen sind ungültig, nicht nur ein anderer Fall. */
	public static Optional<TaskStatus> parse(String raw) {
		if (raw == null) {
			return Optional.empty();
		}
		try {
			return Optional.of(TaskStatus.valueOf(raw));
		}
		catch (IllegalArgumentException e) {
			return Optional.empty();
		}
	}

}
