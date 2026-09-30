# Präsentation „UltimateTodo – Projektvorstellung“

[Zur Übersicht](../../README.md)

Foliensatz (18 Folien, 16:9) für die Vorstellung des Projekts vor der Live-Demo. Er läuft direkt in der Anwendung unter `/präsentation` (lokal <http://localhost:8080/präsentation>; Pfeiltasten oder Pfeil-Schaltflächen, `N` Sprechernotizen, `F` Vollbild, US-182). Inhalt: Ziel und Umfang, Architektur, Technologien und Entscheidungen, Deployment, Sicherheit und Datenintegrität, Tests, Barrierefreiheit, Arbeiten mit Coding-Agenten, ehrlicher Stand und Ablauf der Demo. Die Sprechernotizen stehen je Folie im `<aside>`-Element der jeweiligen Datei.

## Wo liegen die Folien?

| Ort | Zweck |
|---|---|
| `src/frontend/src/features/presentation/slides.tsx` | Folien der In-App-Version als React-Komponenten, inklusive Sprechernotizen |
| `src/frontend/src/features/presentation/presentation.css` | Gestaltung (eigene Klassen, keine Inline-Stile, wegen der Content-Security-Policy) |
| `src/frontend/src/pages/PresentationPage.tsx` | Seite mit Navigation, Tastatur, Notizen und Vollbild; Route in `App.tsx` |
| Dieser Ordner | Quellen für das Slides-Artefakt (siehe unten) |

Inhaltliche Änderungen an den Folien gehören in `slides.tsx`; die Dateien in diesem Ordner sind der Stand für das Slides-Artefakt und müssen bei Textänderungen bewusst nachgezogen werden.

## Inhalt dieses Ordners

| Datei | Zweck |
|---|---|
| `deck.json` | Index der Präsentation: Titel, Folienreihenfolge, Abschnitte, Schriften |
| `slides/<id>.html` | Eine Folie je Datei (1920 × 1080, Inline-Stile, Sprechernotizen am Ende) |

Das Quellformat stammt aus dem Slides-Artefakt von Claude. Die Dateien lassen sich nicht direkt im Browser als Foliensatz anzeigen. Zum Vortragen das Artefakt öffnen oder den Foliensatz dort als PowerPoint (.pptx) oder PDF exportieren. Zum Weiterbearbeiten die Dateien wieder in ein Slides-Artefakt hochladen (`project/deck.json` und `project/slides/*.html`).

## Stand und Pflege

- Erstellt am 30.09.2026 aus `README.md`, `BACKLOG.md`, den Dokumenten unter `docs/` und den fünf ADRs.
- **Vor einem Vortrag prüfen:** Die Testzahlen auf Folie „Tests“ (168 Backend, 126 Frontend, 57 E2E) stammen aus `docs/TESTING.md` (Stand M2) und dem Frontend-Lauf vor US-182; das Frontend hat inzwischen 137 Unit-Tests. Die Folie „Ehrlicher Stand“ muss zum aktuellen Backlog passen. Beides nach Änderungen an Tests oder Meilensteinen aktualisieren.
- Die Folien enthalten nur Aussagen, die in der Projektdokumentation stehen. Bei Widersprüchen gilt die Dokumentation.
