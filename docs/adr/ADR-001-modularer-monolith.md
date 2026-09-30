# ADR-001 – Modularer Monolith

- **Status:** Akzeptiert, Umsetzung ausstehend
- **Datum:** 2026-09-30
- **Bezug:** EPIC-01, EPIC-13, [Architektur](../ARCHITECTURE.md)

## Kontext

UltimateTodo umfasst Benutzer, Projekte, Aufgaben und Benachrichtigungen. Das Projekt soll professionelles Softwaredesign vermitteln und von einem anderen Azubi lokal installiert werden können. Es gibt aktuell keinen Bedarf an unabhängiger Skalierung oder getrennten Entwicklungsteams je Fachgebiet.

## Entscheidung

Ein Spring-Boot-Backend enthält fachlich getrennte Module und wird als eine Anwendung gebaut und betrieben. Code wird nach Fachgebieten organisiert; innerhalb eines Moduls werden HTTP, Anwendungsfälle, Fachregeln und Speicherung getrennt. Die Module kommunizieren über schmale öffentliche Schnittstellen. PostgreSQL ist die gemeinsame Datenbank.

Frontend, Backend und Datenbank sind getrennte Laufzeitbausteine. Das macht das Backend nicht zu einem Microservice-System. Der Begriff „Monolith“ bezieht sich hier auf die gemeinsam ausgelieferten Backend-Module.

## Gründe und Alternativen

- Ein unstrukturierter Monolith wäre zunächst schnell, würde jedoch Fachgrenzen und Lernziele verwischen.
- Microservices ermöglichen unabhängige Bereitstellung, benötigen aber zusätzliche Netzwerkkommunikation, Betriebswerkzeuge und Lösungen für verteilte Konsistenz. Dieser Aufwand löst hier kein vorhandenes Problem.
- Der modulare Monolith ermöglicht verständliches Debugging, klare Verantwortlichkeiten und lokale Datenbanktransaktionen bei überschaubarem Betrieb.

## Konsequenzen

Alle Module werden zusammen getestet und ausgeliefert. Eine Störung des Backend-Prozesses betrifft die gesamte Anwendung. Modulgrenzen müssen durch Code-Review und geeignete Architekturprüfungen geschützt werden; das gemeinsame Deployment erzwingt sie nicht automatisch.

Eine spätere Ausgliederung, etwa der Benachrichtigung, wäre eine neue Entscheidung mit Daten- und Schnittstellenmigration. Sie wird nicht durch vorsorgliche Broker oder leere Service-Gerüste vorweggenommen.

## Überprüfung

M0 zeigt ein gemeinsam startbares Backend mit erkennbaren Fachmodulen. Neue Abhängigkeiten dürfen keine Kreise erzeugen. Erst ein konkreter Bedarf an eigenständigem Betrieb oder Skalierung rechtfertigt eine Neubewertung.
