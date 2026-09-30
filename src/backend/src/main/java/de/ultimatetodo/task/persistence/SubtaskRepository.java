package de.ultimatetodo.task.persistence;

import de.ultimatetodo.task.domain.Subtask;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubtaskRepository extends JpaRepository<Subtask, UUID> {

	List<Subtask> findByTaskIdOrderBySortOrderAscIdAsc(UUID taskId);

	List<Subtask> findByTaskIdIn(Collection<UUID> taskIds);

	// Rechte richten sich nach der Hauptaufgabe (docs/DOMAIN_MODEL.md); der Zugriff über eine fremde task_id
	// darf daher nie etwas finden, selbst mit einer gültigen Subtask-ID einer anderen Aufgabe.
	Optional<Subtask> findByIdAndTaskId(UUID id, UUID taskId);

	@Query("select coalesce(max(s.sortOrder), -1) from Subtask s where s.taskId = :taskId")
	int findMaxSortOrder(@Param("taskId") UUID taskId);

}
