package de.ultimatetodo.task;

import static org.assertj.core.api.Assertions.assertThat;

import de.ultimatetodo.TestcontainersConfiguration;
import de.ultimatetodo.support.ApiTestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** US-062: Aufgaben zuweisen, gegen echtes PostgreSQL. */
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
@Import(TestcontainersConfiguration.class)
class TaskAssignmentApiIT {

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

	@Test
	void zuweisungNurAnMitgliedOderEigentuemerDesProjekts() throws Exception {
		ApiTestClient owner = loggedIn();
		ApiTestClient member = loggedIn();
		ApiTestClient outsider = loggedIn();
		String projectId = createProject(owner, "X");
		addMember(owner, projectId, member);

		MvcResult toMember = owner.post("/api/v1/tasks",
				"{\"projectId\":\"%s\",\"assigneeId\":\"%s\",\"title\":\"An Mitglied\"}".formatted(projectId, id(member)));
		assertThat(toMember.getResponse().getStatus()).as(toMember.getResponse().getContentAsString()).isEqualTo(201);
		assertThat(tree(toMember).get("assigneeId").asString()).isEqualTo(id(member));
		assertThat(tree(toMember).get("assigneeDisplayName").asString()).isEqualTo("Test Person");

		MvcResult toOwnerSelf = owner.post("/api/v1/tasks",
				"{\"projectId\":\"%s\",\"assigneeId\":\"%s\",\"title\":\"An Eigentümer\"}".formatted(projectId, id(owner)));
		assertThat(toOwnerSelf.getResponse().getStatus()).isEqualTo(201);

		MvcResult toOutsider = owner.post("/api/v1/tasks",
				"{\"projectId\":\"%s\",\"assigneeId\":\"%s\",\"title\":\"An Außenstehende\"}".formatted(projectId, id(outsider)));
		assertThat(toOutsider.getResponse().getStatus()).isEqualTo(400);
		assertThat(tree(toOutsider).get("fieldErrors").toString()).contains("assigneeId");
	}

	@Test
	void persoenlicheAufgabeErlaubtNurErstellerSelbstAlsBearbeiter() throws Exception {
		ApiTestClient a = loggedIn();
		ApiTestClient b = loggedIn();

		MvcResult self = a.post("/api/v1/tasks", "{\"assigneeId\":\"%s\",\"title\":\"Selbst\"}".formatted(id(a)));
		assertThat(self.getResponse().getStatus()).isEqualTo(201);

		MvcResult other = a.post("/api/v1/tasks", "{\"assigneeId\":\"%s\",\"title\":\"Andere Person\"}".formatted(id(b)));
		assertThat(other.getResponse().getStatus()).isEqualTo(400);
		assertThat(tree(other).get("fieldErrors").toString()).contains("assigneeId");
	}

	@Test
	void zuweisungKannUeberBearbeitenEntferntWerden() throws Exception {
		ApiTestClient owner = loggedIn();
		ApiTestClient member = loggedIn();
		String projectId = createProject(owner, "X");
		addMember(owner, projectId, member);
		String taskId = tree(owner.post("/api/v1/tasks",
				"{\"projectId\":\"%s\",\"assigneeId\":\"%s\",\"title\":\"X\"}".formatted(projectId, id(member))))
			.get("id").asString();

		MvcResult unassigned = owner.put("/api/v1/tasks/" + taskId,
				"{\"projectId\":\"%s\",\"title\":\"X\",\"status\":\"OPEN\",\"priority\":\"NORMAL\",\"version\":0}"
					.formatted(projectId));
		assertThat(unassigned.getResponse().getStatus()).as(unassigned.getResponse().getContentAsString()).isEqualTo(200);
		assertThat(tree(unassigned).has("assigneeId")).isFalse();
	}

	@Test
	void nurErstellerOderProjektEigentuemerDarfDieAufgabeEinemAnderenProjektZuordnen() throws Exception {
		ApiTestClient owner = loggedIn();
		ApiTestClient member = loggedIn();
		String projectA = createProject(owner, "A");
		String projectB = createProject(owner, "B");
		addMember(owner, projectA, member);
		addMember(owner, projectB, member);

		// Das Mitglied erstellt die Aufgabe in Projekt A; es darf sie zwar sonst bearbeiten, aber nicht verschieben.
		String taskId = tree(member.post("/api/v1/tasks", "{\"projectId\":\"%s\",\"title\":\"X\"}".formatted(projectA)))
			.get("id").asString();

		// Ein zweites Mitglied (nicht Ersteller, nicht Eigentümer) darf nicht verschieben.
		ApiTestClient otherMember = loggedIn();
		addMember(owner, projectA, otherMember);
		addMember(owner, projectB, otherMember);
		MvcResult forbidden = otherMember.put("/api/v1/tasks/" + taskId,
				"{\"projectId\":\"%s\",\"title\":\"X\",\"status\":\"OPEN\",\"priority\":\"NORMAL\",\"version\":0}"
					.formatted(projectB));
		assertThat(forbidden.getResponse().getStatus()).as(forbidden.getResponse().getContentAsString()).isEqualTo(400);
		assertThat(tree(forbidden).get("fieldErrors").toString()).contains("projectId");

		// Der Eigentümer (nicht Ersteller) darf verschieben.
		MvcResult movedByOwner = owner.put("/api/v1/tasks/" + taskId,
				"{\"projectId\":\"%s\",\"title\":\"X\",\"status\":\"OPEN\",\"priority\":\"NORMAL\",\"version\":0}"
					.formatted(projectB));
		assertThat(movedByOwner.getResponse().getStatus()).as(movedByOwner.getResponse().getContentAsString()).isEqualTo(200);
		assertThat(tree(movedByOwner).get("projectId").asString()).isEqualTo(projectB);

		// Der Ersteller selbst darf sie wieder zurückverschieben.
		MvcResult movedByCreator = member.put("/api/v1/tasks/" + taskId,
				"{\"projectId\":\"%s\",\"title\":\"X\",\"status\":\"OPEN\",\"priority\":\"NORMAL\",\"version\":1}"
					.formatted(projectA));
		assertThat(movedByCreator.getResponse().getStatus()).isEqualTo(200);
	}

	@Test
	void zielprojektMussFuerDenHandelndenZugaenglichSein() throws Exception {
		ApiTestClient a = loggedIn();
		ApiTestClient b = loggedIn();
		String foreignProject = createProject(b, "Fremd");
		String taskId = tree(a.post("/api/v1/tasks", "{\"title\":\"X\"}")).get("id").asString();

		MvcResult r = a.put("/api/v1/tasks/" + taskId,
				"{\"projectId\":\"%s\",\"title\":\"X\",\"status\":\"OPEN\",\"priority\":\"NORMAL\",\"version\":0}"
					.formatted(foreignProject));
		assertThat(r.getResponse().getStatus()).isEqualTo(400);
		assertThat(tree(r).get("fieldErrors").toString()).contains("projectId");
	}

	@Test
	void bearbeiterFilterZeigtNurZugewieseneAufgaben() throws Exception {
		ApiTestClient owner = loggedIn();
		ApiTestClient member = loggedIn();
		String projectId = createProject(owner, "X");
		addMember(owner, projectId, member);
		owner.post("/api/v1/tasks", "{\"projectId\":\"%s\",\"assigneeId\":\"%s\",\"title\":\"An Mitglied\"}"
			.formatted(projectId, id(member)));
		owner.post("/api/v1/tasks", "{\"projectId\":\"%s\",\"title\":\"Ohne Bearbeiter\"}".formatted(projectId));

		MvcResult filtered = owner.get("/api/v1/tasks?assigneeId=" + id(member));
		JsonNode items = tree(filtered).get("items");
		assertThat(items.size()).isEqualTo(1);
		assertThat(items.get(0).get("title").asString()).isEqualTo("An Mitglied");
	}

}
