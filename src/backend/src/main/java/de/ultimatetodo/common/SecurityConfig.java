package de.ultimatetodo.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

/**
 * Sicherheitsgrundlage (M0): nur der minimale Healthcheck ist öffentlich, alles andere verlangt Anmeldung.
 * Die Anmeldung selbst und der CSRF-Ablauf folgen in M1a (US-020, US-141); CSRF-Schutz bleibt aktiv.
 */
@Configuration
class SecurityConfig {

	/** Verhindert das von Spring generierte Standardkennwort; echte Konten kommen mit US-020. */
	@Bean
	UserDetailsService noUsersYet() {
		return username -> {
			throw new UsernameNotFoundException("Keine Konten vorhanden");
		};
	}

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness").permitAll()
				.anyRequest().authenticated())
			.exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
			.httpBasic(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.csrf(Customizer.withDefaults());
		return http.build();
	}

}
