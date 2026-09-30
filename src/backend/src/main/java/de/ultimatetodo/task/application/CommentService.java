package de.ultimatetodo.task.application;

import de.ultimatetodo.common.ApiError.FieldError;
import de.ultimatetodo.common.ApiException;
import de.ultimatetodo.task.domain.ActivityType;
import de.ultimatetodo.task.domain.Comment;
import de.ultimatetodo.task.persistence.CommentRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kommentare an Aufgaben (US-080). Rechte prüft ausschließlich {@link TaskService#loadAccessible} — dieselbe
 * Regel wie für die Aufgabe selbst ("Zugriff entspricht der Aufgabe", docs/DOMAIN_MODEL.md).
 */
@Service
public class CommentService {

	private static final int MAX_CONTENT = 5000;
	private static final String CONTENT_INVALID_MESSAGE = "Bitte gib einen Text zwischen 1 und 5.000 Zeichen an.";

	private final CommentRepository comments;
	private final TaskService taskService;
	private final ActivityRecorder activityRecorder;
	private final Clock clock;

	public CommentService(CommentRepository comments, TaskService taskService, ActivityRecorder activityRecorder,
			Clock clock) {
		this.comments = comments;
		this.taskService = taskService;
		this.activityRecorder = activityRecorder;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public List<Comment> list(UUID userId, UUID taskId) {
		taskService.loadAccessible(userId, taskId);
		return comments.findByTaskIdOrderByCreatedAtAscIdAsc(taskId);
	}

	/** Kommentar und zugehörige Aktivität entstehen atomar (US-080), in derselben Transaktion. */
	@Transactional
	public Comment create(UUID userId, UUID taskId, String rawContent) {
		taskService.loadAccessible(userId, taskId);
		String content = validContent(rawContent);
		Instant now = now();
		Comment comment = comments.save(Comment.create(taskId, userId, content, now));
		activityRecorder.record(taskId, userId, ActivityType.COMMENTED, null, null, now);
		return comment;
	}

	private static String validContent(String rawContent) {
		String content = rawContent == null ? "" : rawContent.strip();
		int length = content.codePointCount(0, content.length());
		if (length < 1 || length > MAX_CONTENT) {
			throw ApiException.validation(List.of(new FieldError("content", "INVALID", CONTENT_INVALID_MESSAGE)));
		}
		return content;
	}

	private Instant now() {
		return clock.instant().truncatedTo(ChronoUnit.MICROS);
	}

}
