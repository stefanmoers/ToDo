package de.ultimatetodo.user.persistence;

import de.ultimatetodo.user.domain.User;
import de.ultimatetodo.user.domain.User.Role;
import de.ultimatetodo.user.domain.User.Status;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

	Optional<User> findByEmail(String email);

	/** US-014: Schutz des letzten aktiven Administratorkontos vor Deaktivierung. */
	long countByRoleAndStatus(Role role, Status status);

}
