package de.ultimatetodo.task.persistence;

import de.ultimatetodo.task.domain.Activity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityRepository extends JpaRepository<Activity, UUID> {

	List<Activity> findByTaskIdOrderByCreatedAtAscIdAsc(UUID taskId);

}
