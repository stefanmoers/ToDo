package de.ultimatetodo.tag.application;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Schmale öffentliche Schnittstelle des Tag-Moduls für andere Module (docs/ARCHITECTURE.md): kein direkter
 * Zugriff auf {@code TagRepository} von außerhalb.
 */
public interface TagAccess {

	/** {@code true}, wenn jede der angegebenen IDs existiert und für {@code userId} zugänglich ist. */
	boolean areAccessible(Collection<UUID> tagIds, UUID userId);

	/** Nur für bereits geprüfte IDs (siehe {@link #areAccessible}); prüft die Zugehörigkeit nicht erneut. */
	List<TagSummary> summarize(Collection<UUID> tagIds);

}
