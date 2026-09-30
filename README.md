# UltimateTodo

UltimateTodo wird eine Webanwendung für persönliche und gemeinsame Aufgaben. Sie dient als Referenzprojekt für die Ausbildung und für die Arbeit mit Coding-Agenten. Azubis sollen die Architektur erklären, die Technologien einordnen und die Anwendung selbst installieren können.

**Stand: 30.09.2026 – M0 (Grundlage), M1a (Konten und Anmeldung), M1b (Aufgaben-Kern) und US-023 (Sitzungs-Timeout-Warnung) umgesetzt.** Registrierung, Anmeldung mit Session und CSRF, Profil, Passwortänderung sowie Aufgaben anlegen/ansehen/bearbeiten/löschen/erledigen mit optimistischem Sperren laufen im Compose-Stack. Projekte, Tags, Suche/Filter/Sortierung und Zusammenarbeit folgen ab M1c/M2; ihre Beschreibungen in den Fachdokumenten sind weiterhin Zielbild. Den Status je Story zeigt das [Backlog](BACKLOG.md).

## Was entsteht?

- Benutzerkonten mit Anmeldung und geschützten persönlichen Daten.
- Aufgaben mit Status, Priorität, Fälligkeit, Projekten, Tags und Unteraufgaben.
- Geteilte Projekte, Zuweisungen, Kommentare und Änderungshistorie.
- Suche, Filter, Übersichten, wiederkehrende Aufgaben und Erinnerungen.
- Eine deutsche, barrierearme Oberfläche mit **WCAG 2.2 AA als Qualitätsziel**.

Der vollständige Umfang, die Reihenfolge und prüfbare Akzeptanzkriterien stehen im [Backlog](BACKLOG.md).

## Architektur in einem Absatz

React und TypeScript bilden die Oberfläche im Browser. Das Frontend ruft eine REST-API des Java-21-/Spring-Boot-Backends auf. Das Backend prüft Rechte und fachliche Regeln und speichert Daten in PostgreSQL. Es ist ein **modularer Monolith**: fachlich getrennte Module werden gemeinsam als ein Backend betrieben. Docker Compose startet später die benötigten Dienste. [Architektur mit Diagrammen](docs/ARCHITECTURE.md)

## Technologien

| Aufgabe | Festgelegte Technologien |
|---|---|
| Backend und Build | Java 21, Spring Boot, Maven |
| Oberfläche und Build | React, TypeScript, Vite |
| Daten und Migrationen | PostgreSQL, Flyway |
| Schnittstelle und Anmeldung | REST/JSON, OpenAPI, Spring Security, Session/Cookie |
| Tests | JUnit, Spring Boot Test, Testcontainers, Vitest, Playwright |
| Barrierefreiheit | WCAG 2.2 AA, axe-core, zusätzliche manuelle Prüfungen |
| Lokaler Betrieb | Docker Compose, Spring Boot Actuator |

Die [Technologieübersicht](docs/TECHNOLOGIES.md) erklärt jeweils **was**, **wofür** und **warum**. Die tatsächlich getesteten Versionen stehen in der [Technologieübersicht](docs/TECHNOLOGIES.md#tatsächlich-verwendete-versionen-stand-m0-30092026).

## Orientierung und Start

**Heute:** Der komplette Stack startet mit `cp .env.example .env` (Passwort setzen) und `docker compose up -d --build --wait`; danach ist die Anwendung unter <http://localhost:8080> erreichbar. Konto registrieren, anmelden und Aufgaben anlegen/bearbeiten/erledigen/löschen funktionieren bereits; Projekte, Tags und Zusammenarbeit noch nicht.

**Nach Umsetzung von M0/M1:** Die [Entwicklungsanleitung](docs/DEVELOPMENT.md) beschreibt die Arbeit am Code. Die [Deployment-Anleitung](docs/DEPLOYMENT.md) führt durch Installation, Start, Prüfung, Update und Datensicherung. Die dort genannten künftigen Dateien und Befehle müssen vom umsetzenden Agenten tatsächlich bereitgestellt und überprüft werden.

| Dokument | Frage, die es beantwortet |
|---|---|
| [BACKLOG.md](BACKLOG.md) | Was bauen wir, in welcher Reihenfolge und wann ist es fertig? |
| [AGENTS.md](AGENTS.md) | Wie arbeitet ein Coding-Agent in diesem Repository? |
| [ARCHITECTURE.md](docs/ARCHITECTURE.md) | Welche Bausteine gibt es und wie arbeiten sie zusammen? |
| [TECHNOLOGIES.md](docs/TECHNOLOGIES.md) | Was leisten die eingesetzten Werkzeuge? |
| [DOMAIN_MODEL.md](docs/DOMAIN_MODEL.md) | Welche Daten und fachlichen Regeln gibt es? |
| [ACCESSIBILITY.md](docs/ACCESSIBILITY.md) | Wie entwickeln und prüfen wir Barrierefreiheit? |
| [SECURITY.md](docs/SECURITY.md) | Wie schützen wir Konten und Daten? |
| [TESTING.md](docs/TESTING.md) | Welche Nachweise müssen Tests liefern? |
| [DEVELOPMENT.md](docs/DEVELOPMENT.md) | Wie entwickle und teste ich lokal? |
| [DEPLOYMENT.md](docs/DEPLOYMENT.md) | Wie installiere und betreibe ich die Anwendung? |
| [Architekturentscheidungen](docs/adr/README.md) | Warum wurden diese Entscheidungen getroffen? |

## Projektstruktur

```text
ToDo/
├── README.md
├── BACKLOG.md
├── AGENTS.md
├── .gitignore
├── .env.example             # Konfigurationsvorlage (nur Platzhalter)
├── compose.yaml             # vollständiger Betrieb: web, backend, db
├── compose.dev.yaml         # nur Entwicklungsdatenbank
├── scripts/e2e.sh           # isolierter E2E-/axe-Lauf
├── .github/workflows/ci.yml
├── docs/
│   ├── ARCHITECTURE.md
│   ├── TECHNOLOGIES.md
│   ├── DOMAIN_MODEL.md
│   ├── ACCESSIBILITY.md
│   ├── SECURITY.md
│   ├── TESTING.md
│   ├── DEVELOPMENT.md
│   ├── DEPLOYMENT.md
│   └── adr/                 # Übersicht und fünf erste Entscheidungen
└── src/
    ├── backend/             # Spring Boot (Maven Wrapper, Flyway-Migrationen, Tests)
    └── frontend/            # React, TypeScript, Vite, Vitest, Playwright
```

Die Modulaufteilung innerhalb von `src/backend/` und `src/frontend/` steht in der [Architektur](docs/ARCHITECTURE.md).

## Nächster Arbeitsschritt

**M1c** aus dem Backlog: Projekte, Tags, Suche, Filter, Sortierung, Standardansichten und Dashboard (US-040 bis US-043, US-050 bis US-053, US-110); dazu die Frischinstallations-Probe (US-160). Aus M1b bleiben US-034, US-035 und US-121 bis zur Aktivitätshistorie in M2 als „IN ARBEIT (M1-Teil)“ stehen. Noch nicht automatisiert ausgeführt: die vollständige Playwright-/axe-Suite für Aufgaben (`tasks.spec.ts`, erweitertes `a11y.spec.ts`) über `scripts/e2e.sh` sowie die manuelle Accessibility-Prüfung (US-172) durch den Auftraggeber. Der Coding-Agent liest vorher die [Arbeitsregeln](AGENTS.md) und die zur Story gehörenden Dokumente. Eine Story ist erst abgeschlossen, wenn ihre Akzeptanzkriterien und die Definition of Done erfüllt sind.
