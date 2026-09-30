# Installation und Betrieb

[Zur Übersicht](../README.md) · [Entwicklung](DEVELOPMENT.md) · [Sicherheit](SECURITY.md)

**Stand: M1c, Frischinstallations-Probe durchgeführt (US-160, M1-Anteil).** `compose.yaml`, Images und `.env.example` existieren; Abschnitte 4 und 5 dieser Anleitung wurden auf einem frischen Checkout geprüft, siehe [Installationsprotokoll](reviews/installation-M1.md) — dabei wurde ein Fehler in Abschnitt 5 gefunden und korrigiert (falscher Healthcheck-Pfad). Die Schritte zu Update, Backup und Restore (Abschnitte 6 bis 8) sind noch Planung und werden erst zum Abschluss von US-160 in M4 geprüft.

## 1. Ziel und Voraussetzungen

Standard ist ein einzelner lokaler Rechner mit Docker Compose. `web` liefert das gebaute React-Frontend und leitet `/api` sowie den minimalen Healthcheck an `backend` weiter. `backend` betreibt Spring Boot, `db` PostgreSQL. Nur `web` veröffentlicht einen Port an `127.0.0.1`; Backend und Datenbank sind im vollständigen Deployment nur im Container-Netz erreichbar.

Benötigt werden Git, eine laufende Docker-Engine, Compose v2, ein Browser und ausreichend Platz für Images und Daten. Java, Node und Maven müssen beim vollständigen Container-Build nicht auf dem Host installiert sein. Der erste Build benötigt Netzwerkzugang für Images und Abhängigkeiten. Exakte geprüfte Versionen werden in [TECHNOLOGIES.md](TECHNOLOGIES.md) ergänzt.

Die Befehle verwenden eine Shell unter macOS/Linux oder WSL2. Im späteren Installationsprotokoll wird das tatsächlich geprüfte Betriebssystem genannt. Internetöffentlicher Betrieb benötigt zusätzlich Host/DNS, HTTPS, reale Mailzustellung für Passwort-Reset, geregelte Secrets und Backups; diese Infrastruktur ist noch nicht eingerichtet.

## 2. Release beschaffen und Vollständigkeit prüfen

Der erste lokale Arbeitsstand liegt unter `~/dev/ToDo`. Für einen anderen Rechner muss der Betreiber später eine tatsächliche Repository-Adresse oder ein versioniertes Release-Archiv bereitstellen. Hier wird bewusst keine erfundene Clone-URL angegeben.

```sh
cd ~/dev/ToDo
git status
git log -1 --oneline
docker version
docker compose version
```

Für eine Installation eine konkrete Release-Version verwenden und Commit-ID notieren. Ein uncommitteter Dokumentationsstand ist kein installierbarer Release.

Vor dem Start müssen vorhanden sein:

- `compose.yaml` mit Diensten `web`, `backend`, `db`, Healthchecks und persistentem Datenbankvolume.
- `.env.example` mit allen benötigten Konfigurationsnamen und sicheren Platzhaltern.
- Backend-/Frontend-Code, Maven Wrapper, npm-Lockdatei und reproduzierbare Container-Builds.
- Flyway-Migrationen sowie eine getestete Versionstabelle.

Fehlt davon etwas, die Implementierung vervollständigen; nicht durch improvisierte Produktionskonfiguration umgehen.

## 3. Konfiguration einrichten

Nur bei erstmaliger Einrichtung:

```sh
cp .env.example .env
```

Die `.env` lokal bearbeiten und nicht in Git aufnehmen. Für Passwörter etwa einen Passwortmanager oder, falls vorhanden, `openssl rand -hex 32` verwenden. Keine Beispielwerte als echte Zugangsdaten übernehmen. Die spätere Startprüfung muss leere Pflichtwerte und erkennbare Passwort-Platzhalter zurückweisen.

### Konfigurationsvertrag

**Umgesetzt in M0:** `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` (fehlt es, bricht `docker compose` mit einer verständlichen Meldung ab), `WEB_PORT`, `BACKEND_PORT`, `DEV_DB_PORT`. **Umgesetzt in M1a:** `SESSION_COOKIE_SECURE` (Standard `false`, nur für localhost; hinter HTTPS `true`), `SESSION_TIMEOUT` (Standard `30m`), optional `RATE_LIMIT_LOGIN_ATTEMPTS` und `RATE_LIMIT_REGISTER_ATTEMPTS` (Standard je 10 pro Zeitfenster; nur für Testläufe anheben). **Noch nicht umgesetzt** (kommt mit M2): `APP_BASE_URL`. Die Zurückweisung des Platzhalterpassworts aus `.env.example` ist noch offen (Teil von US-001).

| Variable | Beispiel/Standard | Bedeutung und Weitergabe |
|---|---|---|
| `POSTGRES_DB` | `ultimatetodo` | Datenbankname; an `db` und als Name in Backend-JDBC-URL |
| `POSTGRES_USER` | `ultimatetodo` | Lokales DB-Konto; an `db` und Backend-Benutzerkonfiguration |
| `POSTGRES_PASSWORD` | `REPLACE_WITH_RANDOM_SECRET` | Pflichtsecret; an `db` und Backend-Passwortkonfiguration |
| `WEB_PORT` | `8080` | Nur Host-Port des Webservers; Bindung an `127.0.0.1` |
| `APP_BASE_URL` | `http://localhost:8080` | Sichtbare Basis-URL, besonders für spätere Reset-Links; bei Portänderung mitändern |
| `SESSION_COOKIE_SECURE` | `false` nur für localhost | Zuordnung auf `server.servlet.session.cookie.secure`; bei HTTPS zwingend `true` |
| `SESSION_TIMEOUT` | `30m` | Zuordnung auf `server.servlet.session.timeout` |
| `DEV_DB_PORT` | `5432` | Nur eigenständiges Entwicklungs-Compose und Backend-`dev`-Profil |
| `BACKEND_PORT` | `8081` | Nur Backend auf dem Entwicklerhost; im Betriebscontainer intern 8081 |

Compose muss die Variablen **explizit** an die passenden Container bzw. Spring-Konfigurationen weitergeben. Eine `.env` ist keine automatische Spring-Konfiguration. Im Betriebscontainer lautet die Datenbankadresse `db:5432`, nicht `localhost`. Projektvariablen wie `APP_BASE_URL` benötigen eine implementierte Property-Zuordnung.

Für den lokalen Lernbetrieb kann ein einzelnes PostgreSQL-Konto dienen. Vor öffentlichem Betrieb sind ein eingeschränktes Laufzeitkonto und getrennte Migrationsrechte vorzusehen. Ein mit `POSTGRES_USER` initialisiertes Administrationskonto darf dort nicht ungeprüft als dauerhaft privilegiertes Anwendungskonto dienen.

Image- und Paketversionen sind in Release-Dateien festgelegt; keine `latest`-Tags. Ein späterer Wechsel der PostgreSQL-Hauptversion benötigt einen eigenen getesteten Migrationsablauf, nicht nur ein anderes Image-Tag.

## 4. Bauen und starten – Zielablauf ab M0

Im Repository-Stamm:

```sh
docker compose --project-name ultimatetodo config --quiet
docker compose --project-name ultimatetodo build
docker compose --project-name ultimatetodo up -d --wait
docker compose --project-name ultimatetodo ps
```

`config --quiet` prüft die Compose-Konfiguration, ohne expandierte Geheimnisse auszugeben. Der Build erzeugt Frontend- und Backend-Artefakte in Containern. Ein erfolgreicher Containerstart allein bedeutet noch keine einsatzbereite Anwendung: Healthchecks müssen definiert sein. Das Backend wartet auf gesunde Datenbank und erfolgreiche Migrationen; `web` leitet erst danach nutzbare Anfragen weiter. [Docker: Startreihenfolge und Bereitschaft](https://docs.docker.com/compose/how-tos/startup-order/)

Die lokale Oberfläche soll unter [localhost:8080](http://localhost:8080) verfügbar sein. Bei anderem `WEB_PORT` die URL anpassen. Normalen Betrieb starten und stoppen, ohne Volumes zu löschen:

```sh
docker compose --project-name ultimatetodo stop
docker compose --project-name ultimatetodo start
```

## 5. Funktion und Zustand prüfen

```sh
curl --fail http://localhost:8080/actuator/health
docker compose --project-name ultimatetodo ps
docker compose --project-name ultimatetodo logs --tail=100 backend
```

Erwartung nach erfolgreicher Implementierung: Healthcheck mit HTTP 200 und Zustand `UP`, Dienste gesund, keine fehlgeschlagenen Migrationen. Öffentliche Antworten zeigen keine internen Verbindungsdetails. Falls `curl` nicht vorhanden ist, den Healthcheck im Browser öffnen. Logs vor Weitergabe auf sensible Inhalte prüfen.

Ab M1 zusätzlich:

1. Eigenes Benutzerkonto registrieren und anmelden.
2. Aufgabe mit eindeutigem Titel anlegen und nach Neuladen wiederfinden.
3. Dienste neu starten, gegebenenfalls erneut anmelden und dieselbe Aufgabe wiederfinden.
4. Zweites Konto registrieren; die private Aufgabe des ersten Kontos darf nicht sichtbar sein.
5. Kernablauf mit Tastatur prüfen und wieder abmelden.

Es gibt kein Standardkonto. Die erstmalige Einrichtung eines Administrators wird mit US-014 durch einen konkreten lokalen Bootstrap-Aufruf ergänzt. Ohne diesen noch fehlenden Nachweis ist die administrative Funktion nicht abgenommen.

## 6. Sicherung – Zielablauf ab M4

Ein Docker-Volume schützt bei einem normalen Container-Neustart, ist aber kein Backup. Vor Updates und regelmäßig Datenbank sowie separat die Konfiguration sichern. Dumps enthalten private Inhalte und Passwort-Hashes; Zugriff beschränken und für externe Aufbewahrung verschlüsseln. Aufbewahrungszeit und Verantwortlicher sind vor realer Nutzung festzulegen.

Beispiel für eine Sicherung mit kurzer Schreibpause; nur nach getesteter Implementierung verwenden. Unter `backups/` jeweils einen **neuen**, eindeutig datierten Namen wählen. Eine vorhandene Datei nicht überschreiben:

```sh
mkdir -p backups
umask 077
docker compose --project-name ultimatetodo stop web backend
docker compose --project-name ultimatetodo exec -T db sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc' > backups/vor-update-YYYYMMDD-HHMM.dump
```

Den Rückgabewert des Dump-Befehls prüfen. Anschließend Lesbarkeit prüfen:

```sh
docker compose --project-name ultimatetodo exec -T db pg_restore --list < backups/vor-update-YYYYMMDD-HHMM.dump
docker compose --project-name ultimatetodo start backend web
```

Scheitert die Sicherung oder Prüfung, kein Update durchführen; Ursache beheben und Dienste wieder in einen bekannten Zustand bringen. `pg_restore --list` prüft nur das Archivverzeichnis; erst ein wirklicher Restore belegt Wiederherstellbarkeit. Release-/Commit-ID, PostgreSQL-Version und Sicherungszeitpunkt separat notieren. `.env` und sonstige Secrets getrennt und geschützt sichern. [PostgreSQL: SQL-Dumps und Archivformate](https://www.postgresql.org/docs/current/backup-dump.html)

## 7. Wiederherstellung zuerst getrennt erproben

Nie einen ungeprüften Restore über die einzige vorhandene Datenbank legen. Verwende eine neue Arbeitskopie des **zum Dump passenden Releases**, eine eigene `.env` mit denselben DB-Namen/-Benutzern und einen anderen Web-Port, beispielsweise 8082. Der Compose-Projektname `ultimatetodo-restore` muss ein eigenes Volume erzeugen; Compose darf keine festen globalen Volume-Namen erzwingen.

In dieser getrennten Arbeitskopie:

```sh
docker compose --project-name ultimatetodo-restore up -d --wait db
docker compose --project-name ultimatetodo-restore exec -T db sh -c 'pg_restore --exit-on-error --no-owner --no-acl -U "$POSTGRES_USER" -d "$POSTGRES_DB"' < /ABSOLUTER/PFAD/ZUR/SICHERUNG.dump
docker compose --project-name ultimatetodo-restore up -d --build --wait
```

Der großgeschriebene Pfad ist durch die tatsächliche Sicherungsdatei zu ersetzen. Die Ziel-Datenbank muss leer sein; Backend vor dem Restore nicht starten, damit Flyway sie nicht schon befüllt. Für die Probe keine vorhandenen Tabellen löschen. Bei Fehlern anhalten, Ursache prüfen und eine neue leere Restore-Umgebung nutzen.

Danach Readiness, Anmeldung, bekannte Aufgaben/Projekte, Beziehungen und erwartete Mengen prüfen. Restore-Protokoll mit Dump, Release und Ergebnis erstellen. Erst nach erfolgreicher Probe einen tatsächlichen Wiederherstellungs- oder Umschaltplan ausführen. Daten seit dem Sicherungszeitpunkt sind im Dump nicht enthalten.

## 8. Update und Rückkehrplan

1. Neue Release-Hinweise, Versionskompatibilität, benötigte Konfiguration und Migrationen lesen; freie Kapazität prüfen.
2. Aktuelle Release-ID und Konfiguration sichern. Datenbank sichern und Restore-Fähigkeit nachweisen.
3. Neues Release zunächst in einer getrennten Umgebung gegen eine Kopie der Daten testen. Keine Testmails oder externen Benachrichtigungen an echte Personen senden.
4. Wartungsfenster nutzen, schreibende Dienste anhalten und letzte Sicherung erstellen.
5. Geprüfte neue Release-Arbeitskopie mit eigener korrekter `.env` verwenden, im selben Docker-Kontext den bisherigen Compose-Projektnamen `ultimatetodo` beibehalten, bauen und mit `up -d --wait` starten. Vorher sicherstellen, dass das erwartete bestehende Datenvolume eingebunden wird.
6. Healthcheck, Login, bestehende und neue Aufgaben, Rechte und Neustart prüfen; Ergebnis dokumentieren.

Flyway-Migrationen laufen vor Bereitschaft. Scheitern sie, Fehler untersuchen; keine Historieneinträge löschen und kein blindes `repair`. Ein Zurückwechseln des Anwendungsimages ist nur sicher, wenn das Schema dazu kompatibel ist. Andernfalls Datenbank aus geprüfter Sicherung in getrenntem Volume wiederherstellen und mit der passenden alten Anwendung starten. Vor Umschaltung prüfen; mögliche seitdem entstandene Datenänderungen ausdrücklich berücksichtigen.

## 9. Fehler eingrenzen

| Problem | Prüfung / Maßnahme |
|---|---|
| Port belegt | `WEB_PORT` und `APP_BASE_URL` gemeinsam ändern; erneut konfigurieren/starten |
| Dienste starten nicht | Docker läuft? Freier Speicher? Pflichtvariablen gesetzt? `ps` und begrenzte Logs prüfen |
| Datenbank nicht erreichbar | Interner Host `db`, Healthcheck und Zugangsdaten prüfen |
| Geändertes Passwort wirkt nicht | PostgreSQL-Initialisierungsvariablen ändern kein bestehendes Konto; geregelte Passwortrotation nötig |
| Flyway meldet Fehler | Migration und Datenzustand untersuchen; nicht das Volume löschen |
| Frontend zeigt `401` | Neu anmelden; Sessions können nach Backend-Neustart enden |
| Speichern liefert `403` | Cookie/HTTPS/CSRF-Konfiguration prüfen; Schutz beibehalten |
| Zugriff liefert `409` | Fachlicher Versionskonflikt; aktuelle Daten laden |
| Daten scheinen verschwunden | Richtiger Docker-Kontext, Compose-Projektname und Volume? Keine neue leere Installation mit altem Stand verwechseln |
| Healthcheck ungesund | Datenbankbereitschaft und Migration prüfen; nur Containerzustand „running“ reicht nicht |

`docker compose down` ohne `--volumes` erhält normale benannte Volumes. `down --volumes`, `docker volume rm` und pauschales Aufräumen können Daten endgültig löschen und gehören nicht zum normalen Updateablauf.

## 10. Frischinstallation prüfen

US-160 ist erst erfüllt, wenn die Anleitung auf einem frischen Checkout (ohne vorhandene Volumes und `.env`) wörtlich und ohne Zusatzschritte zum Erfolg führt. Bei Implementierung wird unter `docs/reviews/installation-M1.md` bzw. für das Release ein Protokoll angelegt:

- Datum, Betriebssystem, Docker-/Compose-Version, Release-/Commit-ID.
- Tatsächlich ausgeführte Schritte, Dauer und Abweichungen von der Anleitung.
- Healthcheck, Konto, Aufgabe, Berechtigungsprobe und Persistenz nach Neustart.
- Für M4 zusätzlich Update und Restore; verbleibende Grenzen ausdrücklich nennen.
- Jede notwendige Korrektur in die Anleitung übernehmen und die betroffenen Schritte erneut prüfen.

Bis diese Nachweise existieren, lautet der Status „geplant“ bzw. „noch nicht auf frischem System geprüft“.
