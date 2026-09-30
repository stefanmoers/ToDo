package de.ultimatetodo.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserRulesTest {

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
