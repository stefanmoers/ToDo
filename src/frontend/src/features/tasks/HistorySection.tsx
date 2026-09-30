import { useState, type FormEvent } from 'react'
import { createComment } from '../../api/tasks'
import { useAuth } from '../auth/AuthContext'
import { describeActivity } from './activityMessages'
import { mapTaskApiError } from './errors'
import { formatTimestamp } from './format'
import { useHistory } from './useHistory'

/**
 * US-080 (Kommentieren) und US-081 (Änderungen nachvollziehen) in einer gemeinsamen, chronologischen
 * Zeitleiste. Einträge sind nicht durch Benutzer editierbar; nur das Hinzufügen eines neuen Kommentars ist
 * möglich.
 */
export function HistorySection({ taskId }: { taskId: string }) {
  const { user } = useAuth()
  const { state, addComment } = useHistory(taskId)
  const [content, setContent] = useState('')
  const [error, setError] = useState<string>()
  const [posting, setPosting] = useState(false)

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setPosting(true)
    setError(undefined)
    try {
      const comment = await createComment(taskId, content.trim())
      addComment(comment)
      setContent('')
    }
    catch (err) {
      const mapped = mapTaskApiError(err)
      setError(mapped.errors.content ?? mapped.general ?? 'Es ist ein Fehler aufgetreten.')
    }
    finally {
      setPosting(false)
    }
  }

  if (state.kind === 'loading') {
    return (
      <section aria-labelledby="history-heading">
        <h2 id="history-heading">Verlauf</h2>
        <p role="status">Verlauf wird geladen …</p>
      </section>
    )
  }
  if (state.kind === 'error') {
    return (
      <section aria-labelledby="history-heading">
        <h2 id="history-heading">Verlauf</h2>
        <div role="alert">
          <p>Der Verlauf konnte nicht geladen werden. {state.message}</p>
        </div>
      </section>
    )
  }

  return (
    <section aria-labelledby="history-heading">
      <h2 id="history-heading">Verlauf</h2>
      {state.entries.length === 0 ? (
        <p>Noch keine Einträge.</p>
      ) : (
        <ol className="history-list" aria-label="Verlauf">
          {state.entries.map((entry) => (
            <li key={`${entry.kind}-${entry.id}`}>
              <p className="history-meta">
                {entry.actorDisplayName ?? 'Unbekannt'} · {formatTimestamp(entry.createdAt, user!.timeZone)}
              </p>
              {entry.kind === 'activity' ? <p>{describeActivity(entry.activity)}</p> : <p>{entry.comment.content}</p>}
            </li>
          ))}
        </ol>
      )}
      <form onSubmit={onSubmit} noValidate>
        <div className="field">
          <label htmlFor="new-comment-content">Kommentar hinzufügen</label>
          {error && (
            <p role="alert" className="field-error">
              {error}
            </p>
          )}
          <textarea id="new-comment-content" value={content} onChange={(e) => setContent(e.target.value)} />
        </div>
        <button type="submit" disabled={posting}>
          {posting ? 'Wird gesendet …' : 'Kommentieren'}
        </button>
      </form>
    </section>
  )
}
