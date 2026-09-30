package de.ultimatetodo.task.application;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Rohe, noch ungeprüfte Suche/Filter/Sortierung/Seitenwahl der Aufgabenliste (US-050 bis US-052), wie sie aus den
 * Query-Parametern zusammengestellt wird. {@link TaskListQueryValidator} prüft {@code status}, {@code priority},
 * {@code sortBy} und {@code sortDirection}; {@code projectId}/{@code tagId}/{@code dueDateFrom}/{@code dueDateTo}
 * sind bereits typisiert, weil Spring ungültige Werte darin schon vor Erreichen des Dienstes als 400 abweist.
 */
public record TaskListQuery(String q, List<String> status, List<String> priority, List<UUID> projectId,
		List<UUID> tagId, List<UUID> assigneeId, Boolean overdue, LocalDate dueDateFrom, LocalDate dueDateTo,
		String sortBy, String sortDirection, Integer page, Integer size) {
}
