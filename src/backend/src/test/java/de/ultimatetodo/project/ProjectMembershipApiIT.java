package de.ultimatetodo.project;

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

/** US-060 (Projekt teilen) und US-061 (Mitglied entfernen): Mitgliedschaft gegen echtes PostgreSQL. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ProjectMembershipApiIT {

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

	JsonNode tree(MvcResult r) throws Exception {
		return json.readTree(r.getResponse().getContentAsString());
	}

	String email(ApiTestClient c) throws Exception {
		return tree(c.get("/api/v1/auth/me")).get("email").asString();
	}

	String createProject(ApiTestClient owner, String name) throws Exception {
		return tree(owner.post("/api/v1/projects", "{\"name\":\"%s\"}".formatted(name))).get("id").asString();
	}

	@Test
	void mitgliedWirdUeberEmailAufgenommenUndErhaeltZugriffAufDasProjekt() throws Exception {
		ApiTestClient owner = loggedIn();
		ApiTestClient member = loggedIn();
		String projectId = createProject(owner, "Geteiltes Projekt");

		assertThat(member.get("/api/v1/projects/" + projectId).getResponse().getStatus()).as("vor Aufnahme")
			.isEqualTo(404);

		MvcResult add = owner.post("/api/v1/projects/" + projectId + "/members",
				"{\"email\":\"%s\"}".formatted(email(member)));
		assertThat(add.getResponse().getStatus()).as(add.getResponse().getContentAsString()).isEqualTo(201);

		assertThat(member.get("/api/v1/projects/" + projectId).getResponse().getStatus()).as("nach Aufnahme")
			.isEqualTo(200);
		MvcResult members = owner.get("/api/v1/projects/" + projectId + "/members");
		assertThat(members.getResponse().getStatus()).isEqualTo(200);
		JsonNode memberList = tree(members);
		assertThat(memberList.size()).isEqualTo(1);
		assertThat(memberList.get(0).get("displayName").asString()).isEqualTo("Test Person");
		String memberUserId = tree(member.get("/api/v1/auth/me")).get("id").asString();
		assertThat(memberList.get(0).get("userId").asString()).isEqualTo(memberUserId);
	}

	@Test
	void wiederholteAufnahmeDesselbenMitgliedsDupliziertNichts() throws Exception {
		ApiTestClient owner = loggedIn();
		ApiTestClient member = loggedIn();
		String projectId = createProject(owner, "X");
		String body = "{\"email\":\"%s\"}".formatted(email(member));

		assertThat(owner.post("/api/v1/projects/" + projectId + "/members", body).getResponse().getStatus())
			.isEqualTo(201);
		assertThat(owner.post("/api/v1/projects/" + projectId + "/members", body).getResponse().getStatus())
			.as("erneute Aufnahme bleibt erfolgreich, ohne zu duplizieren").isEqualTo(201);

		Long count = jdbc.queryForObject("select count(*) from membership where project_id = ?", Long.class,
				UUID.fromString(projectId));
		assertThat(count).isEqualTo(1);
	}

	@Test
	void unbekannteEmailWirdMitFeldfehlerAbgewiesen() throws Exception {
		ApiTestClient owner = loggedIn();
		String projectId = createProject(owner, "X");
		MvcResult r = owner.post("/api/v1/projects/" + projectId + "/members",
				"{\"email\":\"nie-registriert-" + UUID.randomUUID() + "@example.test\"}");
		assertThat(r.getResponse().getStatus()).isEqualTo(400);
		assertThat(tree(r).get("fieldErrors").toString()).contains("email");
	}

	@Test
	void deaktiviertesKontoWirdAbgewiesen() throws Exception {
		ApiTestClient owner = loggedIn();
		ApiTestClient inactive = loggedIn();
		String inactiveEmail = email(inactive);
		jdbc.update("update app_user set status = 'INACTIVE' where email = ?", inactiveEmail);
		String projectId = createProject(owner, "X");

		MvcResult r = owner.post("/api/v1/projects/" + projectId + "/members", "{\"email\":\"%s\"}".formatted(inactiveEmail));
		assertThat(r.getResponse().getStatus()).isEqualTo(400);
		assertThat(tree(r).get("fieldErrors").toString()).contains("INACTIVE");
	}

	@Test
	void eigentuemerKannWederAufgenommenNochEntferntWerden() throws Exception {
		ApiTestClient owner = loggedIn();
		String projectId = createProject(owner, "X");

		MvcResult addSelf = owner.post("/api/v1/projects/" + projectId + "/members",
				"{\"email\":\"%s\"}".formatted(email(owner)));
		assertThat(addSelf.getResponse().getStatus()).isEqualTo(400);

		String ownerId = tree(owner.get("/api/v1/auth/me")).get("id").asString();
		assertThat(owner.delete("/api/v1/projects/" + projectId + "/members/" + ownerId).getResponse().getStatus())
			.isEqualTo(400);
	}

	@Test
	void nurEigentuemerDarfMitgliederVerwalten() throws Exception {
		ApiTestClient owner = loggedIn();
		ApiTestClient member = loggedIn();
		ApiTestClient outsider = loggedIn();
		String projectId = createProject(owner, "X");
		owner.post("/api/v1/projects/" + projectId + "/members", "{\"email\":\"%s\"}".formatted(email(member)));

		// Ein Mitglied darf das Projekt sehen, aber keine weiteren Mitglieder verwalten.
		MvcResult memberTriesToAdd = member.post("/api/v1/projects/" + projectId + "/members",
				"{\"email\":\"%s\"}".formatted(email(outsider)));
		assertThat(memberTriesToAdd.getResponse().getStatus()).isEqualTo(403);

		String memberId = tree(member.get("/api/v1/auth/me")).get("id").asString();
		assertThat(member.delete("/api/v1/projects/" + projectId + "/members/" + memberId).getResponse().getStatus())
			.isEqualTo(403);

		// Ein völlig Unbeteiligter erfährt nicht einmal, dass das Projekt existiert.
		assertThat(outsider.post("/api/v1/projects/" + projectId + "/members", "{\"email\":\"x@example.test\"}")
			.getResponse().getStatus()).isEqualTo(404);
	}

	@Test
	void mitgliedEntfernenBereinigtZuweisungAberNichtDieAufgabeSelbst() throws Exception {
		ApiTestClient owner = loggedIn();
		ApiTestClient member = loggedIn();
		String projectId = createProject(owner, "X");
		owner.post("/api/v1/projects/" + projectId + "/members", "{\"email\":\"%s\"}".formatted(email(member)));
		String memberId = tree(member.get("/api/v1/auth/me")).get("id").asString();

		MvcResult created = owner.post("/api/v1/tasks",
				"{\"projectId\":\"%s\",\"assigneeId\":\"%s\",\"title\":\"Zugewiesen\"}".formatted(projectId, memberId));
		assertThat(created.getResponse().getStatus()).as(created.getResponse().getContentAsString()).isEqualTo(201);
		String taskId = tree(created).get("id").asString();
		assertThat(tree(created).get("assigneeId").asString()).isEqualTo(memberId);

		assertThat(owner.delete("/api/v1/projects/" + projectId + "/members/" + memberId).getResponse().getStatus())
			.isEqualTo(204);

		MvcResult afterRemoval = owner.get("/api/v1/tasks/" + taskId);
		assertThat(afterRemoval.getResponse().getStatus()).isEqualTo(200);
		assertThat(tree(afterRemoval).has("assigneeId")).as("Zuweisung entfernt, Aufgabe bleibt").isFalse();
		assertThat(tree(afterRemoval).get("projectId").asString()).isEqualTo(projectId);

		// Das entfernte Mitglied hat auch keinen Zugriff mehr auf das Projekt oder seine Aufgaben.
		assertThat(member.get("/api/v1/projects/" + projectId).getResponse().getStatus()).isEqualTo(404);
		assertThat(member.get("/api/v1/tasks/" + taskId).getResponse().getStatus()).isEqualTo(404);
	}

	@Test
	void entferneneNichtVorhandeneMitgliedschaftLiefert404() throws Exception {
		ApiTestClient owner = loggedIn();
		String projectId = createProject(owner, "X");
		assertThat(owner.delete("/api/v1/projects/" + projectId + "/members/" + UUID.randomUUID()).getResponse()
			.getStatus()).isEqualTo(404);
	}

	@Test
	void projektMitProjektTagsLaesstSichJetztLoeschen() throws Exception {
		// Regression: vor US-060 blockierte die Fremdschlüsselprüfung auf tag.project_id (RESTRICT) das Löschen.
		ApiTestClient owner = loggedIn();
		String projectId = createProject(owner, "X");
		assertThat(owner.post("/api/v1/tags", "{\"name\":\"Wichtig\",\"projectId\":\"%s\"}".formatted(projectId))
			.getResponse().getStatus()).isEqualTo(201);

		assertThat(owner.delete("/api/v1/projects/" + projectId + "?version=0").getResponse().getStatus())
			.isEqualTo(204);
		Long remainingTags = jdbc.queryForObject("select count(*) from tag where project_id = ?", Long.class,
				UUID.fromString(projectId));
		assertThat(remainingTags).isZero();
	}

}
