package de.ultimatetodo.user.application;

import java.util.UUID;

/**
 * Schmaler, öffentlicher Ausschnitt eines Benutzerkontos für andere Module (Mitglieder, Zuweisungen,
 * Kommentar-Autoren, Aktivitäten): nie Passwort-Hash, Rolle oder E-Mail-Adresse.
 */
public record UserSummary(UUID id, String displayName, boolean active) {
}
