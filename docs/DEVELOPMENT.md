# Lokal entwickeln

[Zur Übersicht](../README.md) · [Tests](TESTING.md) · [Fertige Anwendung betreiben](DEPLOYMENT.md)

**Aktueller Stand: M0.** Die Befehle unten wurden am 30.09.2026 auf macOS (Docker 28.3.2, Compose v2.38.2, JDK 21.0.7, Node 24.21.0) ausgeführt: Dev-Datenbank, Backend im `dev`-Profil, Vite-Proxy, `mvnw verify`, Frontend-Prüfungen und `scripts/e2e.sh`. Windows/WSL2 und Linux sind ungeprüft.

## 1. Voraussetzungen

Für macOS/Linux oder Windows mit WSL2 werden benötigt:

- Git und eine IDE bzw. ein Editor.
- JDK 21; `java -version` muss Java 21 zeigen.
- Die in M0 festgelegte Node.js-Version mit npm; anschließend `node --version` und `npm --version` mit der Versionstabelle vergleichen.
- Docker mit laufender Engine und Compose v2; `docker version` und `docker compose version` prüfen.
- Freie lokale Ports: 5173 für Vite, 8081 für Backend, 5432 für PostgreSQL. Portänderungen müssen durch dokumentierte Konfiguration möglich sein.

Maven muss nicht global installiert werden, sobald der Maven Wrapper vorhanden ist. Testcontainers verwendet die Docker-Engine. Persönliche IDE-Dateien bleiben lokal; gemeinsame Einstellungen nur gezielt versionieren.

## 2. Aktuell mit der Dokumentationsbasis arbeiten

Der lokale Ausgangsordner ist:

```sh
cd ~/dev/ToDo
git status
```

Lies zuerst README, Backlog, Architektur, Domänenmodell und die Querschnittsregeln. Der vorbereitete erste Commit umfasst nur Markdown und `.gitignore`. Ein leerer `src/`-Ordner wird von Git nicht gespeichert. Es gibt zunächst kein Remote-Repository; eine spätere Clone-URL darf erst eingetragen werden, wenn sie tatsächlich existiert.

## 3. Zielablauf ab M0: Datenbank starten

Im Repository-Stamm:

```sh
cp .env.example .env
```

Nur beim ersten Einrichten kopieren; eine vorhandene `.env` nicht überschreiben. In der eigenen `.env` mindestens ein zufälliges `POSTGRES_PASSWORD` setzen. Ist Port 5432 bereits belegt (Fehler „port is already allocated“), `DEV_DB_PORT` auf einen freien Port, zum Beispiel `5433`, ändern. Das [Konfigurationsverzeichnis in der Deployment-Anleitung](DEPLOYMENT.md) beschreibt sämtliche Werte und ihre Zuordnung. Beispielkonfiguration bleibt ohne reale Geheimnisse.

```sh
docker compose --project-name ultimatetodo-dev -f compose.dev.yaml up -d --wait db
docker compose --project-name ultimatetodo-dev -f compose.dev.yaml ps
```

`compose.dev.yaml` ist eine eigenständige Datei. Sie startet eine Datenbank mit eigenem Volume, erreichbar nur über `127.0.0.1:${DEV_DB_PORT}`. Sie ist kein Override für das vollständige Deployment. Dadurch bleiben Test-, Entwicklungs- und Betriebsdaten getrennt.

## 4. Zielablauf ab M0: Backend starten

Die spätere Anwendung muss ein `dev`-Profil mit dieser Zuordnung bereitstellen: Host `127.0.0.1`, Port `DEV_DB_PORT`, Datenbank `POSTGRES_DB`, Benutzer `POSTGRES_USER`, Passwort `POSTGRES_PASSWORD`; HTTP-Port `BACKEND_PORT`, Standard 8081.

Spring Boot liest eine Compose-`.env` nicht automatisch. Für ein lokales Terminal kann die **eigene, zuvor geprüfte** Datei mit einfachen Shell-kompatiblen `NAME=WERT`-Zuweisungen geladen werden:

```sh
set -a
. ./.env
set +a
./src/backend/mvnw -f src/backend/pom.xml spring-boot:run -Dspring-boot.run.profiles=dev
```

Das Laden führt Shell-Inhalt aus; ausschließlich die selbst angelegte Konfiguration laden. Alternativ dieselben Variablen in der lokalen IDE-Startkonfiguration setzen, ohne sie zu versionieren. Im Containerbetrieb übernimmt Compose die explizite Weitergabe. Eine `.env` allein exportiert nichts in beliebige Prozesse.

Im `dev`-Profil ist die OpenAPI-Beschreibung unter [localhost:8081/v3/api-docs](http://localhost:8081/v3/api-docs) abrufbar (im Betrieb deaktiviert, nginx blendet `/v3/` aus). Eine Swagger-Oberfläche ist nicht eingerichtet.

Flyway führt Migrationen beim Start aus. Ein Migrationsfehler verhindert Bereitschaft. Der erwartete lokale Prüfpunkt lautet nach Umsetzung [Backend-Readiness](http://localhost:8081/actuator/health/readiness).

## 5. Zielablauf ab M0: Frontend starten

In einem zweiten Terminal, wieder im Repository-Stamm:

```sh
npm --prefix src/frontend ci
npm --prefix src/frontend run dev
```

Die Entwicklungsoberfläche soll unter [localhost:5173](http://localhost:5173) erscheinen. Vite leitet `/api` standardmäßig an `http://127.0.0.1:8081` weiter; ein abweichender Backend-Port wird über eine in M0 dokumentierte Proxy-Einstellung übernommen. Der Browser verwendet relative `/api/v1`-URLs. Keine Zugangsdaten in `VITE_*`-Variablen speichern.

Testanmeldung erfolgt über eine normale Registrierung unter <http://localhost:5173/registrieren> (ab M1a vorhanden; mindestens 15 Zeichen Passwort). Ein Administratorkonto entsteht erst über den in US-014 festzulegenden Bootstrap-Vorgang. Keine Demo-Standardpasswörter in der normalen Anwendung.

## 6. Bauen, testen und debuggen

Die vollständigen Zielbefehle und Testumgebung stehen in [TESTING.md](TESTING.md). Als kurze Prüfung nach Änderungen:

```sh
./src/backend/mvnw -f src/backend/pom.xml verify
npm --prefix src/frontend run typecheck
npm --prefix src/frontend run test:unit
npm --prefix src/frontend run build
```

Für die IDE `src/backend/pom.xml` als Maven-Projekt importieren und JDK 21 wählen. Das Frontend besitzt sein eigenes `package.json`. Breakpoints in einem Anwendungsdienst zeigen den Weg einer Anfrage; Browser-Netzwerkansicht und Anfragekennung helfen, HTTP-Fehler den Backend-Logs zuzuordnen. Debug-Ports nur lokal öffnen.

Ein Backend-Neustart kann lokale Sessions beenden; erneut anmelden. Das ist kein Grund, die Datenbank zu löschen. Frontend-Änderungen und fachliche Fehler niemals durch Abschalten von Spring Security oder CSRF „beheben“.

## 7. Datenbankänderungen und lokale Daten

Neue Migrationen liegen geplant unter `src/backend/src/main/resources/db/migration/`, zum Beispiel `V2__add_task_due_date.sql`. Bestehende angewendete Migrationen nicht bearbeiten. Änderungen an Constraints und Daten müssen sowohl gegen ein leeres Schema als auch gegen den vorherigen Stand getestet werden. ORM-Schemaerzeugung im Betrieb ist deaktiviert; Validierung ist erlaubt.

Normal beenden, ohne Daten zu entfernen:

```sh
docker compose --project-name ultimatetodo-dev -f compose.dev.yaml down
```

**Nur für ausdrücklich entbehrliche lokale Entwicklungsdaten:** Der folgende Befehl löscht das Entwicklungsvolume endgültig. Vorher sicherstellen, dass dies tatsächlich das Entwicklungsprojekt ist und keine Daten gebraucht werden.

```sh
docker compose --project-name ultimatetodo-dev -f compose.dev.yaml down --volumes
```

Anschließendes Starten erzeugt ein leeres Schema durch Flyway. Tests sollen ihre eigenen Daten verwalten; routinemäßige Tests dürfen keine Entwicklungsdatenbank zurücksetzen.

## 8. Zusammenarbeit und typische Fehler

Änderungen einer Story zuordnen, kleine verständliche Commits erstellen und Dokumentation zusammen mit Verhalten aktualisieren. Vor einem Commit `git diff --cached` prüfen; `.env`, Sicherungen und Logs gehören nicht hinein.

| Symptom | Erste Prüfung |
|---|---|
| Java-Build startet nicht | JDK 21, Wrapper vorhanden und ausführbar, Versionskombination korrekt? |
| Testcontainers startet nicht | Docker-Engine erreichbar und erforderliche Images verfügbar? |
| Backend erreicht Datenbank nicht | Dev-Compose bereit, `DEV_DB_PORT` richtig, Variablen wirklich exportiert? |
| Frontend erhält Netzwerkfehler | Backend läuft, Proxy-Ziel richtig, beide Prozesse erreichbar? |
| `401` nach Neustart | Session beendet; neu anmelden |
| `403` bei Speichern/Login | CSRF-Ablauf und Cookie prüfen; Schutz nicht abschalten |
| `409` beim Speichern | Aktuelle Version laden und Entwurf vergleichen |
| Flyway-Prüfsummenfehler | Angewendete Migration verändert? Ursache prüfen; kein blindes `repair` |
| Datenbankpasswort passt nach Änderung nicht | Eine neue Umgebungsvariable ändert kein bestehendes DB-Konto; Passwortrotation gezielt durchführen |

## 9. Vor Freigabe dieser Anleitung

Der M0-Agent ergänzt geprüfte Versionsnummern, erzeugt die genannten Dateien und führt die Befehle auf dem dokumentierten System aus. Ab M1 muss ein frischer Entwickler die Schritte einschließlich erster Registrierung und gespeicherter Aufgabe nachvollziehen können. Nicht getestete Betriebssysteme bleiben ausdrücklich als ungeprüft gekennzeichnet.
