package de.ultimatetodo.task.domain;

import java.time.ZoneOffset;
import java.util.List;

/** Ungültige Fälligkeitsangabe (US-037); die Anwendungsschicht übersetzt dies in Feldfehler. */
public class InvalidDueDateException extends RuntimeException {

	public enum Problem {
		TIME_REQUIRES_DATE, TIME_NONEXISTENT, OFFSET_REQUIRED, OFFSET_INVALID, OFFSET_WITHOUT_TIME
	}

	private final Problem problem;
	private final List<ZoneOffset> validOffsets;

	public InvalidDueDateException(Problem problem) {
		this(problem, List.of());
	}

	public InvalidDueDateException(Problem problem, List<ZoneOffset> validOffsets) {
		super(problem.name());
		this.problem = problem;
		this.validOffsets = validOffsets;
	}

	public Problem problem() { return problem; }

	public List<ZoneOffset> validOffsets() { return validOffsets; }

}
