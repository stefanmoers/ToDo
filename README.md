# UltimateTodo

UltimateTodo wird eine Webanwendung für persönliche und gemeinsame Aufgaben. Sie dient als Referenzprojekt für die Ausbildung und für die Arbeit mit Coding-Agenten. Azubis sollen die Architektur erklären, die Technologien einordnen und die Anwendung selbst installieren können.

**Stand: 30.09.2026 – M0 (Grundlage) und M1a (Konten und Anmeldung) umgesetzt.** Registrierung, Anmeldung mit Session und CSRF, Profil und Passwortänderung laufen im Compose-Stack. Aufgaben und Projekte folgen ab M1b; ihre Beschreibungen in den Fachdokumenten sind weiterhin Zielbild. Den Status je Story zeigt das [Backlog](BACKLOG.md).

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

**Heute (M0):** Der komplette Stack startet mit `cp .env.example .env` (Passwort setzen) und `docker compose up -d --build --wait`; danach ist die Startseite unter <http://localhost:8080> erreichbar. Fachfunktionen gibt es noch nicht.

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

**M1b** aus dem Backlog: Aufgaben anlegen, ansehen, bearbeiten, löschen, erledigen; Priorität, Fälligkeit, Versionskonflikte. Offen aus M1a: US-023 (Sitzungs-Timeout mit Warnung). Der Coding-Agent liest vorher die [Arbeitsregeln](AGENTS.md) und die zur Story gehörenden Dokumente. Eine Story ist erst abgeschlossen, wenn ihre Akzeptanzkriterien und die Definition of Done erfüllt sind.
