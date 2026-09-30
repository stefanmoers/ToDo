# Technologien verständlich erklärt

[Zur Übersicht](../README.md) · [Architektur](ARCHITECTURE.md)

**Status:** Stack festgelegt; die in M0 verwendeten Versionen stehen unten und wurden am 30.09.2026 tatsächlich gebaut und getestet.

| Technologie | Was ist das? | Wofür verwenden wir sie? | Warum hier? |
|---|---|---|---|
| Java 21 | Programmiersprache und Laufzeit | Backend und Fachlogik | Festgelegte gemeinsame Grundlage für die Ausbildung |
| Spring Boot | Rahmen für Java-Anwendungen mit vorkonfigurierten Bausteinen | HTTP-Anwendung, Konfiguration und Integration | Spart wiederkehrende Einrichtung und hält eine normale Java-Anwendung erkennbar |
| Spring Security | Sicherheitsbaustein für Spring | Anmeldung, Sessions, CSRF und Rechteprüfung | Bewährte Mechanismen statt selbst erfundener Passwort- oder Sessionverwaltung |
| Maven | Build- und Abhängigkeitswerkzeug | Java kompilieren, Tests starten, Anwendung paketieren | Ein nachvollziehbarer Build; der Wrapper legt die Maven-Version fest |
| React | Bibliothek für Benutzeroberflächen | Ansichten und wiederverwendbare Bedienelemente | Klare Aufteilung in Komponenten und Zustand |
| TypeScript | JavaScript mit Typprüfung | Frontend-Code und API-Datentypen | Erkennt viele Formfehler vor der Ausführung; ersetzt keine Laufzeitvalidierung |
| Vite | Entwicklungsserver und Frontend-Build-Werkzeug | Schnelle lokale Entwicklung und statische Produktionsdateien | Überschaubarer Aufbau einer Browseranwendung |
| Node.js und npm | Laufzeit für Entwicklungswerkzeuge und Paketverwaltung | Frontend installieren, bauen und testen | Werden für die Werkzeuge gebraucht; die fertige Oberfläche läuft im Browser |
| PostgreSQL | Relationale Datenbank | Benutzer, Projekte und Aufgaben dauerhaft speichern | Beziehungen, Fremdschlüssel und Transaktionen passen zum Datenmodell |
| Flyway | Werkzeug für versionierte Datenbankänderungen | SQL-Migrationen in definierter Reihenfolge anwenden | Jeder Rechner erhält denselben nachvollziehbaren Schemazustand |
| REST und JSON | HTTP-orientierter Schnittstellenstil und Datenformat | Austausch zwischen Browser und Backend | Ressourcen und Standardmethoden sind gut erklärbar und testbar |
| OpenAPI | Maschinenlesbare API-Beschreibung | Pfade, Eingaben, Antworten, Fehler und Authentifizierung dokumentieren | Frontend und Backend teilen einen überprüfbaren Vertrag |
| JUnit / Spring Boot Test | Werkzeuge für Java-Tests | Fachregeln und Zusammenspiel der Backend-Komponenten prüfen | Tests reichen von kleinen Funktionen bis zur laufenden Anwendung |
| Testcontainers | Startet kurzlebige Container für Tests | Integrationstests gegen echtes PostgreSQL | Prüft tatsächliches Datenbankverhalten statt einer abweichenden Ersatzdatenbank |
| Vitest | Testwerkzeug für JavaScript/TypeScript | Frontend-Logik und Komponenten prüfen | Passt zum Vite-Werkzeugkasten; kurze Rückmeldung bei Änderungen |
| Playwright | Steuert echte Browser automatisiert | Vollständige Benutzerabläufe prüfen | Deckt das Zusammenspiel von Frontend, API und Datenbank ab |
| axe-core | Automatische Regeln zur Barrierefreiheit | Gerenderte UI-Zustände prüfen, mit `@axe-core/playwright` integrieren | Findet viele technische Barrieren wiederholbar; manuelle Prüfung bleibt nötig |
| Docker / Docker Compose | Container-Laufzeit und Beschreibung mehrerer Dienste | Webserver, Backend und Datenbank gemeinsam starten | Reproduzierbare lokale Installation mit klaren Zuständigkeiten |
| Spring Boot Actuator | Betriebsendpunkte für Spring Boot | Healthchecks und geschützte Betriebsinformationen | Zustand auch ohne normale Oberfläche prüfen |
| Git | Versionsverwaltung | Änderungen und Entscheidungen nachvollziehen | Ermöglicht Reviews, Vergleich und gemeinsame Arbeit |
| Markdown / Mermaid | Textformate für Dokumentation und Diagramme | Anforderungen und Architektur im Repository pflegen | Dokumentation lässt sich zusammen mit dem Code prüfen |

## Ergänzende Arbeitsannahmen

Spring Data JPA/Hibernate kann den Datenzugriff vereinfachen; es ersetzt weder Flyway noch Constraints. Für React-Komponententests ist React Testing Library vorgesehen, damit Tests Bedienelemente wie Benutzer finden. Ein Java-kompatibles OpenAPI-Werkzeug, etwa springdoc-openapi, wird in M0 gegen die gewählte Spring-Boot-Version geprüft. Diese Hilfsbibliotheken dürfen nicht unabhängig von ihrer Kompatibilität eingeführt werden.

Die frühere Planung nennt JUnit 5. Die genaue Spring-Boot-/JUnit-Kombination ist in M0 zu verifizieren: Der Agent darf weder eine inkompatible Version erzwingen noch einen Wechsel der Hauptversion verschweigen. Wenn die gewählte, gepflegte Boot-Version einen anderen Test-Stack verwaltet, ist die Abweichung nachvollziehbar zu entscheiden.

## Versionsstrategie

| Bestandteil | Heute festgelegt | Nachweis ab M0 |
|---|---|---|
| Java | 21 | Maven-Konfiguration, Container-Basis und CI verwenden Java 21 |
| Spring Boot und verwaltete Java-Bibliotheken | Noch keine Paketversion | Eine gepflegte, zu Java 21 passende Kombination im `pom.xml` |
| Maven | Wrapper vorgesehen | Wrapper-Dateien mit geprüfter Distribution |
| Node, React, TypeScript, Vite, Testwerkzeuge | Noch keine Paketversion | Dokumentierte Node-Version, `package.json` und `package-lock.json`; Installation mit `npm ci` |
| PostgreSQL und Webserver | Noch keine Image-Version | Feste Versionen, für Releases möglichst Image-Digests; gleiche PostgreSQL-Hauptversion in Tests und Betrieb |
| Docker Compose | Compose v2 | Tatsächlich getestete Version in der Entwicklungs- und Deployment-Anleitung |

### Tatsächlich verwendete Versionen (Stand M0, 30.09.2026)

| Bestandteil | Version | Anmerkung |
|---|---|---|
| Java (Entwicklung, Container) | 21.0.7 (Temurin/Zulu) | Container: `eclipse-temurin:21.0.7_6-jdk` bzw. `-jre-alpine` |
| Spring Boot | 4.1.1 | Boot 4: Test-Pakete heißen z. B. `org.springframework.boot.webmvc.test.autoconfigure` |
| Maven (Wrapper) | 3.9.16 | `src/backend/.mvn/wrapper` |
| Test-Stack Backend | JUnit Jupiter 6.0.3, Testcontainers 2.0.5 | von Spring Boot verwaltet (JUnit 6 statt 5 – Abweichung von der früheren Planung, kein Hindernis) |
| Flyway | 12.4.0 | von Spring Boot verwaltet |
| springdoc-openapi | 3.1.1 | `springdoc-openapi-starter-webmvc-api`, funktioniert mit Boot 4.1.1 (im Test-/Dev-Start geprüft) |
| PostgreSQL | 17.11 (`postgres:17.11-alpine`) | identisch in Compose und Testcontainers |
| Node.js / npm | 24.21.0 / 11.19.0 | `src/frontend/.nvmrc`, `engines` in `package.json` |
| React / React Router | 19.3.0 / 7.18.4 | |
| TypeScript / Vite | 6.0.3 / 8.3.1 | |
| Vitest / Testing Library / jsdom | 5.0.2 / React 16.3.3 / 30.1.1 | |
| ESLint | 9.39.5 mit `jsx-a11y` | ESLint 10 ist wegen `eslint-plugin-jsx-a11y` (Peer bis 9) noch nicht nutzbar |
| Playwright / axe-core | 1.63.0 / @axe-core/playwright 4.13.0 | Browser: Chromium |
| nginx | 1.30 (`nginx:1.30-alpine`) | Webserver im `web`-Container |
| Docker / Compose | 28.3.2 / v2.38.2 | Entwicklungsrechner macOS |

Noch nicht auf Image-Digests festgelegt; das folgt vor dem Referenzrelease (M4). Updates erfolgen gezielt mit Tests und Release-Hinweisen; eine neue Versionsnummer allein beweist keine Kompatibilität.

## Offizielle Einstiege

- [Spring Boot: Systemvoraussetzungen](https://docs.spring.io/spring-boot/system-requirements.html) – vor Versionsfestlegung prüfen.
- [Spring Security: CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html) – besonders die Integration von Browseranwendungen.
- [React](https://react.dev/learn), [TypeScript](https://www.typescriptlang.org/docs/), [Vite](https://vite.dev/guide/) und [Maven](https://maven.apache.org/guides/) – Grundlagen.
- [PostgreSQL](https://www.postgresql.org/docs/) und [Flyway](https://documentation.red-gate.com/flyway) – Speicherung und Migrationen.
- [OpenAPI](https://spec.openapis.org/oas/latest.html), [Testcontainers](https://java.testcontainers.org/), [Vitest](https://vitest.dev/guide/) und [Playwright](https://playwright.dev/docs/intro) – Verträge und Tests.

## Lernziel

Ein Azubi soll zu jedem Eintrag einen eigenen Satz zu „Was?“, „Wofür?“ und „Warum?“ sagen können. Er muss dafür nicht jede Annotation oder jede Codezeile auswendig kennen.
