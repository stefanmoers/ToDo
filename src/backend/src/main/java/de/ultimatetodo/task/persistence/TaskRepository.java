package de.ultimatetodo.task.persistence;

import de.ultimatetodo.task.domain.Task;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Alle fachlichen Lesezugriffe filtern nach Zugriffsrecht ("accessible"). Das geerbte {@code findById}
 * wird von der Anwendungsschicht bewusst nie verwendet, da es die Ownership-Prüfung umgehen würde.
 * Künftige Projekt-/Mitgliedschaftsrechte (M1c/M2) erweitern nur diese beiden Abfragen.
 */
public interface TaskRepository extends JpaRepository<Task, UUID> {

	@Query("select t from Task t where t.id = :id and t.creatorId = :userId")
	Optional<Task> findAccessibleById(@Param("id") UUID id, @Param("userId") UUID userId);

	@Query(value = "select t from Task t where t.creatorId = :userId",
			countQuery = "select count(t) from Task t where t.creatorId = :userId")
	Page<Task> findAccessible(@Param("userId") UUID userId, Pageable pageable);

}
