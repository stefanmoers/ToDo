# UltimateTodo

UltimateTodo wird eine Webanwendung für persönliche und gemeinsame Aufgaben. Sie dient als Referenzprojekt für die Ausbildung und für die Arbeit mit Coding-Agenten. Azubis sollen die Architektur erklären, die Technologien einordnen und die Anwendung selbst installieren können.

**Stand: 30.09.2026 – Dokumentationsbasis, noch keine lauffähige Anwendung.** Dieses Repository enthält Anforderungen und Architekturentscheidungen. Backend, Frontend, Build-Dateien, Container und Tests werden erst in den folgenden Meilensteinen umgesetzt. Die Anleitungen beschreiben dafür verbindliche Zielabläufe und behaupten keinen bereits erfolgreichen Start.

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

Die [Technologieübersicht](docs/TECHNOLOGIES.md) erklärt jeweils **was**, **wofür** und **warum**. Außer Java 21 sind konkrete Paketversionen noch nicht festgeschrieben; die kompatible Kombination wird in M0 geprüft und anschließend versioniert.

## Orientierung und Start

**Heute:** Die Dokumentation lesen; es gibt noch keinen Startbefehl für eine Anwendung.

**Nach Umsetzung von M0/M1:** Die [Entwicklungsanleitung](docs/DEVELOPMENT.md) beschreibt die Arbeit am Code. Die [Deployment-Anleitung](docs/DEPLOYMENT.md) führt einen anderen Azubi durch Installation, Start, Prüfung, Update und Datensicherung. Die dort genannten künftigen Dateien und Befehle müssen vom umsetzenden Agenten tatsächlich bereitgestellt und überprüft werden.

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
└── src/                     # zunächst leer; nicht von Git gespeichert
```

Die geplante Aufteilung in `src/backend/` und `src/frontend/` steht in der Architektur. Ein leerer Ordner erscheint nicht in einem Git-Clone; der erste Implementierungsschritt legt ihn bei Bedarf neu an.

## Nächster Arbeitsschritt

Den Coding-Agenten die [Arbeitsregeln](AGENTS.md) und alle Dokumente lesen lassen. Anschließend **M0** aus dem Backlog umsetzen, die geprüften Versionsstände dokumentieren und Ergebnisse einschließlich verbleibender Grenzen berichten. Die übrigen Meilensteine folgen schrittweise. Eine Story ist erst abgeschlossen, wenn ihre Akzeptanzkriterien und die Definition of Done erfüllt sind.
