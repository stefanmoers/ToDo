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

/**
 * Alle fachlichen Lesezugriffe filtern nach Zugriffsrecht (Eigentümer oder Mitglied, US-060); siehe
 * {@code task.persistence.TaskRepository}. Ändernde/verwaltende Aktionen (umbenennen, löschen, Mitglieder
 * verwalten) bleiben dem Eigentümer vorbehalten und prüfen das zusätzlich in {@code ProjectService}.
 */
public interface ProjectRepository extends JpaRepository<Project, UUID> {

	String ACCESSIBLE_CONDITION = "(p.ownerId = :userId or p.id in (select m.projectId from Membership m where m.userId = :userId))";

	@Query("select p from Project p where p.id = :id and " + ACCESSIBLE_CONDITION)
	Optional<Project> findAccessibleById(@Param("id") UUID id, @Param("userId") UUID userId);

	@Query(value = "select p from Project p where " + ACCESSIBLE_CONDITION,
			countQuery = "select count(p) from Project p where " + ACCESSIBLE_CONDITION)
	Page<Project> findAccessible(@Param("userId") UUID userId, Pageable pageable);

	@Query("select p.id from Project p where " + ACCESSIBLE_CONDITION)
	Set<UUID> findAccessibleIds(@Param("userId") UUID userId);

}
