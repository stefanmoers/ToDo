import { useEffect, useRef } from 'react'
import type { Project } from '../../api/projects'
import { diffDrafts, toDraft, type ProjectDraft } from './draft'
import { PROJECT_FIELD_LABELS } from './labels'

export function ConflictNotice({
  draft,
  server,
  deleted,
  onUseServer,
  onKeepMine,
  focusToken,
}: {
  draft: ProjectDraft
  server?: Project
  deleted?: boolean
  onUseServer: (server: Project) => void
  onKeepMine: (server: Project) => void
  focusToken: number
}) {
  const ref = useRef<HTMLDivElement>(null)
  useEffect(() => {
    if (focusToken > 0) ref.current?.focus()
  }, [focusToken])

  if (deleted) {
    return (
      <div ref={ref} tabIndex={-1} role="alert" className="conflict" aria-labelledby="conflict-title">
        <h2 id="conflict-title">Das Projekt wurde inzwischen gelöscht</h2>
        <p>Deine Eingaben wurden nicht gespeichert.</p>
      </div>
    )
  }
  if (!server) return null

  const serverDraft = toDraft(server)
  const differing = diffDrafts(draft, serverDraft)

  return (
    <div ref={ref} tabIndex={-1} role="alert" className="conflict" aria-labelledby="conflict-title">
      <h2 id="conflict-title">Das Projekt wurde zwischenzeitlich geändert</h2>
      <p>
        Deine Eingaben wurden <strong>nicht</strong> gespeichert und stehen weiterhin im Formular.
      </p>
      {differing.map((field) => (
        <div key={field}>
          <h3>{PROJECT_FIELD_LABELS[field] ?? field}</h3>
          <dl>
            <dt>Deine Eingabe</dt>
            <dd>{String(draft[field as keyof ProjectDraft]) || '(leer)'}</dd>
            <dt>Aktuell gespeichert</dt>
            <dd>{String(serverDraft[field as keyof ProjectDraft]) || '(leer)'}</dd>
          </dl>
        </div>
      ))}
      <div className="actions">
        <button type="button" className="secondary" onClick={() => onUseServer(server)}>
          Aktuelle Fassung übernehmen (meine Eingaben verwerfen)
        </button>
        <button type="button" className="secondary" onClick={() => onKeepMine(server)}>
          Meine Eingaben behalten
        </button>
      </div>
    </div>
  )
}
