package de.ultimatetodo.user.application;

import static org.assertj.core.api.Assertions.assertThat;

import de.ultimatetodo.TestcontainersConfiguration;
import de.ultimatetodo.support.ApiTestClient;
import de.ultimatetodo.user.domain.User;
import de.ultimatetodo.user.persistence.UserRepository;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

/**
 * US-014: der Bootstrap-Vorgang wird hier direkt aufgerufen statt über den echten Anwendungsstart, da zum
 * Startzeitpunkt des Testkontexts das befördernde Konto noch nicht registriert ist (Reihenfolgeproblem).
 * {@link AdminBootstrapRunner} bleibt paketsichtbar; dieser Test liegt bewusst im selben Paket.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AdminBootstrapRunnerIT {

	@Autowired MockMvc mvc;
	@Autowired JsonMapper json;
	@Autowired UserRepository users;
	@Autowired Clock clock;

	@Test
	void befoerdertEinBereitsRegistriertesKontoZumAdministrator() throws Exception {
		ApiTestClient client = new ApiTestClient(mvc, json);
		String email = ApiTestClient.newEmail();
		client.registerAndLogin(email);

		new AdminBootstrapRunner(users, clock, email).run(null);

		User promoted = users.findByEmail(email).orElseThrow();
		assertThat(promoted.getRole()).isEqualTo(User.Role.ADMIN);
	}

	@Test
	void unbekannteBootstrapEmailBleibtOhneWirkung() {
		new AdminBootstrapRunner(users, clock, ApiTestClient.newEmail()).run(null);
		// Kein Absturz, keine Ausnahme; es existiert schlicht kein Konto mit dieser Adresse.
	}

	@Test
	void leereBootstrapEmailBleibtOhneWirkung() {
		new AdminBootstrapRunner(users, clock, "").run(null);
		new AdminBootstrapRunner(users, clock, "   ").run(null);
	}

}
