# ADR-004 – PostgreSQL mit Flyway

- **Status:** Akzeptiert, Umsetzung ausstehend
- **Datum:** 2026-09-30
- **Bezug:** EPIC-01, EPIC-13, EPIC-17, [Domänenmodell](../DOMAIN_MODEL.md)

## Kontext

Benutzer, Projekte, Mitgliedschaften und Aufgaben besitzen klare Beziehungen. Fachänderungen und Historie müssen zusammen gespeichert werden. Gleichzeitige Bearbeitung, Updates auf bestehenden Installationen und Datensicherung gehören zum Referenzumfang.

## Entscheidung

PostgreSQL speichert die Daten relational. Fremdschlüssel, eindeutige Schlüssel und Transaktionen sichern Konsistenz. Versionsfelder unterstützen optimistisches Sperren. Flyway führt nummerierte SQL-Migrationen aus; bereits angewendete Migrationen werden nicht nachträglich verändert.

Ein benanntes Compose-Volume speichert lokale Betriebsdaten. Integrationstests nutzen dieselbe PostgreSQL-Hauptversion über Testcontainers. Schemaänderungen durch ein automatisches ORM-`update` sind im Betrieb ausgeschlossen.

## Gründe und Alternativen

Eine relationale Datenbank passt zu Projektmitgliedschaften und verknüpften Aufgaben. PostgreSQL bietet die hierfür benötigten Transaktionen und Constraints und lässt sich lokal als Container betreiben.

Eine Dokumentdatenbank wäre möglich, würde aber die vielen Beziehungen und Konsistenzregeln nicht vereinfachen. SQLite vereinfacht einen Einzelplatzstart, entspricht jedoch nicht dem geplanten Mehrbenutzer- und Testsetup. H2 wird nicht als Ersatz für PostgreSQL-Integrationstests verwendet, da SQL- und Sperrverhalten abweichen können. MySQL wäre grundsätzlich geeignet, ist aber nicht die vereinbarte Datenbank.

## Konsequenzen

SQL, Migrationen und Datenbankbetrieb gehören zum Lernumfang. Jede Schemaänderung benötigt einen Weg vom alten zum neuen Stand. Die Anwendung darf erst bereit sein, wenn erforderliche Migrationen erfolgreich sind.

Volumes sind keine Sicherungen. Dump, Restore und Updateprobe sind Release-Kriterien. Ein PostgreSQL-Hauptversionswechsel erhält einen eigenen überprüften Migrationsplan. Rückkehr zu alter Software ist nach Schemaänderung nicht automatisch möglich.

## Überprüfung

Tests zeigen Aufbau aus leerer Datenbank, Upgrade des vorherigen Release-Schemas, Rollback einer fehlgeschlagenen Transaktion und echte gleichzeitige Updates. Die Betriebsabnahme stellt Daten aus einem Dump in einem getrennten leeren Volume wieder her.
