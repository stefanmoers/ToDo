# Sicherheitsmodell

[Zur Übersicht](../README.md) · [Domänenmodell](DOMAIN_MODEL.md)

**Status:** Verbindliche Anforderungen für die Umsetzung, noch kein geprüfter Sicherheitsnachweis.

## Was wir schützen

Passwörter, Sitzungen, Kontodaten sowie private und geteilte Aufgaben dürfen nur berechtigten Personen zugänglich sein. Besonders zu prüfen sind manipulierte Objekt-IDs, Rechteänderungen während einer Sitzung, schädliche Texte und ungewollte Anfragen über eine fremde Webseite.

**Authentifizierung** beantwortet „Wer bist du?“. **Autorisierung** beantwortet „Darfst du genau diese Aufgabe bearbeiten?“. Beides wird im Backend geprüft.

## Rollen und Objektberechtigungen

| Aktion | Nicht angemeldet | Benutzer | Projekt-Eigentümer | Administrator |
|---|---|---|---|---|
| Registrieren, anmelden, Wiederherstellung anstoßen | Ja | Ja | Ja | Ja |
| Eigenes Profil ändern | Nein | Ja | Ja | Nur eigenes |
| Persönliche Aufgabe lesen/ändern | Nein | Nur Ersteller | Nur Ersteller | Nur Ersteller |
| Projektaufgaben lesen/ändern | Nein | Als Mitglied | Ja | Nur als Mitglied/Eigentümer |
| Projekt ändern, löschen, Mitglieder verwalten | Nein | Nein | Ja | Nur als Eigentümer |
| Konten auflisten und deaktivieren | Nein | Nein | Nein | Ja |
| Geschützte Betriebsinformationen | Nein | Nein | Nein | Ja |

Auch Kommentare, Suche, Tags, Zähler, Erinnerungen und Aktivitäten erben den Zugriffsschutz. Listen werden vor Seiteneinteilung und Zählung gefiltert. Fremde oder nicht vorhandene fachliche Objekte liefern gleichermaßen `404`, um ihre Existenz nicht preiszugeben. Fehlende Anmeldung liefert `401`, eine verbotene bekannte Aktion `403`.

## Session, Cookie und CSRF

Spring Security verwaltet serverseitige Sessions. Das Session-Cookie trägt `HttpOnly`, im HTTPS-Betrieb `Secure` und `SameSite=Lax`. Der lokale HTTP-Lernbetrieb darf `Secure` ausschließlich im ausdrücklich lokalen Profil abschalten. Session-IDs wechseln nach erfolgreicher Anmeldung; Logout entwertet die Sitzung serverseitig und entfernt das Cookie.

Schreibende Anfragen benötigen einen gültigen CSRF-Token, auch Login und Logout. Geplant ist `GET /api/v1/auth/csrf`: Der Browser erhält den zur Sitzung gehörenden Token mit dem vorgesehenen Headernamen und sendet ihn im genannten Header zurück. Nach Login/Logout wird ein frischer Token abgerufen; eine Sitzung kann dabei wechseln. Der Endpoint darf ohne Anmeldung, aber nur über die eigene Origin lesbar sein. Tokenantworten werden nicht gecacht. Die konkrete Integration muss zur gewählten Spring-Security-Version passen und mit echten Browsertests nachgewiesen werden. [Spring Security: CSRF und JavaScript-Integration](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html)

Session-IDs oder Zugriffstokens gehören nicht in `localStorage`. CORS bleibt im vorgesehenen Betrieb unnötig; bei einer späteren Trennung der Origins nur explizit erlaubte Origins verwenden, niemals eine beliebige Origin mit Credentials spiegeln. `SameSite` ersetzt keinen CSRF-Schutz.

Arbeitsannahme: 30 Minuten Inaktivität beenden die Sitzung (`SESSION_TIMEOUT`). **Umgesetzt (US-023):** Die Oberfläche warnt 2 Minuten vor Ablauf zugänglich (Dialog mit „Angemeldet bleiben“/„Jetzt abmelden“); „Angemeldet bleiben“ löst eine echte Anfrage aus, die auch den Server-Timer zurücksetzt. Sitzungsablauf wird nie als erfolgreiches Speichern dargestellt. Offene Lücke: Läuft die Sitzung während einer Bearbeitung tatsächlich ab (401 bei einer anderen Anfrage, nicht über die Warnung erkannt), geht der ungesendete Formularentwurf beim Sprung zur Anmeldeseite noch verloren; das Bewahren von Entwürfen über eine erneute Anmeldung hinweg ist noch nicht umgesetzt.

## Passwörter und Wiederherstellung

- Passwörter werden ausschließlich mit einem adaptiven, gesalzenen Passwort-Hash gespeichert. Spring Security `PasswordEncoder` mit Argon2id ist die Arbeitsannahme; die benötigte Bibliothek und Parameter werden passend zur Umgebung geprüft und dokumentiert.
- Arbeitsannahme für dieses Projekt: 15–128 Unicode-Codepoints; Leerzeichen, Einfügen und Passwortmanager zulassen. Keine stillen Kürzungen, keine Pflichtmischung aus Zeichenklassen. Passwort weder trimmen noch normalisieren.
- Passwort-Hashes, Klartextpasswörter und Reset-Tokens erscheinen nie in API-Antworten oder Logs. Implementiere keine eigene Kryptografie. [OWASP: Passwortspeicherung](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)
- Passwortänderung verlangt das bisherige Passwort. Nach Änderung oder Reset alle bestehenden Sitzungen entwerten und erneute Anmeldung verlangen.
- Wiederherstellung liefert unabhängig von der Existenz einer Adresse dieselbe Meldung. Zufällige Reset-Tokens werden nur gehasht gespeichert, sind einmal nutzbar und verfallen nach 30 Minuten.
- E-Mail-Versand darf im lokalen Lernprofil über einen nur lokal erreichbaren Mailfänger simuliert werden. Reset-Links nicht in allgemeine Logs schreiben. Vor netzwerköffentlichem Betrieb muss der reale Versandweg eingerichtet und geprüft sein.
- Anmeldung und Wiederherstellung erhalten Ratenbegrenzung. Grenzwerte werden konfiguriert und getestet; keine dauerhafte Kontosperre, die Dritte beliebig auslösen können.

Die Anmeldung meldet für unbekannte, inaktive und falsch authentifizierte Konten denselben allgemeinen Fehler. Bei Registrierung muss eine doppelte E-Mail verständlich abgewiesen werden; diese gezielte Rückmeldung ist zu unterscheiden von anonymer Kontosuche.

## Deaktivierung und erste Administration

Eine Deaktivierung verhindert neue Anmeldungen und beendet bestehende Sitzungen spätestens bei der nächsten geschützten Anfrage. Reaktivierung stellt alte Sitzungen nicht wieder her. Keine Selbstzuweisung von `ADMIN` durch Registrierungsdaten oder allgemeine Profilfelder.

Ein erstmaliges Administratorkonto wird später durch einen lokal ausgeführten, dokumentierten Bootstrap-Vorgang eingerichtet; der konkrete Aufruf ist Teil von US-014. Kein voreingestelltes Administratorkennwort, kein allgemein erreichbarer Bootstrap-HTTP-Endpunkt. Das letzte aktive Administratorkonto darf sich nicht selbst deaktivieren.

## Eingaben, Daten und Betrieb

- Alle Eingaben serverseitig validieren; begrenzte Seitengrößen und erlaubte Sortierfelder verwenden. SQL ausschließlich parametrisieren.
- Text wird in React als Text gerendert. Kein ungeprüftes HTML, kein `dangerouslySetInnerHTML` für Benutzereingaben. Bei späterem Rich Text ist ein eigenes Sicherheitskonzept nötig.
- Nur erlaubte Felder aus Requests übernehmen; Benutzer-ID, Rolle, Ersteller und Zeitstempel nicht blind aus dem Request binden.
- HTTP-Sicherheitsheader einschließlich passender Content Security Policy planen. CSP gegen den tatsächlichen Build testen; HTTPS-Betrieb erhält HSTS erst nach bestätigter HTTPS-Konfiguration.
- API-Antworten mit privaten Daten nicht öffentlich cachen. Fehler enthalten stabile Codes und Anfragekennung, aber keine SQL-Details, Stacktraces oder fremden Daten.
- `.env` und Secret-Dateien bleiben außerhalb von Git. `.env.example` enthält nur Platzhalter. Keine Secrets als `VITE_*`: Diese Werte sind im ausgelieferten Browsercode sichtbar.
- Im Compose-Betrieb sind Datenbank und Backend nur intern erreichbar; lediglich `web` bindet im Lernprofil an localhost. Das Entwicklungsprofil darf PostgreSQL nur an `127.0.0.1` veröffentlichen.
- Der öffentliche Healthcheck gibt nur einen Gesamtzustand aus. Andere Actuator-Endpunkte sind nicht öffentlich; insbesondere keine freien `env`, `configprops`, Heapdumps oder Logdateien.
- Logs erfassen Zeit, Schweregrad, Komponente und Anfragekennung, gegebenenfalls technische Benutzer-ID. Keine Aufgabenbeschreibungen, Cookies, Zugangsdaten oder vollständigen Request-Bodies. Personenbezug minimieren.
- Sicherungen schützen wie Originaldaten, Zugriff beschränken und Wiederherstellung testen. Beispiel- und Testdaten sind synthetisch.

## Abnahmefälle

Mindestens zwei Benutzer und ein Administrator prüfen: Fremde ID lesen/ändern/löschen; Suche nach fremdem Text; Zugriff nach Entfernung aus Projekt; Zugriff nach Deaktivierung und Logout; fehlender/falscher CSRF-Token; wiederverwendeter Reset-Token; HTML/SQL als Eingabe; Rollenerhöhung durch Request-Manipulation. Erwartung und tatsächliches Ergebnis gehören in Tests bzw. Abnahmeprotokoll.
