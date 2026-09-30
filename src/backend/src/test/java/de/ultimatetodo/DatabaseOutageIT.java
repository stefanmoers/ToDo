package de.ultimatetodo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** US-003: Readiness meldet einen Datenbankausfall, Liveness bleibt davon unberührt. */
@SpringBootTest(properties = "spring.datasource.hikari.connection-timeout=1000")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@DirtiesContext
class DatabaseOutageIT {

	@Autowired
	MockMvc mvc;

	@Autowired
	PostgreSQLContainer postgres;

	@Test
	void readinessFaelltBeiDatenbankausfallLivenessNicht() throws Exception {
		mvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());

		postgres.stop();

		mvc.perform(get("/actuator/health/readiness"))
			.andExpect(status().isServiceUnavailable())
			.andExpect(jsonPath("$.status").value("DOWN"));
		mvc.perform(get("/actuator/health/liveness"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("UP"));
	}

}
