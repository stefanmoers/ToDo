package de.ultimatetodo.user.application;

import de.ultimatetodo.user.domain.UserInput;
import de.ultimatetodo.user.persistence.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AppUserDetailsService implements UserDetailsService {

	private final UserRepository users;

	public AppUserDetailsService(UserRepository users) {
		this.users = users;
	}

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		return users.findByEmail(UserInput.normalizeEmail(email))
			.map(AppUserDetails::of)
			.orElseThrow(() -> new UsernameNotFoundException("Unbekanntes Konto"));
	}

}
