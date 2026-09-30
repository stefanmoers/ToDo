package de.ultimatetodo.task.domain;

import java.util.Optional;

/** Priorität einer Aufgabe (US-036) in fachlicher Rangfolge LOW &lt; NORMAL &lt; HIGH &lt; URGENT. */
public enum TaskPriority {

	LOW, NORMAL, HIGH, URGENT;

	/** Erwartet exakt einen der deklarierten Namen; andere Schreibweisen sind ungültig, nicht nur ein anderer Fall. */
	public static Optional<TaskPriority> parse(String raw) {
		if (raw == null) {
			return Optional.empty();
		}
		try {
			return Optional.of(TaskPriority.valueOf(raw));
		}
		catch (IllegalArgumentException e) {
			return Optional.empty();
		}
	}

	/** Für spätere Sortierung nach Priorität (M1c); steigt streng mit der Deklarationsreihenfolge. */
	public int rank() {
		return ordinal();
	}

}
