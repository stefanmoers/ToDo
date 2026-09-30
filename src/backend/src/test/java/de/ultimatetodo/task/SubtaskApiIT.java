package de.ultimatetodo.task;

import static org.assertj.core.api.Assertions.assertThat;

import de.ultimatetodo.TestcontainersConfiguration;
import de.ultimatetodo.support.ApiTestClient;
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

/** US-070 (Unteraufgaben verwalten) und US-071 (Fortschritt anzeigen), gegen echtes PostgreSQL. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SubtaskApiIT {

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

	String createSubtask(ApiTestClient c, String taskId, String title) throws Exception {
		MvcResult r = c.post("/api/v1/tasks/" + taskId + "/subtasks", "{\"title\":\"%s\"}".formatted(title));
		JsonNode list = tree(r);
		return list.get(list.size() - 1).get("id").asString();
	}

	@Test
	void anlegenAngehaengtUndFortschrittErscheintAufDerAufgabe() throws Exception {
		ApiTestClient user = loggedIn();
		String taskId = createTask(user, "Umzug");

		MvcResult r1 = user.post("/api/v1/tasks/" + taskId + "/subtasks", "{\"title\":\"Kartons besorgen\"}");
		assertThat(r1.getResponse().getStatus()).as(r1.getResponse().getContentAsString()).isEqualTo(201);
		JsonNode afterFirst = tree(r1);
		assertThat(afterFirst.size()).isEqualTo(1);
		assertThat(afterFirst.get(0).get("title").asString()).isEqualTo("Kartons besorgen");
		assertThat(afterFirst.get(0).get("done").asBoolean()).isFalse();

		MvcResult r2 = user.post("/api/v1/tasks/" + taskId + "/subtasks", "{\"title\":\"Möbel abbauen\"}");
		JsonNode afterSecond = tree(r2);
		assertThat(afterSecond.size()).isEqualTo(2);
		assertThat(afterSecond.get(0).get("sortOrder").asInt()).isLessThan(afterSecond.get(1).get("sortOrder").asInt());

		MvcResult task = user.get("/api/v1/tasks/" + taskId);
		JsonNode subtasks = tree(task).get("subtasks");
		assertThat(subtasks.get("total").asInt()).isEqualTo(2);
		assertThat(subtasks.get("done").asInt()).isEqualTo(0);
	}

	@Test
	void leererOderZuLangerTitelScheitert() throws Exception {
		ApiTestClient user = loggedIn();
		String taskId = createTask(user, "X");

		MvcResult empty = user.post("/api/v1/tasks/" + taskId + "/subtasks", "{\"title\":\"   \"}");
		assertThat(empty.getResponse().getStatus()).isEqualTo(400);
		assertThat(tree(empty).get("fieldErrors").toString()).contains("title");

		MvcResult tooLong = user.post("/api/v1/tasks/" + taskId + "/subtasks",
				"{\"title\":\"%s\"}".formatted("x".repeat(201)));
		assertThat(tooLong.getResponse().getStatus()).isEqualTo(400);
	}

	@Test
	void umbenennenUndErledigtZustandBleibenNachNeuladenErhalten() throws Exception {
		ApiTestClient user = loggedIn();
		String taskId = createTask(user, "X");
		String subtaskId = createSubtask(user, taskId, "Alter Titel");

		MvcResult renamed = user.put("/api/v1/tasks/" + taskId + "/subtasks/" + subtaskId, "{\"title\":\"Neuer Titel\"}");
		assertThat(renamed.getResponse().getStatus()).isEqualTo(200);
		assertThat(tree(renamed).get(0).get("title").asString()).isEqualTo("Neuer Titel");

		MvcResult done = user.put("/api/v1/tasks/" + taskId + "/subtasks/" + subtaskId + "/done", "{\"done\":true}");
		assertThat(tree(done).get(0).get("done").asBoolean()).isTrue();

		// Nach Neuladen (frischer GET) bleiben Titel und Erledigt-Zustand erhalten.
		MvcResult reloaded = user.get("/api/v1/tasks/" + taskId + "/subtasks");
		assertThat(tree(reloaded).get(0).get("title").asString()).isEqualTo("Neuer Titel");
		assertThat(tree(reloaded).get(0).get("done").asBoolean()).isTrue();

		// Rückgängig änderbar (US-071).
		MvcResult undone = user.put("/api/v1/tasks/" + taskId + "/subtasks/" + subtaskId + "/done", "{\"done\":false}");
		assertThat(tree(undone).get(0).get("done").asBoolean()).isFalse();
	}

	@Test
	void alleErledigtSetztDieHauptaufgabeNichtAutomatischAufDone() throws Exception {
		ApiTestClient user = loggedIn();
		String taskId = createTask(user, "X");
		String s1 = createSubtask(user, taskId, "Eins");
		user.put("/api/v1/tasks/" + taskId + "/subtasks/" + s1 + "/done", "{\"done\":true}");

		MvcResult task = user.get("/api/v1/tasks/" + taskId);
		assertThat(tree(task).get("status").asString()).isEqualTo("OPEN");
	}

	@Test
	void verschiebenVertauschtPositionUndIstAmRandWirkungslos() throws Exception {
		ApiTestClient user = loggedIn();
		String taskId = createTask(user, "X");
		String a = createSubtask(user, taskId, "A");
		String b = createSubtask(user, taskId, "B");
		String c = createSubtask(user, taskId, "C");

		// B nach oben: Reihenfolge wird A, C, B -> B, A, C.
		MvcResult moved = user.post("/api/v1/tasks/" + taskId + "/subtasks/" + b + "/move", "{\"direction\":\"UP\"}");
		assertThat(moved.getResponse().getStatus()).as(moved.getResponse().getContentAsString()).isEqualTo(200);
		JsonNode afterMove = tree(moved);
		assertThat(titlesInOrder(afterMove)).containsExactly("B", "A", "C");

		// A (jetzt an zweiter Stelle) nach oben: B, A, C -> A, B, C.
		MvcResult movedAgain = user.post("/api/v1/tasks/" + taskId + "/subtasks/" + a + "/move", "{\"direction\":\"UP\"}");
		assertThat(titlesInOrder(tree(movedAgain))).containsExactly("A", "B", "C");

		// Erste Position nach oben ist wirkungslos.
		MvcResult atTop = user.post("/api/v1/tasks/" + taskId + "/subtasks/" + a + "/move", "{\"direction\":\"UP\"}");
		assertThat(titlesInOrder(tree(atTop))).containsExactly("A", "B", "C");

		// Letzte Position nach unten ist wirkungslos.
		MvcResult atBottom = user.post("/api/v1/tasks/" + taskId + "/subtasks/" + c + "/move", "{\"direction\":\"DOWN\"}");
		assertThat(titlesInOrder(tree(atBottom))).containsExactly("A", "B", "C");
	}

	@Test
	void ungueltigeRichtungWirdAbgewiesen() throws Exception {
		ApiTestClient user = loggedIn();
		String taskId = createTask(user, "X");
		String a = createSubtask(user, taskId, "A");

		MvcResult r = user.post("/api/v1/tasks/" + taskId + "/subtasks/" + a + "/move", "{\"direction\":\"SIDEWAYS\"}");
		assertThat(r.getResponse().getStatus()).isEqualTo(400);
		assertThat(tree(r).get("fieldErrors").toString()).contains("direction");
	}

	@Test
	void loeschenEntferntNurDieUnteraufgabe() throws Exception {
		ApiTestClient user = loggedIn();
		String taskId = createTask(user, "X");
		String a = createSubtask(user, taskId, "A");
		createSubtask(user, taskId, "B");

		MvcResult afterDelete = user.delete("/api/v1/tasks/" + taskId + "/subtasks/" + a);
		assertThat(afterDelete.getResponse().getStatus()).as(afterDelete.getResponse().getContentAsString()).isEqualTo(200);
		assertThat(titlesInOrder(tree(afterDelete))).containsExactly("B");

		MvcResult task = user.get("/api/v1/tasks/" + taskId);
		assertThat(tree(task).get("status").asString()).isEqualTo("OPEN");
	}

	@Test
	void nichtVorhandeneUnteraufgabeLiefert404() throws Exception {
		ApiTestClient user = loggedIn();
		String taskId = createTask(user, "X");
		String unknownId = java.util.UUID.randomUUID().toString();

		MvcResult r = user.put("/api/v1/tasks/" + taskId + "/subtasks/" + unknownId, "{\"title\":\"Neu\"}");
		assertThat(r.getResponse().getStatus()).isEqualTo(404);
	}

	@Test
	void rechteWerdenUeberDieHauptaufgabeGeprueft() throws Exception {
		ApiTestClient owner = loggedIn();
		ApiTestClient outsider = loggedIn();
		String taskId = createTask(owner, "Privat");
		String subtaskId = createSubtask(owner, taskId, "Geheim");

		assertThat(outsider.get("/api/v1/tasks/" + taskId + "/subtasks").getResponse().getStatus()).isEqualTo(404);
		assertThat(outsider.post("/api/v1/tasks/" + taskId + "/subtasks", "{\"title\":\"X\"}").getResponse().getStatus())
			.isEqualTo(404);
		assertThat(
				outsider.put("/api/v1/tasks/" + taskId + "/subtasks/" + subtaskId, "{\"title\":\"X\"}").getResponse().getStatus())
			.isEqualTo(404);
		assertThat(outsider.put("/api/v1/tasks/" + taskId + "/subtasks/" + subtaskId + "/done", "{\"done\":true}")
			.getResponse().getStatus()).isEqualTo(404);
		assertThat(outsider.post("/api/v1/tasks/" + taskId + "/subtasks/" + subtaskId + "/move", "{\"direction\":\"UP\"}")
			.getResponse().getStatus()).isEqualTo(404);
		assertThat(outsider.delete("/api/v1/tasks/" + taskId + "/subtasks/" + subtaskId).getResponse().getStatus())
			.isEqualTo(404);
	}

	@Test
	void loeschenDerAufgabeEntferntIhreUnteraufgabenTransaktional() throws Exception {
		ApiTestClient user = loggedIn();
		String taskId = createTask(user, "X");
		createSubtask(user, taskId, "A");
		createSubtask(user, taskId, "B");

		Integer countBefore = jdbc.queryForObject("select count(*) from subtask where task_id = ?::uuid", Integer.class,
				taskId);
		assertThat(countBefore).isEqualTo(2);

		assertThat(user.delete("/api/v1/tasks/" + taskId + "?version=0").getResponse().getStatus()).isEqualTo(204);

		Integer countAfter = jdbc.queryForObject("select count(*) from subtask where task_id = ?::uuid", Integer.class,
				taskId);
		assertThat(countAfter).isEqualTo(0);
	}

	private static java.util.List<String> titlesInOrder(JsonNode list) {
		java.util.List<String> titles = new java.util.ArrayList<>();
		list.forEach(n -> titles.add(n.get("title").asString()));
		return titles;
	}

}
