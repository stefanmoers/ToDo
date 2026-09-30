# ADR-003 – React, TypeScript und Vite

- **Status:** Akzeptiert, Umsetzung ausstehend
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

Bereits der erste Login-/Aufgabenablauf muss ohne Maus funktionieren. M0/M1 dokumentieren Paketversionen, Build und Prüfungen. Komplexe Komponenten werden vor breiter Wiederverwendung manuell durch den Accessibility-Ausbilder geprüft; die tatsächliche Prüfung ist zu protokollieren.
