package de.ultimatetodo.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class RateLimiterTest {

	private final MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
	private final RateLimiter limiter = new RateLimiter(clock);

	@Test
	void erlaubtVersucheBisZumLimitUndWeistDanachAb() {
		for (int i = 0; i < 3; i++) {
			limiter.hit("k", 3, Duration.ofMinutes(15));
		}
		assertThatThrownBy(() -> limiter.hit("k", 3, Duration.ofMinutes(15)))
			.isInstanceOfSatisfying(ApiException.class, e -> {
				assertThat(e.status()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
				assertThat(e.retryAfterSeconds()).isBetween(1L, 900L);
			});
	}

	@Test
	void nachAblaufDesFenstersSindWiederVersucheMoeglich() {
		for (int i = 0; i < 3; i++) {
			limiter.hit("k", 3, Duration.ofMinutes(15));
		}
		clock.advance(Duration.ofMinutes(15));
		assertThatCode(() -> limiter.hit("k", 3, Duration.ofMinutes(15))).doesNotThrowAnyException();
	}

	@Test
	void schluesselSindUnabhaengig() {
		limiter.hit("a", 1, Duration.ofMinutes(1));
		assertThatCode(() -> limiter.hit("b", 1, Duration.ofMinutes(1))).doesNotThrowAnyException();
	}

	static class MutableClock extends java.time.Clock {
		private Instant now;

		MutableClock(Instant start) { this.now = start; }

		void advance(Duration d) { now = now.plus(d); }

		@Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }

		@Override public java.time.Clock withZone(java.time.ZoneId zone) { return this; }

		@Override public Instant instant() { return now; }
	}

}
