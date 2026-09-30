package de.ultimatetodo.task.application;

import de.ultimatetodo.task.domain.TaskPriority;
import de.ultimatetodo.task.domain.TaskStatus;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/**
 * Geprüfte Filterkriterien der Aufgabenliste (US-050, US-051). Unterschiedliche Filterarten werden mit UND
 * verknüpft, mehrere Werte innerhalb einer Filterart mit ODER (leere Menge = keine Einschränkung dieser Art).
 * {@code dueDateFrom}/{@code dueDateTo} vergleichen das rohe Fälligkeitsdatum ohne Zeitzonenumrechnung
 * („Datumsaufgaben behalten ihr Datum", docs/DOMAIN_MODEL.md); die Kontozeitzone zur Auflösung von „heute" löst
 * ausschließlich der Client, damit das Aufgaben-Modul weiterhin nicht auf das Benutzer-Modul zugreift.
 */
public record TaskListFilter(String searchText, Set<TaskStatus> statuses, Set<TaskPriority> priorities,
		Set<UUID> projectIds, Set<UUID> tagIds, boolean overdueOnly, LocalDate dueDateFrom, LocalDate dueDateTo) {
}
