# Teststrategie

[Zur Übersicht](../README.md) · [Barrierefreiheit](ACCESSIBILITY.md) · [Sicherheit](SECURITY.md)

**Status:** Vorgabe für die Implementierung. Umgesetzt und ausgeführt (Stand 30.09.2026, M0/M1a/M1b/US-023/M1c und der M2-Anteil US-060 bis US-062): Backend-Unit- und Testcontainers-Integrationstests (`mvnw verify`, 128 Tests: 30 Unit, 98 Integration), Frontend-Unit-/Komponententests (`npm run test:unit`, 96 Tests) sowie die vollständige Playwright-E2E-/axe-Suite gegen den echten Compose-Stack (`scripts/e2e.sh`, 48 Tests, alle grün). Noch nicht durchgeführt: die manuelle Accessibility-Prüfung (US-172) durch den Auftraggeber selbst.

Ein Test belegt erwartetes Verhalten mit nachvollziehbaren Eingaben. Viele grüne Tests sind kein Ersatz für richtige Anforderungen. Wir prüfen besonders Rechte, Konflikte, Zeitregeln und Datenverlust, weil Fehler dort schwerer wiegen als eine falsch platzierte Schaltfläche.

## Testarten

| Ebene | Werkzeug | Typisches Beispiel |
|---|---|---|
| Backend-Unit-Test | JUnit | Prioritätsreihenfolge, Fälligkeit, Monatsende |
| Backend-Integration | Spring Boot Test, Testcontainers, PostgreSQL | Migration, Constraints, Transaktion und Versionskonflikt |
| API-Test | Spring Boot Test mit HTTP-/MVC-Unterstützung | `401/403/404/409`, Validierung, CSRF und Antwortformat |
| Frontend-Logik/Komponente | Vitest; React Testing Library als Arbeitsannahme | Formularfehler, zugängliche Labels, Lade- und Konfliktzustand |
| Ende-zu-Ende | Playwright gegen komplette Anwendung | Login → Aufgabe erstellen → neuladen → erledigen |
| Automatische Barrierefreiheit | axe-core mit Playwright | Sichtbare Dialoge und Fehlerzustände prüfen |
| Manuelle Prüfung | Entwickler, Tastatur und Screenreader | Sinnvolle Fokusführung und verständliche Meldungen |
| Betriebsprobe | Docker Compose und dokumentierte Befehle | Frischinstallation, Neustart, Update und Restore |

Unit-Tests sind schnell und isolieren eine Regel. Integrationstests prüfen das Zusammenspiel echter Komponenten. E2E-Tests verfolgen einen Benutzerablauf durch das ganze System. Jede Ebene hat eine andere Aufgabe; alles ausschließlich über E2E zu prüfen wäre langsam und schwer zu diagnostizieren.

## Verbindliche Fälle

- Registrierung mit gültigen, ungültigen und gleichzeitig doppelten E-Mails; Passwort weder im Klartext noch in Antworten/Logs.
- Login, Logout, Sessionablauf, fehlender/falscher CSRF-Token; Reset-Token abgelaufen und wiederverwendet; Rechteverlust durch Deaktivierung.
- Zwei Benutzer mit getrennten privaten Daten. Direkte API-Zugriffe, Suche, Tags und Zähler dürfen keine fremden Daten liefern. Administratorrolle hebt das nicht auf.
- Geteiltes Projekt, Zuweisung, Entfernen eines Mitglieds und Löschen des Projekts mit Aufgaben mehrerer Ersteller; konkurrierende Änderungen berücksichtigen.
- Zwei gleichzeitige Updates derselben Version: genau eines erfolgreich, zweites Konflikt. Löschen einer veralteten Version ebenso prüfen.
- Fachänderung und Aktivität: provozierter Fehler rollt beide zurück. Wiederholungsnachfolger und Erinnerung werden auch unter paralleler Ausführung nicht dupliziert.
- Zeitpunkte nahe Mitternacht, andere Zeitzone, fehlende Uhrzeit, Sommerzeitwechsel, Monatsende und überfällige Wiederholungen; Test-Uhr statt realem Warten.
- API-Seitenbildung, stabile Sortierung und kombinierte Filter; begrenzte Größen; Fehlerformat entspricht OpenAPI.
- Migrationen auf leerem PostgreSQL und von der vorherigen unterstützten Release-Version; dieselbe PostgreSQL-Hauptversion wie im Betrieb.
- Persistenz nach Neustart, Datenbankausfall im Healthcheck, Backup mit erfolgreichem Restore in getrennter Umgebung.

Datenbanktests verwenden echtes PostgreSQL über Testcontainers. Eine H2-Ersatzdatenbank belegt PostgreSQL-Sperren, SQL-Dialekt und Migrationen nicht ausreichend. Tests legen eigene synthetische Benutzer an; keine persönliche Entwicklerdatenbank und keine echten Kundendaten.

## Geplanter lokaler Prüfablauf

**Erst ab M0 nach Anlage der genannten Dateien ausführbar.** Im Repository-Stamm beginnen; Docker muss für Integrationstests laufen.

```sh
./src/backend/mvnw -f src/backend/pom.xml verify
npm --prefix src/frontend ci
npm --prefix src/frontend run lint
npm --prefix src/frontend run typecheck
npm --prefix src/frontend run test:unit
npm --prefix src/frontend run build
```

`verify` muss sowohl Unit- als auch Integrationstests einschließen. `test:unit` läuft einmalig ohne Watch-Modus. Der Agent muss die hier genannten npm-Skripte tatsächlich anlegen. Für Windows ist `src\backend\mvnw.cmd` statt `mvnw` vorgesehen; die Bash-Beispiele gelten für macOS/Linux bzw. WSL2.

E2E-Ablauf nach Installation der zum Lockfile passenden Playwright-Browser:

```sh
cd src/frontend
npx playwright install
npm run test:e2e
npm run test:a11y
```

Die spätere Playwright-Konfiguration muss die Testumgebung selbst starten oder die Vorbereitung exakt dokumentieren. E2E verwendet einen eigenen Compose-Projektnamen, eigene Datenbank/Volumes und eine konfigurierbare Basis-URL; keine Tests gegen echte Nutzerdaten. Installation zusätzlicher Betriebssystempakete auf Linux wird im getesteten Setup gesondert beschrieben.

## Kontinuierliche Integration (CI)

CI bedeutet: Ein zentraler Lauf prüft Änderungen automatisch. Der konkrete Anbieter wird in M0 gewählt; die Befehle bleiben auch lokal verwendbar.

1. Frischen Checkout mit festgelegten Java-/Node-Versionen einrichten.
2. Java-Build, Unit- und Testcontainers-Integrationstests ausführen.
3. Frontend-Abhängigkeiten aus Lockfile installieren, Lint, Typprüfung, Unit-Tests und Produktionsbuild ausführen.
4. Isolierte Anwendung starten, auf echte Bereitschaft warten, Playwright und axe ausführen.
5. Testberichte und bei Fehlern Screenshots/Traces bereitstellen; synthetische Daten verwenden und Tokens aus Ausgaben fernhalten.
6. Testumgebung nach dem Lauf entfernen, auch bei Fehlschlag; persönliche Entwicklungsvolumes unangetastet lassen.

Ein fehlendes Docker-System oder fehlende Browser führt zu einem sichtbar fehlgeschlagenen bzw. nicht ausgeführten Pflichtnachweis, niemals zu einem grünen Scheinergebnis. Externe Pakete können anfangs Netzwerkzugriff benötigen; dieser Bedarf ist zu dokumentieren.

## Welche Prüfung bei welcher Änderung?

| Änderung | Mindestnachweis |
|---|---|
| Fachregel | Unit-Test für Erfolgs-, Grenz- und Fehlerfall |
| Datenmodell/Migration | PostgreSQL-Integration, Altstandmigration und Konsistenz |
| API/Berechtigung | Positiv-/Negativtest mit verschiedenen Benutzern und OpenAPI-Abgleich |
| Formular/Interaktion | Komponentenprüfung, relevanter E2E-Ablauf, axe, manuelle Tastaturprüfung |
| Neues komplexes Bedienelement | Zusätzlich Screenreader- und Fokusprüfung |
| Deployment/Konfiguration | Startprobe, Healthcheck, Persistenz; bei Updateänderung Update-/Restore-Probe |
| Reine Dokumentation | Links, Pfade, Vollständigkeit und Widersprüche prüfen; Befehle bei vorhandener Anwendung verifizieren |

Keine starre Abdeckungszahl ersetzt diese Nachweise. Tests dürfen Verhalten prüfen, statt nur Implementierungsdetails nachzuschreiben. Instabile Tests werden untersucht; Wiederholungen dürfen reproduzierbare Fehler nicht verdecken.

## Ergebnisnachweis

Jede abgeschlossene Story verweist auf Tests und gegebenenfalls ein manuelles Protokoll mit Commit-ID, Datum, Umgebung und tatsächlichem Ergebnis. Nicht ausgeführte Prüfungen werden mit Ursache benannt. Das Referenzrelease verlangt zusätzlich die Frischinstallation aus US-160, den Restore aus US-162 und die manuelle Accessibility-Prüfung aus US-172.
