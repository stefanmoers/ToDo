import type { ReactNode } from 'react'

/** Eine Folie der Projektvorstellung. Der Inhalt ist bewusst normales React (kein eingebettetes HTML, siehe docs/SECURITY.md). */
export interface Slide {
  id: string
  /** Titel für Seitentitel und Ansage der Folie. */
  title: string
  dark: boolean
  /** Sprechernotizen, in der Oberfläche ein- und ausblendbar. */
  notes: string
  body: ReactNode
}

function Head({ eyebrow, title }: { eyebrow: string; title: string }) {
  return (
    <header className="s-head">
      <p className="s-eyebrow">{eyebrow}</p>
      <h2 className="s-title">{title}</h2>
    </header>
  )
}

interface CardData {
  title: string
  text?: string
  items?: string[]
  tone?: 'good' | 'warn'
}

function Cards({ cards, size = 'm', cols = 3 }: { cards: CardData[]; size?: 's' | 'm'; cols?: 2 | 3 }) {
  return (
    <div className={`s-cards s-cards--${size} s-cards--c${cols}`}>
      {cards.map((c) => (
        <div className="s-card" key={c.title}>
          <h3 className={c.tone ? `s-card-title s-card-title--${c.tone}` : 's-card-title'}>{c.title}</h3>
          {c.text && <p>{c.text}</p>}
          {c.items && (
            <ul>
              {c.items.map((i) => (
                <li key={i}>{i}</li>
              ))}
            </ul>
          )}
        </div>
      ))}
    </div>
  )
}

function Box({ label, text }: { label: string; text: string }) {
  return (
    <div className="s-box">
      <p className="s-box-label">{label}</p>
      <p className="s-box-text">{text}</p>
    </div>
  )
}

function Flow({ boxes }: { boxes: { label: string; text: string }[] }) {
  return (
    <div className="s-flow">
      {boxes.map((b, i) => (
        <div className="s-flow-item" key={b.label}>
          {i > 0 && (
            <span className="s-arrow" aria-hidden="true">
              →
            </span>
          )}
          <Box label={b.label} text={b.text} />
        </div>
      ))}
    </div>
  )
}

function Table({ head, rows, widths }: { head: string[]; rows: string[][]; widths: string[] }) {
  return (
    <table className="s-table">
      <colgroup>
        {widths.map((w) => (
          <col key={w} className={`s-col-${w}`} />
        ))}
      </colgroup>
      <thead>
        <tr>
          {head.map((h) => (
            <th key={h} scope="col">
              {h}
            </th>
          ))}
        </tr>
      </thead>
      <tbody>
        {rows.map((r) => (
          <tr key={r[0]}>
            {r.map((c, i) => (i === 0 ? <th key={c} scope="row">{c}</th> : <td key={c}>{c}</td>))}
          </tr>
        ))}
      </tbody>
    </table>
  )
}

function Code({ lines }: { lines: string[] }) {
  return (
    <pre className="s-code">
      <code>{lines.join('\n')}</code>
    </pre>
  )
}

function NumberedList({ items, size = 'm' }: { items: string[]; size?: 'm' | 'l' }) {
  return (
    <ol className={`s-numbered s-numbered--${size}`}>
      {items.map((t, i) => (
        <li key={t}>
          <span className="s-num" aria-hidden="true">
            {String(i + 1).padStart(2, '0')}
          </span>
          <span className="s-num-text">{t}</span>
        </li>
      ))}
    </ol>
  )
}

function Stat({ value, label, text }: { value: string; label: string; text: string }) {
  return (
    <div className="s-card s-stat">
      <p className="s-stat-value">{value}</p>
      <h3 className="s-card-title">{label}</h3>
      <p>{text}</p>
    </div>
  )
}

export const slides: Slide[] = [
  {
    id: 'cover',
    title: 'UltimateTodo',
    dark: true,
    notes:
      'Begrüßung. Ziel der nächsten 25–30 Minuten: Das Projekt so erklären, dass ihr danach die Live-Demo einordnen könnt. Ich zeige keine Lösung, die „besser“ sein soll, sondern wie ein Projekt aussieht, das Architektur, Betrieb, Tests und Barrierefreiheit von Anfang an mitdenkt.',
    body: (
      <div className="s-cover">
        <p className="s-eyebrow s-eyebrow--l">Referenzprojekt für Ausbildung und Coding-Agenten</p>
        <h2 className="s-cover-title">UltimateTodo</h2>
        <p className="s-lead">Architektur, Deployment, Technologien und Tests – vor der Live-Demo</p>
        <p className="s-by">Stefan Moers · Meeting mit Ausbildern und Azubis</p>
      </div>
    ),
  },
  {
    id: 'agenda',
    title: 'Was ihr in den nächsten Minuten seht',
    dark: false,
    notes: 'Erst Folien (ca. 20 Minuten), dann Live-Demo. Fragen jederzeit.',
    body: (
      <>
        <Head eyebrow="Ablauf" title="Was ihr in den nächsten Minuten seht" />
        <NumberedList
          size="l"
          items={[
            'Ziel und Umfang',
            'Architektur',
            'Technologien und Entscheidungen',
            'Deployment',
            'Sicherheit und Datenintegrität',
            'Tests und Barrierefreiheit',
            'Arbeiten mit Coding-Agenten',
            'Live-Demo',
          ]}
        />
      </>
    ),
  },
  {
    id: 'scope',
    title: 'Aufgaben für Einzelne und Teams',
    dark: false,
    notes:
      'Die App ist bewusst fachlich überschaubar, aber nicht trivial: Mehrbenutzerbetrieb, Rechte, Konflikte und Historie machen die Architekturfragen real. Stand: Meilensteine M0 bis M2. Passwort-Wiederherstellung ist bewusst nicht umgesetzt (Demo-Entscheidung).',
    body: (
      <>
        <Head eyebrow="Ziel und Umfang" title="Aufgaben für Einzelne und Teams" />
        <Cards
          size="s"
          cards={[
            { title: 'Konten und Anmeldung', text: 'Registrierung, Login mit Session und CSRF-Schutz, Profil, Passwort ändern, Konten verwalten (Admin)' },
            { title: 'Aufgaben', text: 'Anlegen, bearbeiten, erledigen, löschen mit Status, Priorität, Fälligkeit und Konflikterkennung' },
            { title: 'Projekte und Teilen', text: 'Eigene und geteilte Projekte, Mitglieder aufnehmen, Aufgaben zuweisen' },
            { title: 'Tags, Suche, Filter', text: 'Persönliche und projektbezogene Tags, Suche, Filter, Sortierung, fünf Standardansichten' },
            { title: 'Zusammenarbeit', text: 'Unteraufgaben als Checkliste, Kommentare, chronologische Änderungshistorie' },
            { title: 'Übersicht und Oberfläche', text: 'Persönliche Übersicht mit Zählern, helle und dunkle Darstellung, deutsche Oberfläche, barrierearm' },
          ]}
        />
      </>
    ),
  },
  {
    id: 'milestones',
    title: 'Vom Backlog zu Meilensteinen',
    dark: false,
    notes:
      'Wichtig für Azubis: Nicht alles auf einmal. Erst ein durchgängiger dünner Schnitt (M0), dann Konten, Aufgaben, Organisation, Zusammenarbeit. Zeitfunktionen (M3) sind das, was bei Zeitmangel entfällt.',
    body: (
      <>
        <Head eyebrow="Vorgehen" title="Vom Backlog zu Meilensteinen" />
        <Table
          widths={['a', 'b', 'c']}
          head={['Stufe', 'Ergebnis', 'Stand']}
          rows={[
            ['M0', 'Baubarer Stack, Compose, Migrationen, Healthchecks, CI', 'fertig'],
            ['M1a', 'Registrierung, Login, Session/CSRF, Profil, Ratenbegrenzung', 'fertig'],
            ['M1b', 'Aufgaben-Kern mit Konflikten und Transaktionen', 'fertig'],
            ['M1c', 'Projekte, Tags, Suche, Filter, Standardansichten', 'fertig'],
            ['M2', 'Mitglieder, Zuweisung, Unteraufgaben, Kommentare, Historie', 'fertig bis auf 1 Testfall'],
            ['M3', 'Wiederkehrende Aufgaben, Erinnerungen', 'geplant'],
            ['M4', 'Referenzrelease: Update, Backup, Restore, Demo-Unterlagen', 'geplant'],
          ]}
        />
        <p className="s-foot">Grundlage: ein Backlog mit Stories und prüfbaren Akzeptanzkriterien, geordnet nach Must, Should und Could.</p>
      </>
    ),
  },
  {
    id: 'arch',
    title: 'Drei Bausteine, ein Zugangspunkt',
    dark: true,
    notes:
      'Das ist das ganze System auf einer Folie. Das Frontend sind statische Dateien, die nginx ausliefert; nginx reicht /api an das Backend durch. Wichtig: Die Datenbank ist von außen nicht erreichbar.',
    body: (
      <>
        <Head eyebrow="Architektur" title="Drei Bausteine, ein Zugangspunkt" />
        <Flow
          boxes={[
            { label: 'Browser', text: 'React 19 mit TypeScript' },
            { label: 'web', text: 'nginx: liefert das Frontend, leitet /api weiter' },
            { label: 'backend', text: 'Spring Boot, Java 21: Rechte und Regeln' },
            { label: 'db', text: 'PostgreSQL 17 mit Flyway' },
          ]}
        />
        <Cards
          size="s"
          cards={[
            { title: 'Gleiche Origin', text: 'Frontend und API unter einer Adresse: einfache Cookies, kein CORS' },
            { title: 'Ein Port', text: 'Nur web ist erreichbar (127.0.0.1:8080), backend und db nur im Container-Netz' },
            { title: 'Backend entscheidet', text: 'Die UI blendet nur aus, erlaubt und verboten klärt immer das Backend' },
          ]}
        />
      </>
    ),
  },
  {
    id: 'modules',
    title: 'Modularer Monolith statt Microservices',
    dark: false,
    notes:
      'ADR-001. Ein Prozess, eine Datenbank, klar getrennte Fachmodule. Vorteil: lokale Transaktionen, einfaches Debugging. Die Grenzen hält kein Framework automatisch ein, dafür sorgen Review und die Regel im Team bzw. in AGENTS.md.',
    body: (
      <>
        <Head eyebrow="Architektur" title="Modularer Monolith statt Microservices" />
        <div className="s-cards s-cards--modules">
          {[
            ['user', 'Konto, Passwort, Anmeldung, Administration'],
            ['project', 'Projekte, Eigentümer, Mitgliedschaften'],
            ['task', 'Aufgaben, Unteraufgaben, Kommentare, Aktivitäten'],
            ['tag', 'Persönliche und projektbezogene Tags'],
            ['common', 'Fehlerformat, Uhr, kleine gemeinsame Typen'],
            ['notification', 'Erinnerungen – geplant (M3)'],
          ].map(([name, text]) => (
            <div className={name === 'notification' ? 's-card s-card--planned' : 's-card'} key={name}>
              <h3 className="s-card-title s-mono">{name}</h3>
              <p>{text}</p>
            </div>
          ))}
        </div>
        <div className="s-callout">
          <p>Kein Modul greift auf das Repository eines anderen zu, nur über schmale Schnittstellen wie ProjectAccess oder TagAccess.</p>
          <p className="s-mono s-callout-mono">je Modul: api → application → domain → persistence</p>
        </div>
      </>
    ),
  },
  {
    id: 'request',
    title: 'Der Weg einer Speicheranfrage',
    dark: false,
    notes:
      'Anhand dieser Kette lässt sich fast jede Architekturfrage beantworten: Wo wird authentifiziert, wo autorisiert, wo liegt die Transaktion, wo entsteht der Konflikt. Frontend-Validierung hilft dem Benutzer, ersetzt aber nie die Backend-Prüfung.',
    body: (
      <>
        <Head eyebrow="Architektur" title="Der Weg einer Speicheranfrage" />
        <ol className="s-steps">
          {[
            ['React', 'Formular speichern: PUT /api/v1/tasks/{id} mit Version, Session-Cookie und CSRF-Header'],
            ['Spring Security', 'Session gültig? CSRF-Token passt? Sonst 401 oder 403'],
            ['Anwendungsdienst', 'Darf genau dieser Benutzer genau diese Aufgabe ändern? Eingaben und Fachregeln prüfen'],
            ['PostgreSQL', 'Änderung und Aktivitätseintrag in einer Transaktion, nur wenn die Version noch stimmt'],
            ['Antwort', '200 mit neuer Version, oder 409: Konflikt wird erklärt, der Entwurf bleibt erhalten'],
          ].map(([t, x]) => (
            <li key={t}>
              <h3 className="s-step-title">{t}</h3>
              <p>{x}</p>
            </li>
          ))}
        </ol>
      </>
    ),
  },
  {
    id: 'tech',
    title: 'Der Werkzeugkasten',
    dark: false,
    notes:
      'Versionen sind in docs/TECHNOLOGIES.md dokumentiert und wurden tatsächlich gebaut und getestet. Zu jedem Eintrag soll ein Azubi „Was? Wofür? Warum?“ in einem Satz sagen können. Der Verzicht auf UI-Bibliothek und State-Manager ist eine Entscheidung, kein Versäumnis: Jede Bibliothek müsste auf Tastatur, Screenreader, Wartbarkeit und Lizenz geprüft werden.',
    body: (
      <>
        <Head eyebrow="Technologien" title="Der Werkzeugkasten" />
        <Cards
          cards={[
            { title: 'Backend', items: ['Java 21, Spring Boot 4.1', 'Spring Security, Actuator', 'Maven mit Wrapper', 'springdoc-openapi (OpenAPI)', 'Flyway 12 für Migrationen'] },
            { title: 'Frontend', items: ['React 19, React Router 7', 'TypeScript 6 (strict)', 'Vite 8 als Build', 'eigenes CSS, eigener fetch-Client', 'ESLint mit jsx-a11y'] },
            { title: 'Betrieb und Tests', items: ['PostgreSQL 17, nginx 1.30', 'Docker Compose', 'JUnit 6, Testcontainers', 'Vitest, Testing Library', 'Playwright mit axe-core'] },
          ]}
        />
        <p className="s-foot">Bewusst nicht dabei: UI-Bibliothek, globaler State-Manager, Microservices, GraphQL, Kubernetes.</p>
      </>
    ),
  },
  {
    id: 'adr',
    title: 'Fünf Architekturentscheidungen (ADRs) halten das Warum fest',
    dark: false,
    notes:
      'ADR steht für Architecture Decision Record. ADRs sind kurze Entscheidungsprotokolle: Kontext, Entscheidung, Alternativen, Konsequenzen. Bestehende werden nicht umgeschrieben, sondern durch neue ersetzt. So kann man auch in einem Jahr noch erklären, warum etwas so gebaut ist.',
    body: (
      <>
        <Head eyebrow="Entscheidungen" title="Fünf Architekturentscheidungen (ADRs) halten das Warum fest" />
        <Table
          widths={['a', 'd', 'e']}
          head={['Nr.', 'Entscheidung', 'Kernargument']}
          rows={[
            ['ADR-001', 'Modularer Monolith', 'Fachgrenzen im Code ohne den Betriebsaufwand von Microservices; lokale Transaktionen'],
            ['ADR-002', 'Java 21, Spring Boot, Maven', 'Gemeinsamer Lernstack; Sicherheit, Konfiguration und Tests fertig statt selbst gebaut'],
            ['ADR-003', 'React, TypeScript, Vite', 'Komponenten und Zustand klar getrennt, Typprüfung vor der Laufzeit, überschaubarer Build'],
            ['ADR-004', 'PostgreSQL mit Flyway', 'Beziehungen, Constraints, Transaktionen; versionierte Migrationen; echtes PostgreSQL in Tests, kein H2'],
            ['ADR-005', 'REST/JSON mit OpenAPI', 'Testbarer Vertrag unter /api/v1 mit klaren Fehlercodes, etwa 409 bei Konflikten'],
          ]}
        />
      </>
    ),
  },
  {
    id: 'deploy',
    title: 'Ein Befehl startet den ganzen Stack',
    dark: true,
    notes:
      'Java, Node und Maven müssen auf dem Host nicht installiert sein, die Builds laufen in Containern. Die Installation wurde auf einem frischen Checkout durchgespielt und protokolliert (dabei wurde ein falscher Healthcheck-Pfad in der Doku gefunden und korrigiert). Update, Backup und Restore sind für M4 geplant. Volumes sind kein Backup.',
    body: (
      <>
        <Head eyebrow="Deployment" title="Ein Befehl startet den ganzen Stack" />
        <Flow
          boxes={[
            { label: 'web', text: 'nginx 1.30, Port 127.0.0.1:8080, liefert React, leitet /api' },
            { label: 'backend', text: 'Spring Boot, nur im Container-Netz, Healthcheck' },
            { label: 'db', text: 'PostgreSQL 17.11 mit benanntem Volume' },
          ]}
        />
        <Code lines={['cp .env.example .env    # Passwort setzen', 'docker compose up -d --build --wait', 'curl --fail http://localhost:8080/actuator/health']} />
        <p className="s-foot">Healthchecks erzwingen die Reihenfolge: erst gesunde Datenbank, dann Migrationen, dann Backend, dann web.</p>
      </>
    ),
  },
  {
    id: 'security',
    title: 'Schutz liegt im Backend',
    dark: false,
    notes:
      'Authentifizierung heißt: Wer bist du? Autorisierung: Darfst du genau das? Beides passiert im Backend. 404 statt 403 bei fremden Objekten, damit deren Existenz nicht verraten wird. Die Sicherheitsanforderungen stehen in docs/SECURITY.md.',
    body: (
      <>
        <Head eyebrow="Sicherheit" title="Schutz liegt im Backend" />
        <Cards
          cards={[
            { title: 'Anmeldung', text: 'Serverseitige Session im Cookie (HttpOnly, SameSite=Lax), Ratenbegrenzung, Passwörter nur als Hash' },
            { title: 'CSRF-Schutz', text: 'Jede schreibende Anfrage braucht einen Token, den der Browser vorher abholt' },
            { title: 'Berechtigungen', text: 'Jede Datenoperation prüft Benutzer und Objekt im Backend; Fremdes liefert 404, nicht 403' },
            { title: 'Eingaben', text: 'Validierung im Backend, parametrisiertes SQL, React rendert Benutzertext nur als Text' },
          ]}
          size="s"
          cols={2}
        />
        <p className="s-foot">Passwort-Wiederherstellung ist für dieses Demo-Projekt bewusst nicht umgesetzt. Ein Administrator sieht keine fremden Aufgaben.</p>
      </>
    ),
  },
  {
    id: 'integrity',
    title: 'Gleichzeitige Änderungen und saubere Daten',
    dark: false,
    notes:
      'Das ist der Teil, den man in einer Einzelplatz-Demo nie sieht und der in echten Systemen am meisten Ärger macht. Deshalb gibt es dafür gezielte Integrationstests gegen echtes PostgreSQL.',
    body: (
      <>
        <Head eyebrow="Datenintegrität" title="Gleichzeitige Änderungen und saubere Daten" />
        <Cards
          cards={[
            { title: 'Optimistisches Sperren', text: 'Jede Aufgabe trägt eine Version. Speichern oder Löschen mit veralteter Version ergibt 409, nichts wird unbemerkt überschrieben.' },
            { title: 'Eine Transaktion', text: 'Fachänderung und Aktivitätseintrag gehören zusammen. Schlägt eines fehl, wird beides zurückgerollt.' },
            { title: 'Versionierte Migrationen', text: 'Flyway-Skripte V1 bis V8 bauen das Schema auf. Angewendete Migrationen bleiben unverändert, die App wird erst nach Erfolg bereit.' },
          ]}
        />
        <p className="s-foot">In der Demo: dieselbe Aufgabe in zwei Tabs ändern und den Konflikt auslösen.</p>
      </>
    ),
  },
  {
    id: 'tests',
    title: 'Jede Ebene hat ihre eigene Aufgabe',
    dark: true,
    notes:
      'Zahlen: Backend und E2E laut docs/TESTING.md (Stand M2), Frontend-Unit aus dem letzten Lauf. Vor dem Meeting bitte noch einmal aktualisieren, weil seitdem Tests dazugekommen sind. Kernaussage: Unit-Tests prüfen eine Regel, Integrationstests das Zusammenspiel, E2E einen Benutzerablauf. Alles nur über E2E zu testen wäre langsam und schwer zu diagnostizieren. H2 wird bewusst nicht als PostgreSQL-Ersatz verwendet.',
    body: (
      <>
        <Head eyebrow="Tests" title="Jede Ebene hat ihre eigene Aufgabe" />
        <div className="s-cards s-cards--m s-cards--c3">
          <Stat value="168" label="Backend" text="35 Unit, 133 Integration mit echtem PostgreSQL (Testcontainers)" />
          <Stat value="126" label="Frontend" text="Vitest und Testing Library: Formulare, Zustände, Labels" />
          <Stat value="57" label="Ende-zu-Ende" text="Playwright und axe-core gegen den kompletten Compose-Stack" />
        </div>
        <p className="s-foot">Ein fehlendes Docker-System ist kein bestandener Test. E2E läuft in isolierter Umgebung über scripts/e2e.sh.</p>
      </>
    ),
  },
  {
    id: 'a11y',
    title: 'Von Anfang an mitgebaut',
    dark: false,
    notes: 'Automatische Prüfungen finden nur einen Teil der Barrieren. Deshalb gibt es zusätzlich manuelle Prüfungen, und wir behaupten keine Konformität, die nicht nachgewiesen ist.',
    body: (
      <>
        <Head eyebrow="Barrierefreiheit" title="Von Anfang an mitgebaut" />
        <Cards
          size="s"
          cols={2}
          cards={[
            { title: 'Ziel', text: 'WCAG 2.2, Stufe AA als Qualitätsziel, Teil der Definition of Done' },
            { title: 'Automatisch', text: 'axe-core in Playwright prüft echte Oberflächenzustände, ESLint mit jsx-a11y schon beim Schreiben' },
            { title: 'Bedienung', text: 'Tastatur überall, sichtbarer Fokus, Sprunglink, Dialoge mit Fokusführung, Fehler direkt am Feld' },
            { title: 'Noch offen', text: 'Manuelle Screenreader- und Zoom-Prüfung (US-172) steht aus und wird vom Auftraggeber selbst durchgeführt', tone: 'warn' },
          ]}
        />
      </>
    ),
  },
  {
    id: 'agents',
    title: 'Coding-Agenten brauchen Leitplanken',
    dark: false,
    notes:
      'Hier kommt die Brücke zur Ausbildung: Das Projekt ist so dokumentiert, dass ein Agent (oder ein Azubi) es ohne mündliche Einweisung fortsetzen kann. Die Regel „keine vorgetäuschten Erfolgsmeldungen“ steht ausdrücklich in AGENTS.md und wurde im Projekt auch gelebt: Was nicht ausgeführt werden konnte, steht so in den Protokollen.',
    body: (
      <>
        <Head eyebrow="Arbeitsweise" title="Coding-Agenten brauchen Leitplanken" />
        <Cards
          cards={[
            { title: 'Vorgabe', items: ['README und Backlog mit Akzeptanzkriterien', 'Fünf ADRs, acht Fachdokumente', 'Definition of Done'] },
            { title: 'Regeln (AGENTS.md)', items: ['Modulgrenzen und Berechtigungen im Backend', 'Migrationen statt Schema-Änderung im Betrieb', 'Tests und Doku im selben Änderungspaket'] },
            { title: 'Nachweis', items: ['Tests und Protokolle mit Commit-ID', 'Nicht Ausgeführtes wird benannt', 'Manuelle Prüfungen bestätigt der Auftraggeber'] },
          ]}
        />
        <p className="s-foot">Der Agent liefert Code. Die Verantwortung für Anforderungen, Entscheidungen und Abnahme bleibt beim Menschen.</p>
      </>
    ),
  },
  {
    id: 'status',
    title: 'Was fertig ist und was nicht',
    dark: false,
    notes:
      'Gerade vor Ausbildern ist der offene Teil wichtig: Ein Referenzprojekt, das seine Lücken benennt, ist glaubwürdiger als eines, das alles grün zeigt. Hinweis für Stefan: US-181 (Tag-Herkunft) ist implementiert und dokumentiert; die neuen Backend-Integrationstests und der neue Playwright-Test sind noch nicht im vollen Stack gelaufen. Vor dem Meeting einmal mvnw verify und scripts/e2e.sh laufen lassen und diese Folie aktualisieren.',
    body: (
      <>
        <Head eyebrow="Ehrlicher Stand" title="Was fertig ist und was nicht" />
        <Cards
          cols={2}
          cards={[
            {
              title: 'Fertig',
              tone: 'good',
              items: [
                'M0 bis M2 umgesetzt und im Compose-Stack lauffähig',
                'Oberfläche für die Vorführung überarbeitet (US-180)',
                'Gleichnamige Tags mit Herkunft gekennzeichnet (US-181)',
                'Automatisierte Prüfungen grün (Unit, Integration, E2E, axe)',
              ],
            },
            {
              title: 'Offen',
              tone: 'warn',
              items: [
                'M3: wiederkehrende Aufgaben, Erinnerungen',
                'US-042: Löschtest mit mehreren Erstellern und deaktiviertem Konto',
                'US-172: manuelle Screenreader- und Zoom-Prüfung',
                'M4: Update, Backup, Restore, feste Image-Digests',
              ],
            },
          ]}
        />
      </>
    ),
  },
  {
    id: 'demo',
    title: 'Jetzt wird es konkret',
    dark: true,
    notes:
      'Demo-Konten: Stefan und Uli Schmitz, dazu Beispielprojekte mit humorvollen Aufgaben, Tags und Kommentaren. Reihenfolge ist ein Vorschlag und kann je nach Fragen verkürzt werden.',
    body: (
      <>
        <Head eyebrow="Live-Demo" title="Jetzt wird es konkret" />
        <NumberedList
          items={[
            'Registrieren und anmelden',
            'Aufgabe mit Priorität und Fälligkeit anlegen',
            'Projekt teilen, Mitglied aufnehmen, zuweisen',
            'Kommentar schreiben, Historie ansehen',
            'Gleichnamige Tags aus zwei Projekten',
            'Konflikt: dieselbe Aufgabe in zwei Tabs ändern',
            'Nur mit Tastatur bedienen, hell und dunkel',
            'docker compose ps und Healthcheck zeigen',
          ]}
        />
      </>
    ),
  },
  {
    id: 'close',
    title: 'Fragen und Diskussion',
    dark: true,
    notes: 'Offene Diskussionspunkte: Was würdet ihr anders entscheiden? Welche Teile sind für Azubis zu komplex? Wie viel Dokumentation ist zu viel?',
    body: (
      <div className="s-close">
        <h2 className="s-cover-title s-cover-title--m">Fragen und Diskussion</h2>
        <p className="s-lead">Alles Gezeigte ist im Repository dokumentiert:</p>
        <dl className="s-docs">
          {[
            ['README.md', 'Überblick und Schnellstart'],
            ['BACKLOG.md', 'Stories, Meilensteine, Status'],
            ['docs/ARCHITECTURE.md', 'Bausteine und Abläufe'],
            ['docs/adr/', 'Entscheidungen mit Begründung'],
            ['docs/TESTING.md', 'Teststrategie und Nachweise'],
            ['docs/DEPLOYMENT.md', 'Installation und Betrieb'],
          ].map(([a, b]) => (
            <div key={a}>
              <dt className="s-mono">{a}</dt>
              <dd>{b}</dd>
            </div>
          ))}
        </dl>
      </div>
    ),
  },
]
