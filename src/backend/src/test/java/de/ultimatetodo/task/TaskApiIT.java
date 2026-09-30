package de.ultimatetodo.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.ultimatetodo.TestcontainersConfiguration;
import de.ultimatetodo.support.ApiTestClient;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;
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

/** US-002, US-030 bis US-037, US-120, US-121 (M1-Teil), US-130: Aufgaben-API gegen echtes PostgreSQL. */
@SpringBootTest(properties = { "springdoc.api-docs.enabled=true" })
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
@Import(TestcontainersConfiguration.class)
class TaskApiIT {

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

	static String createBody(String title) {
		return "{\"title\":\"%s\"}".formatted(title);
	}

	JsonNode tree(MvcResult r) throws Exception {
		return json.readTree(r.getResponse().getContentAsString());
	}

	@Test
	void anlegenMitNurTitelSetztStandardwerte() throws Exception {
		ApiTestClient a = loggedIn();
		MvcResult r = a.post("/api/v1/tasks", createBody("Einkaufen"));
		assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(201);
		assertThat(r.getResponse().getHeader("Location")).contains("/api/v1/tasks/");
		JsonNode body = tree(r);
		assertThat(body.get("status").asString()).isEqualTo("OPEN");
		assertThat(body.get("priority").asString()).isEqualTo("NORMAL");
		assertThat(body.get("version").asLong()).isZero();
		assertThat(body.has("description")).isFalse();
		assertThat(body.has("dueDate")).isFalse();
		assertThat(body.has("completedAt")).isFalse();
	}

	@Test
	void anlegenIgnoriertFremdeFelder() throws Exception {
		ApiTestClient a = loggedIn();
		MvcResult r = a.post("/api/v1/tasks",
				"{\"title\":\"X\",\"status\":\"DONE\",\"creatorId\":\"%s\",\"version\":99}".formatted(UUID.randomUUID()));
		assertThat(r.getResponse().getStatus()).isEqualTo(201);
		JsonNode body = tree(r);
		assertThat(body.get("status").asString()).isEqualTo("OPEN");
		assertThat(body.get("version").asLong()).isZero();
	}

	@Test
	void validierungLiefertFeldfehlerFuerAlleFaelle() throws Exception {
		ApiTestClient a = loggedIn();
		assertBadWithField(a.post("/api/v1/tasks", createBody("")), "title");
		assertBadWithField(a.post("/api/v1/tasks", createBody("x".repeat(201))), "title");
		assertBadWithField(a.post("/api/v1/tasks", "{\"title\":\"X\",\"description\":\"%s\"}".formatted("x".repeat(10_001))),
				"description");
		assertBadWithField(a.post("/api/v1/tasks", "{\"title\":\"X\",\"priority\":\"SUPER\"}"), "priority");
		assertBadWithField(a.post("/api/v1/tasks", "{\"title\":\"X\",\"dueDate\":\"2026-02-30\"}"), "dueDate");
		assertBadWithField(a.post("/api/v1/tasks", "{\"title\":\"X\",\"dueTime\":\"09:00\"}"), "dueTime");
		assertBadWithField(a.post("/api/v1/tasks", "{\"title\":\"X\",\"dueDate\":\"2026-07-15\"}"), "dueTimeZone");
		assertBadWithField(
				a.post("/api/v1/tasks", "{\"title\":\"X\",\"dueDate\":\"2026-03-29\",\"dueTime\":\"02:30\",\"dueTimeZone\":\"Europe/Berlin\"}"),
				"dueTime");
		assertBadWithField(
				a.post("/api/v1/tasks", "{\"title\":\"X\",\"dueDate\":\"2026-10-25\",\"dueTime\":\"02:30\",\"dueTimeZone\":\"Europe/Berlin\"}"),
				"dueOffset");
	}

	void assertBadWithField(MvcResult r, String field) throws Exception {
		assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(400);
		assertThat(tree(r).get("fieldErrors").toString()).as(r.getResponse().getContentAsString()).contains(field);
	}

	@Test
	void faelligkeitUeberDieApi() throws Exception {
		ApiTestClient a = loggedIn();
		MvcResult nurDatum = a.post("/api/v1/tasks", "{\"title\":\"X\",\"dueDate\":\"2026-07-15\",\"dueTimeZone\":\"Europe/Berlin\"}");
		JsonNode n1 = tree(nurDatum);
		assertThat(n1.get("dueDate").asString()).isEqualTo("2026-07-15");
		assertThat(n1.has("dueAt")).isFalse();

		MvcResult doppelt = a.post("/api/v1/tasks",
				"{\"title\":\"X\",\"dueDate\":\"2026-10-25\",\"dueTime\":\"02:30\",\"dueTimeZone\":\"Europe/Berlin\",\"dueOffset\":\"+01:00\"}");
		JsonNode n2 = tree(doppelt);
		assertThat(n2.get("dueAt").asString()).isEqualTo("2026-10-25T01:30:00Z");
		assertThat(n2.get("dueOffset").asString()).isEqualTo("+01:00");

		MvcResult vergangen = a.post("/api/v1/tasks", "{\"title\":\"X\",\"dueDate\":\"2000-01-01\",\"dueTimeZone\":\"UTC\"}");
		String id = tree(vergangen).get("id").asString();
		assertThat(tree(vergangen).get("overdue").asBoolean()).isTrue();
		MvcResult completed = a.post("/api/v1/tasks/" + id + "/complete", "{\"version\":0}");
		assertThat(tree(completed).get("overdue").asBoolean()).isFalse();
	}

	@Test
	void listeUndPaginierungMitStabilerSortierung() throws Exception {
		ApiTestClient a = loggedIn();
		ApiTestClient b = loggedIn();
		for (int i = 0; i < 30; i++) {
			assertThat(a.post("/api/v1/tasks", createBody("A-" + i)).getResponse().getStatus()).isEqualTo(201);
		}
		for (int i = 0; i < 3; i++) {
			assertThat(b.post("/api/v1/tasks", createBody("B-" + i)).getResponse().getStatus()).isEqualTo(201);
		}

		JsonNode page0 = tree(a.get("/api/v1/tasks"));
		assertThat(page0.get("totalElements").asLong()).isEqualTo(30);
		assertThat(page0.get("totalPages").asInt()).isEqualTo(2);
		assertThat(page0.get("items").size()).isEqualTo(25);

		JsonNode page1 = tree(a.get("/api/v1/tasks?page=1&size=25"));
		assertThat(page1.get("items").size()).isEqualTo(5);

		JsonNode bList = tree(b.get("/api/v1/tasks"));
		assertThat(bList.get("totalElements").asLong()).isEqualTo(3);

		JsonNode empty = tree(a.get("/api/v1/tasks?page=5"));
		assertThat(empty.get("items").size()).isZero();

		assertThat(a.get("/api/v1/tasks?size=101").getResponse().getStatus()).isEqualTo(400);
		assertThat(a.get("/api/v1/tasks?size=0").getResponse().getStatus()).isEqualTo(400);
		assertThat(a.get("/api/v1/tasks?page=-1").getResponse().getStatus()).isEqualTo(400);
		assertThat(a.get("/api/v1/tasks?page=abc").getResponse().getStatus()).as("keine 500, sondern 400").isEqualTo(400);
	}

	@Test
	void ownershipMitZweiBenutzernFremdeAufgabeIstUeberallNichtGefunden() throws Exception {
		ApiTestClient a = loggedIn();
		ApiTestClient b = loggedIn();
		String id = tree(a.post("/api/v1/tasks", createBody("Geheim A"))).get("id").asString();
		String randomId = UUID.randomUUID().toString();

		int getFremd = b.get("/api/v1/tasks/" + id).getResponse().getStatus();
		int getZufall = b.get("/api/v1/tasks/" + randomId).getResponse().getStatus();
		assertThat(getFremd).isEqualTo(404);
		assertThat(getFremd).isEqualTo(getZufall);

		assertThat(b.put("/api/v1/tasks/" + id, "{\"title\":\"X\",\"status\":\"OPEN\",\"priority\":\"NORMAL\",\"version\":0}")
			.getResponse().getStatus()).as("korrekte Version, trotzdem 404 statt 409").isEqualTo(404);
		assertThat(b.post("/api/v1/tasks/" + id + "/complete", "{\"version\":0}").getResponse().getStatus()).isEqualTo(404);
		assertThat(b.post("/api/v1/tasks/" + id + "/reopen", "{\"version\":0}").getResponse().getStatus()).isEqualTo(404);
		assertThat(b.delete("/api/v1/tasks/" + id + "?version=0").getResponse().getStatus()).isEqualTo(404);

		assertThat(jdbc.queryForObject("select title from task where id = ?", String.class, UUID.fromString(id)))
			.isEqualTo("Geheim A");
		assertThat(jdbc.queryForObject("select version from task where id = ?", Long.class, UUID.fromString(id)))
			.isZero();

		JsonNode bList = tree(b.get("/api/v1/tasks"));
		assertThat(bList.get("totalElements").asLong()).isZero();
	}

	@Test
	void aufgabeMitFremdemOderNichtExistierendemProjektWirdAbgewiesen() throws Exception {
		ApiTestClient a = loggedIn();
		ApiTestClient b = loggedIn();
		String othersProjectId = tree(b.post("/api/v1/projects", "{\"name\":\"Projekt von B\"}")).get("id").asString();

		assertBadWithField(a.post("/api/v1/tasks", "{\"projectId\":\"%s\",\"title\":\"X\"}".formatted(othersProjectId)),
				"projectId");
		assertBadWithField(
				a.post("/api/v1/tasks", "{\"projectId\":\"%s\",\"title\":\"X\"}".formatted(UUID.randomUUID())),
				"projectId");
		assertBadWithField(a.post("/api/v1/tasks", "{\"projectId\":\"keine-uuid\",\"title\":\"X\"}"), "projectId");
	}

	@Test
	void aufgabeInEigenemProjektIstNurFuerDenProjekteigentuemerZugaenglich() throws Exception {
		ApiTestClient a = loggedIn();
		ApiTestClient b = loggedIn();
		String projectId = tree(a.post("/api/v1/projects", "{\"name\":\"Mein Projekt\"}")).get("id").asString();

		MvcResult created = a.post("/api/v1/tasks", "{\"projectId\":\"%s\",\"title\":\"Projektaufgabe\"}".formatted(projectId));
		assertThat(created.getResponse().getStatus()).as(created.getResponse().getContentAsString()).isEqualTo(201);
		String taskId = tree(created).get("id").asString();
		assertThat(tree(created).get("projectId").asString()).isEqualTo(projectId);

		assertThat(tree(a.get("/api/v1/tasks/" + taskId)).get("projectId").asString()).isEqualTo(projectId);
		assertThat(b.get("/api/v1/tasks/" + taskId).getResponse().getStatus()).as("fremdes Projekt, keine Zuständigkeit")
			.isEqualTo(404);
	}

	@Test
	void adminRolleGewaehrtKeinenZugriffAufFremdeAufgaben() throws Exception {
		ApiTestClient a = loggedIn();
		String id = tree(a.post("/api/v1/tasks", createBody("Geheim A"))).get("id").asString();

		ApiTestClient admin = client();
		String adminEmail = ApiTestClient.newEmail();
		admin.registerAndLogin(adminEmail);
		jdbc.update("update app_user set role = 'ADMIN' where email = ?", adminEmail);
		// Neue Anmeldung, damit die Sitzung die geänderte Rolle aus der Datenbank übernimmt.
		assertThat(admin.login(adminEmail, ApiTestClient.PASSWORD).getResponse().getStatus()).isEqualTo(200);

		assertThat(admin.get("/api/v1/tasks/" + id).getResponse().getStatus()).as("ADMIN sieht fremde Aufgabe nicht")
			.isEqualTo(404);
		assertThat(admin.put("/api/v1/tasks/" + id, "{\"title\":\"X\",\"status\":\"OPEN\",\"priority\":\"NORMAL\",\"version\":0}")
			.getResponse().getStatus()).isEqualTo(404);
		assertThat(admin.delete("/api/v1/tasks/" + id + "?version=0").getResponse().getStatus()).isEqualTo(404);
		assertThat(tree(admin.get("/api/v1/tasks")).get("totalElements").asLong()).isZero();
	}

	@Test
	void ohneAnmeldungIstDieApiNichtErreichbar() throws Exception {
		mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/tasks"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void bearbeitenErhoehtVersionUndVerlangtStatusUndVersion() throws Exception {
		ApiTestClient a = loggedIn();
		String id = tree(a.post("/api/v1/tasks", createBody("X"))).get("id").asString();

		MvcResult r1 = a.put("/api/v1/tasks/" + id, "{\"title\":\"Y\",\"status\":\"OPEN\",\"priority\":\"HIGH\",\"version\":0}");
		assertThat(r1.getResponse().getStatus()).as(r1.getResponse().getContentAsString()).isEqualTo(200);
		assertThat(tree(r1).get("version").asLong()).isEqualTo(1);
		assertThat(tree(r1).get("title").asString()).isEqualTo("Y");

		MvcResult r2 = a.put("/api/v1/tasks/" + id, "{\"title\":\"Y\",\"status\":\"OPEN\",\"priority\":\"HIGH\",\"version\":1}");
		assertThat(tree(r2).get("version").asLong()).as("keine tatsächliche Änderung, Version bleibt gleich").isEqualTo(1);

		assertThat(a.put("/api/v1/tasks/" + id, "{\"title\":\"Y\",\"status\":\"OPEN\",\"priority\":\"HIGH\"}").getResponse()
			.getStatus()).isEqualTo(400);
		assertThat(a.put("/api/v1/tasks/" + id, "{\"title\":\"Y\",\"priority\":\"HIGH\",\"version\":1}").getResponse()
			.getStatus()).isEqualTo(400);
	}

	@Test
	void konfliktBeiGleichzeitigerBearbeitungSequenziell() throws Exception {
		ApiTestClient owner = loggedIn();
		String id = tree(owner.post("/api/v1/tasks", createBody("X"))).get("id").asString();

		ApiTestClient sameAccountTabA = client();
		ApiTestClient sameAccountTabB = client();
		// Beide "Tabs" derselben Person lesen dieselbe Ausgangsversion.
		sameAccountTabA.session = owner.session;
		sameAccountTabA.refreshCsrf();
		sameAccountTabB.session = owner.session;
		sameAccountTabB.refreshCsrf();

		MvcResult first = sameAccountTabA.put("/api/v1/tasks/" + id,
				"{\"title\":\"Von A\",\"status\":\"OPEN\",\"priority\":\"NORMAL\",\"version\":0}");
		assertThat(first.getResponse().getStatus()).isEqualTo(200);

		MvcResult second = sameAccountTabB.put("/api/v1/tasks/" + id,
				"{\"title\":\"Von B\",\"status\":\"OPEN\",\"priority\":\"NORMAL\",\"version\":0}");
		assertThat(second.getResponse().getStatus()).isEqualTo(409);
		assertThat(tree(second).get("code").asString()).isEqualTo("VERSION_CONFLICT");

		assertThat(jdbc.queryForObject("select title from task where id = ?", String.class, UUID.fromString(id)))
			.isEqualTo("Von A");
	}

	@Test
	void konfliktBeiParallelerBearbeitungGenauEinErfolg() throws Exception {
		ApiTestClient owner = loggedIn();
		String id = tree(owner.post("/api/v1/tasks", createBody("X"))).get("id").asString();

		int threads = 5;
		ExecutorService pool = Executors.newFixedThreadPool(threads);
		CountDownLatch start = new CountDownLatch(1);
		try {
			var clients = Stream.generate(() -> {
				try {
					ApiTestClient c = client();
					c.session = owner.session;
					c.refreshCsrf();
					return c;
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
			}).limit(threads).toList();

			var tasks = clients.stream().map(c -> (Callable<Integer>) () -> {
				start.await();
				return c
					.put("/api/v1/tasks/" + id, "{\"title\":\"Parallel-%s\",\"status\":\"OPEN\",\"priority\":\"NORMAL\",\"version\":0}"
						.formatted(c.address))
					.getResponse().getStatus();
			}).toList();
			var futures = tasks.stream().map(pool::submit).toList();
			start.countDown();
			int successCount = 0;
			for (Future<Integer> f : futures) {
				int status = f.get();
				assertThat(status).isIn(200, 409);
				if (status == 200) {
					successCount++;
				}
			}
			assertThat(successCount).isEqualTo(1);
			assertThat(jdbc.queryForObject("select version from task where id = ?", Long.class, UUID.fromString(id)))
				.isEqualTo(1L);
		}
		finally {
			pool.shutdownNow();
		}
	}

	@Test
	void loeschenPruefVersionUndIstEndgueltig() throws Exception {
		ApiTestClient a = loggedIn();
		String id = tree(a.post("/api/v1/tasks", createBody("X"))).get("id").asString();
		// Tatsächliche Änderung, damit die Version auf 1 steigt (ohne Änderung bliebe sie bei 0).
		a.put("/api/v1/tasks/" + id, "{\"title\":\"Y\",\"status\":\"OPEN\",\"priority\":\"NORMAL\",\"version\":0}");

		assertThat(a.delete("/api/v1/tasks/" + id + "?version=0").getResponse().getStatus())
			.as("veraltete Version").isEqualTo(409);
		assertThat(a.delete("/api/v1/tasks/" + id).getResponse().getStatus()).as("version fehlt").isEqualTo(400);
		assertThat(a.delete("/api/v1/tasks/" + id + "?version=x").getResponse().getStatus()).as("version ungültig")
			.isEqualTo(400);

		assertThat(a.delete("/api/v1/tasks/" + id + "?version=1").getResponse().getStatus()).isEqualTo(204);
		assertThat(a.get("/api/v1/tasks/" + id).getResponse().getStatus()).isEqualTo(404);
		assertThat(tree(a.get("/api/v1/tasks")).get("totalElements").asLong()).isZero();
		assertThat(a.delete("/api/v1/tasks/" + id + "?version=1").getResponse().getStatus()).as("zweites Löschen")
			.isEqualTo(404);
	}

	@Test
	void erledigenIstIdempotentWiederOeffnenWirktNurAufErledigte() throws Exception {
		ApiTestClient a = loggedIn();
		String id = tree(a.post("/api/v1/tasks", createBody("X"))).get("id").asString();

		MvcResult completed = a.post("/api/v1/tasks/" + id + "/complete", "{\"version\":0}");
		assertThat(completed.getResponse().getStatus()).isEqualTo(200);
		assertThat(tree(completed).get("status").asString()).isEqualTo("DONE");
		assertThat(tree(completed).has("completedAt")).isTrue();
		assertThat(tree(completed).get("version").asLong()).isEqualTo(1);

		// Veraltete Version auf bereits erledigter Aufgabe: wirkungslos, kein Konflikt (Arbeitsannahme).
		MvcResult completedAgain = a.post("/api/v1/tasks/" + id + "/complete", "{\"version\":0}");
		assertThat(completedAgain.getResponse().getStatus()).isEqualTo(200);
		assertThat(tree(completedAgain).get("version").asLong()).isEqualTo(1);

		MvcResult reopened = a.post("/api/v1/tasks/" + id + "/reopen", "{\"version\":1}");
		assertThat(reopened.getResponse().getStatus()).isEqualTo(200);
		assertThat(tree(reopened).get("status").asString()).isEqualTo("OPEN");
		assertThat(tree(reopened).has("completedAt")).isFalse();
		assertThat(tree(reopened).get("version").asLong()).isEqualTo(2);

		// Wieder öffnen einer nicht erledigten Aufgabe ist wirkungslos, unabhängig von der Version.
		MvcResult reopenedAgain = a.post("/api/v1/tasks/" + id + "/reopen", "{\"version\":0}");
		assertThat(reopenedAgain.getResponse().getStatus()).isEqualTo(200);
		assertThat(tree(reopenedAgain).get("version").asLong()).isEqualTo(2);

		// Auf einer offenen Aufgabe gilt die normale Versionsprüfung wieder.
		MvcResult conflict = a.post("/api/v1/tasks/" + id + "/complete", "{\"version\":0}");
		assertThat(conflict.getResponse().getStatus()).isEqualTo(409);
	}

	@Test
	void titelUndBeschreibungTauchenNichtInDenLogsAuf(CapturedOutput output) throws Exception {
		ApiTestClient a = loggedIn();
		String secret = "geheimer-aufgaben-titel-0815";
		a.post("/api/v1/tasks", "{\"title\":\"%s\",\"description\":\"%s\"}".formatted(secret, secret));
		a.post("/api/v1/tasks", "{kaputt");
		assertThat(output.getAll()).doesNotContain(secret);
	}

	@Test
	void openApiBeschreibtDieAufgabenEndpunkte() throws Exception {
		MvcResult r = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/v3/api-docs"))
			.andExpect(status().isOk()).andReturn();
		String body = r.getResponse().getContentAsString();
		assertThat(body).contains("/api/v1/tasks", "/complete", "/reopen", "LOW", "NORMAL", "HIGH", "URGENT");
	}

}
