package de.ultimatetodo.common;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Einfache Ratenbegrenzung im Prozess (Festes Zeitfenster je Schlüssel). Keine dauerhafte Sperre:
 * nach Ablauf des Fensters sind wieder Versuche möglich. Für eine einzelne Backend-Instanz ausreichend
 * (docs/ARCHITECTURE.md); mehrere Instanzen bräuchten einen gemeinsamen Speicher.
 */
@Component
public class RateLimiter {

	private record Window(Instant start, int count) {
	}

	private final Clock clock;
	private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

	public RateLimiter(Clock clock) {
		this.clock = clock;
	}

	/** Zählt einen Versuch und wirft {@code 429}, wenn das Limit im laufenden Fenster überschritten ist. */
	public void hit(String key, int maxAttempts, Duration window) {
		Instant now = clock.instant();
		Window w = windows.compute(key, (k, old) -> {
			if (old == null || !now.isBefore(old.start().plus(window))) {
				return new Window(now, 1);
			}
			return new Window(old.start(), old.count() + 1);
		});
		if (w.count() > maxAttempts) {
			long retry = Math.max(1, Duration.between(now, w.start().plus(window)).toSeconds());
			throw ApiException.tooManyRequests(retry);
		}
		if (windows.size() > 10_000) {
			windows.entrySet().removeIf(e -> !now.isBefore(e.getValue().start().plus(window)));
		}
	}

	/** Setzt den Zähler nach einem Erfolg zurück (nur für Schlüssel eines einzelnen Kontos sinnvoll). */
	public void reset(String key) {
		windows.remove(key);
	}

}
