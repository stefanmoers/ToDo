package de.ultimatetodo.user;

import static org.assertj.core.api.Assertions.assertThat;

import de.ultimatetodo.TestcontainersConfiguration;
import de.ultimatetodo.support.ApiTestClient;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** US-014: Konten auflisten, deaktivieren, reaktivieren, gegen echtes PostgreSQL. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class UserAdminApiIT {

	@Autowired MockMvc mvc;
	@Autowired JsonMapper json;
	@Autowired JdbcTemplate jdbc;

	ApiTestClient client() throws Exception {
		return new ApiTestClient(mvc, json);
	}

	ApiTestClient loggedIn() throws Exception {
		ApiTestClient c = client();
		c.registerAndLogin(ApiTestClient.newEmail());
		return c;
	}

	/** Direkt in der Datenbank befördert, unabhängig vom Bootstrap-Vorgang (der ist Gegenstand von AdminBootstrapRunnerIT). */
	ApiTestClient loggedInAsAdmin() throws Exception {
		ApiTestClient c = client();
		String email = ApiTestClient.newEmail();
		c.registerAndLogin(email);
		jdbc.update("update app_user set role = 'ADMIN' where email = ?", email);
		// Die Sitzung wurde mit der Rolle USER angelegt; eine neue Anmeldung liest die jetzt aktuelle Rolle.
		c.login(email, ApiTestClient.PASSWORD);
		return c;
	}

	JsonNode tree(MvcResult r) throws Exception {
		return json.readTree(r.getResponse().getContentAsString());
	}

	String email(ApiTestClient c) throws Exception {
		return tree(c.get("/api/v1/auth/me")).get("email").asString();
	}

	String id(ApiTestClient c) throws Exception {
		return tree(c.get("/api/v1/auth/me")).get("id").asString();
	}

	@Test
	void normaleBenutzerDuerfenAdminEndpunkteNichtAufrufen() throws Exception {
		ApiTestClient user = loggedIn();
		assertThat(user.get("/api/v1/admin/users").getResponse().getStatus()).isEqualTo(403);
		assertThat(user.post("/api/v1/admin/users/" + UUID.randomUUID() + "/deactivate", "").getResponse().getStatus())
			.isEqualTo(403);
		assertThat(user.post("/api/v1/admin/users/" + UUID.randomUUID() + "/reactivate", "").getResponse().getStatus())
			.isEqualTo(403);
	}

	@Test
	void administratorSiehtKontenOhnePasswortAberOhneAufgabeninhalte() throws Exception {
		ApiTestClient admin = loggedInAsAdmin();
		ApiTestClient user = loggedIn();
		user.post("/api/v1/tasks", "{\"title\":\"Geheime Aufgabe\"}");

		MvcResult r = admin.get("/api/v1/admin/users?size=100");
		assertThat(r.getResponse().getStatus()).isEqualTo(200);
		String body = r.getResponse().getContentAsString();
		assertThat(body).doesNotContain("passwordHash", "Geheime Aufgabe");
		JsonNode items = tree(r).get("items");
		boolean foundUser = false;
		for (JsonNode item : items) {
			if (item.get("email").asString().equals(email(user))) {
				foundUser = true;
				assertThat(item.get("status").asString()).isEqualTo("ACTIVE");
				assertThat(item.get("role").asString()).isEqualTo("USER");
			}
		}
		assertThat(foundUser).isTrue();
	}

	@Test
	void deaktivierenBeendetDieSitzungSpaetestensBeiDerNaechstenAnfrageAberDatenBleibenBestehen() throws Exception {
		ApiTestClient admin = loggedInAsAdmin();
		ApiTestClient user = loggedIn();
		String taskId = tree(user.post("/api/v1/tasks", "{\"title\":\"X\"}")).get("id").asString();

		MvcResult deactivated = admin.post("/api/v1/admin/users/" + id(user) + "/deactivate", "");
		assertThat(deactivated.getResponse().getStatus()).as(deactivated.getResponse().getContentAsString()).isEqualTo(200);
		assertThat(tree(deactivated).get("status").asString()).isEqualTo("INACTIVE");

		assertThat(user.get("/api/v1/tasks/" + taskId).getResponse().getStatus()).isEqualTo(401);
		Integer stillThere = jdbc.queryForObject("select count(*) from task where id = ?::uuid", Integer.class, taskId);
		assertThat(stillThere).isEqualTo(1);
	}

	@Test
	void reaktivierenErlaubtErneuteAnmeldung() throws Exception {
		ApiTestClient admin = loggedInAsAdmin();
		ApiTestClient user = loggedIn();
		String userEmail = email(user);
		String userId = id(user);

		admin.post("/api/v1/admin/users/" + userId + "/deactivate", "");
		// Die alte Sitzung samt ihrem CSRF-Token stirbt genau durch diese nächste Anfrage (SessionEpochFilter);
		// ein echter Browser holt sich danach transparent ein neues Token (client.ts, Wiederholung bei CSRF_INVALID).
		user.refreshCsrf();
		assertThat(user.login(userEmail, ApiTestClient.PASSWORD).getResponse().getStatus()).isEqualTo(401);

		MvcResult reactivated = admin.post("/api/v1/admin/users/" + userId + "/reactivate", "");
		assertThat(reactivated.getResponse().getStatus()).isEqualTo(200);
		assertThat(tree(reactivated).get("status").asString()).isEqualTo("ACTIVE");
		assertThat(user.login(userEmail, ApiTestClient.PASSWORD).getResponse().getStatus()).isEqualTo(200);
	}

	@Test
	void letztesAktivesAdministratorkontoKannNichtDeaktiviertWerden() throws Exception {
		ApiTestClient onlyAdmin = loggedInAsAdmin();
		// Andere Tests dieser Klasse legen in derselben Datenbank ebenfalls Admin-Konten an; ohne diese
		// Bereinigung wäre onlyAdmin hier nicht wirklich das einzige noch aktive.
		jdbc.update("update app_user set status = 'INACTIVE' where role = 'ADMIN' and email <> ?", email(onlyAdmin));

		MvcResult blocked = onlyAdmin.post("/api/v1/admin/users/" + id(onlyAdmin) + "/deactivate", "");
		assertThat(blocked.getResponse().getStatus()).isEqualTo(409);

		ApiTestClient secondAdmin = loggedInAsAdmin();
		MvcResult allowed = secondAdmin.post("/api/v1/admin/users/" + id(onlyAdmin) + "/deactivate", "");
		assertThat(allowed.getResponse().getStatus()).as(allowed.getResponse().getContentAsString()).isEqualTo(200);
	}

	@Test
	void nichtVorhandenesKontoLiefert404() throws Exception {
		ApiTestClient admin = loggedInAsAdmin();
		assertThat(admin.post("/api/v1/admin/users/" + UUID.randomUUID() + "/deactivate", "").getResponse().getStatus())
			.isEqualTo(404);
	}

}
