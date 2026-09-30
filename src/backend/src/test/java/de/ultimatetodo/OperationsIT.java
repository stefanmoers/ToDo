package de.ultimatetodo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** US-003, US-131, US-002 (Migration): Betriebszustand und Migration gegen echtes PostgreSQL. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OperationsIT {

	@Autowired
	MockMvc mvc;

	@Autowired
	DataSource dataSource;

	@Autowired
	PostgreSQLContainer postgres;

	@Test
	void flywayHatBaselineAufLeeremSchemaAngewendet() throws Exception {
		try (Connection c = dataSource.getConnection();
				ResultSet rs = c.createStatement()
					.executeQuery("select version, success from flyway_schema_history order by installed_rank")) {
			assertThat(rs.next()).isTrue();
			assertThat(rs.getString("version")).isEqualTo("1");
			assertThat(rs.getBoolean("success")).isTrue();
		}
	}

	@Test
	void oeffentlicherHealthcheckZeigtNurGesamtzustand() throws Exception {
		mvc.perform(get("/actuator/health"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("UP"))
			.andExpect(jsonPath("$.components").doesNotExist())
			.andExpect(jsonPath("$.details").doesNotExist());
	}

	@Test
	void anfragekennungWirdZurueckgegeben() throws Exception {
		mvc.perform(get("/actuator/health"))
			.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().exists("X-Request-Id"));
	}

	@Test
	void interneEndpunkteSindNichtOeffentlich() throws Exception {
		for (String path : new String[] { "/actuator/env", "/actuator/configprops", "/actuator/heapdump", "/actuator/loggers",
				"/actuator/beans", "/actuator" }) {
			mvc.perform(get(path)).andExpect(status().isUnauthorized());
		}
	}

	@Test
	void geschuetzteApiOhneAnmeldungLiefert401() throws Exception {
		mvc.perform(get("/api/v1/tasks")).andExpect(status().isUnauthorized());
	}

}
