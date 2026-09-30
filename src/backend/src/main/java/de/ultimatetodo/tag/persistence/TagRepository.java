package de.ultimatetodo.tag.persistence;

import de.ultimatetodo.tag.domain.Tag;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Alle fachlichen Lesezugriffe filtern nach Zugriffsrecht; siehe {@code task.persistence.TaskRepository}. */
public interface TagRepository extends JpaRepository<Tag, UUID> {

	@Query("select t from Tag t where t.id = :id and (t.ownerId = :userId or t.projectId in :accessibleProjectIds)")
	Optional<Tag> findAccessibleById(@Param("id") UUID id, @Param("userId") UUID userId,
			@Param("accessibleProjectIds") Set<UUID> accessibleProjectIds);

	@Query("select t from Tag t where t.ownerId = :userId or t.projectId in :accessibleProjectIds "
			+ "order by t.name")
	List<Tag> findAccessible(@Param("userId") UUID userId, @Param("accessibleProjectIds") Set<UUID> accessibleProjectIds);

	@Query("select count(t) from Tag t where t.id in :ids and (t.ownerId = :userId or t.projectId in :accessibleProjectIds)")
	long countAccessible(@Param("ids") Collection<UUID> ids, @Param("userId") UUID userId,
			@Param("accessibleProjectIds") Set<UUID> accessibleProjectIds);

	/** US-042: räumt Projekt-Tags vor dem Löschen eines Projekts ab (die Fremdschlüssel-Prüfung wäre sonst RESTRICT). */
	@Modifying
	@Query("delete from Tag t where t.projectId = :projectId")
	void deleteByProjectId(@Param("projectId") UUID projectId);

}
