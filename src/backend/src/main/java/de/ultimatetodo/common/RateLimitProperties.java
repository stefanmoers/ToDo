package de.ultimatetodo.common;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(int loginAttempts, Duration loginWindow, int registerAttempts,
		Duration registerWindow, int passwordChangeAttempts, Duration passwordChangeWindow) {
}
