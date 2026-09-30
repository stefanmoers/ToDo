package de.ultimatetodo.task;

import static org.assertj.core.api.Assertions.assertThat;

import de.ultimatetodo.TestcontainersConfiguration;
import de.ultimatetodo.support.ApiTestClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** US-081: Aktivitätshistorie einer Aufgabe, gegen echtes PostgreSQL. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ActivityApiIT {

	@Autowired MockMvc mvc;
	@Autowired JsonMapper json;

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

	String id(ApiTestClient c) throws Exception {
		return tree(c.get("/api/v1/auth/me")).get("id").asString();
	}

	String email(ApiTestClient c) throws Exception {
		return tree(c.get("/api/v1/auth/me")).get("email").asString();
	}

	String createProject(ApiTestClient owner, String name) throws Exception {
		return tree(owner.post("/api/v1/projects", "{\"name\":\"%s\"}".formatted(name))).get("id").asString();
	}

	void addMember(ApiTestClient owner, String projectId, ApiTestClient member) throws Exception {
		assertThat(owner.post("/api/v1/projects/" + projectId + "/members", "{\"email\":\"%s\"}".formatted(email(member)))
			.getResponse().getStatus()).isEqualTo(201);
	}

	JsonNode activitiesOf(ApiTestClient c, String taskId) throws Exception {
		return tree(c.get("/api/v1/tasks/" + taskId + "/activities"));
	}

	JsonNode last(JsonNode activities) {
		return activities.get(activities.size() - 1);
	}

	@Test
	void anlegenErzeugtEinenCreatedEintrag() throws Exception {
		ApiTestClient user = loggedIn();
		MvcResult created = user.post("/api/v1/tasks", "{\"title\":\"X\"}");
		String taskId = tree(created).get("id").asString();

		JsonNode activities = activitiesOf(user, taskId);
		assertThat(activities.size()).isEqualTo(1);
		assertThat(activities.get(0).get("type").asString()).isEqualTo("CREATED");
		assertThat(activities.get(0).get("actorDisplayName").asString()).isEqualTo("Test Person");
	}

	@Test
	void aenderungenAnTitelStatusPrioritaetErzeugenEigeneEintraegeMitAlterUndNeuerAngabe() throws Exception {
		ApiTestClient user = loggedIn();
		String taskId = tree(user.post("/api/v1/tasks", "{\"title\":\"Alt\"}")).get("id").asString();

		user.put("/api/v1/tasks/" + taskId,
				"{\"title\":\"Neu\",\"status\":\"IN_PROGRESS\",\"priority\":\"HIGH\",\"version\":0}");

		JsonNode activities = activitiesOf(user, taskId);
		// Alle drei Änderungen teilen sich denselben Zeitstempel; die Reihenfolge muss trotzdem stabil der
		// Prüfreihenfolge in TaskService.recordChangeActivities folgen, nicht der zufälligen Aktivitäts-ID.
		assertThat(typesOf(activities)).containsExactly("CREATED", "TITLE_CHANGED", "STATUS_CHANGED", "PRIORITY_CHANGED");
		JsonNode titleChange = findFirst(activities, "TITLE_CHANGED");
		assertThat(titleChange.get("fromValue").asString()).isEqualTo("Alt");
		assertThat(titleChange.get("toValue").asString()).isEqualTo("Neu");
		JsonNode statusChange = findFirst(activities, "STATUS_CHANGED");
		assertThat(statusChange.get("fromValue").asString()).isEqualTo("OPEN");
		assertThat(statusChange.get("toValue").asString()).isEqualTo("IN_PROGRESS");
		JsonNode priorityChange = findFirst(activities, "PRIORITY_CHANGED");
		assertThat(priorityChange.get("fromValue").asString()).isEqualTo("NORMAL");
		assertThat(priorityChange.get("toValue").asString()).isEqualTo("HIGH");
	}

	@Test
	void nurBeschreibungAendernErzeugtKeinenEintrag() throws Exception {
		ApiTestClient user = loggedIn();
		String taskId = tree(user.post("/api/v1/tasks", "{\"title\":\"X\"}")).get("id").asString();

		user.put("/api/v1/tasks/" + taskId,
				"{\"title\":\"X\",\"description\":\"Neue Beschreibung\",\"status\":\"OPEN\",\"priority\":\"NORMAL\",\"version\":0}");

		JsonNode activities = activitiesOf(user, taskId);
		assertThat(activities.size()).isEqualTo(1);
		assertThat(activities.get(0).get("type").asString()).isEqualTo("CREATED");
	}

	@Test
	void ohneAenderungErzeugtKeinenWeiterenEintrag() throws Exception {
		ApiTestClient user = loggedIn();
		String taskId = tree(user.post("/api/v1/tasks", "{\"title\":\"X\"}")).get("id").asString();

		user.put("/api/v1/tasks/" + taskId, "{\"title\":\"X\",\"status\":\"OPEN\",\"priority\":\"NORMAL\",\"version\":0}");

		assertThat(activitiesOf(user, taskId).size()).isEqualTo(1);
	}

	@Test
	void erledigenUndWiederOeffnenErzeugenEigeneArtenStattStatusChanged() throws Exception {
		ApiTestClient user = loggedIn();
		String taskId = tree(user.post("/api/v1/tasks", "{\"title\":\"X\"}")).get("id").asString();

		user.post("/api/v1/tasks/" + taskId + "/complete", "{\"version\":0}");
		user.post("/api/v1/tasks/" + taskId + "/reopen", "{\"version\":1}");

		JsonNode activities = activitiesOf(user, taskId);
		assertThat(typesOf(activities)).contains("COMPLETED", "REOPENED").doesNotContain("STATUS_CHANGED");
	}

	@Test
	void zuweisungsAenderungZeigtDenNamenDesBearbeiters() throws Exception {
		ApiTestClient owner = loggedIn();
		ApiTestClient member = loggedIn();
		String projectId = createProject(owner, "X");
		addMember(owner, projectId, member);
		String taskId = tree(owner.post("/api/v1/tasks", "{\"projectId\":\"%s\",\"title\":\"X\"}".formatted(projectId)))
			.get("id").asString();

		owner.put("/api/v1/tasks/" + taskId, "{\"projectId\":\"%s\",\"assigneeId\":\"%s\",\"title\":\"X\",\"status\":\"OPEN\",\"priority\":\"NORMAL\",\"version\":0}"
			.formatted(projectId, id(member)));

		JsonNode assigneeChange = findFirst(activitiesOf(owner, taskId), "ASSIGNEE_CHANGED");
		JsonNode from = assigneeChange.get("fromValue");
		assertThat(from == null || from.isNull()).isTrue();
		assertThat(assigneeChange.get("toValue").asString()).isEqualTo("Test Person");
	}

	@Test
	void projektAenderungZeigtDenProjektnamen() throws Exception {
		ApiTestClient user = loggedIn();
		String projectId = createProject(user, "Zielprojekt");
		String taskId = tree(user.post("/api/v1/tasks", "{\"title\":\"X\"}")).get("id").asString();

		user.put("/api/v1/tasks/" + taskId,
				"{\"projectId\":\"%s\",\"title\":\"X\",\"status\":\"OPEN\",\"priority\":\"NORMAL\",\"version\":0}"
					.formatted(projectId));

		JsonNode projectChange = findFirst(activitiesOf(user, taskId), "PROJECT_CHANGED");
		assertThat(projectChange.get("toValue").asString()).isEqualTo("Zielprojekt");
	}

	@Test
	void reihenfolgeIstChronologischStabil() throws Exception {
		ApiTestClient user = loggedIn();
		String taskId = tree(user.post("/api/v1/tasks", "{\"title\":\"X\"}")).get("id").asString();
		user.put("/api/v1/tasks/" + taskId, "{\"title\":\"Y\",\"status\":\"OPEN\",\"priority\":\"NORMAL\",\"version\":0}");
		user.post("/api/v1/tasks/" + taskId + "/complete", "{\"version\":1}");

		JsonNode activities = activitiesOf(user, taskId);
		assertThat(activities.get(0).get("type").asString()).isEqualTo("CREATED");
		assertThat(activities.get(1).get("type").asString()).isEqualTo("TITLE_CHANGED");
		assertThat(activities.get(2).get("type").asString()).isEqualTo("COMPLETED");
	}

	@Test
	void rechteWerdenUeberDieHauptaufgabeGeprueft() throws Exception {
		ApiTestClient owner = loggedIn();
		ApiTestClient outsider = loggedIn();
		String taskId = tree(owner.post("/api/v1/tasks", "{\"title\":\"Privat\"}")).get("id").asString();

		assertThat(outsider.get("/api/v1/tasks/" + taskId + "/activities").getResponse().getStatus()).isEqualTo(404);
	}

	private static java.util.List<String> typesOf(JsonNode activities) {
		java.util.List<String> types = new java.util.ArrayList<>();
		activities.forEach(a -> types.add(a.get("type").asString()));
		return types;
	}

	private static JsonNode findFirst(JsonNode activities, String type) {
		for (JsonNode a : activities) {
			if (type.equals(a.get("type").asString())) {
				return a;
			}
		}
		throw new AssertionError("Kein Eintrag vom Typ " + type + " gefunden: " + activities);
	}

}
