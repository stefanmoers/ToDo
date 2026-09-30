package de.ultimatetodo.user.application;

import de.ultimatetodo.user.domain.User;
import de.ultimatetodo.user.domain.UserInput;
import de.ultimatetodo.user.persistence.UserRepository;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * US-014: sichere erstmalige Admin-Einrichtung. Kein öffentlicher Bootstrap-HTTP-Endpunkt und kein
 * Standardkennwort (docs/SECURITY.md): stattdessen befördert ein beim Start gesetzter, nur lokal in der
 * Umgebungskonfiguration hinterlegter E-Mail-Wert ({@code ADMIN_BOOTSTRAP_EMAIL}) ein bereits selbst
 * registriertes Konto zum Administrator. Ohne passendes Konto passiert nichts außer einer Log-Warnung; die
 * E-Mail-Adresse selbst erscheint nie im Log.
 */
@Component
class AdminBootstrapRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

	private final UserRepository users;
	private final Clock clock;
	private final String bootstrapEmail;

	AdminBootstrapRunner(UserRepository users, Clock clock, @Value("${app.admin-bootstrap-email:}") String bootstrapEmail) {
		this.users = users;
		this.clock = clock;
		this.bootstrapEmail = bootstrapEmail;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (bootstrapEmail == null || bootstrapEmail.isBlank()) {
			return;
		}
		String email = UserInput.normalizeEmail(bootstrapEmail);
		User user = users.findByEmail(email).orElse(null);
		if (user == null) {
			log.warn("ADMIN_BOOTSTRAP_EMAIL verweist auf kein registriertes Konto; bitte zuerst regulär registrieren.");
			return;
		}
		if (user.promoteToAdmin(clock.instant())) {
			users.save(user);
			log.info("Konto {} wurde beim Start zum Administrator befördert (ADMIN_BOOTSTRAP_EMAIL).", user.getId());
		}
	}

}
