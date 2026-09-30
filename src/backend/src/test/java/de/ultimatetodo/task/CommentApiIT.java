package de.ultimatetodo.task;

import static org.assertj.core.api.Assertions.assertThat;

import de.ultimatetodo.TestcontainersConfiguration;
import de.ultimatetodo.support.ApiTestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** US-080: Kommentare an Aufgaben, gegen echtes PostgreSQL. */
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
@Import(TestcontainersConfiguration.class)
class CommentApiIT {

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

	String createTask(ApiTestClient c, String title) throws Exception {
		return tree(c.post("/api/v1/tasks", "{\"title\":\"%s\"}".formatted(title))).get("id").asString();
	}

	@Test
	void kommentarZeigtAutorUndZeitpunktUndErscheintInChronologischerListe() throws Exception {
		ApiTestClient user = loggedIn();
		String taskId = createTask(user, "X");

		MvcResult created = user.post("/api/v1/tasks/" + taskId + "/comments", "{\"content\":\"Erster Kommentar\"}");
		assertThat(created.getResponse().getStatus()).as(created.getResponse().getContentAsString()).isEqualTo(201);
		JsonNode body = tree(created);
		assertThat(body.get("content").asString()).isEqualTo("Erster Kommentar");
		assertThat(body.get("authorDisplayName").asString()).isEqualTo("Test Person");
		assertThat(body.has("createdAt")).isTrue();

		user.post("/api/v1/tasks/" + taskId + "/comments", "{\"content\":\"Zweiter Kommentar\"}");
		MvcResult list = user.get("/api/v1/tasks/" + taskId + "/comments");
		JsonNode comments = tree(list);
		assertThat(comments.size()).isEqualTo(2);
		assertThat(comments.get(0).get("content").asString()).isEqualTo("Erster Kommentar");
		assertThat(comments.get(1).get("content").asString()).isEqualTo("Zweiter Kommentar");
	}

	@Test
	void leererOderZuLangerTextScheitert() throws Exception {
		ApiTestClient user = loggedIn();
		String taskId = createTask(user, "X");

		MvcResult blank = user.post("/api/v1/tasks/" + taskId + "/comments", "{\"content\":\"   \"}");
		assertThat(blank.getResponse().getStatus()).isEqualTo(400);
		assertThat(tree(blank).get("fieldErrors").toString()).contains("content");

		MvcResult tooLong = user.post("/api/v1/tasks/" + taskId + "/comments",
				"{\"content\":\"%s\"}".formatted("x".repeat(5001)));
		assertThat(tooLong.getResponse().getStatus()).isEqualTo(400);
	}

	@Test
	void htmlWirdUnveraendertAlsTextGespeichertUndZurueckgegeben() throws Exception {
		ApiTestClient user = loggedIn();
		String taskId = createTask(user, "X");
		String htmlLike = "<script>alert(1)</script>";

		MvcResult created = user.post("/api/v1/tasks/" + taskId + "/comments",
				"{\"content\":\"%s\"}".formatted(htmlLike));
		assertThat(created.getResponse().getStatus()).isEqualTo(201);
		assertThat(tree(created).get("content").asString()).isEqualTo(htmlLike);
	}

	@Test
	void rechteWerdenUeberDieHauptaufgabeGeprueft() throws Exception {
		ApiTestClient owner = loggedIn();
		ApiTestClient outsider = loggedIn();
		String taskId = createTask(owner, "Privat");

		assertThat(outsider.get("/api/v1/tasks/" + taskId + "/comments").getResponse().getStatus()).isEqualTo(404);
		assertThat(outsider.post("/api/v1/tasks/" + taskId + "/comments", "{\"content\":\"X\"}").getResponse()
			.getStatus()).isEqualTo(404);
	}

	@Test
	void kommentarUndAktivitaetEntstehenAtomar() throws Exception {
		ApiTestClient user = loggedIn();
		String taskId = createTask(user, "X");

		user.post("/api/v1/tasks/" + taskId + "/comments", "{\"content\":\"Kommentar\"}");

		JsonNode activities = tree(user.get("/api/v1/tasks/" + taskId + "/activities"));
		boolean hasCommented = false;
		for (JsonNode a : activities) {
			if ("COMMENTED".equals(a.get("type").asString())) {
				hasCommented = true;
			}
		}
		assertThat(hasCommented).isTrue();
	}

	@Test
	void inhaltTauchtNichtInDenLogsAuf(CapturedOutput output) throws Exception {
		ApiTestClient user = loggedIn();
		String taskId = createTask(user, "X");
		String secret = "geheimer-kommentar-inhalt-4711";

		user.post("/api/v1/tasks/" + taskId + "/comments", "{\"content\":\"%s\"}".formatted(secret));
		assertThat(output.getAll()).doesNotContain(secret);
	}

	@Test
	void loeschenDerAufgabeEntferntIhreKommentareTransaktional() throws Exception {
		ApiTestClient user = loggedIn();
		String taskId = createTask(user, "X");
		user.post("/api/v1/tasks/" + taskId + "/comments", "{\"content\":\"X\"}");

		Integer countBefore = jdbc.queryForObject("select count(*) from comment where task_id = ?::uuid", Integer.class,
				taskId);
		assertThat(countBefore).isEqualTo(1);

		assertThat(user.delete("/api/v1/tasks/" + taskId + "?version=0").getResponse().getStatus()).isEqualTo(204);

		Integer countAfter = jdbc.queryForObject("select count(*) from comment where task_id = ?::uuid", Integer.class,
				taskId);
		assertThat(countAfter).isEqualTo(0);
	}

}
