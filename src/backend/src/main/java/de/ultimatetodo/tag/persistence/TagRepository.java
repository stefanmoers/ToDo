package de.ultimatetodo.tag.persistence;

import de.ultimatetodo.tag.domain.Tag;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Alle fachlichen Lesezugriffe filtern nach Zugriffsrecht; siehe {@code task.persistence.TaskRepository}. */
public interface TagRepository extends JpaRepository<Tag, UUID> {

	@Query("select t from Tag t where t.id = :id and (t.ownerId = :userId or t.projectId in :ownedProjectIds)")
	Optional<Tag> findAccessibleById(@Param("id") UUID id, @Param("userId") UUID userId,
			@Param("ownedProjectIds") Set<UUID> ownedProjectIds);

	@Query("select t from Tag t where t.ownerId = :userId or t.projectId in :ownedProjectIds "
			+ "order by t.name")
	List<Tag> findAccessible(@Param("userId") UUID userId, @Param("ownedProjectIds") Set<UUID> ownedProjectIds);

	@Query("select count(t) from Tag t where t.id in :ids and (t.ownerId = :userId or t.projectId in :ownedProjectIds)")
	long countAccessible(@Param("ids") Collection<UUID> ids, @Param("userId") UUID userId,
			@Param("ownedProjectIds") Set<UUID> ownedProjectIds);

}
