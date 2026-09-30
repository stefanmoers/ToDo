# Arbeitsregeln für Coding-Agenten

Diese Regeln gelten für das gesamte Repository. UltimateTodo ist ein Ausbildungsprojekt. Verständlichkeit, nachvollziehbare Entscheidungen, Sicherheit und Barrierefreiheit sind Produktanforderungen.

## Vor der ersten Implementierung

1. Lies `README.md`, `BACKLOG.md`, alle acht Fachdateien unter `docs/` und die fünf ADRs einschließlich ADR-Übersicht vollständig.
2. Prüfe vorhandene Dateien und den Git-Status. Bewahre Änderungen anderer Personen. Überschreibe keine fremde Arbeit und verwende kein destruktives Zurücksetzen.
3. Benenne die Story-IDs, die du umsetzt, ihre Abhängigkeiten und die vorgesehenen Prüfungen.
4. Unterscheide dokumentierte Ziele von tatsächlich vorhandener Funktion. Dieser Ausgangsstand enthält bewusst keinen Anwendungscode.

Bei Folgeaufträgen lies die geänderte Dokumentation und die zur Story gehörenden Regeln erneut. Nutzeraufträge haben Vorrang; Widersprüche zwischen Anforderungen nicht stillschweigend durch eigene Produktentscheidungen auflösen.

## Umsetzung

- Dokumentation, sichtbare UI-Texte und verständliche Fehlermeldungen auf Deutsch. Technische Bezeichner und API-Felder dürfen konsistent englisch sein.
- Halte Java 21, Spring Boot, React/TypeScript/Vite, PostgreSQL und den festgelegten Werkzeugkasten ein. Keine Microservices, kein GraphQL, kein Kubernetes und kein zusätzlicher globaler State-Manager ohne begründete neue Entscheidung.
- Organisiere Code nach Fachgebieten und halte Modulgrenzen ein. Controller koordinieren HTTP; Anwendungsdienste setzen Anwendungsfälle um; fachliche Regeln gehören nicht in React-Komponenten.
- Prüfe jede Datenoperation im Backend auf Benutzer- und Objektberechtigung. UI-Ausblendung ist keine Autorisierung.
- Verwende Migrationen für jede Schemaänderung. Bereits angewendete Migrationen bleiben unverändert; ergänze eine neue Migration.
- Verwende typisierte API-Datenmodelle statt Datenbankobjekte als Antwort. Dokumentiere die Schnittstelle mit OpenAPI.
- Setze semantisches HTML, Tastaturbedienung, verständliche Fokusführung und zugängliche Fehlerbehandlung sofort mit um.
- Wähle Abhängigkeiten sparsam. Eine UI-Bibliothek braucht eine dokumentierte Prüfung von Tastatur, Screenreader, Wartbarkeit und Lizenz. Das bloße Versprechen „accessible“ reicht nicht.
- Versioniere Maven Wrapper, npm-Lockdatei, Laufzeitversionen und feste Container-Versionen. Verwende keine unbestimmten `latest`-Images.
- Keine echten Passwörter, Tokens, personenbezogenen Testdaten oder Datenbankkopien im Repository. Beispiele enthalten ausschließlich erkennbare Platzhalter.
- Führe keine Veröffentlichung, keinen Push und keine Einrichtung externer Dienste ohne passenden Auftrag aus.

## Entscheidungen und offene Punkte

Die fünf akzeptierten ADRs halten den vereinbarten Stack fest. Die als **Arbeitsannahmen** gekennzeichneten Detailregeln im Domänenmodell sind nachvollziehbare Startvorgaben. Überprüfe sie vor der jeweils betroffenen Story. Eine andere fachliche Wirkung muss zuerst sichtbar dokumentiert und mit dem Auftraggeber geklärt werden; normale technische Umsetzungsdetails darfst du selbst lösen.

Neue wesentliche Architekturentscheidungen erhalten eine ADR mit Kontext, Alternativen und Konsequenzen. Ändere akzeptierte Entscheidungen nicht rückwirkend, sondern ersetze sie durch eine neue verlinkte ADR. Führe keine Funktionen außerhalb des Backlogs beiläufig ein.

## Prüfungen und Abschluss

1. Ergänze aussagekräftige Tests für neues Verhalten und Fehlerfälle nach `docs/TESTING.md`. Teste Berechtigungen mit mindestens zwei Benutzern; teste relevante Konflikte und Persistenz mit echtem PostgreSQL über Testcontainers.
2. Prüfe Frontend-Zustände einschließlich Dialogen, Formularfehlern und leerer Listen mit Playwright/axe und manuell nach `docs/ACCESSIBILITY.md`.
3. Führe die betroffenen Prüfungen aus. Berichte exakt, was gelaufen ist, was erfolgreich war und was noch nicht überprüft werden konnte. Ein fehlendes Docker-System ist kein bestandener Integrationstest.
4. Aktualisiere Dokumentation, OpenAPI, Konfigurationsbeispiele und Backlog-Status im selben Änderungspaket wie das Verhalten.
5. Markiere eine Story erst nach erfüllten Akzeptanzkriterien und Definition of Done als erledigt. Manuelle Prüfungen nur als durchgeführt eintragen, wenn sie tatsächlich stattfanden; der Auftraggeber führt sie selbst durch und bestätigt sie. Es gibt keine Abnahmen durch Dritte.
6. Halte Änderungen und Commits überschaubar. Ein sinnvoller Commit nennt Zweck und gegebenenfalls Story-ID. Prüfe vor einem Commit, ob Secrets oder erzeugte Dateien enthalten sind.

## Erster Implementierungsauftrag

Beginne mit M0: lauffähiges Grundgerüst, überprüfte Versionskombination, reproduzierbarer Build, Compose-Grundlage, Flyway, Healthchecks und minimale Prüfungen. Liefere keine leeren Tests oder vorgetäuschten Erfolgsmeldungen. Danach M1 als durchgängigen Ablauf vom Login bis zur gespeicherten Aufgabe umsetzen. Barrierefreiheit, Sicherheit und Dokumentation gehören zu jedem Schritt.
