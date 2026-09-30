package de.ultimatetodo.task.persistence;

import de.ultimatetodo.task.domain.TaskTag;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskTagRepository extends JpaRepository<TaskTag, UUID> {

	List<TaskTag> findByTaskId(UUID taskId);

	List<TaskTag> findByTaskIdIn(Collection<UUID> taskIds);

	@Modifying
	@Query("delete from TaskTag t where t.taskId = :taskId")
	void deleteByTaskId(@Param("taskId") UUID taskId);

}
