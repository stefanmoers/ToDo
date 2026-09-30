# Architekturentscheidungen (ADRs)

[Zur Projektübersicht](../../README.md) · [Architektur](../ARCHITECTURE.md)

Ein **Architecture Decision Record** hält eine wichtige Entscheidung samt Gründen fest. Damit kann ein Azubi erklären, warum eine Technologie oder Struktur gewählt wurde, ohne die gesamte Entstehungsgeschichte zu kennen.

## Entscheidungsregister

| Nummer | Entscheidung | Status | Datum |
|---|---|---|---|
| [ADR-001](ADR-001-modularer-monolith.md) | Modularer Monolith | Akzeptiert | 30.09.2026 |
| [ADR-002](ADR-002-backend-technologie.md) | Java 21, Spring Boot und Maven | Akzeptiert | 30.09.2026 |
| [ADR-003](ADR-003-frontend-technologie.md) | React, TypeScript und Vite | Akzeptiert | 30.09.2026 |
| [ADR-004](ADR-004-datenbank.md) | PostgreSQL mit Flyway | Akzeptiert | 30.09.2026 |
| [ADR-005](ADR-005-rest-api.md) | REST/JSON mit OpenAPI | Akzeptiert | 30.09.2026 |

„Akzeptiert“ bedeutet hier: Die Technologierichtung wurde für das Projekt festgelegt. Es bedeutet nicht „bereits implementiert oder getestet“. Konkrete Versionsnummern und markierte Detailannahmen müssen bei der Umsetzung noch verifiziert werden.

## Wann eine neue ADR?

Bei Entscheidungen mit längerfristigen Auswirkungen, etwa einer neuen Authentifizierungsart, wesentlichen zusätzlichen Infrastruktur oder einem Wechsel des Datenmodells. Nicht jede Umbenennung oder kleine Hilfsbibliothek braucht eine eigene ADR. Die Entscheidung soll in Aufwand und Nutzen angemessen dokumentiert werden.

Neue ADRs erhalten die nächste freie Nummer. Bestehende akzeptierte Entscheidungen nicht stillschweigend umschreiben. Eine neue Entscheidung verlinkt ihre Vorgängerin; diese wird als „ersetzt durch ADR-…“ markiert. Historie und Gründe bleiben lesbar.

## Vorlage

```markdown
# ADR-NNN – Kurzer Titel

- Status: Vorgeschlagen / Akzeptiert / Verworfen / Ersetzt
- Datum: JJJJ-MM-TT
- Bezug: Betroffene Stories, Dokumente und frühere ADRs

## Kontext

Welches konkrete Problem lösen wir? Welche Rahmenbedingungen gelten?

## Entscheidung

Was gilt künftig? Was muss noch überprüft werden?

## Gründe und Alternativen

Welche realistischen Alternativen gab es, und warum passt die gewählte Lösung?

## Konsequenzen

Welche Vorteile, Nachteile und Folgearbeiten entstehen?

## Überprüfung

Welche Nachweise sind nötig, und wann sollte die Entscheidung neu betrachtet werden?
```
