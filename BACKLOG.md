# UltimateTodo – Produkt- und Umsetzungsbacklog

**Stand: 30.09.2026. Alle Implementierungsstories sind offen.** Die Dokumentationsbasis ist erstellt; das bedeutet nicht, dass die beschriebenen Funktionen bereits existieren.

[Einstieg](README.md) · [Fachregeln](docs/DOMAIN_MODEL.md) · [Architektur](docs/ARCHITECTURE.md)

## 1. Ziel und Umfang

UltimateTodo unterstützt persönliche und gemeinsame Aufgaben. Die Referenzlösung soll typische professionelle Anforderungen zeigen: Konten, Berechtigungen, dauerhafte Daten, REST-API, barrierefreie Oberfläche, Konfliktbehandlung, Tests und nachvollziehbaren Betrieb. Azubis sollen Architektur und Technologieauswahl erklären können, auch wenn ein Agent wesentliche Teile des Codes geschrieben hat.

Nicht vorgesehen sind native Mobilanwendungen, Offline-Synchronisation, Unternehmens-SSO, Microsoft-365-/Google-Workspace-Integration, Abrechnung, KI-generierte Aufgaben, große Dateianhänge, Echtzeit-Chat und frei konfigurierbare Jira-Workflows. Drag & Drop ist optional; falls später umgesetzt, benötigt es eine gleichwertige Bedienung ohne Ziehen.

## 2. Rollen und Priorität

- **Gast:** Registrierung, Anmeldung und Passwortwiederherstellung; keine Fachdaten.
- **Benutzer:** eigenes Konto und persönliche Aufgaben; Zusammenarbeit in freigegebenen Projekten.
- **Projekt-Eigentümer:** zusätzlich Projekt und Mitgliedschaften verwalten.
- **Administrator:** Konten deaktivieren/reaktivieren und Systemzustand prüfen; kein pauschaler Zugriff auf private Inhalte.

M0 bis M4 beschreiben die Umsetzungsreihenfolge, keine unterschiedlichen Qualitätsniveaus. Sicherheit, Barrierefreiheit und Dokumentation beginnen in M0. Eine Story mit mehreren Kriterien wird nicht abgeschlossen, wenn nur ein Teil umgesetzt ist. Statuswerte: `OFFEN`, `IN ARBEIT`, `BLOCKIERT` mit konkretem Grund, `ABGENOMMEN` mit Nachweis.

## 3. Meilensteine

| Meilenstein | Ergebnis | Stories / Abhängigkeiten |
|---|---|---|
| M0 – Grundlage | Baubarer Stack, Compose, Migrationen, Healthchecks, erste Test- und UI-Grundlage | US-001, US-003, US-004, US-130, US-131; technische Basis für US-140, US-150, US-170–172 |
| M1 – Persönliche Aufgaben | Registrierung, Login, persönliche Aufgaben und Projekte, Suche, Filter, Dashboard, sichere Konfliktbehandlung | US-002, US-010–012, US-020–022, US-030–037, US-040–043, US-050–053, US-110, US-120–121; Installationsprobe US-160 |
| M2 – Zusammenarbeit | Mitglieder, Zuweisungen, Unteraufgaben, Kommentare und sichtbare Historie; Kontoverwaltung vervollständigen | US-013–014, US-060–062, US-070–071, US-080–081; M1 vorhanden |
| M3 – Zeitfunktionen | Wiederkehrende Aufgaben und persönliche In-App-Erinnerungen | US-090, US-100; stabile Zeit- und Transaktionsregeln aus M1/M2 |
| M4 – Referenzrelease | Vollständige Abnahme, Updates, Restore und Übergabe an anderen Azubi | US-140, US-150–151, US-160–162, US-170–172 abschließend nachweisen; M1–M3 erfüllt |

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

## 5. EPIC-01 – Anwendungsgrundlage und Dokumentation

### US-001 – Anwendung reproduzierbar starten · M0 · OFFEN

Als Betreiber möchte ich alle Dienste mit dokumentierten Schritten starten können.

- Webserver/Frontend, Backend und PostgreSQL starten über Docker Compose; externe Konfiguration erfordert keine Codeänderung.
- Ein frischer Checkout lässt sich mit den dokumentierten Versionen bauen; Wrapper, Lockdatei, Konfigurationsbeispiel und feste Container-Versionen sind vorhanden.
- Fehlende Pflichtkonfiguration und fehlgeschlagene Starts führen zu verständlichen, geheimnisfreien Fehlermeldungen.

### US-002 – Daten dauerhaft speichern · M1 · OFFEN

Als Benutzer möchte ich Konten, Projekte und Aufgaben nach einem Neustart wiederfinden.

- Alle drei Datenarten liegen in PostgreSQL; normaler Neustart und `compose down` ohne Volume-Löschung erhalten sie.
- Flyway initialisiert ein leeres Schema und aktualisiert einen bestehenden Stand versioniert.
- Ein Test liest zuvor angelegte Daten nach einem Anwendungsneustart wieder; keine automatische Löschung bei Start.

### US-003 – Betriebszustand prüfen · M0 · OFFEN

Als Betreiber möchte ich die Einsatzbereitschaft ohne UI feststellen.

- Actuator stellt einen minimalen Healthcheck bereit; Readiness meldet Datenbankausfälle als nicht bereit.
- Liveness wird nicht allein durch einen vorübergehenden Datenbankausfall negativ.
- Öffentlich sind keine internen Verbindungsdaten sichtbar; erfolgreiche und fehlerhafte Zustände sind getestet.

### US-004 – Dokumentation als Einstieg nutzen · M0 · OFFEN

Als Azubi möchte ich Anforderungen, Architektur und erste Befehle an einem Ort finden.

- README verlinkt alle Fachdateien; reale Pfade und Befehle stimmen nach Umsetzung mit dem Repository überein.
- Getestete Versionen und verbleibende Einschränkungen sind eingetragen; Zielabläufe sind von verifizierten Abläufen unterscheidbar.
- Dokumentation bleibt Deutsch und erklärt neue Begriffe; wesentliche Entscheidungen besitzen ADRs.

## 6. EPIC-02 – Benutzerverwaltung

### US-010 – Konto registrieren · M1 · OFFEN

Als Gast möchte ich ein Konto mit eindeutiger Login-E-Mail anlegen.

- Pflichtfelder, E-Mail und Passwortregeln werden serverseitig geprüft; doppelte E-Mails auch bei parallelen Anfragen abgewiesen.
- Nur ein sicherer Passwort-Hash wird gespeichert; Selbstregistrierung erzeugt ausschließlich die Rolle `USER`.
- Das neue aktive Konto kann sich anmelden; Fehler sind verständlich und zugänglich.

### US-011 – Eigenes Profil bearbeiten · M1 · OFFEN

Als Benutzer möchte ich mein Profil sehen und Anzeigename sowie Zeitzone bearbeiten.

- Login-E-Mail und Erstellungszeit sind sichtbar; die E-Mail bleibt in der ersten Version unveränderlich.
- Nur der angemeldete Benutzer kann sein Profil ändern; Rolle und Kontostatus sind dort nicht bearbeitbar.
- Änderungen werden validiert; ein Zeitzonenwechsel verändert keine gespeicherten Fälligkeiten rückwirkend.

### US-012 – Passwort ändern · M1 · OFFEN

Als Benutzer möchte ich mein Passwort ersetzen können.

- Das bisherige Passwort ist erforderlich; das neue erfüllt die dokumentierten Regeln.
- Danach funktioniert nur das neue Passwort; alte Sitzungen werden entwertet und eine neue Anmeldung verlangt.
- Falsches bisheriges Passwort verändert keine Daten; Passwörter erscheinen nie in Logs.

### US-013 – Zugang wiederherstellen · M2 · OFFEN

Als Benutzer möchte ich ein vergessenes Passwort zurücksetzen können.

- Die Anforderung verrät nicht, ob ein Konto existiert; Anfragen sind begrenzt.
- Reset-Tokens verfallen nach der festgelegten Frist, sind nur einmal verwendbar und werden nach Erfolg ungültig; alte Sitzungen enden.
- Lokaler Mailfänger oder echter Versandweg sind dokumentiert; keine Reset-Links in normalen Anwendungslogs.

### US-014 – Konten administrieren · M2 · OFFEN

Als Administrator möchte ich Konten auflisten, deaktivieren und reaktivieren.

- Ein deaktiviertes Konto erhält weder neue noch fortgesetzte Sitzungszugriffe; Daten bleiben bestehen.
- Normale Benutzer können diese Aktionen nicht ausführen; Administration gewährt keinen Zugriff auf Aufgabeninhalte.
- Sichere erstmalige Admin-Einrichtung ist dokumentiert; kein Standardkennwort; letztes aktives Admin-Konto bleibt erhalten.

## 7. EPIC-03 – Authentifizierung und Autorisierung

### US-020 – Anmelden · M1 · OFFEN

Als registrierter Benutzer möchte ich sicher auf meine Daten zugreifen.

- Gültige Zugangsdaten eines aktiven Kontos erzeugen eine Session; ungültige werden mit allgemeiner Meldung abgewiesen.
- Navigation und Neuladen innerhalb der gültigen Session erhalten den Anmeldestatus.
- Geschützte APIs liefern ohne Anmeldung `401`; Session-Cookie und CSRF-Ablauf entsprechen dem Sicherheitsmodell.

### US-021 – Abmelden · M1 · OFFEN

Als Benutzer möchte ich eine Sitzung zuverlässig beenden.

- Logout entwertet die Sitzung auf dem Server und entfernt das Cookie.
- Eine Wiederverwendung des bisherigen Cookies erlaubt keinen Zugriff; erneute Anmeldung ist erforderlich.
- Logout ist gegen CSRF geschützt; andere Benutzer oder private Daten erscheinen nach Logout nicht aus einem UI-Cache.

### US-022 – Zugriff auf eigene und freigegebene Daten begrenzen · M1/M2 · OFFEN

Als Benutzer möchte ich meine privaten Inhalte vor anderen schützen.

- Backend prüft jede Lese- und Schreiboperation einschließlich Suche, Zähler und abhängiger Daten.
- Geänderte URLs, fremde IDs und manipulierte Request-Felder umgehen keine Rechte.
- Tests mit zwei Benutzern und Admin decken persönliche Aufgaben ab M1 und entzogene Projektmitgliedschaft ab M2 ab.

## 8. EPIC-04 – Aufgabenverwaltung

### US-030 – Aufgabe anlegen · M1 · OFFEN

Als Benutzer möchte ich eine Aufgabe bereits mit einem Titel erstellen können.

- Standard ist `OPEN` und `NORMAL`; Projekt, Beschreibung, Fälligkeit und Tags sind optional; fremde Zuweisung folgt in M2.
- Backend setzt Ersteller und Zeitstempel; leere oder zu lange Titel und ungültige Fälligkeiten werden abgewiesen.
- Speicherung ist dauerhaft; Fehler lassen den Formulareintrag erhalten und werden dem passenden Feld zugeordnet.

### US-031 – Aufgabe ansehen · M1/M2 · OFFEN

Als berechtigter Benutzer möchte ich alle Informationen einer Aufgabe sehen.

- Ansicht zeigt Titel, Beschreibung, Status, Priorität, Fälligkeit, Ersteller, Projekt, Tags und Zeitstempel.
- Ab M2 gehören Bearbeiter, Unteraufgaben, Kommentare und Aktivitäten zur vollständigen Ansicht.
- Nicht vorhandene und unzugängliche Aufgaben führen zu einer verständlichen Nicht-gefunden-Ansicht ohne fremde Daten.

### US-032 – Aufgabe bearbeiten · M1/M2 · OFFEN

Als berechtigter Benutzer möchte ich Aufgabenangaben ändern.

- Felder aus US-030 sowie Status und ab M2 Bearbeiter sind änderbar; Ersteller und technische Zeitstempel nicht frei editierbar.
- Änderungszeit und Version steigen bei einer tatsächlichen Änderung; Fachregeln gelten auch bei direktem API-Aufruf.
- Veraltete Versionen erzeugen `409`; Entwurf bleibt erhalten. Projektwechsel beachtet die Rechte- und Bereinigungsregeln des Domänenmodells.

### US-033 – Aufgabe löschen · M1 · OFFEN

Als berechtigter Benutzer möchte ich überflüssige Aufgaben entfernen.

- Ein zugänglicher Bestätigungsdialog nennt die konkrete Aufgabe und die endgültige Wirkung.
- Löschen mit aktueller Version entfernt die Aufgabe und abhängige Daten in einer Transaktion; fremde oder veraltete Löschversuche scheitern.
- Die Aufgabe verschwindet aus Listen; anschließendes Lesen liefert `404`.

### US-034 – Aufgabe erledigen · M1 · OFFEN

Als Benutzer möchte ich erledigte Arbeit kennzeichnen.

- Status wechselt nach `DONE`, Abschlusszeit wird serverseitig gesetzt, Aktivität gespeichert.
- Erledigte Aufgabe erscheint in der entsprechenden Ansicht und zählt nicht als überfällig.
- Erneutes Abschließen verändert den Abschluss nicht und erzeugt ab M3 keinen doppelten Nachfolger.

### US-035 – Aufgabe wieder öffnen · M1 · OFFEN

Als Benutzer möchte ich eine erledigte Aufgabe erneut bearbeiten.

- Status wechselt von `DONE` nach `OPEN`; Abschlusszeit wird geleert.
- Änderung erhält Aktivität und neue Version.
- Ab M3 bleiben bereits erzeugte Wiederholungsnachfolger bestehen; erneutes Abschließen dupliziert sie nicht.

### US-036 – Priorität setzen · M1 · OFFEN

Als Benutzer möchte ich wichtige Aufgaben unterscheiden.

- `LOW`, `NORMAL`, `HIGH`, `URGENT` sind auswählbar und nachträglich änderbar.
- Anzeige nutzt Text oder zugängliche Symbole zusätzlich zur Farbe.
- Sortierung verwendet die fachliche Reihenfolge; unbekannte Werte werden abgewiesen.

### US-037 – Fälligkeit setzen und entfernen · M1 · OFFEN

Als Benutzer möchte ich Aufgaben mit oder ohne Termin verwalten.

- Datum ist optional; Uhrzeit nur zusammen mit Datum und Zeitzone. Entfernen des Datums entfernt die Uhrzeit.
- Überfälligkeit folgt den dokumentierten Zeitregeln; erledigte Aufgaben sind ausgeschlossen.
- Tests prüfen Datumsgrenzen, Zeitumstellung und Anzeige in einer anderen Zeitzone.

## 9. EPIC-05 – Projekte und Organisation

### US-040 – Projekt anlegen · M1 · OFFEN

Als Benutzer möchte ich Aufgaben nach einem Thema bündeln.

- Name ist Pflicht, Beschreibung optional; Ersteller wird Eigentümer.
- Neues Projekt ist zunächst privat; ein anderer Benutzer kann es nicht lesen.
- Ungültige Namen werden verständlich abgewiesen.

### US-041 – Projekt bearbeiten · M1 · OFFEN

Als Eigentümer möchte ich Name und Beschreibung ändern.

- Nur der Eigentümer darf Projektmetadaten ändern; Mitgliedschaft genügt nicht.
- Validierung und Versionsprüfung verhindern ungültige oder verlorene Änderungen.
- Bestehende Aufgaben und Mitgliedschaften bleiben erhalten.

### US-042 – Projekt ohne Aufgabenverlust löschen · M1/M2 · OFFEN

Als Eigentümer möchte ich ein Projekt auflösen, ohne Aufgaben zu verlieren.

- Bestätigung erklärt, dass Aufgaben zu persönlichen Aufgaben ihrer jeweiligen Ersteller werden.
- Projektbezug wird entfernt; unzulässige Zuweisungen, Tags und Erinnerungen werden transaktional bereinigt.
- Fremde Mitglieder verlieren Zugriff; Tests prüfen ab M2 mehrere Ersteller, einschließlich eines deaktivierten Kontos.

### US-043 – Tags verwalten · M1 · OFFEN

Als Benutzer möchte ich Aufgaben unabhängig von ihrer Projektansicht markieren.

- Mehrere Tags pro Aufgabe sind möglich, hinzufügbar und entfernbar.
- Namen sind pro persönlichem bzw. Projekt-Geltungsbereich eindeutig; private Tags werden nicht global angezeigt.
- Tags sind filterbar; Entfernen einer Zuordnung löscht keine andere Aufgabe.

## 10. EPIC-06 – Suche, Filter und Sortierung

### US-050 – Aufgaben durchsuchen · M1 · OFFEN

Als Benutzer möchte ich Aufgaben anhand von Text finden.

- Suche umfasst Titel und Beschreibung ohne Beachtung der Großschreibung.
- Nur zugängliche Aufgaben zählen als Treffer; Seitenzahlen verraten keine fremden Treffer.
- Leerer Suchtext zeigt die erlaubte Liste; leere Trefferliste erklärt das Ergebnis.

### US-051 – Aufgaben filtern · M1/M2 · OFFEN

Als Benutzer möchte ich relevante Aufgaben eingrenzen.

- Filter für Status, Priorität, Projekt, Tag, Fälligkeit und Überfälligkeit; Bearbeiter kommt in M2 hinzu.
- Unterschiedliche Filter werden mit UND verknüpft; mehrere Werte desselben Filters mit ODER.
- Aktive Filter sind sichtbar und per Tastatur zurücksetzbar; Seitengröße ist begrenzt.

### US-052 – Aufgaben sortieren · M1 · OFFEN

Als Benutzer möchte ich eine nachvollziehbare Reihenfolge wählen.

- Sortierfelder: erstellt, geändert, Fälligkeit, Priorität, Titel; auf- und absteigend.
- Gleichstände werden stabil nach ID aufgelöst; Aufgaben ohne Fälligkeit stehen bei Fälligkeitssortierung immer zuletzt.
- Nicht erlaubte Sortierfelder werden abgewiesen; Auswahl ist zugänglich beschriftet.

### US-053 – Standardansichten nutzen · M1 · OFFEN

Als Benutzer möchte ich häufige Ansichten direkt öffnen.

- Ansichten: Alle, Heute, Demnächst, Überfällig, Erledigt.
- Heute/Demnächst/Überfällig schließen erledigte Aufgaben aus; Alle enthält alle zugänglichen Aufgaben.
- Tagesgrenzen und Zeitraum „Demnächst“ folgen dem Domänenmodell; jede Ansicht bietet einen sinnvollen Leerzustand.

## 11. EPIC-07 – Zusammenarbeit

### US-060 – Projekt teilen · M2 · OFFEN

Als Eigentümer möchte ich registrierte Benutzer zur Zusammenarbeit aufnehmen.

- Nur Eigentümer fügen über die genaue Login-E-Mail einen aktiven Benutzer hinzu; keine frei durchsuchbare öffentliche Benutzerliste.
- Mitgliedschaft ist eindeutig und dauerhaft; wiederholte Aufnahme dupliziert nichts.
- Mitglied erhält Zugriff auf Projektaufgaben. Arbeitsannahme: direkte Aufnahme ohne separate Einladungsannahme; UI benennt diese Wirkung.

### US-061 – Mitglied entfernen · M2 · OFFEN

Als Eigentümer möchte ich eine Freigabe beenden.

- Entfernter Benutzer verliert sofort API-Zugriff; seine Zuweisungen und Erinnerungen werden bereinigt.
- Historische Kommentare und Aktivitäten bleiben erhalten.
- Eigentümer kann nicht über Mitgliederentfernung entfernt werden; parallele Zuweisung darf keine ungültige Referenz hinterlassen.

### US-062 – Aufgabe zuweisen · M2 · OFFEN

Als Projektmitglied möchte ich die Zuständigkeit sichtbar machen.

- Nur aktive Projektmitglieder oder Eigentümer sind zuweisbar; Zuweisung kann entfernt werden.
- Änderung wird protokolliert und versionsgeprüft.
- Ein nicht berechtigter Benutzer kann weder fremde Aufgaben zuweisen noch durch Zuweisung Zugang erhalten.

## 12. EPIC-08 – Unteraufgaben

### US-070 – Unteraufgaben verwalten · M2 · OFFEN

Als Benutzer möchte ich eine Aufgabe in kleine Schritte zerlegen.

- Mehrere Unteraufgaben mit Titel, Erledigt-Zustand und stabiler Reihenfolge können angelegt, umbenannt und gelöscht werden.
- Leere Titel scheitern; Rechte werden über die Hauptaufgabe geprüft.
- Reihenfolge ist auch ohne Ziehbewegung bedienbar; keine verschachtelten Unteraufgaben.

### US-071 – Fortschritt anzeigen · M2 · OFFEN

Als Benutzer möchte ich einzelne Schritte abhaken und den Fortschritt sehen.

- Erledigt-Zustand bleibt nach Neuladen erhalten und ist rückgängig änderbar.
- Fortschritt erscheint als verständlicher Text, etwa „3 von 5 erledigt“.
- Alle Unteraufgaben erledigt setzt die Hauptaufgabe nicht automatisch auf `DONE`.

## 13. EPIC-09 – Kommentare und Aktivitätshistorie

### US-080 – Kommentieren · M2 · OFFEN

Als berechtigter Benutzer möchte ich Informationen direkt an der Aufgabe ergänzen.

- Kommentare zeigen Autor und Zeitpunkt; leerer oder zu langer Text wird abgewiesen.
- Nur berechtigte Benutzer lesen oder ergänzen Kommentare; HTML wird als Text dargestellt.
- Kommentar und zugehörige Aktivität entstehen atomar. Bearbeiten/Löschen von Kommentaren ist zunächst nicht erforderlich.

### US-081 – Änderungen nachvollziehen · M2 · OFFEN

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

### US-110 – Persönliche Übersicht ansehen · M1 · OFFEN

Als Benutzer möchte ich offene Arbeit schnell überblicken.

- Übersicht zeigt nicht erledigte, heute fällige, überfällige, erledigte sowie offene hohe/dringende Aufgaben.
- Alle Zahlen und Detailverweise verwenden dieselben Rechte- und Zeitregeln wie die Listen.
- Jeder Zähler führt zu seiner passenden Liste; nach Änderung stimmen Übersicht und Liste überein.

## 17. EPIC-13 – Gleichzeitige Änderungen und Datenintegrität

### US-120 – Konflikte erkennen · M1 · OFFEN

Als Benutzer möchte ich Änderungen anderer Personen nicht unbemerkt überschreiben.

- Zwei Clients lesen Version 5; nach erfolgreichem Speichern von Client A wird der Schreibversuch von B mit Version 5 als `409` abgewiesen.
- Frontend erklärt den Konflikt, erhält den Entwurf und bietet aktuelles Neuladen an; kein automatisches stilles Überschreiben.
- Gleiche Prüfung gilt für Löschen und betroffene Projektänderungen; Paralleltest prüft den tatsächlichen Datenbankzustand.

### US-121 – Zusammengehörige Änderungen gemeinsam speichern · M1–M3 · OFFEN

Als Benutzer möchte ich keine halbfertigen Datenänderungen erhalten.

- Aufgabe und Aktivität werden gemeinsam gespeichert oder gemeinsam zurückgerollt.
- Projektlöschung, Mitgliederentfernung und ab M3 Wiederholungsnachfolger bleiben auch bei Fehlern konsistent.
- Ein gezielt ausgelöster Fehler beweist den Rollback; Unique Constraints verhindern doppelte Mitgliedschaften und Nachfolger.

## 18. EPIC-14 – Logging und Monitoring

### US-130 – Verständliche Logs erhalten · M0 · OFFEN

Als Betreiber möchte ich Fehler eingrenzen können.

- Logs enthalten Zeit, Schweregrad, Komponente, Meldung und bei HTTP-Anfragen eine Anfragekennung.
- Ein Fehler lässt sich einer Anfrage zuordnen, ohne Passwort, Cookie, Reset-Token oder Aufgabenbeschreibung zu protokollieren.
- Erwartete Benutzerfehler und unerwartete Systemfehler sind unterscheidbar; keine ungefilterten Request-Bodies.

### US-131 – Betriebsinformationen begrenzen · M0 · OFFEN

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

## 20. EPIC-16 – Tests und Ausbildungsnachweise

### US-150 – Automatisierte Qualitätsprüfungen · ab M0, Abschluss M4 · OFFEN

Als Entwickler möchte ich Regressionen vor einem Release erkennen.

- Unit-, PostgreSQL-Integrations-, API-, Frontend-, E2E- und axe-Prüfungen laufen mit dokumentierten Befehlen.
- Die Build-Pipeline führt die vereinbarten Prüfungen aus und scheitert bei Fehlern; übersprungene Tests werden nicht als bestanden gewertet.
- Berichte sind auswertbar, Daten synthetisch und Tests unabhängig; Migration von leerem Schema und vorherigem Release wird geprüft.

### US-151 – Architektur erklären · M4 · OFFEN

Als Azubi möchte ich die Referenzlösung fachlich und technisch vorstellen können.

- Der Weg vom Speichern im Browser bis zur Datenbank wird anhand des Diagramms erklärt.
- Technologien, Migrationen, Sessions, Berechtigungen, Testarten, Konflikte und Healthchecks werden in eigenen Worten eingeordnet.
- Ein Ausbilder dokumentiert Rückfragen und Ergebnis; auswendig gelesene Codezeilen sind kein Abnahmekriterium.

## 21. EPIC-17 – Installation, Deployment und Updates

### US-160 – Installation durch einen anderen Azubi · Probe M1, Abschluss M4 · OFFEN

Als neuer Betreiber möchte ich allein mit Repository und Anleitung starten können.

- Ein bisher unbeteiligter Azubi installiert auf einem frischen System ohne mündliche Zusatzschritte.
- Er konfiguriert eigene Secrets, startet Dienste, prüft Healthcheck und führt Registrierung, Login, Aufgabe, Neustart und erneutes Lesen durch.
- Versionen, Dauer, Abweichungen und nötige Korrekturen werden protokolliert; keine erfundenen Abnahmeeinträge.

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

### US-172 – Manuelle Accessibility-Abnahme · ab M1, Abschluss M4 · OFFEN

Als Ausbilder möchte ich Bedienbarkeit prüfen, die ein automatischer Scan nicht beurteilt.

- Tastaturablauf, mindestens eine dokumentierte Screenreader-/Browser-Kombination sowie Zoom und Reflow werden je größerem Meilenstein geprüft.
- Protokoll nennt Build, Umgebung, Prüfer, Schritte, Ergebnis und offene Befunde.
- Der WebUI-/Accessibility-Ausbilder prüft das Referenzrelease; offene anwendbare A-/AA-Verstöße verhindern dessen Abnahme.

## 23. Definition of Done

Eine Story ist erst `ABGENOMMEN`, wenn alle zutreffenden Punkte nachgewiesen sind:

- Akzeptanzkriterien erfüllt, inklusive Fehler-, Leer- und Berechtigungsfällen.
- Passende automatisierte Tests erfolgreich; keine bekannten still übersprungenen Pflichtprüfungen.
- Frontend per Tastatur bedienbar, zugängliche Beschriftung und Fokusführung; relevante axe-Scans und manuelle Prüfungen durchgeführt.
- Sicherheitsregeln, serverseitige Validierung, Konflikt- und Transaktionsregeln eingehalten.
- Schemaänderungen als Flyway-Migration, Schnittstellen in OpenAPI dokumentiert.
- README, Fachdateien, Konfiguration und Betriebsbefehle stimmen mit dem tatsächlichen Stand überein.
- Review ohne offene blockierende Befunde; Nachweise mit Story-ID und konkretem Build dokumentiert.
- Keine Secrets, lokalen Daten oder erzeugten Build-Dateien im Commit.

Für das Referenzrelease kommen die unabhängige Installation, geprüfter Restore, Updateprobe, Architekturvorstellung und manuelle Accessibility-Abnahme hinzu. Ein erfolgreicher Build allein erfüllt diese Definition nicht.

## 24. Vor der jeweiligen Umsetzung zu konkretisieren

| Zeitpunkt | Offener Umsetzungsentscheid | Bestehende Vorgabe |
|---|---|---|
| M0 | Gepflegte kompatible Paketversionen, konkreter Webserver, OpenAPI-Integration, CI-Umgebung | Festgelegter Stack, Java 21, reproduzierbarer Build |
| M1 | Zugängliche UI-Komponenten und genaue Gestaltung | WCAG 2.2 AA; keine Bibliothek allein nach Aussehen wählen |
| M2 | Bootstrap-Aufruf für Admin und lokaler Mailfänger | Keine Standardkennwörter, kein öffentliches Reset-Token-Logging |
| M3 | Beispiele für Zeit- und Wiederholungsregeln bestätigen | Markierte Arbeitsannahmen im Domänenmodell |
| Vor öffentlichem Betrieb | Host, DNS/TLS, echter Versand, Secret- und Backup-Aufbewahrung | Lokales Referenzdeployment bleibt Standardumfang |

Diese Punkte verhindern das Anlegen der Dokumentationsbasis und den Beginn von M0 nicht. Produktänderungen werden sichtbar abgestimmt; technische Detailentscheidungen werden nachvollziehbar dokumentiert.
