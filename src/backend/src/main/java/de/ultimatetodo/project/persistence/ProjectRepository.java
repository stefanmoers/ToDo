package de.ultimatetodo.project.persistence;

import de.ultimatetodo.project.domain.Project;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Alle fachlichen Lesezugriffe filtern nach Zugriffsrecht; siehe {@code task.persistence.TaskRepository}. */
public interface ProjectRepository extends JpaRepository<Project, UUID> {

	@Query("select p from Project p where p.id = :id and p.ownerId = :userId")
	Optional<Project> findAccessibleById(@Param("id") UUID id, @Param("userId") UUID userId);

	@Query(value = "select p from Project p where p.ownerId = :userId",
			countQuery = "select count(p) from Project p where p.ownerId = :userId")
	Page<Project> findAccessible(@Param("userId") UUID userId, Pageable pageable);

	@Query("select p.id from Project p where p.ownerId = :userId")
	Set<UUID> findIdsByOwner(@Param("userId") UUID userId);

}
