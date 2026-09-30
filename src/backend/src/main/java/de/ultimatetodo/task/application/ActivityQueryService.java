package de.ultimatetodo.task.application;

import de.ultimatetodo.task.domain.Activity;
import de.ultimatetodo.task.persistence.ActivityRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Liest die Aktivitätshistorie einer Aufgabe (US-081). Zugriff entspricht der Aufgabe selbst. */
@Service
public class ActivityQueryService {

	private final ActivityRepository activities;
	private final TaskService taskService;

	public ActivityQueryService(ActivityRepository activities, TaskService taskService) {
		this.activities = activities;
		this.taskService = taskService;
	}

	@Transactional(readOnly = true)
	public List<Activity> list(UUID userId, UUID taskId) {
		taskService.loadAccessible(userId, taskId);
		return activities.findByTaskIdOrderByCreatedAtAscIdAsc(taskId);
	}

}
