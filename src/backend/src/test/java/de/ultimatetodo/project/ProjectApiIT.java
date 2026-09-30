package de.ultimatetodo.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** US-040 bis US-042: Projekt-API gegen echtes PostgreSQL, inklusive Zusammenspiel mit Aufgaben. */
@SpringBootTest(properties = { "springdoc.api-docs.enabled=true" })
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ProjectApiIT {

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

	static String createBody(String name) {
		return "{\"name\":\"%s\"}".formatted(name);
	}

	JsonNode tree(MvcResult r) throws Exception {
		return json.readTree(r.getResponse().getContentAsString());
	}

	@Test
	void anlegenMitNurNameSetztStandardwerte() throws Exception {
		ApiTestClient a = loggedIn();
		MvcResult r = a.post("/api/v1/projects", createBody("Umzug"));
		assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(201);
		assertThat(r.getResponse().getHeader("Location")).contains("/api/v1/projects/");
		JsonNode body = tree(r);
		assertThat(body.get("name").asString()).isEqualTo("Umzug");
		assertThat(body.get("version").asLong()).isZero();
		assertThat(body.has("description")).isFalse();
	}

	@Test
	void validierungLiefertFeldfehler() throws Exception {
		ApiTestClient a = loggedIn();
		assertBadWithField(a.post("/api/v1/projects", createBody("")), "name");
		assertBadWithField(a.post("/api/v1/projects", createBody("x".repeat(101))), "name");
		assertBadWithField(
				a.post("/api/v1/projects", "{\"name\":\"X\",\"description\":\"%s\"}".formatted("x".repeat(10_001))),
				"description");
	}

	void assertBadWithField(MvcResult r, String field) throws Exception {
		assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(400);
		assertThat(tree(r).get("fieldErrors").toString()).contains(field);
	}

	@Test
	void listeZeigtNurEigeneProjekte() throws Exception {
		ApiTestClient a = loggedIn();
		ApiTestClient b = loggedIn();
		assertThat(a.post("/api/v1/projects", createBody("A1")).getResponse().getStatus()).isEqualTo(201);
		assertThat(a.post("/api/v1/projects", createBody("A2")).getResponse().getStatus()).isEqualTo(201);
		assertThat(b.post("/api/v1/projects", createBody("B1")).getResponse().getStatus()).isEqualTo(201);

		assertThat(tree(a.get("/api/v1/projects")).get("totalElements").asLong()).isEqualTo(2);
		assertThat(tree(b.get("/api/v1/projects")).get("totalElements").asLong()).isEqualTo(1);
	}

	@Test
	void ownershipMitZweiBenutzernFremdesProjektIstUeberallNichtGefunden() throws Exception {
		ApiTestClient a = loggedIn();
		ApiTestClient b = loggedIn();
		String id = tree(a.post("/api/v1/projects", createBody("Geheim A"))).get("id").asString();
		String randomId = UUID.randomUUID().toString();

		int getFremd = b.get("/api/v1/projects/" + id).getResponse().getStatus();
		assertThat(getFremd).isEqualTo(404);
		assertThat(getFremd).isEqualTo(b.get("/api/v1/projects/" + randomId).getResponse().getStatus());

		assertThat(b.put("/api/v1/projects/" + id, "{\"name\":\"X\",\"version\":0}").getResponse().getStatus())
			.as("korrekte Version, trotzdem 404 statt 409").isEqualTo(404);
		assertThat(b.delete("/api/v1/projects/" + id + "?version=0").getResponse().getStatus()).isEqualTo(404);

		assertThat(jdbc.queryForObject("select name from project where id = ?", String.class, UUID.fromString(id)))
			.isEqualTo("Geheim A");
	}

	@Test
	void bearbeitenErhoehtVersionUndVerlangtVersion() throws Exception {
		ApiTestClient a = loggedIn();
		String id = tree(a.post("/api/v1/projects", createBody("Alt"))).get("id").asString();

		MvcResult r1 = a.put("/api/v1/projects/" + id, "{\"name\":\"Neu\",\"version\":0}");
		assertThat(r1.getResponse().getStatus()).as(r1.getResponse().getContentAsString()).isEqualTo(200);
		assertThat(tree(r1).get("version").asLong()).isEqualTo(1);

		assertThat(a.put("/api/v1/projects/" + id, "{\"name\":\"Neu\"}").getResponse().getStatus()).as("version fehlt")
			.isEqualTo(400);
	}

	@Test
	void konfliktBeiVeralteterVersion() throws Exception {
		ApiTestClient a = loggedIn();
		String id = tree(a.post("/api/v1/projects", createBody("X"))).get("id").asString();
		assertThat(a.put("/api/v1/projects/" + id, "{\"name\":\"Y\",\"version\":0}").getResponse().getStatus())
			.isEqualTo(200);

		MvcResult conflict = a.put("/api/v1/projects/" + id, "{\"name\":\"Z\",\"version\":0}");
		assertThat(conflict.getResponse().getStatus()).isEqualTo(409);
		assertThat(tree(conflict).get("code").asString()).isEqualTo("VERSION_CONFLICT");
	}

	@Test
	void loeschenOhneAufgabenverlustLoestAufgabenTransaktional() throws Exception {
		ApiTestClient a = loggedIn();
		String projectId = tree(a.post("/api/v1/projects", createBody("Wird gelöscht"))).get("id").asString();
		MvcResult taskResult = a.post("/api/v1/tasks",
				"{\"projectId\":\"%s\",\"title\":\"Projektaufgabe\"}".formatted(projectId));
		assertThat(taskResult.getResponse().getStatus()).as(taskResult.getResponse().getContentAsString()).isEqualTo(201);
		String taskId = tree(taskResult).get("id").asString();
		assertThat(tree(taskResult).get("projectId").asString()).isEqualTo(projectId);

		assertThat(a.delete("/api/v1/projects/" + projectId + "?version=0").getResponse().getStatus()).isEqualTo(204);
		assertThat(a.get("/api/v1/projects/" + projectId).getResponse().getStatus()).isEqualTo(404);

		// Die Aufgabe bleibt erhalten und wird wieder zur persönlichen Aufgabe ihres Erstellers (US-042).
		MvcResult afterDelete = a.get("/api/v1/tasks/" + taskId);
		assertThat(afterDelete.getResponse().getStatus()).isEqualTo(200);
		assertThat(tree(afterDelete).has("projectId")).isFalse();
		assertThat(jdbc.queryForObject("select project_id from task where id = ?", String.class, UUID.fromString(taskId)))
			.isNull();
	}

	@Test
	void loeschenPruefVersion() throws Exception {
		ApiTestClient a = loggedIn();
		String id = tree(a.post("/api/v1/projects", createBody("X"))).get("id").asString();
		a.put("/api/v1/projects/" + id, "{\"name\":\"Y\",\"version\":0}");

		assertThat(a.delete("/api/v1/projects/" + id + "?version=0").getResponse().getStatus()).as("veraltete Version")
			.isEqualTo(409);
		assertThat(a.delete("/api/v1/projects/" + id + "?version=1").getResponse().getStatus()).isEqualTo(204);
	}

	@Test
	void ohneAnmeldungIstDieApiNichtErreichbar() throws Exception {
		mvc.perform(MockMvcRequestBuilders.get("/api/v1/projects")).andExpect(status().isUnauthorized());
	}

	@Test
	void openApiBeschreibtDieProjektEndpunkte() throws Exception {
		MvcResult r = mvc.perform(MockMvcRequestBuilders.get("/v3/api-docs")).andExpect(status().isOk()).andReturn();
		assertThat(r.getResponse().getContentAsString()).contains("/api/v1/projects");
	}

}
