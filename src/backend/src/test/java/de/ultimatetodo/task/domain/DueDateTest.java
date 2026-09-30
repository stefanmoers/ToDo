package de.ultimatetodo.task.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.ultimatetodo.task.domain.InvalidDueDateException.Problem;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

/** US-037: Fälligkeit, insbesondere die Sommerzeit-Randfälle in Europe/Berlin 2026. */
class DueDateTest {

	static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");

	@Test
	void reinesDatumIstEndeDesTagesInGespeicherterZone() {
		DueDate sommer = DueDate.resolve(LocalDate.of(2026, 7, 15), null, BERLIN, null);
		assertThat(sommer.deadline()).isEqualTo(Instant.parse("2026-07-15T22:00:00Z"));
		assertThat(sommer.exactInstant()).isNull();

		DueDate winter = DueDate.resolve(LocalDate.of(2026, 1, 15), null, BERLIN, null);
		assertThat(winter.deadline()).isEqualTo(Instant.parse("2026-01-15T23:00:00Z"));
	}

	@Test
	void ueberfaelligGenauAbDemFaelligkeitszeitpunkt() {
		DueDate due = DueDate.resolve(LocalDate.of(2026, 7, 15), LocalTime.of(12, 0), BERLIN, ZoneOffset.of("+02:00"));
		Instant deadline = due.deadline();
		Task t = Task.create(UUID.randomUUID(), null, "X", null, TaskPriority.NORMAL, due, Instant.EPOCH);
		assertThat(t.isOverdue(deadline.minusNanos(1))).isFalse();
		assertThat(t.isOverdue(deadline)).isTrue();
	}

	@Test
	void fruehjahrsLueckeWirdAbgewiesen() {
		// 2026-03-29 02:00 -> 03:00 in Europe/Berlin: 02:30 gibt es an diesem Tag nicht.
		assertThatThrownBy(resolve(LocalDate.of(2026, 3, 29), LocalTime.of(2, 30), BERLIN, null))
			.isInstanceOf(InvalidDueDateException.class)
			.satisfies(e -> assertThat(((InvalidDueDateException) e).problem()).isEqualTo(Problem.TIME_NONEXISTENT));
	}

	@Test
	void herbstUeberlappungBrauchtOffsetAuswahl() {
		// 2026-10-25 03:00 -> 02:00 in Europe/Berlin: 02:30 kommt zweimal vor.
		LocalDate date = LocalDate.of(2026, 10, 25);
		LocalTime time = LocalTime.of(2, 30);

		assertThatThrownBy(resolve(date, time, BERLIN, null)).isInstanceOf(InvalidDueDateException.class)
			.satisfies(e -> {
				InvalidDueDateException ex = (InvalidDueDateException) e;
				assertThat(ex.problem()).isEqualTo(Problem.OFFSET_REQUIRED);
				assertThat(ex.validOffsets()).containsExactlyInAnyOrder(ZoneOffset.of("+02:00"), ZoneOffset.of("+01:00"));
			});

		DueDate sommerzeit = DueDate.resolve(date, time, BERLIN, ZoneOffset.of("+02:00"));
		assertThat(sommerzeit.deadline()).isEqualTo(Instant.parse("2026-10-25T00:30:00Z"));

		DueDate normalzeit = DueDate.resolve(date, time, BERLIN, ZoneOffset.of("+01:00"));
		assertThat(normalzeit.deadline()).isEqualTo(Instant.parse("2026-10-25T01:30:00Z"));

		assertThatThrownBy(resolve(date, time, BERLIN, ZoneOffset.of("+03:00")))
			.isInstanceOf(InvalidDueDateException.class)
			.satisfies(e -> assertThat(((InvalidDueDateException) e).problem()).isEqualTo(Problem.OFFSET_INVALID));
	}

	@Test
	void eindeutigeZeitMitPassendemOffsetOkMitFalschemAbgewiesen() {
		LocalDate date = LocalDate.of(2026, 7, 15);
		LocalTime time = LocalTime.of(9, 0);
		DueDate ok = DueDate.resolve(date, time, BERLIN, ZoneOffset.of("+02:00"));
		assertThat(ok.deadline()).isEqualTo(Instant.parse("2026-07-15T07:00:00Z"));
		assertThatThrownBy(resolve(date, time, BERLIN, ZoneOffset.of("+01:00")))
			.isInstanceOf(InvalidDueDateException.class)
			.satisfies(e -> assertThat(((InvalidDueDateException) e).problem()).isEqualTo(Problem.OFFSET_INVALID));
	}

	@Test
	void gleicheLokaleZeitInAnderenZonenErgibtUnterschiedlicheInstants() {
		LocalDate date = LocalDate.of(2026, 7, 15);
		LocalTime time = LocalTime.of(9, 0);
		DueDate auckland = DueDate.resolve(date, time, ZoneId.of("Pacific/Auckland"), null);
		DueDate losAngeles = DueDate.resolve(date, time, ZoneId.of("America/Los_Angeles"), null);
		assertThat(auckland.deadline()).isNotEqualTo(losAngeles.deadline());
	}

	@Test
	void uhrzeitOhneDatumUndOffsetOhneUhrzeitWerdenAbgewiesen() {
		assertThatThrownBy(resolve(null, LocalTime.of(9, 0), BERLIN, null)).isInstanceOf(InvalidDueDateException.class)
			.satisfies(e -> assertThat(((InvalidDueDateException) e).problem()).isEqualTo(Problem.TIME_REQUIRES_DATE));
		assertThatThrownBy(resolve(LocalDate.of(2026, 7, 15), null, BERLIN, ZoneOffset.of("+02:00")))
			.isInstanceOf(InvalidDueDateException.class)
			.satisfies(e -> assertThat(((InvalidDueDateException) e).problem()).isEqualTo(Problem.OFFSET_WITHOUT_TIME));
	}

	@Test
	void ohneDatumUndZeitGibtEsKeineFaelligkeit() {
		assertThat(DueDate.resolve(null, null, null, null)).isNull();
	}

	private static ThrowingCallable resolve(LocalDate date, LocalTime time, ZoneId zone, ZoneOffset offset) {
		return () -> DueDate.resolve(date, time, zone, offset);
	}

}
