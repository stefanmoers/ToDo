# UltimateTodo

UltimateTodo wird eine Webanwendung für persönliche und gemeinsame Aufgaben. Sie dient als Referenzprojekt für die Ausbildung und für die Arbeit mit Coding-Agenten. Azubis sollen die Architektur erklären, die Technologien einordnen und die Anwendung selbst installieren können.

**Stand: 30.09.2026 – M0 bis M2 umgesetzt, bis auf einen offenen Testfall in US-042** (M0, M1a, M1b, US-023, M1c und M2 — Mitgliedschaft, Zuweisung, Unteraufgaben, Kommentare, Aktivitätshistorie und Kontenadministration; Details je Story im [Backlog](BACKLOG.md)). Registrierung, Anmeldung mit Session und CSRF, Profil, Passwortänderung, Aufgaben anlegen/ansehen/bearbeiten/löschen/erledigen mit optimistischem Sperren, Unteraufgaben (Checkliste mit Fortschrittsanzeige), Projekte anlegen/bearbeiten/löschen/teilen (inkl. Zuordnung von Aufgaben zu eigenen und geteilten Projekten), Mitglieder aufnehmen/entfernen, Aufgaben Mitgliedern zuweisen, Kommentare und eine chronologische Änderungshistorie je Aufgabe, Tags (persönlich oder projektbezogen, Zuordnung zu Aufgaben), Suche, Filter, Sortierung, fünf Standardansichten, eine persönliche Übersicht mit Zählern sowie Konten auflisten/deaktivieren/reaktivieren für Administratoren laufen im Compose-Stack. Eine echte Passwort-Wiederherstellung ist für dieses Demo-Projekt bewusst nicht umgesetzt (Auftraggeber-Entscheidung, siehe Backlog zu US-013); M3 (Wiederkehrende Aufgaben, Erinnerungen) ist weiterhin Zielbild.

**Oberfläche:** Die Gestaltung wurde für die Vorführung überarbeitet: einheitliche helle und dunkle Farbgebung, breiterer Seitenrahmen, klarere Aufgabenkarten und mobil aufklappbare Filter. Die automatisierten Prüfungen sind grün; die manuelle Screenreader-Prüfung und ein echter Browser-Zoom-Test stehen noch aus ([US-180](BACKLOG.md#us-180--oberfläche-einheitlich-und-übersichtlich-gestalten--nach-m2-vor-der-vorführung--should--in-arbeit), [Prüfprotokoll](docs/reviews/ui-US180.md)).

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

**Heute:** Der komplette Stack startet mit `cp .env.example .env` (Passwort setzen) und `docker compose up -d --build --wait`; danach ist die Anwendung unter <http://localhost:8080> erreichbar. Konto registrieren, anmelden, Aufgaben und Projekte anlegen/bearbeiten/erledigen/löschen, Unteraufgaben verwalten, Projekte teilen und Aufgaben Mitgliedern zuweisen, Aufgaben kommentieren und ihre Änderungshistorie einsehen, Tags anlegen/zuordnen/filtern, Aufgaben durchsuchen/filtern/sortieren, Standardansichten, die persönliche Übersicht sowie Konten auflisten/deaktivieren/reaktivieren als Administrator (erstmalige Einrichtung über `ADMIN_BOOTSTRAP_EMAIL`, siehe [DEPLOYMENT.md](docs/DEPLOYMENT.md)) funktionieren bereits. Eine echte Passwort-Wiederherstellung ist für dieses Demo-Projekt bewusst nicht umgesetzt.

Die Projektvorstellung (18 Folien mit Sprechernotizen) läuft ohne Anmeldung direkt in der Anwendung: <http://localhost:8080/präsentation> (Bedienung mit Pfeiltasten oder den Pfeilen neben der Folie, `N` Notizen, `F` Vollbild; Story US-182). Quellen und Pflegehinweise stehen in [docs/praesentation](docs/praesentation/README.md).

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
| [Präsentation](docs/praesentation/README.md) | Wie stelle ich das Projekt in etwa 20 Minuten vor? |

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

**M1c ist inhaltlich abgeschlossen; M2 bis auf einen offenen Testfall ebenfalls.** Die Frischinstallations-Probe (US-160) ist für ihren M1-Anteil durchgeführt und protokolliert ([Installationsprotokoll](docs/reviews/installation-M1.md)), ihr M4-Anteil (Update, Sicherung, Wiederherstellung) folgt erst dort. Aus M2 sind Mitgliedschaft, Zuweisung, Unteraufgaben, Kommentare, die Aktivitätshistorie und Kontenadministration (US-060 bis US-062, US-070, US-071, US-080, US-081, US-014) umgesetzt und schließen dabei die zugehörigen M2-Teile von US-022, US-031, US-032 und US-051 sowie die zuvor an der Aktivitätshistorie hängenden Protokollierungs-Akzeptanzkriterien von US-034, US-035 und US-121 (M1-Teil) ab. US-042 bleibt `IN ARBEIT`: die Löschung selbst arbeitet korrekt (Aufgaben bleiben erhalten, Mitgliedschaften/Tags/Zuweisungen werden bereinigt), es fehlt aber noch der in ihrem eigenen Akzeptanzkriterium verlangte Test mit mehreren Erstellern einschließlich eines deaktivierten Kontos. US-013 (Passwort-Reset) ist mit vom Auftraggeber bewusst reduziertem Umfang erledigt: statt einer echten Wiederherstellung informiert die Anwendung, dass diese im Demo-Projekt nicht verfügbar ist. Nächster sinnvoller Schritt ist damit, den offenen Testfall von US-042 zu ergänzen; danach ist M2 vollständig und **M3 – Zeitfunktionen** (US-090 Wiederkehrende Aufgaben, US-100 Persönliche Erinnerungen) folgt. Automatisiert geprüft (Backend-/Frontend-Unit-, Integrations- und die vollständige Playwright-/axe-Suite über `scripts/e2e.sh`, alle grün); noch offen: die manuelle Accessibility-Prüfung (US-172) durch den Auftraggeber selbst. Der Coding-Agent liest vorher die [Arbeitsregeln](AGENTS.md) und die zur Story gehörenden Dokumente. Eine Story ist erst abgeschlossen, wenn ihre Akzeptanzkriterien und die Definition of Done erfüllt sind.
