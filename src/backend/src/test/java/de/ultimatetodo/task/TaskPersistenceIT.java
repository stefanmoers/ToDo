package de.ultimatetodo.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.ultimatetodo.TestcontainersConfiguration;
import de.ultimatetodo.task.application.TaskDraft;
import de.ultimatetodo.task.application.TaskService;
import de.ultimatetodo.task.domain.Task;
import de.ultimatetodo.task.persistence.TaskRepository;
import de.ultimatetodo.user.domain.User;
import de.ultimatetodo.user.persistence.UserRepository;
import java.sql.Date;
import java.sql.Time;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * US-002, US-120, US-121 (M1-Teil): Datenbank-Constraints als zweite Verteidigungslinie, Versionsschutz auch bei
 * parallelem Zugriff auf Datenbankebene, und Transaktions-Rollback über Service- und Repository-Aufrufe hinweg.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class TaskPersistenceIT {

	@Autowired JdbcTemplate jdbc;
	@Autowired UserRepository users;
	@Autowired TaskRepository tasks;
	@Autowired TaskService taskService;
	@Autowired PlatformTransactionManager txManager;

	UUID createUser() {
		User u = User.register("persistence-" + UUID.randomUUID() + "@example.test", "Test", "hash", "Europe/Berlin",
				Instant.now());
		return users.saveAndFlush(u).getId();
	}

	static TaskDraft draft(String title) {
		return new TaskDraft(null, null, title, null, null, null, null, null, null, null);
	}

	@Test
	void datenbankConstraintsWirkenAlsZweiteVerteidigungslinie() {
		UUID creator = createUser();

		assertInsertFails(creator, "   ", "OPEN", "NORMAL", null, null, null);
		assertInsertFails(creator, "X", "FOO", "NORMAL", null, null, null);
		assertInsertFails(creator, "X", "OPEN", "MEGA", null, null, null);
		assertInsertFails(creator, "X", "DONE", "NORMAL", null, null, null);
		assertInsertFails(creator, "X", "OPEN", "NORMAL", null, Time.valueOf("09:00:00"), null);
		assertInsertFails(creator, "X", "OPEN", "NORMAL", Date.valueOf("2026-07-15"), null, null);
		assertThatThrownBy(() -> jdbc.update(
				"insert into task (id, creator_id, title, status, priority, created_at, updated_at) values (?,?,?,?,?,now(),now())",
				UUID.randomUUID(), UUID.randomUUID(), "X", "OPEN", "NORMAL")).isInstanceOf(DataAccessException.class);
	}

	void assertInsertFails(UUID creator, String title, String status, String priority, Date dueDate, Time dueTime,
			String dueTimeZone) {
		assertThatThrownBy(() -> jdbc.update(
				"insert into task (id, creator_id, title, status, priority, due_date, due_time, due_time_zone, created_at, updated_at) "
						+ "values (?,?,?,?,?,?,?,?,now(),now())",
				UUID.randomUUID(), creator, title, status, priority, dueDate, dueTime, dueTimeZone))
			.isInstanceOf(DataAccessException.class);
	}

	@Test
	void versionsschutzWirktAuchBeiParallelerAenderungAufDatenbankebene() {
		UUID creator = createUser();
		UUID taskId = taskService.create(creator, draft("X")).getId();
		TransactionTemplate tx = new TransactionTemplate(txManager);
		ExecutorService other = Executors.newSingleThreadExecutor();
		try {
			assertThatThrownBy(() -> tx.execute(status -> {
				Task task = tasks.findAccessibleById(taskId, creator, Set.of()).orElseThrow();
				// In einem separaten Thread, sonst teilt JdbcTemplate die Verbindung der laufenden JPA-Transaktion.
				try {
					other.submit(() -> jdbc.update("update task set version = version + 1, title = 'von außen geändert' where id = ?",
							taskId)).get();
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
				task.update(null, null, "geändert", null, task.getStatus(), task.getPriority(), null, Instant.now());
				tasks.flush();
				return null;
			})).isInstanceOf(ObjectOptimisticLockingFailureException.class);
		}
		finally {
			other.shutdownNow();
		}
		assertThat(jdbc.queryForObject("select title from task where id = ?", String.class, taskId))
			.isEqualTo("von außen geändert");
	}

	@Test
	void rollbackMachtBereitsGeflushteAenderungenDerSelbenTransaktionRueckgaengig() {
		UUID creator = createUser();
		UUID taskId = taskService.create(creator, draft("Alt")).getId();
		TransactionTemplate tx = new TransactionTemplate(txManager);

		assertThatThrownBy(() -> tx.execute(status -> {
			// Wird durch TaskService.update() sofort geflusht (flushOrConflict()).
			taskService.update(creator, taskId,
					new TaskDraft(null, null, "Neu", null, "OPEN", "NORMAL", null, null, null, null), 0L);
			// Verstößt gegen den Titel-Constraint und bricht die Transaktion ab.
			jdbc.update(
					"insert into task (id, creator_id, title, status, priority, created_at, updated_at) values (?,?,?,?,?,now(),now())",
					UUID.randomUUID(), creator, "   ", "OPEN", "NORMAL");
			return null;
		})).isInstanceOf(DataAccessException.class);

		assertThat(jdbc.queryForObject("select title from task where id = ?", String.class, taskId)).isEqualTo("Alt");
		assertThat(jdbc.queryForObject("select version from task where id = ?", Long.class, taskId)).isZero();
	}

}
