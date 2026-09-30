package de.ultimatetodo.task;

import static org.assertj.core.api.Assertions.assertThat;

import de.ultimatetodo.BackendApplication;
import de.ultimatetodo.TestcontainersConfiguration;
import de.ultimatetodo.task.application.TaskDraft;
import de.ultimatetodo.task.application.TaskService;
import de.ultimatetodo.task.domain.Task;
import de.ultimatetodo.task.domain.TaskStatus;
import de.ultimatetodo.user.application.UserService;
import de.ultimatetodo.user.domain.User;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * US-002: Aufgaben (inkl. Fälligkeit, Status, Version) bleiben nach einem Neustart des Backends erhalten.
 * Bewusst kein {@code @SpringBootTest}: Ein eigener, von {@code @ServiceConnection} unabhängiger Testcontainer
 * überlebt hier zwei aufeinanderfolgende, unabhängige Spring-Kontexte (ein Kontext-Test-Container würde mit dem
 * ersten Kontext-Schließen ebenfalls gestoppt).
 */
class TaskRestartPersistenceIT {

	static PostgreSQLContainer postgres;

	@BeforeAll
	static void startContainer() {
		postgres = new PostgreSQLContainer(DockerImageName.parse(TestcontainersConfiguration.POSTGRES_IMAGE));
		postgres.start();
	}

	@AfterAll
	static void stopContainer() {
		postgres.stop();
	}

	ConfigurableApplicationContext startApp() {
		// Kommandozeilenargumente statt .properties(...): application.yml setzt spring.datasource.url selbst
		// (Standard leer über ${DB_URL:}) und würde .properties(...) als "Default-Properties" sonst überschreiben.
		return new SpringApplicationBuilder(BackendApplication.class)
			.run("--spring.datasource.url=" + postgres.getJdbcUrl(),
					"--spring.datasource.driver-class-name=org.postgresql.Driver",
					"--spring.datasource.username=" + postgres.getUsername(),
					"--spring.datasource.password=" + postgres.getPassword(), "--server.port=0");
	}

	@Test
	void aufgabenUeberlebenEinenNeustartDesBackends() {
		UUID userId;
		UUID taskId;
		try (ConfigurableApplicationContext first = startApp()) {
			UserService userService = first.getBean(UserService.class);
			TaskService taskService = first.getBean(TaskService.class);
			User user = userService.register("restart-" + UUID.randomUUID() + "@example.test", "Neustart-Test",
					"a".repeat(20), "Europe/Berlin", "127.0.0.1");
			userId = user.getId();
			Task task = taskService.create(userId,
					new TaskDraft(null, null, "Überlebt Neustart", "Beschreibung bleibt erhalten", null, "HIGH",
							"2026-12-24", "18:00", "Europe/Berlin", null));
			taskId = task.getId();
			taskService.complete(userId, taskId, task.getVersion());
		}

		try (ConfigurableApplicationContext second = startApp()) {
			TaskService taskService = second.getBean(TaskService.class);
			Task reloaded = taskService.get(userId, taskId);
			assertThat(reloaded.getTitle()).isEqualTo("Überlebt Neustart");
			assertThat(reloaded.getDescription()).isEqualTo("Beschreibung bleibt erhalten");
			assertThat(reloaded.getStatus()).isEqualTo(TaskStatus.DONE);
			assertThat(reloaded.getCompletedAt()).isNotNull();
			assertThat(reloaded.getPriority().name()).isEqualTo("HIGH");
			assertThat(reloaded.getDue()).isNotNull();
			assertThat(reloaded.getDue().date().toString()).isEqualTo("2026-12-24");
			assertThat(reloaded.getVersion()).isEqualTo(1L);

			JdbcTemplate jdbc = new JdbcTemplate(second.getBean(DataSource.class));
			// Anzahl der Migrationsdateien unter db/migration; bei jeder neuen Migration hier mitpflegen.
			int migrationCount = 7;
			assertThat(jdbc.queryForObject("select count(*) from flyway_schema_history where success = true", Integer.class))
				.as("keine erneute Migration, kein Datenverlust beim zweiten Start").isEqualTo(migrationCount);
			assertThat(jdbc.queryForObject("select count(distinct version) from flyway_schema_history", Integer.class))
				.isEqualTo(migrationCount);
		}
	}

}
