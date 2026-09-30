package de.ultimatetodo.user.application;

import de.ultimatetodo.user.domain.User;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** Angemeldeter Benutzer im Security-Kontext. Enthält die Sitzungsgeneration (authEpoch) zur Entwertung alter Sitzungen. */
public record AppUserDetails(UUID id, String email, String passwordHash, boolean active, int authEpoch, String role)
		implements UserDetails {

	public static AppUserDetails of(User u) {
		return new AppUserDetails(u.getId(), u.getEmail(), u.getPasswordHash(), u.isActive(), u.getAuthEpoch(),
				u.getRole().name());
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_" + role));
	}

	@Override
	public String getPassword() { return passwordHash; }

	@Override
	public String getUsername() { return email; }

	@Override
	public boolean isEnabled() { return active; }

}
