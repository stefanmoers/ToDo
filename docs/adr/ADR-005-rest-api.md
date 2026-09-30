# ADR-005 – REST/JSON mit OpenAPI

- **Status:** Akzeptiert, Umsetzung ausstehend
- **Datum:** 2026-09-30
- **Bezug:** EPIC-03–13, [Architektur](../ARCHITECTURE.md), [Sicherheit](../SECURITY.md)

## Kontext

Frontend und Backend brauchen einen verständlichen, testbaren Vertrag. Rechte, Validierung, Seiteneinteilung und Konflikte müssen unabhängig von der Benutzeroberfläche korrekt behandelt werden.

## Entscheidung

Wir verwenden HTTP-orientierte REST-Endpunkte unter `/api/v1` mit JSON für fachliche Nutzdaten. OpenAPI beschreibt Methoden, Parameter, Pflichtfelder, Datentypen, Antworten, Fehler und Authentifizierung. Die konkrete kompatible OpenAPI-Integration wird in M0 festgelegt; die Spezifikation wird mit der Implementierung geprüft und bei Änderungen aktualisiert. Eine separate handgeschriebene `API.md` ist zunächst nicht erforderlich.

Beispiele für geplante Ressourcen, noch keine vorhandenen Endpunkte:

| Methode und Pfad | Bedeutung | Typisches Ergebnis |
|---|---|---|
| `GET /api/v1/tasks` | Berechtigte Aufgaben suchen/filtern/sortieren | `200`, seitengenaue Liste |
| `POST /api/v1/tasks` | Aufgabe anlegen | `201`, Repräsentation und `Location` |
| `GET /api/v1/tasks/{id}` | Eine berechtigte Aufgabe lesen | `200` oder `404` |
| `PUT /api/v1/tasks/{id}` | Bearbeitbare Felder samt erwarteter `version` speichern | `200` mit neuer Version oder `409` |
| `DELETE /api/v1/tasks/{id}?version=5` | Aufgabe mit erwarteter Version löschen | `204`, `404` oder `409` |
| `GET /api/v1/projects` | Zugängliche Projekte auflisten | `200` |
| `GET /api/v1/auth/csrf` | Token für schreibende Anfragen beschaffen | `200`, Token und Headername |
| `POST /api/v1/auth/login` | Anmeldedaten prüfen und Session eröffnen | `200` oder `401` |
| `POST /api/v1/auth/logout` | Session serverseitig entwerten | `204` |
| `GET /api/v1/auth/me` | Aktuelles eigenes Profil lesen | `200` oder `401` |

`PUT` ersetzt die vollständige bearbeitbare Darstellung; optionale Felder werden durch explizites `null` geleert. Technische Felder bleiben serververwaltet. Fehlende Versionsangaben bei geschützten Änderungen liefern `400`. Der Versionsparameter bei `DELETE` ist eine Arbeitsannahme, damit auch Löschkonflikte konsistent prüfbar sind. Wenn später HTTP-Preconditions eingeführt werden, muss ihr Vertrag ausdrücklich dokumentiert werden.

## Gemeinsame Konventionen

- DTOs, also eigens definierte Ein-/Ausgabemodelle, statt serialisierter Datenbank-Entities. Keine Passwort-Hashes, internen Rollenänderungsfelder oder versehentlich nachgeladenen Beziehungen.
- IDs als UUID-Zeichenfolgen; Datumswerte `YYYY-MM-DD`; technische Zeitpunkte ISO 8601 in UTC; fachliche Zeitzonen gesondert.
- Listen: `page` ab 0, `size` standardmäßig 25, höchstens 100; `items`, `page`, `size`, `totalElements`, `totalPages` als explizites Antwortmodell. Zählung nur über zugängliche Daten.
- Erlaubte Sortier- und Filterfelder werden in OpenAPI aufgezählt, einschließlich stabiler Zweitsortierung und Behandlung fehlender Fälligkeit.
- Fehler haben mindestens `status`, stabilen maschinenlesbaren `code`, deutsche `message` und `requestId`; Feldfehler zusätzlich `fieldErrors`. Keine Stacktraces oder SQL-Ausgaben.
- `400`: ungültige Eingabe; `401`: Anmeldung erforderlich; `403`: verbotene Aktion bzw. CSRF; `404`: Ressource fehlt oder ist nicht sichtbar; `409`: fachlicher/Versionskonflikt; `429`: Ratenlimit; `500`: unerwarteter Serverfehler.
- Security-Fehler werden in denselben API-Vertrag eingebunden, statt HTML-Anmeldeseiten als vermeintliche JSON-Antwort zu liefern. Der Login muss die Session-Sicherheitsmechanismen von Spring Security korrekt nutzen.
- HTTP-GET verändert keine fachlichen Daten. Ein CSRF-Tokenabruf kann technisch eine Session initialisieren. Schreibzugriffe einschließlich Login/Logout bleiben CSRF-geschützt.
- Fachliche API und Browser teilen dieselbe Origin. Session-Cookie authentifiziert; ein zusätzlicher CSRF-Header schützt Änderungen.

OpenAPI wird spätestens mit der ersten fachlichen API verfügbar. Ein etwaiges Swagger-UI ist im lokalen Entwicklungsprofil hilfreich, im Netzwerkbetrieb aber nicht automatisch öffentlich. Der genaue Dokumentationspfad wird bei Implementierung in der Entwicklungsanleitung ergänzt.

## Gründe und Alternativen

REST nutzt bekannte HTTP-Methoden und Statuscodes; JSON ist in Java und TypeScript gut verarbeitbar. OpenAPI unterstützt eine überprüfbare gemeinsame Beschreibung. GraphQL würde flexible Abfragen erlauben, fügte für diesen klaren Umfang jedoch zusätzliche Komplexität bei Berechtigung, Caching und Betrieb hinzu. Eine rein interne RPC-Schnittstelle wäre weniger anschaulich für die geplanten HTTP-Lernziele.

## Konsequenzen und Überprüfung

Frontend und Backend müssen denselben Vertrag verwenden. Ein API-Test prüft nicht nur Erfolgsfälle, sondern Rechte, Fehlerformat und Konflikte. Der Prefix `/v1` ist noch keine beliebige Rückwärtskompatibilitätsgarantie: Änderungen innerhalb der Version sollen kompatibel bleiben; unvermeidbare Brüche erhalten eine dokumentierte neue Version oder einen ausdrücklich vereinbarten Migrationsweg.

OpenAPI wird mit realen Antworten und Eingaben abgeglichen. Ein Dokumentationsgenerator allein erkennt nicht jede fachliche Abweichung.
