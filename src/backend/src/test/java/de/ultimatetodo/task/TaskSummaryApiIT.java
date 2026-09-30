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

/** US-110: die fünf Zähler der persönlichen Übersicht gegen echtes PostgreSQL. */
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
@Import(TestcontainersConfiguration.class)
class TaskSummaryApiIT {

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

	@Test
	void zaehltNichtErledigteHeuteFaelligeUeberfaelligeErledigteUndOffeneHoheDringende() throws Exception {
		ApiTestClient a = loggedIn();
		// "Heute" liegt bewusst in der Zukunft, damit die Aufgabe unabhängig vom Ausführungszeitpunkt des Tests
		// nicht zusätzlich als überfällig zählt (überfällig vergleicht gegen die echte Serverzeit, nicht "today").
		String today = java.time.LocalDate.now(java.time.ZoneOffset.UTC).plusYears(5).toString();
		// Nicht erledigt, ohne Fälligkeit: zählt nur bei "offen".
		a.post("/api/v1/tasks", "{\"title\":\"Offen ohne Fälligkeit\"}");
		// Heute fällig, offen.
		a.post("/api/v1/tasks", "{\"title\":\"Heute fällig\",\"dueDate\":\"%s\",\"dueTimeZone\":\"UTC\"}".formatted(today));
		// Überfällig, offen.
		a.post("/api/v1/tasks", "{\"title\":\"Überfällig\",\"dueDate\":\"2000-01-01\",\"dueTimeZone\":\"UTC\"}");
		// Erledigt.
		String doneId = tree(a.post("/api/v1/tasks", "{\"title\":\"Fertig\"}")).get("id").asString();
		a.post("/api/v1/tasks/" + doneId + "/complete", "{\"version\":0}");
		// Offen und hoch/dringend.
		a.post("/api/v1/tasks", "{\"title\":\"Dringend offen\",\"priority\":\"URGENT\"}");
		// Erledigt, aber sonst überfällig/hochprior: darf weder bei offen noch überfällig noch hoch/dringend zählen.
		String doneUrgentId = tree(a.post("/api/v1/tasks",
				"{\"title\":\"Dringend aber erledigt\",\"priority\":\"URGENT\",\"dueDate\":\"2000-01-01\",\"dueTimeZone\":\"UTC\"}"))
			.get("id").asString();
		a.post("/api/v1/tasks/" + doneUrgentId + "/complete", "{\"version\":0}");

		JsonNode summary = tree(a.get("/api/v1/tasks/summary?today=" + today));
		assertThat(summary.get("openCount").asLong()).isEqualTo(4);
		assertThat(summary.get("dueTodayCount").asLong()).isEqualTo(1);
		assertThat(summary.get("overdueCount").asLong()).isEqualTo(1);
		assertThat(summary.get("doneCount").asLong()).isEqualTo(2);
		assertThat(summary.get("highPriorityOpenCount").asLong()).isEqualTo(1);
	}

	@Test
	void zaehltNurEigeneAufgaben() throws Exception {
		ApiTestClient a = loggedIn();
		ApiTestClient b = loggedIn();
		a.post("/api/v1/tasks", "{\"title\":\"Von A\"}");
		b.post("/api/v1/tasks", "{\"title\":\"Von B 1\"}");
		b.post("/api/v1/tasks", "{\"title\":\"Von B 2\"}");

		assertThat(tree(a.get("/api/v1/tasks/summary?today=2026-05-10")).get("openCount").asLong()).isEqualTo(1);
		assertThat(tree(b.get("/api/v1/tasks/summary?today=2026-05-10")).get("openCount").asLong()).isEqualTo(2);
	}

	@Test
	void ohneTodayWirdMitFeldfehlerAbgewiesenStattStillEinenServerwertZuVerwenden() throws Exception {
		ApiTestClient a = loggedIn();
		MvcResult r = a.get("/api/v1/tasks/summary");
		assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(400);
		assertThat(tree(r).get("fieldErrors").toString()).contains("today");
	}

}
