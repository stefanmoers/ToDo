# Barrierefreiheit

[Zur Übersicht](../README.md) · [Teststrategie](TESTING.md) · [EPIC-18 im Backlog](../BACKLOG.md)

**Ziel: WCAG 2.2, Konformitätsstufe AA.** Damit sind alle anwendbaren Kriterien der Stufen A und AA gemeint. Dies ist eine Entwicklungs- und Abnahmevorgabe, keine Behauptung über die heutige Dokumentationsbasis oder eine bereits zertifizierte Anwendung. Maßgeblich ist die [W3C-Empfehlung WCAG 2.2](https://www.w3.org/TR/WCAG22/); rechtliche Konformität wird hier nicht behauptet.

Barrierefreiheit gehört zu Entwurf, Implementierung, Tests und Definition of Done. Der WebUI-/Accessibility-Ausbilder soll die grundlegenden Bedienmuster früh und das Referenzrelease abschließend prüfen.

## Konkrete Regeln für UltimateTodo

| Bereich | Umsetzung und Prüfung |
|---|---|
| Semantik | Echte Buttons, Links, Formulare und Überschriften; sinnvolle Regionen wie `main` und `nav`; Sprunglink zum Inhalt |
| Beschriftung | Jedes Feld hat ein sichtbares Label; Icon-Buttons haben einen verständlichen zugänglichen Namen; dekorative Icons werden ausgeblendet |
| Tastatur | Sämtliche Aktionen ohne Maus; nachvollziehbare Tab-Reihenfolge; keine unerreichbaren Aktionen oder unbeabsichtigten Tastaturfallen |
| Fokus | Immer sichtbar; keine positiven `tabindex`-Werte; Fokuswechsel nach Navigation, Dialogen und Löschungen gezielt festlegen |
| Farbe | Status und Fehler zusätzlich durch Text oder Symbole vermitteln |
| Formulare | Pflichtangaben erklären; Fehler am Feld zuordnen, etwa mit `aria-describedby`; Fehlerübersicht mit erreichbaren Feldverweisen |
| Statusmeldungen | Speichern, Laden, leere Suche und Konflikte wahrnehmbar melden; passende Live-Regionen sparsam einsetzen |
| Sprache und Orientierung | Dokumentensprache Deutsch, verständliche Seitentitel und konsistente Navigation; Sprachwechsel kennzeichnen |

Diese Projektregeln konkretisieren unter anderem 1.3.1, 1.4.1, 2.1.1, 2.1.2, 2.4.1, 2.4.3, 2.4.7, 3.1.1, 3.3.1–2 und 4.1.2–3. Die Tabelle ist keine vollständige WCAG-Checkliste. Für jedes Feature sind alle anwendbaren A-/AA-Kriterien zu prüfen. [WCAG 2.2: Kriterien und Geltungsbereich](https://www.w3.org/TR/WCAG22/)

## Kontrast, Vergrößerung und Bedienelemente

- Normaler Text erreicht mindestens **4,5:1** Kontrast. Für großen Text gilt **3:1**; „groß“ bedeutet mindestens 18 pt oder 14 pt fett. Notwendige sichtbare Grenzen und Zustände von Bedienelementen benötigen grundsätzlich 3:1 zum angrenzenden Hintergrund, soweit das Kriterium anwendbar ist. [Textkontrast](https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html), [Nicht-Text-Kontrast](https://www.w3.org/WAI/WCAG22/Understanding/non-text-contrast.html)
- Textvergrößerung auf 200 % darf keine Information oder Funktion verlieren. Bei einer Breite von **320 CSS-Pixeln**, zum Beispiel 400 % Zoom bei 1280 Pixeln Ausgangsbreite, soll der normale Inhalt ohne horizontales Scrollen lesbar bleiben. Ausnahmen für notwendige zweidimensionale Inhalte nicht pauschal auf Aufgabenlisten anwenden. [Reflow](https://www.w3.org/WAI/WCAG22/Understanding/reflow.html)
- Ein fokussiertes Element darf durch selbst erzeugte Inhalte wie Sticky-Header nicht vollständig verdeckt sein (2.4.11 AA). Als Projektziel halten wir den Fokus möglichst vollständig frei. Das ist von den weitergehenden AAA-Kriterien zu unterscheiden. [Fokus nicht verdeckt](https://www.w3.org/WAI/WCAG22/Understanding/focus-not-obscured-minimum)
- Zeigerziele erfüllen 2.5.8 AA: grundsätzlich mindestens **24 × 24 CSS-Pixel** oder die ausdrücklich geregelten Abstands-/Ausnahmebedingungen. Für zentrale Aktionen streben wir größere Flächen an. 44 × 44 ist keine allgemeine AA-Pflicht. [Mindestzielgröße](https://www.w3.org/WAI/WCAG22/Understanding/target-size-minimum)
- Ziehbewegungen erhalten eine Alternative durch einzelne Klicks/Taps und Tastatur, etwa „Nach oben“/„Nach unten“. Keine Funktion ausschließlich über Hover oder Drag & Drop anbieten.

## Dialoge, Datumsauswahl und Login

Ein Dialog erhält einen Namen. Beim Öffnen geht der Fokus an eine sinnvolle Stelle im Dialog; die Tastatur bleibt im modalen Dialog. Escape schließt ihn, sofern keine begründete besondere Interaktion entgegensteht. Beim Schließen kehrt der Fokus zum Auslöser zurück oder nach dessen Löschung zu einem logisch folgenden Element. Beim endgültigen Löschen ist „Abbrechen“ ein sinnvoller erster Fokus. Der Hintergrund bleibt inaktiv. [WAI-Muster für modale Dialoge](https://www.w3.org/WAI/ARIA/apg/patterns/dialog-modal/)

Bevor ein eigener Datepicker, eine Combobox oder ein Menü entsteht, prüfen wir native HTML-Elemente oder nachweislich geeignete Komponenten. ARIA ergänzt fehlende Semantik; es repariert keine unbedienbare Interaktion allein durch Attribute. Datum und Uhrzeit müssen auch ohne präzise Zeigerbewegung einzugeben sein.

Login und Passwortänderung erlauben Passwortmanager und Einfügen. Passwörter müssen nicht aus dem Gedächtnis abgeschrieben werden; geeignete `autocomplete`-Werte werden gesetzt. Keine Rätsel oder unnötigen kognitiven Tests als alleiniger Zugangsweg. [Zugängliche Authentifizierung](https://www.w3.org/WAI/WCAG22/Understanding/accessible-authentication-minimum.html)

Zeitlimits werden zugänglich angekündigt und verlängerbar gestaltet. Animationen berücksichtigen reduzierte Bewegung; keine blinkenden Inhalte. Wiederholte Eingaben werden vermieden. Falls Hilfefunktionen angeboten werden, bleiben sie konsistent auffindbar.

## Automatische Prüfungen

Playwright öffnet reale UI-Zustände und führt dort axe-core über `@axe-core/playwright` aus. Ein geschlossener Dialog ist dadurch noch nicht geprüft. [Playwright: Accessibility-Tests](https://playwright.dev/docs/accessibility-testing)

Mindestens zu scannen sind:

1. Login und Registrierung, jeweils normal und mit Fehlern.
2. Dashboard und Aufgabenliste, gefüllt und leer.
3. Erstellen/Bearbeiten, einschließlich Validierungsfehlern und Fälligkeitsauswahl.
4. Löschdialog, Konflikthinweis, Filter und geöffnete komplexe Auswahlfelder.
5. Geteiltes Projekt, Unteraufgaben, Kommentare und Erinnerungen, sobald vorhanden.

Die Konfiguration enthält die in der festgeschriebenen axe-Version unterstützten A-/AA-Regeln für WCAG 2.0, 2.1 und 2.2; allein ein Tag `wcag22aa` erfasst frühere Kriterien nicht. Best-Practice-Regeln können zusätzlich laufen. Ergebnisse und Version werden gespeichert. Im geprüften Umfang bleiben keine ungeklärten Regelverstöße; neue schwerwiegende Befunde lassen die Pipeline scheitern. Ein belegter Fehlalarm braucht eine eng begrenzte, begründete Ausnahme mit Wiedervorlage, keinen globalen Regelausschluss.

Ein automatischer Scan kann sinnvolle Lesereihenfolge, verständliche Texte oder gute Bedienführung nicht vollständig beurteilen. Ein leerer Befundbericht beweist daher keine WCAG-Konformität. [Grenzen automatisierter Tests](https://playwright.dev/docs/accessibility-testing)

## Manuelle Prüfung je größerem Meilenstein

Den folgenden Ablauf zunächst ohne Maus durchführen:

1. Registrieren bzw. anmelden; Fehler provozieren und korrigieren.
2. Aufgabe anlegen, Priorität und Fälligkeit wählen, speichern.
3. Aufgabe finden, filtern, bearbeiten und Versionskonflikt nachvollziehen.
4. Erledigen, wieder öffnen, löschen und Dialog einmal abbrechen.
5. Ab M2 Mitglied aufnehmen, Unteraufgabe abhaken und Kommentar anlegen; ab M3 Wiederholung und Erinnerung konfigurieren.
6. Abmelden und Sessionablauf mit Warnung und erneuter Anmeldung prüfen.

Zusätzlich denselben Kernablauf mit mindestens einer dokumentierten Kombination aus Screenreader und Browser, etwa NVDA/Firefox unter Windows oder VoiceOver/Safari unter macOS, prüfen. Namen, Rollen, Zustände, Überschriften, Meldungen und Fokus nachvollziehen. Ein zweites unterstütztes System ist für das Referenzrelease wünschenswert; Ergebnisse nicht auf ungeprüfte Kombinationen übertragen.

Zoom bei 200 % und Reflow bei 320 CSS-Pixeln prüfen; Kontraste und vergrößerte Textabstände kontrollieren. Die Oberfläche soll ohne Informationsverlust nutzbar bleiben. Manuelle Prüfung ergänzt axe auch dann, wenn alle automatischen Tests grün sind.

## Abnahmeprotokoll

Bei Implementierung unter `docs/reviews/` ablegen, beispielsweise `accessibility-M1.md`:

| Angabe | Einzutragender Nachweis |
|---|---|
| Build und Zeitpunkt | Commit-ID, Datum |
| Umgebung | Betriebssystem, Browser, Screenreader mit Versionen |
| Prüfer | Tatsächlich prüfende Person |
| Ablauf | Schritte und getestete UI-Zustände |
| Befund | Erwartung, tatsächliches Ergebnis, betroffenes Kriterium |
| Nachprüfung | Korrektur, Ergebnis und offene Einschränkungen |

Für das Referenzrelease verhindern offene anwendbare A-/AA-Verstöße die Abnahme. Die Freigabe darf nicht stellvertretend für den Ausbilder erfunden werden.
