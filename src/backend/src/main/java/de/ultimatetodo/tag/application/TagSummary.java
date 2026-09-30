package de.ultimatetodo.tag.application;

import java.util.UUID;

/** Minimale, öffentliche Darstellung eines Tags für andere Module (z. B. task, um Aufgaben mit Tags anzuzeigen). */
public record TagSummary(UUID id, String name) {
}
