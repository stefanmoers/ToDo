package de.ultimatetodo.task.persistence;

import de.ultimatetodo.task.domain.Task;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Alle fachlichen Lesezugriffe filtern nach Zugriffsrecht ("accessible"). Das geerbte {@code findById}
 * wird von der Anwendungsschicht bewusst nie verwendet, da es die Ownership-Prüfung umgehen würde.
 * {@code ownedProjectIds} kommt aus {@code project.application.ProjectAccess} (schmale Schnittstelle statt
 * direktem Zugriff auf das Projekt-Modul); Mitgliedschaftsrechte (M2) erweitern nur diese Prüfung und
 * {@link TaskSpecifications#accessibleTo}. Die Liste (US-050 bis US-052: Suche, Filter, Sortierung) läuft über
 * {@code findAll(Specification, Pageable)} aus {@link JpaSpecificationExecutor}, da feste JPQL-Methoden die
 * beliebig kombinierbaren Filter- und Sortierkriterien nicht mehr abbilden können.
 */
public interface TaskRepository extends JpaRepository<Task, UUID>, JpaSpecificationExecutor<Task> {

	@Query("select t from Task t where t.id = :id and (t.creatorId = :userId or t.projectId in :ownedProjectIds)")
	Optional<Task> findAccessibleById(@Param("id") UUID id, @Param("userId") UUID userId,
			@Param("ownedProjectIds") Set<UUID> ownedProjectIds);

	/** Für US-042: löst Aufgaben von einem gelöschten Projekt (sie bleiben erhalten, werden aber wieder persönlich). */
	@Modifying
	@Query("update Task t set t.projectId = null where t.projectId = :projectId")
	void detachFromProject(@Param("projectId") UUID projectId);

}
