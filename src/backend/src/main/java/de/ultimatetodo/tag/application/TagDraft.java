package de.ultimatetodo.tag.application;

/** Rohe, ungeprüfte Eingabe aus der API. {@code projectId} ist {@code null} für einen persönlichen Tag. */
public record TagDraft(String name, String projectId) {
}
