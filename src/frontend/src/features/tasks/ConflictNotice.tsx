import { useEffect, useRef } from 'react'
import type { Task } from '../../api/tasks'
import { diffDrafts, toDraft, type TaskDraft } from './draft'
import { TASK_FIELD_LABELS } from './labels'

/**
 * Zeigt einen Versionskonflikt (US-032, US-120) an: die eigenen Eingaben bleiben im Formular erhalten,
 * bis sich der Benutzer bewusst für „aktuelle Fassung“ oder „meine Eingaben“ entscheidet.
 */
export function ConflictNotice({
  draft,
  server,
  deleted,
  viewerTimeZone,
  onUseServer,
  onKeepMine,
  focusToken,
}: {
  draft: TaskDraft
  server?: Task
  deleted?: boolean
  viewerTimeZone: string
  onUseServer: (server: Task) => void
  onKeepMine: (server: Task) => void
  focusToken: number
}) {
  const ref = useRef<HTMLDivElement>(null)
  useEffect(() => {
    if (focusToken > 0) ref.current?.focus()
  }, [focusToken])

  if (deleted) {
    return (
      <div ref={ref} tabIndex={-1} role="alert" className="conflict" aria-labelledby="conflict-title">
        <h2 id="conflict-title">Die Aufgabe wurde inzwischen gelöscht</h2>
        <p>Deine Eingaben wurden nicht gespeichert.</p>
      </div>
    )
  }
  if (!server) return null

  const serverDraft = toDraft(server, viewerTimeZone)
  const differing = diffDrafts(draft, serverDraft)

  return (
    <div ref={ref} tabIndex={-1} role="alert" className="conflict" aria-labelledby="conflict-title">
      <h2 id="conflict-title">Die Aufgabe wurde zwischenzeitlich geändert</h2>
      <p>
        Jemand – vielleicht du in einem anderen Fenster – hat diese Aufgabe gespeichert, nachdem du sie geöffnet hast.
        Deine Eingaben wurden <strong>nicht</strong> gespeichert und stehen weiterhin im Formular.
      </p>
      {differing.map((field) => (
        <div key={field}>
          <h3>{TASK_FIELD_LABELS[field] ?? field}</h3>
          <dl>
            <dt>Deine Eingabe</dt>
            <dd>{String(draft[field as keyof TaskDraft]) || '(leer)'}</dd>
            <dt>Aktuell gespeichert</dt>
            <dd>{String(serverDraft[field as keyof TaskDraft]) || '(leer)'}</dd>
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
