package de.ultimatetodo.task.persistence;

import de.ultimatetodo.task.domain.Task;
import de.ultimatetodo.task.domain.TaskPriority;
import de.ultimatetodo.task.domain.TaskSortField;
import de.ultimatetodo.task.domain.TaskStatus;
import de.ultimatetodo.task.domain.TaskTag;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/**
 * Bausteine der Aufgabenliste (US-050 bis US-052): jede Methode liefert einen unabhängigen Filterausschnitt oder
 * {@code null} („keine Einschränkung dieser Art"), damit {@code Specification.where(...).and(...)}-Ketten
 * beliebige Kombinationen mit UND verknüpfen können; mehrere Werte derselben Art werden bereits innerhalb einer
 * einzelnen Methode mit ODER verknüpft. {@link #orderedBy} setzt die Sortierung selbst über {@code query.orderBy}
 * statt über die von {@code Pageable} mitgelieferte {@code Sort}, weil „nach Priorität"/„nach Fälligkeit mit
 * Aufgaben ohne Fälligkeit zuletzt" (US-052) keine einfachen Spalten-Sortierungen sind.
 */
public final class TaskSpecifications {

	private TaskSpecifications() {
	}

	public static Specification<Task> accessibleTo(UUID userId, Set<UUID> ownedProjectIds) {
		return (root, query, cb) -> cb.or(cb.equal(root.get("creatorId"), userId),
				ownedProjectIds.isEmpty() ? cb.disjunction() : root.get("projectId").in(ownedProjectIds));
	}

	public static Specification<Task> hasSearchText(String q) {
		if (q == null || q.isBlank()) {
			return null;
		}
		String pattern = "%" + q.strip().toLowerCase() + "%";
		return (root, query, cb) -> cb.or(cb.like(cb.lower(root.<String>get("title")), pattern),
				cb.like(cb.lower(cb.coalesce(root.<String>get("description"), "")), pattern));
	}

	public static Specification<Task> hasStatus(Set<TaskStatus> statuses) {
		if (statuses == null || statuses.isEmpty()) {
			return null;
		}
		return (root, query, cb) -> root.get("status").in(statuses);
	}

	public static Specification<Task> hasPriority(Set<TaskPriority> priorities) {
		if (priorities == null || priorities.isEmpty()) {
			return null;
		}
		return (root, query, cb) -> root.get("priority").in(priorities);
	}

	public static Specification<Task> hasProject(Set<UUID> projectIds) {
		if (projectIds == null || projectIds.isEmpty()) {
			return null;
		}
		return (root, query, cb) -> root.get("projectId").in(projectIds);
	}

	/** Subquery statt Join: eine Aufgabe kann mehrere Tags haben, ohne dass das die Trefferzeilen vervielfacht. */
	public static Specification<Task> hasTag(Set<UUID> tagIds) {
		if (tagIds == null || tagIds.isEmpty()) {
			return null;
		}
		return (root, query, cb) -> {
			Subquery<UUID> sub = query.subquery(UUID.class);
			Root<TaskTag> taskTag = sub.from(TaskTag.class);
			sub.select(taskTag.get("taskId")).where(taskTag.get("tagId").in(tagIds));
			return root.get("id").in(sub);
		};
	}

	/** Überfällig ab genau dem Fälligkeitszeitpunkt; erledigte Aufgaben sind nie überfällig (docs/DOMAIN_MODEL.md). */
	public static Specification<Task> isOverdue(Instant now) {
		return (root, query, cb) -> cb.and(cb.notEqual(root.get("status"), TaskStatus.DONE),
				cb.isNotNull(root.get("dueDeadline")), cb.lessThanOrEqualTo(root.<Instant>get("dueDeadline"), now));
	}

	/**
	 * Vergleicht das rohe Fälligkeitsdatum, nicht den (bereits um einen Tag verschobenen) Überfällig-Zeitpunkt;
	 * beide Grenzen inklusive.
	 */
	public static Specification<Task> dueDateBetween(LocalDate from, LocalDate to) {
		if (from == null && to == null) {
			return null;
		}
		return (root, query, cb) -> {
			Expression<LocalDate> dueDate = root.get("dueDate");
			if (from != null && to != null) {
				return cb.between(dueDate, from, to);
			}
			return from != null ? cb.greaterThanOrEqualTo(dueDate, from) : cb.lessThanOrEqualTo(dueDate, to);
		};
	}

	/**
	 * Setzt die Sortierung direkt auf der {@code CriteriaQuery}, nicht über {@code Pageable}; wirkt sich deshalb
	 * gezielt nur auf die eigentliche Inhaltsabfrage aus, nicht auf die von Spring Data intern gebildete
	 * Zählabfrage (deren Ergebnistyp {@code Long} ist und die keine ORDER BY-Klausel vertragen würde).
	 */
	public static Specification<Task> orderedBy(TaskSortField field, boolean ascending) {
		return (root, query, cb) -> {
			if (!Long.class.equals(query.getResultType()) && !long.class.equals(query.getResultType())) {
				query.orderBy(buildOrders(root, cb, field, ascending));
			}
			return cb.conjunction();
		};
	}

	private static List<Order> buildOrders(Root<Task> root, CriteriaBuilder cb, TaskSortField field,
			boolean ascending) {
		List<Order> orders = new ArrayList<>();
		switch (field) {
			case CREATED_AT -> orders.add(order(cb, root.get("createdAt"), ascending));
			case UPDATED_AT -> orders.add(order(cb, root.get("updatedAt"), ascending));
			case TITLE -> orders.add(order(cb, cb.lower(root.<String>get("title")), ascending));
			case DUE -> {
				// Aufgaben ohne Fälligkeit stehen unabhängig von der Richtung immer zuletzt (US-052).
				Expression<Integer> hasNoDue = cb.<Integer>selectCase().when(cb.isNull(root.get("dueDeadline")), 1)
					.otherwise(0);
				orders.add(cb.asc(hasNoDue));
				orders.add(order(cb, root.get("dueDeadline"), ascending));
			}
			case PRIORITY -> {
				Expression<Integer> rank = cb.<Integer>selectCase()
					.when(cb.equal(root.get("priority"), TaskPriority.LOW), 0)
					.when(cb.equal(root.get("priority"), TaskPriority.NORMAL), 1)
					.when(cb.equal(root.get("priority"), TaskPriority.HIGH), 2)
					.otherwise(3);
				orders.add(order(cb, rank, ascending));
			}
		}
		orders.add(order(cb, root.get("id"), ascending));
		return orders;
	}

	private static Order order(CriteriaBuilder cb, Expression<?> expression, boolean ascending) {
		return ascending ? cb.asc(expression) : cb.desc(expression);
	}

}
