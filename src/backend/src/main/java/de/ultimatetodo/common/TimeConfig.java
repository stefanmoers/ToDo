package de.ultimatetodo.common;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Austauschbare Uhr: Zeitregeln werden in Tests mit einer festen Uhr geprüft. */
@Configuration
class TimeConfig {

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

}
