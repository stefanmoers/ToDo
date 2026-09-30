package de.ultimatetodo.project.application;

import java.util.UUID;

/**
 * Schmaler, öffentlicher Ausschnitt eines Projekts für andere Module (US-181: Herkunft eines Tags anzeigen):
 * Name und Eigentümer, nie Beschreibung oder Mitgliederliste. {@code ownerDisplayName} ist {@code null}, wenn
 * das Konto nicht mehr aufgelöst werden kann.
 */
public record ProjectSummary(UUID id, String name, UUID ownerId, String ownerDisplayName) {
}
