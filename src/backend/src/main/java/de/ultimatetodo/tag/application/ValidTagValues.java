package de.ultimatetodo.tag.application;

import java.util.UUID;

/** Geprüfte, normalisierte Werte einer Tag-Eingabe. {@code projectId} ist nur formal geprüft (gültige UUID);
 * die Eigentümerschaft prüft {@code TagService} über {@code ProjectAccess}. */
public record ValidTagValues(String name, UUID projectId) {
}
