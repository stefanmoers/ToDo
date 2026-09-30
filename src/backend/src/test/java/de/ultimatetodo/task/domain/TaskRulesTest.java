package de.ultimatetodo.task.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TaskRulesTest {

	static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");
	static final Instant T1 = Instant.parse("2026-01-02T10:00:00Z");

	@Test
	void titelWirdGetrimmtUndInCodepointsGezaehlt() {
		assertThat(TaskInput.isValidTitle(TaskInput.normalizeTitle("  Einkaufen  "))).isTrue();
		assertThat(TaskInput.normalizeTitle("  Einkaufen  ")).isEqualTo("Einkaufen");
		assertThat(TaskInput.isValidTitle("")).isFalse();
		assertThat(TaskInput.isValidTitle("   ".strip())).isFalse();
		assertThat(TaskInput.isValidTitle("x".repeat(200))).isTrue();
		assertThat(TaskInput.isValidTitle("x".repeat(201))).isFalse();
		assertThat(TaskInput.isValidTitle("😀".repeat(200))).isTrue();
	}

	@Test
	void beschreibungWirdBeiLeereZuNullSonstUnveraendertGelassen() {
		assertThat(TaskInput.normalizeDescription(null)).isNull();
		assertThat(TaskInput.normalizeDescription("   ")).isNull();
		assertThat(TaskInput.normalizeDescription("  hat Leerzeichen  ")).isEqualTo("  hat Leerzeichen  ");
		assertThat(TaskInput.isValidDescription(null)).isTrue();
		assertThat(TaskInput.isValidDescription("x".repeat(10_000))).isTrue();
		assertThat(TaskInput.isValidDescription("x".repeat(10_001))).isFalse();
	}

	@Test
	void prioritaetWirdGeparstUndHatFachlicheRangfolge() {
		assertThat(TaskPriority.parse("URGENT")).contains(TaskPriority.URGENT);
		assertThat(TaskPriority.parse("urgent")).isEmpty();
		assertThat(TaskPriority.parse("SUPER")).isEmpty();
		assertThat(TaskPriority.LOW.rank()).isLessThan(TaskPriority.NORMAL.rank());
		assertThat(TaskPriority.NORMAL.rank()).isLessThan(TaskPriority.HIGH.rank());
		assertThat(TaskPriority.HIGH.rank()).isLessThan(TaskPriority.URGENT.rank());
	}

	@Test
	void statusWirdGeparst() {
		assertThat(TaskStatus.parse("DONE")).contains(TaskStatus.DONE);
		assertThat(TaskStatus.parse("done")).isEmpty();
		assertThat(TaskStatus.parse("FOO")).isEmpty();
	}

	@Test
	void datumZeitZoneOffsetParser() {
		assertThat(TaskInput.parseDate("2026-02-30")).isEmpty();
		assertThat(TaskInput.parseDate("2026-02-28")).isPresent();
		assertThat(TaskInput.parseTime("24:00")).isEmpty();
		assertThat(TaskInput.parseTime("9:00")).isEmpty();
		assertThat(TaskInput.parseTime("09:00:30")).isEmpty();
		assertThat(TaskInput.parseTime("09:00")).isPresent();
		assertThat(TaskInput.parseZone("Mars/Base")).isEmpty();
		assertThat(TaskInput.parseZone("+02:00")).isEmpty();
		assertThat(TaskInput.parseZone("Europe/Berlin")).isPresent();
		assertThat(TaskInput.parseOffset("+02:00")).isPresent();
		assertThat(TaskInput.parseOffset("2:00")).isEmpty();
	}

	@Test
	void anlegenSetztOpenUndNormal() {
		Task t = Task.create(UUID.randomUUID(), null, "Einkaufen", null, TaskPriority.NORMAL, null, T0);
		assertThat(t.getStatus()).isEqualTo(TaskStatus.OPEN);
		assertThat(t.getPriority()).isEqualTo(TaskPriority.NORMAL);
		assertThat(t.getCompletedAt()).isNull();
		assertThat(t.getCreatedAt()).isEqualTo(t.getUpdatedAt()).isEqualTo(T0);
	}

	@Test
	void erledigenSetztZeitpunktUndIstBeimZweitenMalWirkungslos() {
		Task t = Task.create(UUID.randomUUID(), null, "X", null, TaskPriority.NORMAL, null, T0);
		assertThat(t.complete(T1)).isTrue();
		assertThat(t.getStatus()).isEqualTo(TaskStatus.DONE);
		assertThat(t.getCompletedAt()).isEqualTo(T1);
		Instant t2 = T1.plusSeconds(60);
		assertThat(t.complete(t2)).isFalse();
		assertThat(t.getCompletedAt()).isEqualTo(T1);
	}

	@Test
	void wiederOeffnenLeertZeitpunktUndIstAufOffenerAufgabeWirkungslos() {
		Task t = Task.create(UUID.randomUUID(), null, "X", null, TaskPriority.NORMAL, null, T0);
		assertThat(t.reopen(T1)).isFalse();
		t.complete(T1);
		assertThat(t.reopen(T1.plusSeconds(5))).isTrue();
		assertThat(t.getStatus()).isEqualTo(TaskStatus.OPEN);
		assertThat(t.getCompletedAt()).isNull();
	}

	@Test
	void bearbeitenOhneAenderungLiefertFalseUndLaesstUpdatedAtUnveraendert() {
		Task t = Task.create(UUID.randomUUID(), null, "X", "Beschreibung", TaskPriority.NORMAL, null, T0);
		boolean changed = t.update(null, "X", "Beschreibung", TaskStatus.OPEN, TaskPriority.NORMAL, null, T1);
		assertThat(changed).isFalse();
		assertThat(t.getUpdatedAt()).isEqualTo(T0);
	}

	@Test
	void bearbeitenAufDoneSetztUndAufAndereEntferntCompletedAt() {
		Task t = Task.create(UUID.randomUUID(), null, "X", null, TaskPriority.NORMAL, null, T0);
		t.update(null, "X", null, TaskStatus.DONE, TaskPriority.NORMAL, null, T1);
		assertThat(t.getCompletedAt()).isEqualTo(T1);
		t.update(null, "X", null, TaskStatus.IN_PROGRESS, TaskPriority.NORMAL, null, T1.plusSeconds(1));
		assertThat(t.getCompletedAt()).isNull();
	}

	@Test
	void erledigteAufgabeMitFaelligkeitInDerVergangenheitIstNichtUeberfaellig() {
		DueDate due = DueDate.resolve(java.time.LocalDate.of(2020, 1, 1), null, ZoneId.of("UTC"), null);
		Task t = Task.create(UUID.randomUUID(), null, "X", null, TaskPriority.NORMAL, due, T0);
		assertThat(t.isOverdue(T1)).isTrue();
		t.complete(T1);
		assertThat(t.isOverdue(T1)).isFalse();
	}

}
