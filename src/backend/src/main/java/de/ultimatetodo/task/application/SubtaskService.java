package de.ultimatetodo.task.application;

import de.ultimatetodo.common.ApiError.FieldError;
import de.ultimatetodo.common.ApiException;
import de.ultimatetodo.task.domain.Subtask;
import de.ultimatetodo.task.domain.TaskInput;
import de.ultimatetodo.task.persistence.SubtaskRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Anwendungsfälle der Unteraufgaben (US-070/071). Keine eigene Rechteprüfung: jede Operation lädt zuerst die
 * Hauptaufgabe über {@link TaskService#loadAccessible} (docs/DOMAIN_MODEL.md, "Rechte werden über die
 * Hauptaufgabe geprüft") und wirft dabei 404, falls sie dem Handelnden nicht zugänglich ist.
 */
@Service
public class SubtaskService {

	private static final String TITLE_REQUIRED_MESSAGE = "Bitte gib einen Titel zwischen 1 und 200 Zeichen an.";

	private final SubtaskRepository subtasks;
	private final TaskService taskService;
	private final Clock clock;

	public SubtaskService(SubtaskRepository subtasks, TaskService taskService, Clock clock) {
		this.subtasks = subtasks;
		this.taskService = taskService;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public List<Subtask> list(UUID userId, UUID taskId) {
		taskService.loadAccessible(userId, taskId);
		return subtasks.findByTaskIdOrderBySortOrderAscIdAsc(taskId);
	}

	/** Fortschritt mehrerer Aufgaben in einer Abfrage statt einer je Aufgabe (Listen-/Übersichtsanzeige). */
	@Transactional(readOnly = true)
	public Map<UUID, List<Subtask>> forTasks(Collection<UUID> taskIds) {
		if (taskIds.isEmpty()) {
			return Map.of();
		}
		return subtasks.findByTaskIdIn(taskIds).stream().collect(Collectors.groupingBy(Subtask::getTaskId));
	}

	@Transactional
	public List<Subtask> create(UUID userId, UUID taskId, String rawTitle) {
		taskService.loadAccessible(userId, taskId);
		String title = validTitle(rawTitle);
		int nextOrder = subtasks.findMaxSortOrder(taskId) + 1;
		subtasks.save(Subtask.create(taskId, title, nextOrder, now()));
		return orderedList(taskId);
	}

	@Transactional
	public List<Subtask> rename(UUID userId, UUID taskId, UUID subtaskId, String rawTitle) {
		taskService.loadAccessible(userId, taskId);
		Subtask subtask = load(taskId, subtaskId);
		subtask.rename(validTitle(rawTitle), now());
		return orderedList(taskId);
	}

	@Transactional
	public List<Subtask> setDone(UUID userId, UUID taskId, UUID subtaskId, boolean done) {
		taskService.loadAccessible(userId, taskId);
		Subtask subtask = load(taskId, subtaskId);
		subtask.setDone(done, now());
		return orderedList(taskId);
	}

	@Transactional
	public List<Subtask> delete(UUID userId, UUID taskId, UUID subtaskId) {
		taskService.loadAccessible(userId, taskId);
		Subtask subtask = load(taskId, subtaskId);
		subtasks.delete(subtask);
		return orderedList(taskId);
	}

	/**
	 * US-070: Reihenfolge ist auch ohne Ziehbewegung bedienbar. Vertauscht die Position mit dem Vorgänger/
	 * Nachfolger in der aktuellen Liste; am jeweiligen Rand wirkungslos.
	 */
	@Transactional
	public List<Subtask> move(UUID userId, UUID taskId, UUID subtaskId, boolean up) {
		taskService.loadAccessible(userId, taskId);
		List<Subtask> ordered = subtasks.findByTaskIdOrderBySortOrderAscIdAsc(taskId);
		int index = indexOf(ordered, subtaskId);
		int neighborIndex = up ? index - 1 : index + 1;
		if (neighborIndex >= 0 && neighborIndex < ordered.size()) {
			Subtask subtask = ordered.get(index);
			Subtask neighbor = ordered.get(neighborIndex);
			int subtaskOrder = subtask.getSortOrder();
			int neighborOrder = neighbor.getSortOrder();
			Instant now = now();
			subtask.setSortOrder(neighborOrder, now);
			neighbor.setSortOrder(subtaskOrder, now);
		}
		return orderedList(taskId);
	}

	private int indexOf(List<Subtask> ordered, UUID subtaskId) {
		for (int i = 0; i < ordered.size(); i++) {
			if (ordered.get(i).getId().equals(subtaskId)) {
				return i;
			}
		}
		throw notFound();
	}

	private Subtask load(UUID taskId, UUID subtaskId) {
		return subtasks.findByIdAndTaskId(subtaskId, taskId).orElseThrow(this::notFound);
	}

	private List<Subtask> orderedList(UUID taskId) {
		return subtasks.findByTaskIdOrderBySortOrderAscIdAsc(taskId);
	}

	private static String validTitle(String rawTitle) {
		String title = TaskInput.normalizeTitle(rawTitle);
		if (!TaskInput.isValidTitle(title)) {
			throw ApiException.validation(List.of(new FieldError("title", "INVALID", TITLE_REQUIRED_MESSAGE)));
		}
		return title;
	}

	private ApiException notFound() {
		return new ApiException(HttpStatus.NOT_FOUND, "SUBTASK_NOT_FOUND", "Die Unteraufgabe wurde nicht gefunden.");
	}

	private Instant now() {
		return clock.instant().truncatedTo(ChronoUnit.MICROS);
	}

}
