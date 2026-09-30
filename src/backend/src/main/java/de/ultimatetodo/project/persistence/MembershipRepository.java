package de.ultimatetodo.project.persistence;

import de.ultimatetodo.project.domain.Membership;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {

	List<Membership> findByProjectIdOrderByCreatedAtAscIdAsc(UUID projectId);

	Optional<Membership> findByProjectIdAndUserId(UUID projectId, UUID userId);

	boolean existsByProjectIdAndUserId(UUID projectId, UUID userId);

	@Query("select m.projectId from Membership m where m.userId = :userId")
	Set<UUID> findProjectIdsByUserId(@Param("userId") UUID userId);

	long deleteByProjectIdAndUserId(UUID projectId, UUID userId);

}
