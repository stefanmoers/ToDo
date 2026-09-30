package de.ultimatetodo.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class UserRulesTest {

	static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");
	static final Instant T1 = Instant.parse("2026-01-02T10:00:00Z");

	@Test
	void deaktivierenUndReaktivierenSindWirkungslosWennBereitsImZielzustand() {
		User u = User.register("max@example.test", "Max", "hash", "Europe/Berlin", T0);
		assertThat(u.reactivate(T1)).isFalse();
		assertThat(u.deactivate(T1)).isTrue();
		assertThat(u.getStatus()).isEqualTo(User.Status.INACTIVE);
		assertThat(u.deactivate(T1.plusSeconds(1))).isFalse();
		assertThat(u.reactivate(T1.plusSeconds(1))).isTrue();
		assertThat(u.getStatus()).isEqualTo(User.Status.ACTIVE);
	}

	@Test
	void befoerdernZumAdministratorIstIdempotent() {
		User u = User.register("max@example.test", "Max", "hash", "Europe/Berlin", T0);
		assertThat(u.getRole()).isEqualTo(User.Role.USER);
		assertThat(u.promoteToAdmin(T1)).isTrue();
		assertThat(u.getRole()).isEqualTo(User.Role.ADMIN);
		assertThat(u.promoteToAdmin(T1.plusSeconds(1))).isFalse();
	}

	@Test
	void passwortGrenzenZaehlenCodepoints() {
		assertThat(PasswordPolicy.isValid("a".repeat(14))).isFalse();
		assertThat(PasswordPolicy.isValid("a".repeat(15))).isTrue();
		assertThat(PasswordPolicy.isValid("a".repeat(128))).isTrue();
		assertThat(PasswordPolicy.isValid("a".repeat(129))).isFalse();
		assertThat(PasswordPolicy.isValid("😀".repeat(15))).isTrue();
		assertThat(PasswordPolicy.isValid(null)).isFalse();
	}

	@Test
	void passwortWirdNichtGetrimmt() {
		assertThat(PasswordPolicy.isValid(" ".repeat(15))).isTrue();
	}

	@Test
	void emailWirdNormalisiertUndGeprueft() {
		assertThat(UserInput.normalizeEmail("  Max@Example.DE ")).isEqualTo("max@example.de");
		assertThat(UserInput.isValidEmail("max@example.de")).isTrue();
		assertThat(UserInput.isValidEmail("max@example")).isFalse();
		assertThat(UserInput.isValidEmail("a b@example.de")).isFalse();
		assertThat(UserInput.isValidEmail("a".repeat(250) + "@b.de")).isFalse();
	}

	@Test
	void zeitzonenMuessenGueltigeIanaNamenSein() {
		assertThat(UserInput.isValidTimeZone("Europe/Berlin")).isTrue();
		assertThat(UserInput.isValidTimeZone("UTC")).isTrue();
		assertThat(UserInput.isValidTimeZone("Mars/Base")).isFalse();
		assertThat(UserInput.isValidTimeZone("+02:00")).isFalse();
		assertThat(UserInput.isValidTimeZone("")).isFalse();
	}

	@Test
	void anzeigenameLaenge() {
		assertThat(UserInput.isValidName(UserInput.normalizeName("   "))).isFalse();
		assertThat(UserInput.isValidName("x".repeat(100))).isTrue();
		assertThat(UserInput.isValidName("x".repeat(101))).isFalse();
	}

}
