package de.ultimatetodo.project.application;

import de.ultimatetodo.user.application.UserSummary;
import java.time.Instant;

/** Ein Projektmitglied mit aufgelöstem Anzeigenamen (US-060, US-061). */
public record ProjectMember(UserSummary user, Instant addedAt) {
}
