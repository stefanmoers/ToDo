package de.ultimatetodo.tag.application;

import de.ultimatetodo.tag.domain.Tag;
import java.util.UUID;

/**
 * Ein Tag samt Herkunft (US-181): Bei Projekt-Tags Name und Eigentümer des Projekts, damit gleichnamige Tags
 * verschiedener Projekte oder Geltungsbereiche unterscheidbar sind. Bei persönlichen Tags sind die
 * Projektfelder {@code null}.
 */
public record TagView(Tag tag, String projectName, UUID projectOwnerId, String projectOwnerDisplayName) {
}
