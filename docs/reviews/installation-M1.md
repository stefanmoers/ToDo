# Installationsprotokoll – Frischinstallations-Probe (US-160, M1-Anteil)

[Zur Installationsanleitung](../DEPLOYMENT.md) · [Backlog](../../BACKLOG.md)

## Rahmendaten

- **Datum:** 30.09.2026, 12:41–12:55 UTC.
- **Betriebssystem:** Windows 11 Pro, Build 10.0.26200 (Git-Bash/MinGW64 als Shell).
- **Docker:** Docker 29.5.3 (build d1c06ef); Docker Compose v5.1.4.
- **Release-/Commit-ID:** `c5ad231` (Zweig `main`).
- **Geprüft von:** Coding-Agent (Claude), im Auftrag des Auftraggebers.

## Abweichungen von der Anleitung und Begründung

Dieses Protokoll prüft Abschnitte 4 und 5 der [Installationsanleitung](../DEPLOYMENT.md) (M1-Umfang von US-160). Abschnitte 6 bis 8 (Sicherung, Wiederherstellung, Update) sind laut Anleitung erst zum Abschluss von US-160 in M4 fällig und wurden hier **nicht** geprüft.

Die Probe lief nicht auf einem zweiten physischen Rechner, sondern in einem frischen `git clone` des lokalen Arbeitsstands in ein separates Arbeitsverzeichnis auf demselben Host (kein zweiter Rechner verfügbar). Damit war sichergestellt, dass weder eine `.env` noch ein zum Compose-Projektnamen passendes Datenbankvolume vorher existierten. Zwei Punkte weichen dadurch von einer echten Zweitinstallation ab:

1. **Docker-Layer-Cache**: Das Docker-Build nutzte teilweise bereits vorhandene Basis-Image- und Zwischenschritte aus früheren Builds auf demselben Host (Build dauerte nur ~14 s). Ein Kaltstart auf einem wirklich neuen Rechner ohne jeden Docker-Cache wäre langsamer, sollte aber inhaltlich gleich ablaufen, da keine Schritte übersprungen werden, nur Zwischenergebnisse wiederverwendet werden.
2. **Compose-Projektname und Port**: Auf diesem Host läuft bereits ein eigener Compose-Stack unter dem Standardnamen `ultimatetodo` (Port 8080, seit mehreren Stunden aktiv – offenbar eine bestehende Instanz des Auftraggebers). Um diese laufende Instanz und ihr Datenvolume `ultimatetodo_db-data` (Erstellzeitpunkt vor Beginn dieser Sitzung, also nicht durch die Probe selbst entstanden) nicht zu berühren, lief die Probe unter `--project-name ultimatetodo-install-probe` mit `WEB_PORT=18090` statt des Standardport 8080. Das ist ausschließlich eine Namens-/Portänderung; die geprüften Befehle, das Compose-File und die Container-Images sind identisch mit einer Standardinstallation.

Diese Abweichungen ändern nichts an der Aussagekraft der Prüfung von Build, Migration, Healthcheck, Kontoanlage, Persistenz und Rechtetrennung.

## Durchgeführte Schritte

| Schritt | Befehl/Aktion | Ergebnis | Dauer |
|---|---|---|---|
| Konfiguration prüfen | `docker compose --project-name ultimatetodo-install-probe config --quiet` | Erfolgreich, keine Ausgabe | < 1 s |
| Bauen | `docker compose --project-name ultimatetodo-install-probe build` | Beide Images gebaut | 13,8 s (teilw. Layer-Cache, siehe oben) |
| Starten | `docker compose --project-name ultimatetodo-install-probe up -d --wait` | Alle drei Dienste `healthy` | 27,4 s |
| Zustand | `docker compose --project-name ultimatetodo-install-probe ps` | `db`, `backend`, `web` alle `healthy` | – |
| Migrationen | `docker compose … logs backend` | Flyway: 5 Migrationen erfolgreich auf V5 angewendet, keine Fehler | – |
| Healthcheck | `curl --fail http://localhost:18090/actuator/health/readiness` (wörtlich aus der Anleitung) | **Fehlgeschlagen: HTTP 404** | – |
| Healthcheck (korrigiert) | `curl --fail http://localhost:18090/actuator/health` | Erfolgreich: `{"groups":["liveness","readiness"],"status":"UP"}` | – |

## Gefundener und korrigierter Fehler

Der in Abschnitt 5 der Anleitung dokumentierte Befehl `curl --fail http://localhost:8080/actuator/health/readiness` schlägt fehl (`HTTP 404`). Die nginx-Konfiguration (`src/frontend/docker/nginx.conf`) leitet öffentlich ausschließlich den exakten Pfad `/actuator/health` an das Backend weiter; alle übrigen `/actuator/…`-Pfade liefern bewusst `404` (Betriebsendpunkte sind von außen nicht erreichbar, siehe `smoke.spec.ts` und `docs/SECURITY.md`). Der `/readiness`-Unterpfad ist nur **intern** über den Healthcheck des Backend-Containers direkt gegen `backend:8081` erreichbar (`compose.yaml`), nicht über den öffentlichen Webserver. Die Anleitung verwechselte diese beiden Ebenen.

**Korrektur:** [DEPLOYMENT.md](../DEPLOYMENT.md) Abschnitt 5 auf `curl --fail http://localhost:8080/actuator/health` geändert; das ist derselbe Pfad, den auch `BACKLOG.md` (US-171) bereits als öffentlich erreichbar dokumentiert und den `smoke.spec.ts` prüft. Nach der Korrektur lief der Schritt wie erwartet.

## Fachlicher Funktionstest (ab M1)

1. **Konto registrieren und anmelden** (Konto A, `installprobe-a@example.test`): erfolgreich, direkte Weiterleitung zur Aufgabenliste.
2. **Aufgabe anlegen und nach Neuladen wiederfinden**: Aufgabe „Frischinstallations-Probe UT-160-42" angelegt; Detailseite zeigt sie korrekt mit Zeitstempel.
3. **Dienste neu starten** (`docker compose … stop` / `start`) **und dieselbe Aufgabe wiederfinden**: nach Neustart war eine erneute Anmeldung nötig (Sitzung endet beim Backend-Neustart, erwartetes Verhalten), danach zeigte die Aufgabenliste die zuvor angelegte Aufgabe unverändert („Aufgaben – Seite 1 von 1 (1 Aufgabe)"). Persistenz über einen Neustart hinweg bestätigt.
4. **Zweites Konto registrieren; private Aufgabe des ersten Kontos darf nicht sichtbar sein**: Konto B (`installprobe-b@example.test`) sieht nach Anmeldung „Du hast noch keine Aufgaben." — die Aufgabe von Konto A ist nicht sichtbar. Rechtetrennung bestätigt.
5. **Kernablauf mit Tastatur prüfen und wieder abmelden**: Abmelden (Konto B) über Mausklick durchgeführt und erfolgreich zur Anmeldeseite zurückgekehrt. Eine eigenständige manuelle Tastaturprüfung war im Rahmen dieser Werkzeugsitzung nicht zuverlässig automatisierbar (Browserfenster ließ sich nicht immer für Tastatureingaben in den Vordergrund holen); die vollständige Tastaturbedienbarkeit (Sprunglink, Formulare, Dialoge, Fokusfalle, Fokus-Rückgabe) ist stattdessen bereits automatisiert durch die `Tastatur:`-Tests in `e2e/a11y.spec.ts` nachgewiesen, die in derselben Sitzung gegen denselben Softwarestand grün liefen (siehe `docs/TESTING.md`). Dieser Punkt gilt damit als durch automatisierte statt durch diese zusätzliche manuelle Handlung bestätigt.

## Ergebnis

Die Anleitung führt nach der oben beschriebenen Korrektur wörtlich zum Erfolg. Healthcheck, Kontoanlage, Aufgabe anlegen, Persistenz nach Neustart und Rechtetrennung zwischen zwei Konten sind bestätigt. US-160 gilt damit für den M1-Anteil als erfüllt; der M4-Anteil (Update, Sicherung, Wiederherstellung) bleibt offen und wird erst mit dem entsprechenden Funktionsumfang geprüft.

## Aufräumen

Nach Abschluss der Probe wurden der Compose-Stack `ultimatetodo-install-probe` inklusive Volumes entfernt (`down --volumes`) und das temporäre Klon-Verzeichnis gelöscht. Die bereits laufende `ultimatetodo`-Instanz des Auftraggebers und ihr Datenvolume blieben unangetastet.
