# UltimateTodo – Produkt- und Umsetzungsbacklog

**Stand: 30.09.2026. M0, M1a, der Aufgaben-Kern (M1b), US-023, M1c (US-040 bis 043, US-050 bis 053, US-110) und M2 bis auf einen offenen Testfall sind umgesetzt und automatisiert geprüft; die Frischinstallations-Probe (US-160) ist für ihren M1-Anteil durchgeführt.** M2 umfasst Mitgliedschaft, Zuweisung, Unteraufgaben, Kommentare, die Aktivitätshistorie und Kontenadministration (US-060 bis US-062, US-070, US-071, US-080, US-081, US-014), inklusive der zugehörigen M2-Teile von US-022/031/032/051 sowie der zuvor an US-081 hängenden Aktivitäts-Anteile von US-034/035/121. US-042 bleibt `IN ARBEIT`: ihr eigenes Akzeptanzkriterium verlangt einen Löschtest mit mehreren Erstellern einschließlich eines deaktivierten Kontos, den es bisher nicht gibt. US-013 (Passwort-Reset) ist mit reduziertem Umfang erledigt: eine Auftraggeber-Entscheidung nimmt die echte Wiederherstellung aus diesem Demo-Projekt heraus (siehe die Story selbst). Der M4-Anteil von US-160 (Update, Sicherung, Wiederherstellung) bleibt offen. Den genauen Stand je Story zeigen die `> **Stand:**`-Vermerke unter der jeweiligen Story.

**UI-Stand (US-180):** Die visuelle Überarbeitung ist implementiert und automatisiert geprüft; die manuelle Screenreader-Prüfung und ein echter Browser-Zoom-Test stehen noch aus. [Prüfprotokoll](docs/reviews/ui-US180.md).

[Einstieg](README.md) · [Fachregeln](docs/DOMAIN_MODEL.md) · [Architektur](docs/ARCHITECTURE.md)

## 1. Ziel und Umfang

UltimateTodo unterstützt persönliche und gemeinsame Aufgaben. Die Referenzlösung soll typische professionelle Anforderungen zeigen: Konten, Berechtigungen, dauerhafte Daten, REST-API, barrierefreie Oberfläche, Konfliktbehandlung, Tests und nachvollziehbaren Betrieb. Das Projekt wird von einer Person gemeinsam mit einem Coding-Agenten gebaut und später Azubis als Referenzlösung vorgeführt und mit deren eigenen Lösungen verglichen. Es gibt keine formalen Abnahmen durch Dritte; Nachweise erbringen Tests und die Prüfprotokolle der Entwickler.

Nicht vorgesehen sind native Mobilanwendungen, Offline-Synchronisation, Unternehmens-SSO, Microsoft-365-/Google-Workspace-Integration, Abrechnung, KI-generierte Aufgaben, große Dateianhänge, Echtzeit-Chat und frei konfigurierbare Jira-Workflows. Drag & Drop ist optional; falls später umgesetzt, benötigt es eine gleichwertige Bedienung ohne Ziehen.

## 2. Rollen und Priorität

- **Gast:** Registrierung, Anmeldung und Passwortwiederherstellung; keine Fachdaten.
- **Benutzer:** eigenes Konto und persönliche Aufgaben; Zusammenarbeit in freigegebenen Projekten.
- **Projekt-Eigentümer:** zusätzlich Projekt und Mitgliedschaften verwalten.
- **Administrator:** Konten deaktivieren/reaktivieren und Systemzustand prüfen; kein pauschaler Zugriff auf private Inhalte.

M0 bis M4 beschreiben die Umsetzungsreihenfolge, keine unterschiedlichen Qualitätsniveaus. Jede Story trägt zusätzlich eine Priorität nach MoSCoW: **Must** (ohne sie ist das Referenzprodukt unvollständig), **Should** (wichtig, aber ein Release ist ohne sie vertretbar), **Could** (nützliche Ergänzung). Innerhalb eines Meilensteins gilt die Reihenfolge der Slices aus Abschnitt 3. Sicherheit, Barrierefreiheit und Dokumentation beginnen in M0. Eine Story mit mehreren Kriterien wird nicht abgeschlossen, wenn nur ein Teil umgesetzt ist. Statuswerte: `OFFEN`, `IN ARBEIT`, `BLOCKIERT` mit konkretem Grund, `ERLEDIGT` mit Nachweis (Tests, Commit-ID). Stories über mehrere Meilensteine (etwa „M1/M2“) werden je Meilenstein als Teilstatus geführt und sind erst nach dem letzten Teil `ERLEDIGT`.

## 3. Meilensteine

| Slice | Ergebnis | Stories / Abhängigkeiten |
|---|---|---|
| M0 – Grundlage | Baubarer Stack, Compose, Migrationen, Healthchecks, erste Test- und UI-Grundlage, CI | US-001, US-003, US-004, US-130, US-131; technische Basis für US-140, US-150, US-170–172 |
| M1a – Konten und Anmeldung | Registrierung, Login/Logout, Session/CSRF, Profil, Passwort ändern, Ratenbegrenzung | US-010–012, US-020, US-021, US-023, US-141, US-022 (M1-Teil), US-002 (Teil) |
| M1b – Aufgaben-Kern | Aufgaben anlegen, ansehen, bearbeiten, löschen, erledigen; Priorität, Fälligkeit; Konflikte und Transaktionen | US-030–037, US-120, US-121 (M1-Teil), US-002; M1a |
| M1c – Organisation und Findbarkeit | Projekte, Tags, Suche, Filter, Sortierung, Standardansichten, Dashboard; Frischinstallations-Probe | US-040–043, US-050–053, US-110, US-160; M1b |
| M2 – Zusammenarbeit | Mitglieder, Zuweisungen, Unteraufgaben, Kommentare und sichtbare Historie; Kontoverwaltung vervollständigen | US-013–014, US-060–062, US-070–071, US-080–081 und M2-Teile von US-022/031/032/042/051; M1 vorhanden |
| M3 – Zeitfunktionen | Wiederkehrende Aufgaben und persönliche In-App-Erinnerungen | US-090, US-100; stabile Zeit- und Transaktionsregeln aus M1/M2 |
| M4 – Referenzrelease | Vollständiger Nachweis, Updates, Restore, Demo-Unterlagen | US-140, US-150, US-161, US-162, US-170–172 abschließend nachweisen; M1–M3 erfüllt |

### Priorisierung

| Priorität | Stories |
|---|---|
| Must | US-001–004, US-010–012, US-020–022, US-030–037, US-040–042, US-050–053, US-110, US-120–121, US-130–131, US-140, US-141, US-150, US-170, US-171 |
| Should | US-013, US-014, US-023, US-043, US-060–062, US-070–071, US-080–081, US-160–162, US-172, US-180, US-181 |
| Could | US-090, US-100, US-151 (Demo-Unterlagen), US-182 (Präsentation in der Anwendung) |

Begründung: Ein sicherer, barrierefreier Kern mit persönlichen Aufgaben (M0/M1) ist das Referenzprodukt. Zusammenarbeit (M2) zeigt Berechtigungen und Konsistenz, die Zeitfunktionen (M3) sind fachlich anspruchsvoll, aber verzichtbar, falls Zeit fehlt. Betrieb (US-160–162) wird vor dem Referenzrelease durchgespielt, gilt aber als Should, weil kein Dritter es abnimmt.

Bereits in M1 werden Aktivitätsdaten zusammen mit Fachänderungen gespeichert; die vollständige Anzeige folgt in M2. Vor M2 kann es noch keine Zuweisung an andere Personen geben. UI und API sollen später vorgesehene Funktionen nicht durch funktionslose Platzhalter vortäuschen.

## 4. Epic-Übersicht

| Epic | Thema |
|---|---|
| EPIC-01 | Anwendungsgrundlage und Dokumentation |
| EPIC-02 | Benutzerverwaltung |
| EPIC-03 | Authentifizierung und Autorisierung |
| EPIC-04 | Aufgabenverwaltung |
| EPIC-05 | Projekte und Organisation |
| EPIC-06 | Suche, Filter und Sortierung |
| EPIC-07 | Zusammenarbeit |
| EPIC-08 | Unteraufgaben |
| EPIC-09 | Kommentare und Aktivitätshistorie |
| EPIC-10 | Wiederkehrende Aufgaben |
| EPIC-11 | Erinnerungen und Benachrichtigungen |
| EPIC-12 | Dashboard |
| EPIC-13 | Gleichzeitige Änderungen und Datenintegrität |
| EPIC-14 | Logging und Monitoring |
| EPIC-15 | Sicherheit |
| EPIC-16 | Tests und Ausbildungsnachweise |
| EPIC-17 | Installation, Deployment und Updates |
| EPIC-18 | Barrierefreiheit |
| EPIC-19 | Visuelle Gestaltung für die Vorführung |

## 5. EPIC-01 – Anwendungsgrundlage und Dokumentation

### US-001 – Anwendung reproduzierbar starten · M0 · IN ARBEIT

> **Stand:** Umgesetzt und geprüft: Compose-Start von web/backend/db mit Healthchecks, feste Versionen, Wrapper und Lockdatei, `.env.example`, verständlicher Abbruch bei fehlendem `POSTGRES_PASSWORD`. Offen: Zurückweisen des Platzhalterpassworts, verständliche Backend-Meldung bei fehlender Konfiguration außerhalb von Compose.

Als Betreiber möchte ich alle Dienste mit dokumentierten Schritten starten können.

- Webserver/Frontend, Backend und PostgreSQL starten über Docker Compose; externe Konfiguration erfordert keine Codeänderung.
- Ein frischer Checkout lässt sich mit den dokumentierten Versionen bauen; Wrapper, Lockdatei, Konfigurationsbeispiel und feste Container-Versionen sind vorhanden.
- Fehlende Pflichtkonfiguration und fehlgeschlagene Starts führen zu verständlichen, geheimnisfreien Fehlermeldungen.

### US-002 – Daten dauerhaft speichern · M1 · IN ARBEIT

> **Stand:** Konten und Aufgaben (inkl. Fälligkeit, Status, Version) nachgewiesen bestandsfest über einen Backend-Neustart (`TaskRestartPersistenceIT`: zwei unabhängige Spring-Kontexte gegen denselben Testcontainer, `flyway_schema_history` bleibt konsistent). Offen: Projekte (M1c).

Als Benutzer möchte ich Konten, Projekte und Aufgaben nach einem Neustart wiederfinden.

- Alle drei Datenarten liegen in PostgreSQL; normaler Neustart und `compose down` ohne Volume-Löschung erhalten sie.
- Flyway initialisiert ein leeres Schema und aktualisiert einen bestehenden Stand versioniert.
- Ein Test liest zuvor angelegte Daten nach einem Anwendungsneustart wieder; keine automatische Löschung bei Start.

### US-003 – Betriebszustand prüfen · M0 · ERLEDIGT

> **Stand:** Nachweis: `OperationsIT`, `DatabaseOutageIT` (Readiness DOWN/503 bei gestopptem PostgreSQL, Liveness bleibt UP, nur Gesamtzustand öffentlich), Stand M0 vom 30.09.2026.

Als Betreiber möchte ich die Einsatzbereitschaft ohne UI feststellen.

- Actuator stellt einen minimalen Healthcheck bereit; Readiness meldet Datenbankausfälle als nicht bereit.
- Liveness wird nicht allein durch einen vorübergehenden Datenbankausfall negativ.
- Öffentlich sind keine internen Verbindungsdaten sichtbar; erfolgreiche und fehlerhafte Zustände sind getestet.

### US-004 – Dokumentation als Einstieg nutzen · M0 · IN ARBEIT

> **Stand:** README, Entwicklungs- und Deployment-Anleitung sowie Versionstabelle sind auf M0 abgestimmt; Dev-Ablauf (Dev-DB, `spring-boot:run`, Vite-Proxy) und Compose-Lauf wurden ausgeführt. Offen: Abgleich bei jeder weiteren Story.

Als Entwickler möchte ich Anforderungen, Architektur und erste Befehle an einem Ort finden.

- README verlinkt alle Fachdateien; reale Pfade und Befehle stimmen nach Umsetzung mit dem Repository überein.
- Getestete Versionen und verbleibende Einschränkungen sind eingetragen; Zielabläufe sind von verifizierten Abläufen unterscheidbar.
- Dokumentation bleibt Deutsch und erklärt neue Begriffe; wesentliche Entscheidungen besitzen ADRs.

## 6. EPIC-02 – Benutzerverwaltung

### US-010 – Konto registrieren · M1 · IN ARBEIT

> **Stand:** Umgesetzt und automatisiert nachgewiesen (`AuthApiIT`: Validierung, Argon2id, parallele Doppelregistrierung, Rolle nur `USER`; Playwright/axe für Registrierung normal und mit Fehlern). Offen: manuelle Tastatur-/Screenreader-Prüfung (US-172) für die Definition of Done.

Als Gast möchte ich ein Konto mit eindeutiger Login-E-Mail anlegen.

- Pflichtfelder, E-Mail, Passwortregeln und eine gültige IANA-Zeitzone (aus dem Browser vorgeschlagen, änderbar) werden serverseitig geprüft; doppelte E-Mails auch bei parallelen Anfragen abgewiesen.
- Nur ein sicherer Passwort-Hash wird gespeichert; Selbstregistrierung erzeugt ausschließlich die Rolle `USER`.
- Das neue aktive Konto kann sich anmelden; Fehler sind verständlich und zugänglich.

### US-011 – Eigenes Profil bearbeiten · M1 · IN ARBEIT

> **Stand:** Umgesetzt und getestet (`AuthApiIT`, `auth.spec.ts`): nur Anzeigename und Zeitzone änderbar, E-Mail/Rolle nicht. Offen: manuelle Tastatur-/Screenreader-Prüfung (US-172) für die Definition of Done.

Als Benutzer möchte ich mein Profil sehen und Anzeigename sowie Zeitzone bearbeiten.

- Login-E-Mail und Erstellungszeit sind sichtbar; die E-Mail bleibt in der ersten Version unveränderlich.
- Nur der angemeldete Benutzer kann sein Profil ändern; Rolle und Kontostatus sind dort nicht bearbeitbar.
- Änderungen werden validiert; ein Zeitzonenwechsel verändert keine gespeicherten Fälligkeiten rückwirkend.

### US-012 – Passwort ändern · M1 · IN ARBEIT

> **Stand:** Umgesetzt und getestet: alte und parallele Sitzungen enden über die Sitzungsgeneration, altes Passwort wird abgelehnt, Passwörter nicht in Logs. Offen: manuelle Tastatur-/Screenreader-Prüfung (US-172) für die Definition of Done.

Als Benutzer möchte ich mein Passwort ersetzen können.

- Das bisherige Passwort ist erforderlich; das neue erfüllt die dokumentierten Regeln.
- Danach funktioniert nur das neue Passwort; alte Sitzungen werden entwertet und eine neue Anmeldung verlangt.
- Falsches bisheriges Passwort verändert keine Daten; Passwörter erscheinen nie in Logs.

### US-013 – Zugang wiederherstellen · M2 · ERLEDIGT (reduzierter Umfang)

> **Stand:** Auftraggeber-Entscheidung (30.09.2026): eine echte Passwort-Wiederherstellung (Reset-Token, Mailfänger, Versandweg) ist für dieses Demo-Projekt bewusst nicht umgesetzt — die ursprünglichen Akzeptanzkriterien unten gelten damit nicht mehr; siehe [SECURITY.md](docs/SECURITY.md) für die zurückgestellte Vorgabe, falls die Einschränkung später aufgehoben wird. Umgesetzt und getestet (`AuthPages.test.tsx`, `auth.spec.ts`, `a11y.spec.ts`) ist stattdessen: „Passwort vergessen?“ auf der Anmeldeseite führt zu einer eigenen Seite, die verständlich erklärt, dass die Wiederherstellung im Demo-Projekt nicht verfügbar ist, und zurück zur Anmeldung führt. Kein Reset-Token, kein E-Mail-Versand, keine neue Angriffsfläche.

Als Benutzer möchte ich ein vergessenes Passwort zurücksetzen können.

- ~~Die Anforderung verrät nicht, ob ein Konto existiert; Anfragen sind begrenzt.~~
- ~~Reset-Tokens verfallen nach der festgelegten Frist, sind nur einmal verwendbar und werden nach Erfolg ungültig; alte Sitzungen enden.~~
- ~~Lokaler Mailfänger oder echter Versandweg sind dokumentiert; keine Reset-Links in normalen Anwendungslogs.~~

### US-014 – Konten administrieren · M2 · ERLEDIGT

> **Stand:** Umgesetzt und getestet (`UserAdminApiIT`, `AdminBootstrapRunnerIT`, `UserRulesTest`, `AdminPages.test.tsx`, `App.test.tsx`): `/api/v1/admin/users` listet Konten mit E-Mail, Anzeigename, Rolle und Status, ohne Passwort und ohne Aufgaben-/Projektinhalte; nur die Rolle ADMIN erreicht diese Pfade (`SecurityConfig`, serverseitig erzwungen, unabhängig vom ausgeblendeten „Administration“-Link im Frontend). Deaktivieren beendet Sitzungen spätestens bei der nächsten Anfrage (bestehender `SessionEpochFilter`), Daten bleiben bestehen; Reaktivieren erlaubt erneute Anmeldung. Das letzte aktive Administratorkonto kann nicht deaktiviert werden (`409 LAST_ACTIVE_ADMIN`), unabhängig davon, wer die Aktion auslöst. Erstmalige Admin-Einrichtung: `ADMIN_BOOTSTRAP_EMAIL` befördert ein bereits selbst registriertes Konto beim Anwendungsstart (`AdminBootstrapRunner`) — kein Standardkennwort, kein öffentlicher Bootstrap-Endpunkt (siehe [SECURITY.md](docs/SECURITY.md), [DEPLOYMENT.md](docs/DEPLOYMENT.md)). Offen: kein automatisierter E2E-Test für den Admin-Handlungspfad selbst, da der Bootstrap-Vorgang nur beim Anwendungsstart läuft und ein zur Laufzeit von Playwright registriertes Konto diesen nicht nachträglich durchlaufen kann, ohne den Compose-Stack mitten im Testlauf neu zu starten; die Zugriffsbeschränkung für normale Benutzer ist per E2E geprüft (`admin.spec.ts`).

Als Administrator möchte ich Konten auflisten, deaktivieren und reaktivieren.

- Ein deaktiviertes Konto erhält weder neue noch fortgesetzte Sitzungszugriffe; Daten bleiben bestehen.
- Normale Benutzer können diese Aktionen nicht ausführen; Administration gewährt keinen Zugriff auf Aufgabeninhalte.
- Sichere erstmalige Admin-Einrichtung ist dokumentiert; kein Standardkennwort; letztes aktives Admin-Konto bleibt erhalten.

## 7. EPIC-03 – Authentifizierung und Autorisierung

### US-020 – Anmelden · M1 · IN ARBEIT

> **Stand:** Umgesetzt und getestet: Session-ID-Wechsel, CSRF für alle schreibenden Anfragen, `401`/`403` im JSON-Format, Status bleibt beim Neuladen. Cookie-Flag `Secure` ist konfigurierbar, aber im lokalen Betrieb aus. Offen: manuelle Tastatur-/Screenreader-Prüfung (US-172) für die Definition of Done.

Als registrierter Benutzer möchte ich sicher auf meine Daten zugreifen.

- Gültige Zugangsdaten eines aktiven Kontos erzeugen eine Session; ungültige werden mit allgemeiner Meldung abgewiesen.
- Navigation und Neuladen innerhalb der gültigen Session erhalten den Anmeldestatus.
- Geschützte APIs liefern ohne Anmeldung `401`; Session-Cookie und CSRF-Ablauf entsprechen dem Sicherheitsmodell.

### US-021 – Abmelden · M1 · IN ARBEIT

> **Stand:** Umgesetzt und getestet: Logout entwertet Sitzung serverseitig, löscht Cookie, ist CSRF-geschützt; Clientzustand wird geleert. Offen: manuelle Tastatur-/Screenreader-Prüfung (US-172) für die Definition of Done.

Als Benutzer möchte ich eine Sitzung zuverlässig beenden.

- Logout entwertet die Sitzung auf dem Server und entfernt das Cookie.
- Eine Wiederverwendung des bisherigen Cookies erlaubt keinen Zugriff; erneute Anmeldung ist erforderlich.
- Logout ist gegen CSRF geschützt; andere Benutzer oder private Daten erscheinen nach Logout nicht aus einem UI-Cache.

### US-022 – Zugriff auf eigene und freigegebene Daten begrenzen · M1/M2 · ERLEDIGT

> **Stand:** M1a-Teil (Konten, kein Fremdzugriff auf Profil, Deaktivierung wirkt bei nächster Anfrage) getestet. M1b-Teil (Aufgaben) umgesetzt und getestet: jede Aufgaben-Operation filtert serverseitig nach Ersteller (`TaskRepository.findAccessible*`), fremde und nicht existierende Aufgaben sind ununterscheidbar (`404`, kein `409` trotz korrekter Version), auch für ein Konto mit Rolle `ADMIN` (`TaskApiIT`). M2-Teil (Projektmitgliedschaft) umgesetzt und getestet (`ProjectMembershipApiIT`, `TaskAssignmentApiIT`): „zugänglich“ schließt seit US-060 Eigentümer *und* Mitglieder ein (`ProjectRepository.ACCESSIBLE_CONDITION`); für eigentümerspezifische Aktionen (Mitglied aufnehmen/entfernen, Projekt löschen) liefert ein Mitglied ohne Eigentümerrechte bewusst `403` statt `404` (die Zugehörigkeit ist bekannt, nur die Handlung ist ihm nicht erlaubt), fremde Dritte weiterhin `404`. Unteraufgaben (US-070/071), Kommentare (US-080) und die Aktivitätshistorie (US-081) prüfen die Rechte einheitlich über dieselbe Hauptaufgaben-Prüfung (`TaskService.loadAccessible`) und sind ebenfalls getestet (`SubtaskApiIT`, `CommentApiIT`, `ActivityApiIT`: fremder Benutzer erhält überall `404`).

Als Benutzer möchte ich meine privaten Inhalte vor anderen schützen.

- Backend prüft jede Lese- und Schreiboperation einschließlich Suche, Zähler und abhängiger Daten.
- Geänderte URLs, fremde IDs und manipulierte Request-Felder umgehen keine Rechte.
- Tests mit zwei Benutzern und Admin decken persönliche Aufgaben ab M1 und entzogene Projektmitgliedschaft ab M2 ab.

### US-023 – Sitzung bei Inaktivität beenden · M1 · Should · IN ARBEIT

> **Stand:** Umgesetzt und mit gefälschter Uhr getestet (`SessionTimeoutWarning.test.tsx`, `SessionExpiredDialog.test.tsx`): 2 Minuten vor Ablauf erscheint ein zugänglicher Dialog („Angemeldet bleiben“/„Jetzt abmelden“); „Angemeldet bleiben“ löst eine echte Anfrage aus (setzt auch den Server-Timer zurück, nicht nur lokal). Läuft die Sitzung tatsächlich ab oder liefert eine geschützte Anfrage `401`, bleibt die aktuelle Seite bestehen (kein Sprung zur Anmeldung) und ein Dialog verlangt die erneute Anmeldung; ein ungesendeter Formularentwurf bleibt dabei nachweislich erhalten. Offen: manuelle Tastatur-/Screenreader-Prüfung (US-172) und die axe-/Playwright-Prüfung mit echtem, verkürztem `SESSION_TIMEOUT`.

Als Benutzer möchte ich rechtzeitig gewarnt werden, bevor meine Sitzung abläuft.

- Nach 30 Minuten Inaktivität endet die Session serverseitig; die Oberfläche warnt vorher zugänglich (Statusmeldung, per Tastatur bedienbar) und ermöglicht die Verlängerung.
- Ungesendete Formulareingaben bleiben bei erneuter Anmeldung innerhalb der laufenden Seite erhalten; Ablauf wird nie als erfolgreiches Speichern dargestellt.
- Die Frist ist konfigurierbar und testbar (Test-Uhr).

## 8. EPIC-04 – Aufgabenverwaltung

### US-030 – Aufgabe anlegen · M1 · ERLEDIGT

> **Stand:** Umgesetzt und getestet (`TaskApiIT`, `TaskPages.test.tsx`, `tasks.spec.ts`): Titel Pflicht (1–200 Zeichen), Beschreibung/Priorität/Fälligkeit/Projekt optional, Server setzt Ersteller/Status `OPEN`/Priorität `NORMAL`/Zeitstempel, fremde Felder (`status`, `creatorId`, `version`) werden ignoriert. Ein Projekt darf nur zugeordnet werden, wenn der Handelnde selbst Eigentümer ist (`400`, sonst). Tags werden erst nach dem Anlegen über `PUT /api/v1/tasks/{id}/tags` zugeordnet (US-043). Offen: manuelle Tastatur-/Screenreader-Prüfung (US-172).

Als Benutzer möchte ich eine Aufgabe bereits mit einem Titel erstellen können.

- Standard ist `OPEN` und `NORMAL`; Projekt, Beschreibung, Fälligkeit und Tags sind optional; fremde Zuweisung folgt in M2.
- Backend setzt Ersteller und Zeitstempel; leere oder zu lange Titel und ungültige Fälligkeiten werden abgewiesen.
- Speicherung ist dauerhaft; Fehler lassen den Formulareintrag erhalten und werden dem passenden Feld zugeordnet.

### US-031 – Aufgabe ansehen · M1/M2 · ERLEDIGT

> **Stand:** Umgesetzt und getestet: Detailansicht und paginierte Liste (Standard 25/max. 100, stabile Sortierung `createdAt`/`id`) zeigen Titel, Beschreibung, Status, Priorität, Fälligkeit, Ersteller, Bearbeiter, Projekt, Tags, Unteraufgaben, Kommentare, Aktivitätshistorie und Zeitstempel; Ersteller- und Bearbeiter-Name werden serverseitig aufgelöst (`UserAccess.summarize`) statt roher IDs. Unzugängliche/nicht vorhandene Aufgaben liefern eine einheitliche Nicht-gefunden-Ansicht ohne fremde Daten.

Als berechtigter Benutzer möchte ich alle Informationen einer Aufgabe sehen.

- Ansicht zeigt Titel, Beschreibung, Status, Priorität, Fälligkeit, Ersteller, Projekt, Tags und Zeitstempel.
- Ab M2 gehören Bearbeiter, Unteraufgaben, Kommentare und Aktivitäten zur vollständigen Ansicht.
- Nicht vorhandene und unzugängliche Aufgaben führen zu einer verständlichen Nicht-gefunden-Ansicht ohne fremde Daten.

### US-032 – Aufgabe bearbeiten · M1/M2 · ERLEDIGT

> **Stand:** M1-Teil umgesetzt und getestet (`TaskApiIT`, `TaskPages.test.tsx`, `tasks.spec.ts`): Titel/Beschreibung/Priorität/Status/Fälligkeit änderbar, Version steigt nur bei tatsächlicher Änderung, veraltete Version liefert `409` mit erhaltenem Formularentwurf; die Oberfläche zeigt die abweichenden Felder und lässt bewusst zwischen „aktuelle Fassung“ und „eigene Eingabe“ wählen (US-120). M2-Teil umgesetzt und getestet (`TaskAssignmentApiIT`): Bearbeiter ist änderbar (nur Eigentümer/Mitglieder des aktuellen Projekts bzw. bei persönlichen Aufgaben nur der Ersteller selbst sind zuweisbar); ein Projektwechsel ist enger geregelt als die übrige Bearbeitung – nur Ersteller oder Eigentümer des *aktuellen* Projekts dürfen die Aufgabe einem anderen Projekt zuordnen, das Zielprojekt muss dem Handelnden zugänglich sein.

Als berechtigter Benutzer möchte ich Aufgabenangaben ändern.

- Felder aus US-030 sowie Status und ab M2 Bearbeiter sind änderbar; Ersteller und technische Zeitstempel nicht frei editierbar.
- Änderungszeit und Version steigen bei einer tatsächlichen Änderung; Fachregeln gelten auch bei direktem API-Aufruf.
- Veraltete Versionen erzeugen `409`; Entwurf bleibt erhalten. Projektwechsel beachtet die Rechte- und Bereinigungsregeln des Domänenmodells.

### US-033 – Aufgabe löschen · M1 · ERLEDIGT

> **Stand:** Umgesetzt und getestet: zugänglicher Bestätigungsdialog (WAI-ARIA-Dialogmuster, Fokus-Rückkehr, Escape) nennt die Aufgabe und die endgültige Wirkung; Löschen erfordert die aktuelle Version (`DELETE ?version=`), fehlende/veraltete Version liefert `400`/`409`; anschließendes Lesen liefert `404`. Offen: manuelle Tastatur-/Screenreader-Prüfung (US-172).

Als berechtigter Benutzer möchte ich überflüssige Aufgaben entfernen.

- Ein zugänglicher Bestätigungsdialog nennt die konkrete Aufgabe und die endgültige Wirkung.
- Löschen mit aktueller Version entfernt die Aufgabe und abhängige Daten in einer Transaktion; fremde oder veraltete Löschversuche scheitern.
- Die Aufgabe verschwindet aus Listen; anschließendes Lesen liefert `404`.

### US-034 – Aufgabe erledigen · M1 · ERLEDIGT

> **Stand:** Umgesetzt und getestet (`TaskApiIT`, `ActivityApiIT`): Status wechselt nach `DONE`, Abschlusszeit wird serverseitig gesetzt, überfällig ausgeschlossen; erneutes Abschließen (auch mit veralteter Version) ist wirkungslos statt eines Konflikts (Arbeitsannahme, siehe [DOMAIN_MODEL.md](docs/DOMAIN_MODEL.md)). Seit US-081 (M2) entsteht dabei ein eigener `COMPLETED`-Aktivitätseintrag (nicht die allgemeine Statusänderung), sichtbar im Verlauf der Aufgabe.

Als Benutzer möchte ich erledigte Arbeit kennzeichnen.

- Status wechselt nach `DONE`, Abschlusszeit wird serverseitig gesetzt, Aktivität gespeichert.
- Erledigte Aufgabe erscheint in der entsprechenden Ansicht und zählt nicht als überfällig.
- Erneutes Abschließen verändert den Abschluss nicht und erzeugt ab M3 keinen doppelten Nachfolger.

### US-035 – Aufgabe wieder öffnen · M1 · ERLEDIGT

> **Stand:** Umgesetzt und getestet (`TaskApiIT`, `ActivityApiIT`): Status wechselt von `DONE` zurück nach `OPEN`, Abschlusszeit wird geleert, neue Version; auf einer nicht erledigten Aufgabe wirkungslos statt eines Konflikts (Arbeitsannahme, siehe [DOMAIN_MODEL.md](docs/DOMAIN_MODEL.md)). Seit US-081 (M2) entsteht dabei ein eigener `REOPENED`-Aktivitätseintrag. Offen: Wiederholungsnachfolger erst M3.

Als Benutzer möchte ich eine erledigte Aufgabe erneut bearbeiten.

- Status wechselt von `DONE` nach `OPEN`; Abschlusszeit wird geleert.
- Änderung erhält Aktivität und neue Version.
- Ab M3 bleiben bereits erzeugte Wiederholungsnachfolger bestehen; erneutes Abschließen dupliziert sie nicht.

### US-036 – Priorität setzen · M1 · ERLEDIGT

> **Stand:** Umgesetzt und getestet: `LOW`/`NORMAL`/`HIGH`/`URGENT` auswählbar und änderbar, fachliche Rangfolge (`TaskPriority.rank()`), Anzeige mit deutschem Text zusätzlich zur Farbe (WCAG 1.4.1), unbekannte Werte werden mit `400` abgewiesen. Sortierung nach Priorität ist erst mit US-052 (M1c) vorgesehen.

Als Benutzer möchte ich wichtige Aufgaben unterscheiden.

- `LOW`, `NORMAL`, `HIGH`, `URGENT` sind auswählbar und nachträglich änderbar.
- Anzeige nutzt Text oder zugängliche Symbole zusätzlich zur Farbe.
- Sortierung verwendet die fachliche Reihenfolge; unbekannte Werte werden abgewiesen.

### US-037 – Fälligkeit setzen und entfernen · M1 · ERLEDIGT

> **Stand:** Umgesetzt und getestet (`DueDateTest`, `TaskApiIT`, `dueTime.test.ts`): Datum optional, Uhrzeit nur mit Datum und (vom Client vorbelegter) IANA-Zeitzone; Entfernen des Datums entfernt Uhrzeit/Zone/Offset. Sommerzeit-Randfälle geprüft: eine nicht existierende lokale Uhrzeit (Frühjahrslücke) wird abgewiesen, eine doppelte lokale Uhrzeit (Herbstüberlappung) verlangt eine Offset-Auswahl. Überfälligkeit gilt ab genau dem Fälligkeitszeitpunkt und schließt erledigte Aufgaben aus (Arbeitsannahmen, siehe [DOMAIN_MODEL.md](docs/DOMAIN_MODEL.md)). Anzeige in einer von der Aufgaben-Zeitzone abweichenden Kontozeitzone ist umgesetzt.

Als Benutzer möchte ich Aufgaben mit oder ohne Termin verwalten.

- Datum ist optional; Uhrzeit nur zusammen mit Datum und Zeitzone. Entfernen des Datums entfernt die Uhrzeit.
- Überfälligkeit folgt den dokumentierten Zeitregeln; erledigte Aufgaben sind ausgeschlossen.
- Tests prüfen Datumsgrenzen, Zeitumstellung und Anzeige in einer anderen Zeitzone.

## 9. EPIC-05 – Projekte und Organisation

### US-040 – Projekt anlegen · M1 · ERLEDIGT

> **Stand:** Umgesetzt und getestet (`ProjectApiIT`, `ProjectPages.test.tsx`, `projects.spec.ts`): Name Pflicht (1–100 Zeichen), Beschreibung optional (≤10.000 Zeichen), Ersteller wird Eigentümer, neues Projekt ist für andere Benutzer nicht sichtbar, bis der Eigentümer es über US-060 teilt.

Als Benutzer möchte ich Aufgaben nach einem Thema bündeln.

- Name ist Pflicht, Beschreibung optional; Ersteller wird Eigentümer.
- Neues Projekt ist zunächst privat; ein anderer Benutzer kann es nicht lesen.
- Ungültige Namen werden verständlich abgewiesen.

### US-041 – Projekt bearbeiten · M1 · ERLEDIGT

> **Stand:** Umgesetzt und getestet: nur der Eigentümer darf ändern (fremdes Projekt liefert überall `404`, auch mit korrekter Version statt `409`); Validierung und optimistisches Sperren (`409` bei veralteter Version, Formularentwurf bleibt erhalten) analog zu Aufgaben.

Als Eigentümer möchte ich Name und Beschreibung ändern.

- Nur der Eigentümer darf Projektmetadaten ändern; Mitgliedschaft genügt nicht.
- Validierung und Versionsprüfung verhindern ungültige oder verlorene Änderungen.
- Bestehende Aufgaben und Mitgliedschaften bleiben erhalten.

### US-042 – Projekt ohne Aufgabenverlust löschen · M1/M2 · IN ARBEIT

> **Stand:** M1-Teil umgesetzt und getestet (`ProjectApiIT.loeschenOhneAufgabenverlustLoestAufgabenTransaktional`, `projects.spec.ts`): Löschen entfernt den Projektbezug transaktional (`task.project_id = null`), die Aufgabe bleibt erhalten und wird zur persönlichen Aufgabe ihres Erstellers; die Bestätigung nennt diese Wirkung. M2-Teil umgesetzt und getestet (`ProjectMembershipApiIT.mitgliedEntfernenBereinigtZuweisungAberNichtDieAufgabeSelbst`, `ProjectMembershipApiIT.projektMitProjektTagsLaesstSichJetztLoeschen`): Projektlöschung bereinigt zusätzlich alle Mitgliedschaften, projektbezogenen Tags und deren Zuordnungen sowie Zuweisungen an Projektaufgaben transaktional. Erinnerungen bleiben offen (erst M3/US-100).

Als Eigentümer möchte ich ein Projekt auflösen, ohne Aufgaben zu verlieren.

- Bestätigung erklärt, dass Aufgaben zu persönlichen Aufgaben ihrer jeweiligen Ersteller werden.
- Projektbezug wird entfernt; unzulässige Zuweisungen, Tags und Erinnerungen werden transaktional bereinigt.
- Fremde Mitglieder verlieren Zugriff; Tests prüfen ab M2 mehrere Ersteller, einschließlich eines deaktivierten Kontos.

### US-043 – Tags verwalten · M1 · ERLEDIGT

> **Stand:** Umgesetzt und getestet (`TagApiIT`, `TagsPage.test.tsx`/`TaskPages.test.tsx`, `tags.spec.ts`): ein Tag gehört entweder einem Benutzer persönlich oder genau einem eigenen Projekt (DB-CHECK, nicht beides); Namen sind je Geltungsbereich fallunabhängig eindeutig (`409` bei Duplikat). Zuordnung/Entfernung an einer Aufgabe ersetzt die gesamte Tag-Liste über `PUT /api/v1/tasks/{id}/tags`, ändert bewusst nicht deren Version (Arbeitsannahme, siehe [Fachregeln](docs/DOMAIN_MODEL.md)); fremde oder unbekannte Tags werden serverseitig abgelehnt. Löschen eines Tags oder einer Aufgabe entfernt nur die Zuordnung, nie die andere Seite. Die Aufgabenliste ist nach einem Tag filterbar.

Als Benutzer möchte ich Aufgaben unabhängig von ihrer Projektansicht markieren.

- Mehrere Tags pro Aufgabe sind möglich, hinzufügbar und entfernbar.
- Namen sind pro persönlichem bzw. Projekt-Geltungsbereich eindeutig; private Tags werden nicht global angezeigt.
- Tags sind filterbar; Entfernen einer Zuordnung löscht keine andere Aufgabe.

## 10. EPIC-06 – Suche, Filter und Sortierung

### US-050 – Aufgaben durchsuchen · M1 · ERLEDIGT

> **Stand:** Umgesetzt und getestet (`TaskListQueryApiIT`, `TaskPages.test.tsx`, `tasklist-query.spec.ts`): `q` sucht in Titel und Beschreibung ohne Beachtung der Großschreibung (Teilstring); die Ownership-Prüfung der Liste gilt unverändert, fremde Aufgaben sind nie Treffer. Leerer Suchtext zeigt die erlaubte Liste; eine leere Trefferliste erklärt sich über den allgemeinen „Keine Aufgaben für diese Auswahl“-Hinweis mit Rücksetzmöglichkeit.

Als Benutzer möchte ich Aufgaben anhand von Text finden.

- Suche umfasst Titel und Beschreibung ohne Beachtung der Großschreibung.
- Nur zugängliche Aufgaben zählen als Treffer; Seitenzahlen verraten keine fremden Treffer.
- Leerer Suchtext zeigt die erlaubte Liste; leere Trefferliste erklärt das Ergebnis.

### US-051 – Aufgaben filtern · M1/M2 · ERLEDIGT

> **Stand:** M1-Teil umgesetzt und getestet (`TaskListQueryApiIT`, `TaskPages.test.tsx`, `tasklist-query.spec.ts`): Filter für Status, Priorität, Projekt, Tag, Fälligkeitsbereich (`dueDateFrom`/`dueDateTo`, vergleicht das rohe Fälligkeitsdatum) und Überfälligkeit; unterschiedliche Filterarten werden mit UND verknüpft, mehrere Werte derselben Art (z. B. zwei Status) mit ODER. Aktive Filter sind über die Bedienelemente selbst sichtbar und über „Alle Filter zurücksetzen“ per Tastatur zurücksetzbar; die Seitengröße bleibt durch `Paging` begrenzt. M2-Teil umgesetzt und getestet (`TaskAssignmentApiIT.bearbeiterFilterZeigtNurZugewieseneAufgaben`): Bearbeiter-Filter „Nur mir zugewiesene Aufgaben“ ergänzt die bestehenden Filter nach demselben UND/ODER-Muster.

Als Benutzer möchte ich relevante Aufgaben eingrenzen.

- Filter für Status, Priorität, Projekt, Tag, Fälligkeit und Überfälligkeit; Bearbeiter kommt in M2 hinzu.
- Unterschiedliche Filter werden mit UND verknüpft; mehrere Werte desselben Filters mit ODER.
- Aktive Filter sind sichtbar und per Tastatur zurücksetzbar; Seitengröße ist begrenzt.

### US-052 – Aufgaben sortieren · M1 · ERLEDIGT

> **Stand:** Umgesetzt und getestet (`TaskListQueryApiIT`, `TaskPages.test.tsx`, `tasklist-query.spec.ts`): Sortierfelder `createdAt`, `updatedAt`, `due`, `priority`, `title`, je auf- oder absteigend; Priorität sortiert nach der fachlichen Rangfolge (nicht alphabetisch), Titel ohne Beachtung der Großschreibung. Gleichstände lösen sich stabil über die ID auf; bei `sortBy=due` stehen Aufgaben ohne Fälligkeit unabhängig von der Richtung immer zuletzt. Nicht erlaubte Werte für `sortBy`/`sortDirection` liefern `400` statt eines stillschweigenden Rückfalls auf den Standard; die Auswahl ist ein einzelnes, beschriftetes `<select>`.

Als Benutzer möchte ich eine nachvollziehbare Reihenfolge wählen.

- Sortierfelder: erstellt, geändert, Fälligkeit, Priorität, Titel; auf- und absteigend.
- Gleichstände werden stabil nach ID aufgelöst; Aufgaben ohne Fälligkeit stehen bei Fälligkeitssortierung immer zuletzt.
- Nicht erlaubte Sortierfelder werden abgewiesen; Auswahl ist zugänglich beschriftet.

### US-053 – Standardansichten nutzen · M1 · ERLEDIGT

> **Stand:** Umgesetzt und getestet (`TaskPages.test.tsx`, `tasklist-query.spec.ts`): die fünf Ansichten sind Kurzverweise, die feste Kombinationen der US-051-Filter setzen (Heute/Demnächst/Erledigt zusätzlich mit Status-Einschränkung, Überfällig über das `overdue`-Flag, Alle ohne Einschränkung); „heute“ löst der Client in der Kontozeitzone des Betrachters auf und sendet sie explizit (Modulgrenze, siehe [Fachregeln](docs/DOMAIN_MODEL.md)). Die aktive Ansicht ist über `aria-current="page"` erkennbar. Jede Ansicht nutzt denselben Leerzustand wie die gefilterte Liste allgemein.

Als Benutzer möchte ich häufige Ansichten direkt öffnen.

- Ansichten: Alle, Heute, Demnächst, Überfällig, Erledigt.
- Heute/Demnächst/Überfällig schließen erledigte Aufgaben aus; Alle enthält alle zugänglichen Aufgaben.
- Tagesgrenzen und Zeitraum „Demnächst“ folgen dem Domänenmodell; jede Ansicht bietet einen sinnvollen Leerzustand.

## 11. EPIC-07 – Zusammenarbeit

### US-060 – Projekt teilen · M2 · ERLEDIGT

> **Stand:** Umgesetzt und getestet (`ProjectMembershipApiIT`, `ProjectPages.test.tsx`, `membership.spec.ts`): der Eigentümer nimmt über die genaue Login-E-Mail ein aktives Konto auf (`POST /api/v1/projects/{id}/members`); unbekannte oder deaktivierte E-Mails werden am Feld gemeldet, wiederholte Aufnahme desselben Kontos dupliziert nichts, der Eigentümer selbst kann nicht als Mitglied aufgenommen werden. Das Mitglied erhält direkten Zugriff ohne separate Einladungsannahme (Arbeitsannahme, siehe [DOMAIN_MODEL.md](docs/DOMAIN_MODEL.md)); die Oberfläche benennt diese Wirkung. Nur der Eigentümer sieht die Verwaltung; Mitglieder sehen die Mitgliederliste, aber keine Aufnehmen-/Entfernen-Bedienelemente.

Als Eigentümer möchte ich registrierte Benutzer zur Zusammenarbeit aufnehmen.

- Nur Eigentümer fügen über die genaue Login-E-Mail einen aktiven Benutzer hinzu; keine frei durchsuchbare öffentliche Benutzerliste.
- Mitgliedschaft ist eindeutig und dauerhaft; wiederholte Aufnahme dupliziert nichts.
- Mitglied erhält Zugriff auf Projektaufgaben. Arbeitsannahme: direkte Aufnahme ohne separate Einladungsannahme; UI benennt diese Wirkung.

### US-061 – Mitglied entfernen · M2 · ERLEDIGT

> **Stand:** Umgesetzt und getestet (`ProjectMembershipApiIT`, `ProjectPages.test.tsx`, `membership.spec.ts`): Entfernen (`DELETE /api/v1/projects/{id}/members/{userId}`) ist dem Eigentümer vorbehalten, hinter einem zugänglichen Bestätigungsdialog (WAI-ARIA-Dialogmuster, Fokus-Rückkehr, Escape). Der entfernte Benutzer verliert sofort den Zugriff (`404` statt `200`/`403` bei weiteren Aufrufen); seine Zuweisungen an Aufgaben dieses Projekts werden transaktional entfernt, die Aufgaben selbst bleiben erhalten. Der Eigentümer kann nicht über diesen Weg entfernt werden (`404`, da er kein Mitgliedschaftsdatensatz ist). Erinnerungen sind noch nicht betroffen (erst M3/US-100).

Als Eigentümer möchte ich eine Freigabe beenden.

- Entfernter Benutzer verliert sofort API-Zugriff; seine Zuweisungen und Erinnerungen werden bereinigt.
- Historische Kommentare und Aktivitäten bleiben erhalten.
- Eigentümer kann nicht über Mitgliederentfernung entfernt werden; parallele Zuweisung darf keine ungültige Referenz hinterlassen.

### US-062 – Aufgabe zuweisen · M2 · ERLEDIGT

> **Stand:** Umgesetzt und getestet (`TaskAssignmentApiIT`, `ActivityApiIT`, `TaskPages.test.tsx`, `membership.spec.ts`): eine Aufgabe mit Projektbezug ist nur an den Projekteigentümer oder ein aktuelles Mitglied zuweisbar, eine persönliche Aufgabe (ohne Projekt) nur an den Ersteller selbst; ungültige Zuweisungen werden am Feld abgewiesen. Die Zuweisung ist über das Bearbeitungsformular entfernbar (leere Auswahl) und teilt sich die vorhandene Versionsprüfung von US-032/US-120 (kein separater Konfliktpfad). Die Aufgabenliste bietet einen Bearbeiter-Filter „Nur mir zugewiesene Aufgaben“ (US-051-Ergänzung). Seit US-081 erzeugt eine Zuweisungsänderung einen `ASSIGNEE_CHANGED`-Aktivitätseintrag mit dem Bearbeiternamen zum Zeitpunkt der Änderung — das zuvor fehlende Akzeptanzkriterium „Änderung wird protokolliert“ ist damit erfüllt.

Als Projektmitglied möchte ich die Zuständigkeit sichtbar machen.

- Nur aktive Projektmitglieder oder Eigentümer sind zuweisbar; Zuweisung kann entfernt werden.
- Änderung wird protokolliert und versionsgeprüft.
- Ein nicht berechtigter Benutzer kann weder fremde Aufgaben zuweisen noch durch Zuweisung Zugang erhalten.

## 12. EPIC-08 – Unteraufgaben

### US-070 – Unteraufgaben verwalten · M2 · ERLEDIGT

> **Stand:** Umgesetzt und getestet (`SubtaskApiIT`, `TaskPages.test.tsx`, `subtasks.spec.ts`): Unteraufgaben gehören zu `task` (V7, `subtask.task_id` FK `ON DELETE CASCADE`); Anlegen, Umbenennen und Löschen sind über `/api/v1/tasks/{id}/subtasks[...]` möglich. Leere oder zu lange Titel (1–200 Zeichen nach Trim, dieselbe Regel wie Aufgabentitel, `TaskInput`) werden am Feld gemeldet; jede Operation prüft die Rechte über die Hauptaufgabe (`TaskService.loadAccessible`) — ein fremder Benutzer erhält für jede Unteraufgaben-Operation `404`, genau wie für die Aufgabe selbst. Reihenfolge (`sortOrder`) ist über „Nach oben“/„Nach unten“-Schaltflächen ohne Ziehbewegung bedienbar, an den Rändern wirkungslos; keine verschachtelten Unteraufgaben (genau eine Ebene).

Als Benutzer möchte ich eine Aufgabe in kleine Schritte zerlegen.

- Mehrere Unteraufgaben mit Titel, Erledigt-Zustand und stabiler Reihenfolge können angelegt, umbenannt und gelöscht werden.
- Leere Titel scheitern; Rechte werden über die Hauptaufgabe geprüft.
- Reihenfolge ist auch ohne Ziehbewegung bedienbar; keine verschachtelten Unteraufgaben.

### US-071 – Fortschritt anzeigen · M2 · ERLEDIGT

> **Stand:** Umgesetzt und getestet (`SubtaskApiIT`, `TaskPages.test.tsx`, `subtasks.spec.ts`): der Erledigt-Zustand ist über eine Checkbox rückgängig änderbar und bleibt nach Neuladen erhalten (echte Persistenz, kein reiner Client-Zustand). Der Fortschritt erscheint als Text „X von Y erledigt“ sowohl auf der Detailseite als auch in der Aufgabenliste (`TaskResponse.subtasks`, serverseitig berechnet, ändert nicht die Version der Aufgabe — Arbeitsannahme analog zu Tags, siehe [DOMAIN_MODEL.md](docs/DOMAIN_MODEL.md)). Alle Unteraufgaben erledigt setzt die Hauptaufgabe nachweislich nicht automatisch auf `DONE` (eigener Testfall).

Als Benutzer möchte ich einzelne Schritte abhaken und den Fortschritt sehen.

- Erledigt-Zustand bleibt nach Neuladen erhalten und ist rückgängig änderbar.
- Fortschritt erscheint als verständlicher Text, etwa „3 von 5 erledigt“.
- Alle Unteraufgaben erledigt setzt die Hauptaufgabe nicht automatisch auf `DONE`.

## 13. EPIC-09 – Kommentare und Aktivitätshistorie

### US-080 – Kommentieren · M2 · ERLEDIGT

> **Stand:** Umgesetzt und getestet (`CommentApiIT`, `TaskPages.test.tsx`, `history.spec.ts`): Kommentare zeigen Autor und Zeitpunkt; leerer/reiner Leerzeichen-Text und Text über 5.000 Zeichen werden am Feld abgewiesen. Rechte prüft ausschließlich die Hauptaufgabe (`TaskService.loadAccessible`), keine eigene Berechtigungslogik; ein fremder Benutzer erhält `404`, wie bei der Aufgabe selbst. Inhalt wird unverändert als Text gespeichert und über React nie als HTML gerendert (kein `dangerouslySetInnerHTML`), auch mit `<script>`-artigem Inhalt geprüft. Kommentar und zugehörige `COMMENTED`-Aktivität entstehen in derselben Transaktion (`CommentService.create`). Bearbeiten/Löschen von Kommentaren ist bewusst nicht umgesetzt (nicht gefordert).

Als berechtigter Benutzer möchte ich Informationen direkt an der Aufgabe ergänzen.

- Kommentare zeigen Autor und Zeitpunkt; leerer oder zu langer Text wird abgewiesen.
- Nur berechtigte Benutzer lesen oder ergänzen Kommentare; HTML wird als Text dargestellt.
- Kommentar und zugehörige Aktivität entstehen atomar. Bearbeiten/Löschen von Kommentaren ist zunächst nicht erforderlich.

### US-081 – Änderungen nachvollziehen · M2 · ERLEDIGT

> **Stand:** Umgesetzt und getestet (`ActivityApiIT`, `TaskPages.test.tsx`, `history.spec.ts`): die Historie zeigt Erstellen, Titel-, Status-, Prioritäts-, Zuweisungs-, Fälligkeits- und Projektänderung (jeweils als eigener Eintrag mit alter/neuer Angabe, `Task.update` liefert dazu die tatsächlich geänderten Felder statt nur eines Bool'schen Flags), Erledigen und Wiederöffnen (eigene Arten statt einer allgemeinen Statusänderung) sowie Kommentare, gemeinsam chronologisch sortiert (Frontend führt Aktivitäten und Kommentare zu einer Zeitleiste zusammen). Bearbeiter- und Projektname werden zum Zeitpunkt der Änderung aufgelöst und als Text gespeichert, damit die Historie auch nach einer späteren Umbenennung/Entfernung des referenzierten Datensatzes lesbar bleibt. Zugriff entspricht der Aufgabe (`ActivityQueryService` prüft über `TaskService.loadAccessible`); Einträge sind über die API nicht änderbar. Änderungen an der Beschreibung erzeugen bewusst keinen Eintrag (nicht im Akzeptanzkriterium genannt).

Als Benutzer möchte ich wichtige Änderungen einer Aufgabe verstehen.

- Historie zeigt Erstellen, Titel-, Status-, Prioritäts-, Zuweisungs-, Fälligkeits- und Projektänderung sowie Kommentare, Erledigen und Wiederöffnen.
- Jeder Eintrag hat Zeitpunkt, handelnden Benutzer und verständliche Aktion; chronologische Reihenfolge ist stabil.
- Zugriff entspricht der Aufgabe; Einträge sind nicht durch normale Benutzer änderbar und enthalten keine Geheimnisse.

## 14. EPIC-10 – Wiederkehrende Aufgaben

### US-090 – Wiederholung einstellen · M3 · OFFEN

Als Benutzer möchte ich tägliche, wöchentliche oder monatliche Aufgaben wiederholen.

- Wiederholung ist optional und verlangt Fälligkeit; Rhythmus, Anker und Zeitzone sind verständlich sichtbar und für künftige Vorkommen deaktivierbar.
- Abschließen erzeugt genau einen offenen Nachfolger nach den Zeit- und Kopierregeln des Domänenmodells.
- Tests prüfen paralleles/doppeltes Abschließen, Wiederöffnen, verpasste Termine, Monatsende und Zeitumstellung; keine Duplikate.

## 15. EPIC-11 – Erinnerungen und Benachrichtigungen

### US-100 – Persönliche Erinnerung erhalten · M3 · OFFEN

Als Benutzer möchte ich an zugängliche, offene Aufgaben erinnert werden.

- Erinnerung zur Fälligkeit, 10 Minuten, eine Stunde oder einen Tag davor; berechneter Zeitpunkt wird vor Speichern angezeigt.
- In-App-Mitteilung wird dauerhaft gespeichert und kann als gelesen markiert werden; keine Browser-Push- oder E-Mail-Erinnerung erforderlich.
- Neustart erzeugt keine Duplikate; abgeschlossene/gelöschte Aufgaben und entzogene Rechte stoppen ausstehende Erinnerungen.
- Fälligkeitsänderung, Datum ohne Uhrzeit und überfällige Jobs folgen den dokumentierten Regeln und sind getestet.

## 16. EPIC-12 – Dashboard

### US-110 – Persönliche Übersicht ansehen · M1 · ERLEDIGT

> **Stand:** Umgesetzt und getestet (`TaskSummaryApiIT`, `DashboardPage.test.tsx`, `dashboard.spec.ts`): eigene Seite „Übersicht“ (Menüpunkt vor „Meine Aufgaben“; die Startseite bleibt die Aufgabenliste) zeigt fünf Zähler — nicht erledigt, heute fällig, überfällig, erledigt, offen mit hoher/dringender Priorität —, die serverseitig mit denselben Spezifikationen wie die Listen berechnet werden (`TaskSpecifications`, `TaskService.summary`). Jeder Zähler verlinkt auf dieselben Filterparameter, mit denen er berechnet wurde; nach einer Änderung stimmen Übersicht und Liste überein, weil beide dieselbe Quelle abfragen. `today` ist ein Pflichtparameter, den der Client in seiner Kontozeitzone auflöst (Modulgrenze).

Als Benutzer möchte ich offene Arbeit schnell überblicken.

- Übersicht zeigt nicht erledigte, heute fällige, überfällige, erledigte sowie offene hohe/dringende Aufgaben.
- Alle Zahlen und Detailverweise verwenden dieselben Rechte- und Zeitregeln wie die Listen.
- Jeder Zähler führt zu seiner passenden Liste; nach Änderung stimmen Übersicht und Liste überein.

## 17. EPIC-13 – Gleichzeitige Änderungen und Datenintegrität

### US-120 – Konflikte erkennen · M1 · ERLEDIGT

> **Stand:** Umgesetzt und getestet, für Bearbeiten und Löschen (`TaskApiIT`: sequenzieller und paralleler Konflikttest — bei gleichzeitigen Schreibversuchen genau ein Erfolg, alle anderen `409`; DB-seitiger Versionsschutz zusätzlich in `TaskPersistenceIT` nachgewiesen). Die Oberfläche zeigt die abweichenden Felder und bietet „aktuelle Fassung übernehmen“ oder „eigene Eingabe behalten“ statt automatischem Überschreiben (`tasks.spec.ts`). Projektänderungen sind erst ab M1c relevant.

Als Benutzer möchte ich Änderungen anderer Personen nicht unbemerkt überschreiben.

- Zwei Clients lesen Version 5; nach erfolgreichem Speichern von Client A wird der Schreibversuch von B mit Version 5 als `409` abgewiesen.
- Frontend erklärt den Konflikt, erhält den Entwurf und bietet aktuelles Neuladen an; kein automatisches stilles Überschreiben.
- Gleiche Prüfung gilt für Löschen und betroffene Projektänderungen; Paralleltest prüft den tatsächlichen Datenbankzustand.

### US-121 – Zusammengehörige Änderungen gemeinsam speichern · M1–M3 · IN ARBEIT

> **Stand:** M1-Teil (Aufgaben) umgesetzt und getestet: `TaskPersistenceIT` löst gezielt einen Fehler in derselben Transaktion aus und beweist den Rollback einer bereits geflushten Änderung. Seit US-080/081 (M2) gibt es die Aktivitätstabelle; Aufgabe und Aktivität entstehen nachweislich gemeinsam (`ActivityApiIT`, `CommentApiIT.kommentarUndAktivitaetEntstehenAtomar`), allerdings bisher nur als Erfolgsfall geprüft, nicht durch einen gezielt ausgelösten Fehler wie bei `TaskPersistenceIT`. Projektlöschung/Mitgliederentfernung (M2) mit gezieltem Fehler und Wiederholungsnachfolger (M3) bleiben offen.

Als Benutzer möchte ich keine halbfertigen Datenänderungen erhalten.

- Aufgabe und Aktivität werden gemeinsam gespeichert oder gemeinsam zurückgerollt.
- Projektlöschung, Mitgliederentfernung und ab M3 Wiederholungsnachfolger bleiben auch bei Fehlern konsistent.
- Ein gezielt ausgelöster Fehler beweist den Rollback; Unique Constraints verhindern doppelte Mitgliedschaften und Nachfolger.

## 18. EPIC-14 – Logging und Monitoring

### US-130 – Verständliche Logs erhalten · M0 · IN ARBEIT

> **Stand:** Umgesetzt: Anfragekennung, einheitliches Fehlerformat, Test dass Passwörter/CSRF-Token nicht im Log stehen (`AuthApiIT`); gleiche Prüfung jetzt auch für Aufgabentitel/-beschreibung (`TaskApiIT`).

Als Betreiber möchte ich Fehler eingrenzen können.

- Logs enthalten Zeit, Schweregrad, Komponente, Meldung und bei HTTP-Anfragen eine Anfragekennung.
- Ein Fehler lässt sich einer Anfrage zuordnen, ohne Passwort, Cookie, Reset-Token oder Aufgabenbeschreibung zu protokollieren.
- Erwartete Benutzerfehler und unerwartete Systemfehler sind unterscheidbar; keine ungefilterten Request-Bodies.

### US-131 – Betriebsinformationen begrenzen · M0 · IN ARBEIT

> **Stand:** Umgesetzt und getestet: nur `/actuator/health` (nginx) bzw. Health-Gruppen (Backend) öffentlich, `env`/`configprops`/`heapdump`/`loggers`/`beans` liefern 401 bzw. 404, Compose wartet auf Datenbankbereitschaft. Offen: expliziter Test, dass eine fehlgeschlagene Migration die Bereitschaft verhindert; geschützte Detail-Ansicht für Administratoren (ab M2).

Als Betreiber möchte ich Zustände sehen, ohne interne Daten offenzulegen.

- Minimaler öffentlicher Healthcheck und geschützte Details sind getrennt.
- Compose wartet auf Datenbankbereitschaft; fehlgeschlagene Migrationen verhindern Einsatzbereitschaft.
- Nicht erforderliche Actuator-Endpunkte sind deaktiviert oder nicht exponiert; öffentlicher Zugriff wird getestet.

## 19. EPIC-15 – Sicherheit

### US-140 – Sicherheitsregeln nachweisen · ab M0, Abschluss M4 · OFFEN

Als Benutzer möchte ich auf sichere Verarbeitung meiner Daten vertrauen.

- [SECURITY.md](docs/SECURITY.md) ist umgesetzt: Session, CSRF, Passwörter, Objektberechtigungen, Validierung und sichere Konfiguration.
- Negative API-Tests decken Rechteumgehung, Rollenerhöhung, Tokenmissbrauch, schädliche Texte und Session-Entwertung ab.
- Keine echten Secrets im Repository oder Browser-Build; Abhängigkeiten auf bekannte Probleme prüfen und relevante offene Befunde dokumentieren/beheben.

### US-141 – Missbrauch begrenzen und Header setzen · M1 · Must · IN ARBEIT

> **Stand:** Umgesetzt: Ratenbegrenzung für Login und Registrierung (`429` mit `Retry-After`, keine Dauersperre, Test mit fester Uhr), Sicherheitsheader und CSP im nginx (im E2E gegen den echten Build geprüft), `no-store` für die Token-Antwort. Offen: Ratenbegrenzung für Passwort-Wiederherstellung (M2).

Als Betreiber möchte ich Angriffsflächen der Anmeldung und des Browsers reduzieren.

- Anmeldung (und ab M2 Passwort-Wiederherstellung) haben konfigurierbare Ratenbegrenzung ohne dauerhafte Kontosperre; Grenzwerte sind getestet.
- Sicherheitsheader einschließlich Content Security Policy sind gegen den tatsächlichen Build getestet; private API-Antworten sind nicht öffentlich cachebar.

## 20. EPIC-16 – Tests und Ausbildungsnachweise

### US-150 – Automatisierte Qualitätsprüfungen · ab M0, Abschluss M4 · OFFEN

Als Entwickler möchte ich Regressionen vor einem Release erkennen.

- Unit-, PostgreSQL-Integrations-, API-, Frontend-, E2E- und axe-Prüfungen laufen mit dokumentierten Befehlen.
- Die Build-Pipeline führt die vereinbarten Prüfungen aus und scheitert bei Fehlern; übersprungene Tests werden nicht als bestanden gewertet.
- Berichte sind auswertbar, Daten synthetisch und Tests unabhängig; Migration von leerem Schema und vorherigem Release wird geprüft.

### US-151 – Demo-Unterlagen für die Vorführung · M4 · Could · OFFEN

Als Entwickler möchte ich die Referenzlösung Azubis verständlich vorführen und mit deren Lösungen vergleichen können.

- Kurze Vorführabfolge: Weg vom Speichern im Browser bis zur Datenbank anhand des Architekturdiagramms, Technologien, Migrationen, Sessions, Berechtigungen, Testarten, Konflikte und Healthchecks.
- Vergleichspunkte (Architekturentscheidungen, Rechte-/Konfliktbehandlung, Tests, Barrierefreiheit) als Checkliste für den Vergleich mit den Azubi-Lösungen.
- Es gibt keine Abnahme durch Dritte.

## 21. EPIC-17 – Installation, Deployment und Updates

### US-160 – Frischinstallation prüfen · Probe M1c, Abschluss M4 · Should · IN ARBEIT

> **Stand:** M1-Probe durchgeführt und protokolliert ([Installationsprotokoll](docs/reviews/installation-M1.md)): Bauen, Starten, Healthcheck, Kontoanlage, Aufgabe anlegen, Persistenz nach Neustart und Rechtetrennung zwischen zwei Konten liefen wie in [DEPLOYMENT.md](docs/DEPLOYMENT.md) beschrieben; dabei wurde ein falscher Healthcheck-Pfad in der Anleitung gefunden und korrigiert. Nicht auf einem physisch zweiten Rechner geprüft, siehe Abweichungen im Protokoll. Offen bis M4: Update, Sicherung und Wiederherstellung (Abschnitte 6 bis 8 der Anleitung).

Als Betreiber möchte ich mit Repository und Anleitung auf einem frischen System starten können.

- Die Anleitung wird auf einem frisch geklonten Checkout (ohne vorhandene Volumes und `.env`) wörtlich ausgeführt, ohne undokumentierte Zusatzschritte.
- Secrets konfigurieren, Dienste starten, Healthcheck prüfen, Registrierung, Login, Aufgabe, Neustart und erneutes Lesen durchführen.
- Versionen, Dauer, Abweichungen und nötige Korrekturen werden protokolliert; keine erfundenen Einträge.

### US-161 – Anwendung aktualisieren · M4 · OFFEN

Als Betreiber möchte ich einen neuen Release kontrolliert einspielen.

- Dokumentierter Ablauf verwendet eine konkrete Version, Vorabprüfung und Sicherung; Migrationen erhalten bestehende Daten.
- Update wird von einem vorigen Release aus getestet, einschließlich Anmeldung und vorhandener Aufgaben.
- Rückkehrplan unterscheidet Imagewechsel von Datenbank-Restore; inkompatible alte Software wird nicht gegen ein neues Schema gestartet.

### US-162 – Sichern und wiederherstellen · M4 · OFFEN

Als Betreiber möchte ich Daten nach einem Ausfall wiederherstellen können.

- Dokumentierte Sicherung umfasst Datenbank und separat geschützte Konfiguration; Sicherungsfehler sind erkennbar.
- Restore erfolgt zuerst in einer leeren, getrennten Umgebung mit passender Anwendungsversion.
- Bekannte Benutzer/Projekte/Aufgaben, Beziehungen und Mengen stimmen; Ergebnis und Sicherungsstand werden protokolliert.

## 22. EPIC-18 – Barrierefreiheit

### US-170 – Oberfläche ohne Maus bedienen · ab M0, Abschluss M4 · OFFEN

Als Benutzer mit Tastatur oder assistiver Technik möchte ich alle Funktionen nutzen.

- Semantische Elemente, Namen, Labels, Sprache und Überschriften sind korrekt; Fokus bleibt sichtbar und sinnvoll geführt.
- Anmeldung, Aufgaben, Filter, Dialoge, Konflikte und ab M2/M3 weitere Features sind vollständig mit Tastatur bedienbar.
- Kontraste, Zoom/Reflow, Zielgrößen und Authentifizierung erfüllen die jeweils anwendbaren A-/AA-Kriterien aus [ACCESSIBILITY.md](docs/ACCESSIBILITY.md).

### US-171 – Barrieren automatisiert erkennen · ab M0, Abschluss M4 · OFFEN

Als Entwickler möchte ich erkennbare Barrieren bei jeder Änderung finden.

- Playwright mit axe-core prüft Seiten und geöffnete Dialoge einschließlich Fehlerzuständen mit WCAG-2.2-AA-relevanten Regeln.
- Keine ungeklärten Regelverstöße im geprüften Umfang; insbesondere neue schwerwiegende Verstöße lassen die Pipeline scheitern.
- Keine pauschalen Regelausschlüsse; ein Scan ohne Befund wird nicht als vollständige WCAG-Konformität ausgegeben.

### US-172 – Manuelle Accessibility-Prüfung · ab M1, Abschluss M4 · Should · OFFEN

Als Entwickler möchte ich Bedienbarkeit prüfen, die ein automatischer Scan nicht beurteilt.

- Tastaturablauf, mindestens eine dokumentierte Screenreader-/Browser-Kombination (zum Beispiel VoiceOver/Safari) sowie Zoom und Reflow werden je größerem Meilenstein geprüft.
- Protokoll nennt Build, Umgebung, Schritte, Ergebnis und offene Befunde.
- Offene anwendbare A-/AA-Verstöße werden behoben oder als bekannte Einschränkung dokumentiert; eine Konformität wird nicht behauptet.

## 23. EPIC-19 – Visuelle Gestaltung für die Vorführung

### US-180 – Oberfläche einheitlich und übersichtlich gestalten · nach M2, vor der Vorführung · Should · IN ARBEIT

> **Stand (30.09.2026, Schritte 1–4 implementiert):** Das gemeinsame helle/dunkle Gestaltungssystem, die gegliederte Aufgabenliste und die Übertragung auf Übersicht, Projekte, Tags, Details, Formulare, Anmeldung, Profil und Administration sind umgesetzt. Mobil sind Filter über einen beschrifteten Schalter erreichbar; die Administrationstabelle besitzt einen per Tastatur scrollbaren Bereich. Lint, Typprüfung, Build, 114 Frontend-Unit-Tests und 58 Playwright-/axe-Tests sind erfolgreich; die Sichtprüfung bei Desktopbreite, 320 CSS-Pixeln sowie eine rechnerische Prüfung zentraler Kontraste sind in [ui-US180.md](docs/reviews/ui-US180.md) protokolliert. Die manuelle Screenreader-Prüfung durch den Auftraggeber (US-172) und ein echter Browser-Zoom-Test stehen noch aus. Abhängigkeiten: US-050–053, US-110 und US-170–172. Fachfunktionen und API-Verträge sind unverändert.

Als Vorführender möchte ich eine moderne, frische und klar gegliederte Oberfläche zeigen, damit Ausbilder und Azubis die Funktionen schnell erfassen und die Anwendung gern bedienen.

**Umsetzungsplan in überprüfbaren Schritten:**

1. Ein gemeinsames Gestaltungssystem für Farben, Schriftgrößen, Abstände, Flächen, Navigation, Eingaben, Schaltflächen und Fokuszustände festlegen. Helles und dunkles Farbschema vollständig gestalten; Farben und notwendige Elementgrenzen nach [ACCESSIBILITY.md](docs/ACCESSIBILITY.md) messen.
2. Die Aufgabenliste als Pilot umgestalten: breiterer Seitenrahmen, erkennbare Hauptaktion und aktive Standardansicht, Suche und Sortierung nahe an den Ergebnissen, Filter am Desktop neben der Liste und auf schmalen Bildschirmen vollständig erreichbar. Aufgaben nach Titel, Status, Priorität, Fälligkeit und Aktion klar gliedern. Aktive Filter, Leer-, Lade-, Fehler- und Konfliktzustände erhalten dieselbe Sorgfalt.
3. Übersicht, Projekte, Tags, Details, Formulare, Anmeldung, Profil, Administration und Dialoge mit denselben Regeln gestalten. Wiederkehrende Muster zentral pflegen; keine neue UI-Bibliothek ohne die in `AGENTS.md` geforderte Prüfung.
4. Nach jedem größeren Schritt den tatsächlichen Stand und offene Punkte hier dokumentieren. Desktop-, Mobil- sowie Hell-/Dunkel-Ansichten visuell prüfen und die betroffenen Frontend-Tests ausführen. Vor Abschluss zusätzlich Tastatur, sichtbaren und nicht verdeckten Fokus, Kontrast, 200-%-Zoom, 320-CSS-Pixel-Reflow, reduzierte Bewegung und die vorhandene Playwright-/axe-Suite prüfen. Die manuelle Screenreader-Prüfung aus US-172 bleibt ein eigener Nachweis des Auftraggebers.

**Akzeptanzkriterien:**

- Die Aufgabenliste nutzt den Platz auf großen Bildschirmen sinnvoll; Filter verdecken die Ergebnisse nicht und sind auf kleinen Bildschirmen ohne Maus nutzbar.
- Navigation, Status, Priorität, aktive Filter und Aktionen sind visuell klar, auch ohne Farbe unterscheidbar und im gesamten bestehenden Frontend einheitlich.
- Bei 320 CSS-Pixeln und 200 % Zoom bleiben Inhalte und Funktionen ohne unerwartetes horizontales Scrollen verfügbar; Fokus, Zielgrößen und Kontraste erfüllen die anwendbaren Regeln aus `ACCESSIBILITY.md`.
- Bestehende Fachabläufe, Filterparameter, Berechtigungen, Fehlermeldungen und Fokusführung funktionieren weiterhin. Automatische und tatsächlich durchgeführte manuelle Prüfungen sind mit Ergebnis dokumentiert; axe allein gilt nicht als Konformitätsnachweis.

### US-181 – Gleichnamige Tags eindeutig erkennbar machen · nach M2 · Should · IN ARBEIT

> **Stand (30.09.2026, umgesetzt, Nachweis teilweise offen):** Variante A ist implementiert. Backend: `GET/POST/PUT /api/v1/tags` liefern bei Projekt-Tags zusätzlich `projectName`, `projectOwnerId` und `projectOwnerDisplayName` (über die neue `ProjectAccess.summarize`, kein Zugriff des Tag-Moduls auf Projekt- oder Benutzer-Repositories); die Liste ist nach Name, dann persönlich vor Projekt, dann Projektname, Eigentümername und ID sortiert. Frontend: Tag-Auswahl an der Aufgabe und Tag-Filter der Aufgabenliste hängen die Herkunft nur bei mehrfach vorkommenden Namen an („Backen · Projekt „Kaffee“ (Stefan Moers)“, eigenes Projekt „(du)“, sonst „persönlich“); die Seite „Tags“ nennt bei Projekten anderer Eigentümer deren Namen in der Überschrift. Der zugängliche Name der Kontrollkästchen enthält die Herkunft. Tests: 4 neue Fälle in `TagApiIT` (Zwei-Benutzer-Test, Sortierung, Antwort bei Anlegen/Umbenennen, Zugriffsverlust), `origin.test.ts`, 4 neue Fälle in `TaskPages.test.tsx`/`TagsPage.test.tsx` und ein Playwright-/axe-Test in `tags.spec.ts`. **Tatsächlich ausgeführt:** Typprüfung, ESLint und alle 126 Frontend-Unit-Tests (in einem Container mit Node 22.22.2, nicht mit der festgelegten Version 24.21.0). **Nicht ausgeführt:** `TagApiIT` (Java 21 und Maven-Abhängigkeiten standen dort nicht zur Verfügung; die geänderten Quelldateien wurden nur auf Syntaxfehler geprüft), der neue Playwright-Test und ein Gesamtlauf der E2E-Suite. Bis diese Läufe grün sind und die manuelle Screenreader-Prüfung (US-172) erfolgt ist, bleibt die Story `IN ARBEIT`. Beim Vorführungstest mit zwei Konten fiel auf, dass die Tag-Auswahl einer Aufgabe gleichnamige Tags mehrfach und ohne Herkunftsangabe auflistet. Ausgangslage: Konto A und Konto B besitzen jeweils ein eigenes Projekt „Operation Kaffee & Kuchen“ mit einem Projekt-Tag „Backen“. Konto B ist zusätzlich Mitglied in Projekt A. In der Aufgabe von Projekt A sieht B dann zwei Tags „Backen“ und kann nicht erkennen, welches zu welchem Projekt gehört. Nur eines davon ist der Aufgabe zugeordnet. Die Fachregel „Namen sind je Geltungsbereich eindeutig“ (US-043) ist dabei nicht verletzt: die Tags liegen in verschiedenen Geltungsbereichen. Sichtbar wird das Problem erst, weil die Auswahl mehrere Geltungsbereiche zusammen anzeigt. Nachweis: API-Abfrage `GET /api/v1/tags` als Konto B, Screenshot der Tag-Auswahl.

Als Benutzer möchte ich in Tag-Listen erkennen, woher ein Tag stammt, damit ich bei gleichen Namen nicht versehentlich das falsche Tag zuordne oder filtere.

**Entscheidung des Auftraggebers (30.09.2026):** Variante A, die Herkunft wird gekennzeichnet. Jeder Eintrag zeigt Geltungsbereich und bei Projekt-Tags das Projekt samt Eigentümer, z. B. „Backen · Projekt Operation Kaffee & Kuchen (Stefan Moers)“ bzw. „Backen · persönlich“. Fachregeln und Daten bleiben unverändert. Verworfen wurde das Zusammenführen gleichnamiger Tags, weil es die Bedeutung der Projekt-Tags verändern würde.

**Weiterhin offen (Auftraggeber):** Dürfen zwei Projekte mit gleichem Namen für dieselbe Person sichtbar sein? Das ist bisher nicht geregelt und verstärkt das Problem, denn die Kennzeichnung „Projekt, Eigentümer“ unterscheidet gleichnamige Projekte verschiedener Eigentümer nur über den Eigentümernamen. Möglich sind eine Warnung beim Anlegen oder der Eigentümername in Listen.

**Akzeptanzkriterien:**

- Die Tag-Auswahl in Aufgabenformular und Aufgabenansicht, der Tag-Filter der Aufgabenliste und die Seite „Tags“ unterscheiden gleichnamige Tags erkennbar. Die Kennzeichnung steht im Text und ist damit auch ohne Farbe lesbar.
- Der zugängliche Name jedes Kontrollkästchens enthält die Herkunft, sodass Screenreader-Nutzer zwei gleichnamige Tags unterscheiden können (Bezug: [ACCESSIBILITY.md](docs/ACCESSIBILITY.md)). Zwei Kontrollkästchen mit identischem Namen gelten als Mangel.
- Die API liefert dafür die nötigen Angaben (Projektname und Eigentümer) typisiert und in OpenAPI beschrieben, ohne mehr fremde Daten preiszugeben, als der Betrachter bereits sehen darf.
- Die Reihenfolge ist stabil: nach Name, dann nach Geltungsbereich (persönlich vor Projekt), dann nach Projektname.
- Bestehendes Verhalten bleibt unverändert: Eindeutigkeit je Geltungsbereich, Rechte zum Anlegen und Ändern, Filterparameter, Zuordnung per `PUT /api/v1/tasks/{id}/tags`.

**Testfälle (Zwei-Benutzer-Test nach `docs/TESTING.md`):**

1. Benutzer A und B legen je ein Projekt gleichen Namens mit einem Tag „Backen“ an; B wird Mitglied in As Projekt. In As Aufgabe zeigt B die Tag-Auswahl: beide „Backen“ erscheinen unterscheidbar, das zugeordnete ist als gewählt erkennbar.
2. Zusätzlich existiert bei B ein persönliches Tag „Backen“. Drei Einträge mit drei verschiedenen Herkunftsangaben.
3. B ordnet As Projekt-Tag zu und filtert danach in der Aufgabenliste. Das Ergebnis enthält nur Aufgaben mit genau diesem Tag, nicht die mit Bs eigenem.
4. Playwright/axe: keine Verstöße in der Tag-Auswahl mit Dubletten; Screenreader-Name je Eintrag eindeutig (manuell in US-172 zu prüfen).
5. Regression: Ein Benutzer ohne gleichnamige Tags sieht unveränderte, kurze Einträge ohne überflüssigen Zusatz.

**Abhängigkeiten:** US-043, US-053, US-060–062, US-170–172, US-180.

### US-182 – Projektpräsentation in der Anwendung · Could · UMGESETZT, Nachweis teilweise offen

> **Stand (01.10.2026):** Die Projektvorstellung (18 Folien, Quellen in [docs/praesentation](docs/praesentation/README.md)) ist als öffentliche Seite unter `/präsentation` (Alias `/praesentation`, Einzelfolie `/präsentation/7`) in das Frontend eingebaut. Die Route liegt außerhalb von `AuthProvider` und `Layout`, ruft keine API auf und braucht keine Anmeldung; die Folien sind React-Komponenten mit eigenen CSS-Klassen (keine Inline-Stile, keine externen Schriften), damit die Content-Security-Policy des Webservers unverändert bleibt. **Tatsächlich ausgeführt:** Typprüfung und ESLint (auch auf dem Entwicklungsrechner), 137 Frontend-Unit-Tests einschließlich 10 neuer Fälle in `PresentationPage.test.tsx`, zwei neue Playwright-Tests in `e2e/presentation.spec.ts` gegen `vite preview` (Container mit Node 22, nicht 24.21.0) und ein axe-Lauf über alle 18 Folien ohne Verstoß. **Nicht ausgeführt:** Aufruf gegen den echten Compose-Stack mit nginx (`docker compose up -d --build`, dann <http://localhost:8080/präsentation>), Gesamtlauf der E2E-Suite und die manuelle Screenreader-Prüfung (US-172).

Als Vortragender möchte ich die Projektvorstellung direkt aus der laufenden Anwendung starten, damit ich sie vor der Live-Demo ohne zusätzliche Software zeigen kann.

**Akzeptanzkriterien:**

- `/präsentation` (auch als `/praesentation` und `/präsentation/<Nr>`) zeigt die Folie ohne Anmeldung; eine ungültige Nummer wird auf die erste bzw. letzte Folie begrenzt.
- Jede Folie hat Schaltflächen „Vorherige Folie“ und „Nächste Folie“ (Pfeile links/rechts); am Anfang bzw. Ende sind sie als nicht verfügbar gekennzeichnet, behalten aber den Fokus.
- Tastatur: Pfeil links/rechts, Bild auf/ab, Pos1/Ende, `N` (Sprechernotizen), `F` (Vollbild). Die Kürzel greifen nicht in Eingabefeldern.
- Die Folie skaliert im 16:9-Format mit dem Fenster; bei schmalen Fenstern stehen die Pfeile unter der Folie.
- Aktuelle Folie und Position werden Screenreadern angekündigt; Kontraste, Fokus und Tastaturbedienung entsprechen [ACCESSIBILITY.md](docs/ACCESSIBILITY.md).
- Bestehende Seiten und Routen bleiben unverändert erreichbar (Regression über die vorhandenen Tests).

**Abhängigkeiten:** US-001, US-170–172.

## 24. Definition of Done

Eine Story ist erst `ERLEDIGT`, wenn alle zutreffenden Punkte nachgewiesen sind:

- Akzeptanzkriterien erfüllt, inklusive Fehler-, Leer- und Berechtigungsfällen.
- Passende automatisierte Tests erfolgreich; keine bekannten still übersprungenen Pflichtprüfungen.
- Frontend per Tastatur bedienbar, zugängliche Beschriftung und Fokusführung; relevante axe-Scans und manuelle Prüfungen durchgeführt.
- Sicherheitsregeln, serverseitige Validierung, Konflikt- und Transaktionsregeln eingehalten.
- Schemaänderungen als Flyway-Migration, Schnittstellen in OpenAPI dokumentiert.
- README, Fachdateien, Konfiguration und Betriebsbefehle stimmen mit dem tatsächlichen Stand überein.
- Selbst-Review (gegebenenfalls mit `/code-review`) ohne offene blockierende Befunde; Nachweise mit Story-ID und konkretem Build dokumentiert.
- Keine Secrets, lokalen Daten oder erzeugten Build-Dateien im Commit.

Für das Referenzrelease kommen die geprüfte Frischinstallation, ein geprüfter Restore, die Updateprobe und die manuelle Accessibility-Prüfung hinzu. Ein erfolgreicher Build allein erfüllt diese Definition nicht.

## 25. Vor der jeweiligen Umsetzung zu konkretisieren

| Zeitpunkt | Offener Umsetzungsentscheid | Bestehende Vorgabe |
|---|---|---|
| M0 | Gepflegte kompatible Paketversionen, konkreter Webserver, OpenAPI-Integration, CI-Umgebung | Festgelegter Stack, Java 21, reproduzierbarer Build |
| M1 | Zugängliche UI-Komponenten und genaue Gestaltung | WCAG 2.2 AA; keine Bibliothek allein nach Aussehen wählen |
| M2 | ~~Bootstrap-Aufruf für Admin und lokaler Mailfänger~~ — geklärt: `ADMIN_BOOTSTRAP_EMAIL` (US-014); Passwort-Wiederherstellung ohne Mailfänger für dieses Demo-Projekt bewusst nicht umgesetzt (US-013, Auftraggeber-Entscheidung) | Keine Standardkennwörter, kein öffentliches Reset-Token-Logging |
| M3 | Beispiele für Zeit- und Wiederholungsregeln bestätigen | Markierte Arbeitsannahmen im Domänenmodell |
| Vor öffentlichem Betrieb | Host, DNS/TLS, echter Versand, Secret- und Backup-Aufbewahrung | Lokales Referenzdeployment bleibt Standardumfang |

Diese Punkte verhindern das Anlegen der Dokumentationsbasis und den Beginn von M0 nicht. Produktänderungen werden sichtbar abgestimmt; technische Detailentscheidungen werden nachvollziehbar dokumentiert.
