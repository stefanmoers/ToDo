package de.ultimatetodo.task;

import static org.assertj.core.api.Assertions.assertThat;

import de.ultimatetodo.TestcontainersConfiguration;
import de.ultimatetodo.support.ApiTestClient;
import java.util.List;
import java.util.stream.StreamSupport;
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

/** US-050 (Suche), US-051 (Filter) und US-052 (Sortierung) der Aufgabenliste gegen echtes PostgreSQL. */
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
@Import(TestcontainersConfiguration.class)
class TaskListQueryApiIT {

	@Autowired MockMvc mvc;
	@Autowired JsonMapper json;

	ApiTestClient loggedIn() throws Exception {
		ApiTestClient c = new ApiTestClient(mvc, json);
		c.registerAndLogin(ApiTestClient.newEmail());
		return c;
	}

	JsonNode tree(MvcResult r) throws Exception {
		return json.readTree(r.getResponse().getContentAsString());
	}

	List<String> titles(JsonNode listResponse) {
		return StreamSupport.stream(listResponse.get("items").spliterator(), false).map(n -> n.get("title").asString())
			.toList();
	}

	@Test
	void sucheFindetTitelUndBeschreibungOhneBeachtungDerGrossKleinschreibung() throws Exception {
		ApiTestClient a = loggedIn();
		a.post("/api/v1/tasks", "{\"title\":\"Steuererklärung abgeben\",\"description\":\"Belege sortieren\"}");
		a.post("/api/v1/tasks", "{\"title\":\"Einkaufen\",\"description\":\"Milch und Brot kaufen\"}");

		assertThat(titles(tree(a.get("/api/v1/tasks?q=STEUER")))).containsExactly("Steuererklärung abgeben");
		assertThat(titles(tree(a.get("/api/v1/tasks?q=milch")))).containsExactly("Einkaufen");
		assertThat(titles(tree(a.get("/api/v1/tasks?q=nichts-davon")))).isEmpty();
		assertThat(titles(tree(a.get("/api/v1/tasks?q=")))).hasSize(2);
	}

	@Test
	void sucheZeigtNurZugaenglicheAufgaben() throws Exception {
		ApiTestClient a = loggedIn();
		ApiTestClient b = loggedIn();
		a.post("/api/v1/tasks", "{\"title\":\"Geheime Aufgabe von A\"}");
		b.post("/api/v1/tasks", "{\"title\":\"Aufgabe von B\"}");

		assertThat(titles(tree(b.get("/api/v1/tasks?q=Geheime")))).isEmpty();
		assertThat(titles(tree(a.get("/api/v1/tasks?q=Geheime")))).containsExactly("Geheime Aufgabe von A");
	}

	@Test
	void filterVerschiedenerArtWerdenMitUndMehrereWerteDerselbenArtMitOderVerknuepft() throws Exception {
		ApiTestClient a = loggedIn();
		String t1 = tree(a.post("/api/v1/tasks", "{\"title\":\"T1\",\"priority\":\"HIGH\"}")).get("id").asString();
		String t2 = tree(a.post("/api/v1/tasks", "{\"title\":\"T2\",\"priority\":\"HIGH\"}")).get("id").asString();
		a.post("/api/v1/tasks/" + t2 + "/complete", "{\"version\":0}");
		a.post("/api/v1/tasks", "{\"title\":\"T3\",\"priority\":\"LOW\"}");
		String t4 = tree(a.post("/api/v1/tasks", "{\"title\":\"T4\",\"priority\":\"URGENT\"}")).get("id").asString();

		JsonNode result = tree(a.get("/api/v1/tasks?status=OPEN&status=IN_PROGRESS&priority=HIGH&priority=URGENT"));
		assertThat(titles(result)).containsExactlyInAnyOrder("T1", "T4");
		assertThat(t1).isNotBlank();
		assertThat(t4).isNotBlank();
	}

	@Test
	void filterNachProjektUndTagZeigenNurZugeordneteAufgaben() throws Exception {
		ApiTestClient a = loggedIn();
		String projectId = tree(a.post("/api/v1/projects", "{\"name\":\"Mein Projekt\"}")).get("id").asString();
		a.post("/api/v1/tasks", "{\"title\":\"Ohne Projekt\"}");
		a.post("/api/v1/tasks", "{\"projectId\":\"%s\",\"title\":\"Mit Projekt\"}".formatted(projectId));

		assertThat(titles(tree(a.get("/api/v1/tasks?projectId=" + projectId)))).containsExactly("Mit Projekt");

		String tagId = tree(a.post("/api/v1/tags", "{\"name\":\"Wichtig\"}")).get("id").asString();
		String taggedId = tree(a.post("/api/v1/tasks", "{\"title\":\"Getaggt\"}")).get("id").asString();
		a.put("/api/v1/tasks/" + taggedId + "/tags", "{\"tagIds\":[\"%s\"]}".formatted(tagId));

		assertThat(titles(tree(a.get("/api/v1/tasks?tagId=" + tagId)))).containsExactly("Getaggt");
	}

	@Test
	void overdueFilterZeigtNurUeberfaelligeUndNichtErledigteAufgaben() throws Exception {
		ApiTestClient a = loggedIn();
		String overdueId = tree(a.post("/api/v1/tasks", "{\"title\":\"Überfällig\",\"dueDate\":\"2000-01-01\",\"dueTimeZone\":\"UTC\"}"))
			.get("id").asString();
		String erledigtUeberfaelligId = tree(
				a.post("/api/v1/tasks", "{\"title\":\"Überfällig aber erledigt\",\"dueDate\":\"2000-01-01\",\"dueTimeZone\":\"UTC\"}"))
			.get("id").asString();
		a.post("/api/v1/tasks/" + erledigtUeberfaelligId + "/complete", "{\"version\":0}");
		a.post("/api/v1/tasks", "{\"title\":\"Nicht überfällig\"}");

		assertThat(titles(tree(a.get("/api/v1/tasks?overdue=true")))).containsExactly("Überfällig");
		assertThat(overdueId).isNotBlank();
	}

	@Test
	void dueDateBereichVergleichtDasRoheFaelligkeitsdatum() throws Exception {
		ApiTestClient a = loggedIn();
		a.post("/api/v1/tasks", "{\"title\":\"Zu früh\",\"dueDate\":\"2026-01-05\",\"dueTimeZone\":\"UTC\"}");
		a.post("/api/v1/tasks", "{\"title\":\"Im Bereich\",\"dueDate\":\"2026-01-11\",\"dueTimeZone\":\"UTC\"}");
		a.post("/api/v1/tasks", "{\"title\":\"Zu spät\",\"dueDate\":\"2026-01-20\",\"dueTimeZone\":\"UTC\"}");
		a.post("/api/v1/tasks", "{\"title\":\"Ohne Fälligkeit\"}");

		assertThat(titles(tree(a.get("/api/v1/tasks?dueDateFrom=2026-01-10&dueDateTo=2026-01-12"))))
			.containsExactly("Im Bereich");
	}

	@Test
	void sortierungNachErstelltGeaendertUndTitelIstStabilUndOhneBeachtungDerGrossKleinschreibungBeiTitel()
			throws Exception {
		ApiTestClient a = loggedIn();
		a.post("/api/v1/tasks", "{\"title\":\"banana\"}");
		a.post("/api/v1/tasks", "{\"title\":\"Apple\"}");
		a.post("/api/v1/tasks", "{\"title\":\"cherry\"}");

		assertThat(titles(tree(a.get("/api/v1/tasks?sortBy=title&sortDirection=asc")))).containsExactly("Apple", "banana",
				"cherry");
		assertThat(titles(tree(a.get("/api/v1/tasks?sortBy=title&sortDirection=desc")))).containsExactly("cherry", "banana",
				"Apple");
		assertThat(titles(tree(a.get("/api/v1/tasks?sortBy=createdAt&sortDirection=asc")))).containsExactly("banana",
				"Apple", "cherry");
	}

	@Test
	void sortierungNachPrioritaetFolgtDerFachlichenRangfolgeNichtDemAlphabet() throws Exception {
		ApiTestClient a = loggedIn();
		a.post("/api/v1/tasks", "{\"title\":\"P-URGENT\",\"priority\":\"URGENT\"}");
		a.post("/api/v1/tasks", "{\"title\":\"P-LOW\",\"priority\":\"LOW\"}");
		a.post("/api/v1/tasks", "{\"title\":\"P-HIGH\",\"priority\":\"HIGH\"}");
		a.post("/api/v1/tasks", "{\"title\":\"P-NORMAL\",\"priority\":\"NORMAL\"}");

		assertThat(titles(tree(a.get("/api/v1/tasks?sortBy=priority&sortDirection=asc")))).containsExactly("P-LOW",
				"P-NORMAL", "P-HIGH", "P-URGENT");
		assertThat(titles(tree(a.get("/api/v1/tasks?sortBy=priority&sortDirection=desc")))).containsExactly("P-URGENT",
				"P-HIGH", "P-NORMAL", "P-LOW");
	}

	@Test
	void sortierungNachFaelligkeitStelltAufgabenOhneFaelligkeitUnabhaengigVonDerRichtungZuletzt() throws Exception {
		ApiTestClient a = loggedIn();
		a.post("/api/v1/tasks", "{\"title\":\"Ohne Fälligkeit\"}");
		a.post("/api/v1/tasks", "{\"title\":\"Später fällig\",\"dueDate\":\"2026-08-01\",\"dueTimeZone\":\"UTC\"}");
		a.post("/api/v1/tasks", "{\"title\":\"Früher fällig\",\"dueDate\":\"2026-01-01\",\"dueTimeZone\":\"UTC\"}");

		assertThat(titles(tree(a.get("/api/v1/tasks?sortBy=due&sortDirection=asc")))).containsExactly("Früher fällig",
				"Später fällig", "Ohne Fälligkeit");
		assertThat(titles(tree(a.get("/api/v1/tasks?sortBy=due&sortDirection=desc")))).containsExactly("Später fällig",
				"Früher fällig", "Ohne Fälligkeit");
	}

	@Test
	void ungueltigeStatusPrioritaetSortierfeldUndSortierrichtungWerdenAbgewiesen() throws Exception {
		ApiTestClient a = loggedIn();
		assertBadWithField(a.get("/api/v1/tasks?status=FOO"), "status");
		assertBadWithField(a.get("/api/v1/tasks?priority=FOO"), "priority");
		assertBadWithField(a.get("/api/v1/tasks?sortBy=unbekannt"), "sortBy");
		assertBadWithField(a.get("/api/v1/tasks?sortDirection=hoch"), "sortDirection");
	}

	void assertBadWithField(MvcResult r, String field) throws Exception {
		assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(400);
		assertThat(tree(r).get("fieldErrors").toString()).as(r.getResponse().getContentAsString()).contains(field);
	}

}
