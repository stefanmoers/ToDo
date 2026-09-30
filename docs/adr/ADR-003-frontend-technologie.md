# ADR-003 – React, TypeScript und Vite

- **Status:** Akzeptiert, umgesetzt (Stand 30.09.2026, siehe Umsetzungsstand)
- **Datum:** 2026-09-30
- **Bezug:** EPIC-01, EPIC-04, EPIC-18, [Barrierefreiheit](../ACCESSIBILITY.md)

## Kontext

Die Browseroberfläche soll Formulare, Listen, Filter und gemeinsame Aufgaben übersichtlich darstellen. Zustände wie Laden, Fehler, Versionskonflikt und leere Listen müssen ebenso verständlich sein wie der Normalfall. Barrierefreiheit ist ein zentrales Ausbildungs- und Qualitätsziel.

## Entscheidung

React bildet die UI, TypeScript prüft Datentypen, Vite dient Entwicklung und Produktionsbuild. Das Ergebnis ist eine Browseranwendung mit statisch auslieferbaren Dateien. Fachfeatures erhalten eigene Bereiche; gemeinsame Komponenten dienen wiederkehrenden Bedienmustern.

Vitest prüft Frontend-Logik und Komponenten, Playwright vollständige Abläufe. axe-core wird in geöffnete UI-Zustände integriert. Ziel ist WCAG 2.2 AA einschließlich manueller Tastatur- und Screenreader-Prüfung.

## Gründe und Alternativen

React-Komponenten machen Oberfläche und Zustand gut trennbar; TypeScript unterstützt einen verlässlichen API-Vertrag. Vite hält den Build für eine reine Browseranwendung überschaubar.

Serverseitige HTML-Templates wären eine einfachere Alternative für wenige Formulare, entsprächen aber nicht der geplanten Trennung von Frontend und REST-Backend. Angular oder Vue sind mögliche UI-Technologien, ändern jedoch den vereinbarten Stack. Ein zusätzliches Fullstack-Framework mit Server-Rendering löst hier keinen nachgewiesenen Bedarf.

## Konsequenzen

Zwei Werkzeugketten sind zu pflegen: Java/Maven und Node/npm. Ein Frontend-Build ersetzt keine Backend-Prüfung und TypeScript keine Laufzeitvalidierung. Serverdaten und lokale Eingaben müssen bewusst getrennt werden, damit Konflikte keinen Entwurf vernichten.

Anfangs reicht React-Zustand ohne Redux. Eine spätere Bibliothek für Serverdaten darf bei konkretem Bedarf begründet ergänzt werden. UI-Komponenten werden anhand von Semantik, Tastatur, Fokus und Screenreader geprüft. Eine aufwendig selbst gebaute Combobox ist kein Lernziel an sich.

## Überprüfung

Bereits der erste Login-/Aufgabenablauf muss ohne Maus funktionieren. M0/M1 dokumentieren Paketversionen, Build und Prüfungen. Komplexe Komponenten werden vor breiter Wiederverwendung manuell vom Entwickler geprüft; die tatsächliche Prüfung ist zu protokollieren.

## Umsetzungsstand (30.09.2026)

Die Entscheidung ist umgesetzt; der Text oben bleibt unverändert. Die tatsächlich eingesetzten Versionen stehen in [TECHNOLOGIES.md](../TECHNOLOGIES.md) (React 19.3.0, React Router 7.18.4, TypeScript 6.0.3, Vite 8.3.1, Node 24.21.0). Zur Abgrenzung von den ursprünglichen Überlegungen:

- **Zustand:** Es gibt weiterhin keinen globalen Zustandsmanager und keine Bibliothek für Serverdaten. Serverzugriffe laufen über einen schlanken `fetch`-Client (`src/frontend/src/api/client.ts`) mit CSRF-Header und einheitlicher Fehlerbehandlung, Zustand über React-Hooks und einen Authentifizierungs-Kontext.
- **Oberfläche:** Keine UI-Bibliothek; Gestaltung über ein eigenes CSS-System mit hellem und dunklem Farbschema (US-180). Damit entfiel die in diesem ADR verlangte Bibliotheksprüfung.
- **Auslieferung:** Statische Dateien im nginx-Container, `/api` wird an das Backend weitergeleitet.
- **Noch offen:** Die manuelle Tastatur- und Screenreader-Prüfung nach [ACCESSIBILITY.md](../ACCESSIBILITY.md) (US-172) wurde noch nicht durchgeführt. Automatische axe-Prüfungen ersetzen sie nicht.
