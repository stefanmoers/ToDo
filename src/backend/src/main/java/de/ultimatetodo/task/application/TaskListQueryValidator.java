package de.ultimatetodo.task.application;

import de.ultimatetodo.common.ApiError.FieldError;
import de.ultimatetodo.common.ApiException;
import de.ultimatetodo.task.domain.TaskPriority;
import de.ultimatetodo.task.domain.TaskSortField;
import de.ultimatetodo.task.domain.TaskStatus;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Prüft die Sortier- und Filterfelder der Aufgabenliste (US-051, US-052). Nicht erlaubte Sortierfelder oder
 * unbekannte Status-/Prioritätswerte werden abgewiesen (400), nicht stillschweigend ignoriert.
 */
@Component
public class TaskListQueryValidator {

	public record Result(Set<TaskStatus> statuses, Set<TaskPriority> priorities, TaskSortField sortField,
			boolean ascending) {
	}

	public Result validate(TaskListQuery query) {
		List<FieldError> errors = new ArrayList<>();

		Set<TaskStatus> statuses = new LinkedHashSet<>();
		for (String raw : orEmpty(query.status())) {
			Optional<TaskStatus> parsed = TaskStatus.parse(raw);
			if (parsed.isEmpty()) {
				errors.add(new FieldError("status", "INVALID", "Ungültiger Status. Erlaubt sind OPEN, IN_PROGRESS, DONE."));
			}
			else {
				statuses.add(parsed.get());
			}
		}

		Set<TaskPriority> priorities = new LinkedHashSet<>();
		for (String raw : orEmpty(query.priority())) {
			Optional<TaskPriority> parsed = TaskPriority.parse(raw);
			if (parsed.isEmpty()) {
				errors.add(
						new FieldError("priority", "INVALID", "Ungültige Priorität. Erlaubt sind LOW, NORMAL, HIGH, URGENT."));
			}
			else {
				priorities.add(parsed.get());
			}
		}

		TaskSortField sortField = TaskSortField.CREATED_AT;
		if (query.sortBy() != null && !query.sortBy().isBlank()) {
			Optional<TaskSortField> parsed = TaskSortField.parse(query.sortBy());
			if (parsed.isEmpty()) {
				errors.add(new FieldError("sortBy", "INVALID",
						"Ungültiges Sortierfeld. Erlaubt sind createdAt, updatedAt, due, priority, title."));
			}
			else {
				sortField = parsed.get();
			}
		}

		// Standard wie bisher fest verdrahtet: neueste zuerst.
		boolean ascending = false;
		if (query.sortDirection() != null && !query.sortDirection().isBlank()) {
			if (query.sortDirection().equals("asc")) {
				ascending = true;
			}
			else if (query.sortDirection().equals("desc")) {
				ascending = false;
			}
			else {
				errors.add(new FieldError("sortDirection", "INVALID", "Ungültige Sortierrichtung. Erlaubt sind asc, desc."));
			}
		}

		if (!errors.isEmpty()) {
			throw ApiException.validation(errors);
		}
		return new Result(statuses, priorities, sortField, ascending);
	}

	private static List<String> orEmpty(List<String> list) {
		return list == null ? List.of() : list;
	}

}
