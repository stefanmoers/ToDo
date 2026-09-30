import { useState, type FormEvent } from 'react'
import { createSubtask, deleteSubtask, moveSubtask, renameSubtask, setSubtaskDone, type Subtask } from '../../api/tasks'
import { mapTaskApiError } from './errors'
import { useSubtasks } from './useSubtasks'

/** US-070 (verwalten) und US-071 (Fortschritt): Rechte richten sich nach der Hauptaufgabe (Backend prüft das). */
export function SubtasksSection({ taskId }: { taskId: string }) {
  const { state, applyList } = useSubtasks(taskId)
  const [title, setTitle] = useState('')
  const [addError, setAddError] = useState<string>()
  const [adding, setAdding] = useState(false)
  const [rowError, setRowError] = useState<{ id: string; message: string } | undefined>()
  const [busyId, setBusyId] = useState<string | null>(null)
  const [renamingId, setRenamingId] = useState<string | null>(null)
  const [renameValue, setRenameValue] = useState('')

  async function onAddSubmit(e: FormEvent) {
    e.preventDefault()
    setAdding(true)
    setAddError(undefined)
    try {
      const list = await createSubtask(taskId, title.trim())
      applyList(list)
      setTitle('')
    }
    catch (err) {
      const mapped = mapTaskApiError(err)
      setAddError(mapped.errors.title ?? mapped.general ?? 'Es ist ein Fehler aufgetreten.')
    }
    finally {
      setAdding(false)
    }
  }

  async function run(id: string, action: () => Promise<Subtask[]>) {
    setBusyId(id)
    setRowError(undefined)
    try {
      const list = await action()
      applyList(list)
    }
    catch (err) {
      setRowError({ id, message: mapTaskApiError(err).general ?? 'Es ist ein Fehler aufgetreten.' })
    }
    finally {
      setBusyId(null)
    }
  }

  // Setzt den Erledigt-Zustand sofort lokal (bevor die Antwort da ist): eine Checkbox ist ein natives,
  // vom Browser sofort umgeschaltetes Element — ohne das würde der nächste Render sie kurz auf den alten
  // (noch nicht bestätigten) Wert zurückspringen lassen, bis die Antwort eintrifft.
  async function toggleDone(current: Subtask, done: boolean) {
    if (state.kind !== 'loaded') return
    applyList(state.subtasks.map((s) => (s.id === current.id ? { ...s, done } : s)))
    setBusyId(current.id)
    setRowError(undefined)
    try {
      const list = await setSubtaskDone(taskId, current.id, done)
      applyList(list)
    }
    catch (err) {
      applyList(state.subtasks.map((s) => (s.id === current.id ? { ...s, done: current.done } : s)))
      setRowError({ id: current.id, message: mapTaskApiError(err).general ?? 'Es ist ein Fehler aufgetreten.' })
    }
    finally {
      setBusyId(null)
    }
  }

  async function submitRename(e: FormEvent, id: string) {
    e.preventDefault()
    const value = renameValue.trim()
    await run(id, () => renameSubtask(taskId, id, value))
    setRenamingId(null)
  }

  if (state.kind === 'loading') {
    return (
      <section aria-labelledby="subtasks-heading">
        <h2 id="subtasks-heading">Unteraufgaben</h2>
        <p role="status">Unteraufgaben werden geladen …</p>
      </section>
    )
  }
  if (state.kind === 'error') {
    return (
      <section aria-labelledby="subtasks-heading">
        <h2 id="subtasks-heading">Unteraufgaben</h2>
        <div role="alert">
          <p>Die Unteraufgaben konnten nicht geladen werden. {state.message}</p>
        </div>
      </section>
    )
  }

  const { subtasks } = state
  const doneCount = subtasks.filter((s) => s.done).length

  return (
    <section aria-labelledby="subtasks-heading">
      <h2 id="subtasks-heading">Unteraufgaben</h2>
      {subtasks.length > 0 && (
        <p>
          {doneCount} von {subtasks.length} erledigt
        </p>
      )}
      {subtasks.length === 0 ? (
        <p>Noch keine Unteraufgaben.</p>
      ) : (
        <ul className="task-list" aria-label="Unteraufgaben">
          {subtasks.map((s, index) => {
            const busy = busyId === s.id
            return (
              <li key={s.id} className={s.done ? 'task done' : 'task'}>
                <input
                  type="checkbox"
                  id={`subtask-done-${s.id}`}
                  aria-label={s.title}
                  checked={s.done}
                  disabled={busy}
                  onChange={(e) => toggleDone(s, e.target.checked)}
                />{' '}
                {renamingId === s.id ? (
                  <form onSubmit={(e) => submitRename(e, s.id)}>
                    <label htmlFor={`subtask-rename-${s.id}`} className="visually-hidden">
                      Titel von „{s.title}“ ändern
                    </label>
                    <input
                      id={`subtask-rename-${s.id}`}
                      value={renameValue}
                      onChange={(e) => setRenameValue(e.target.value)}
                    />
                    <button type="submit" disabled={busy}>
                      Speichern
                    </button>
                    <button type="button" onClick={() => setRenamingId(null)} disabled={busy}>
                      Abbrechen
                    </button>
                  </form>
                ) : (
                  <label htmlFor={`subtask-done-${s.id}`} className="task-title">
                    {s.title}
                  </label>
                )}
                {rowError?.id === s.id && (
                  <p role="alert" className="field-error">
                    {rowError.message}
                  </p>
                )}
                {renamingId !== s.id && (
                  <div className="actions">
                    <button
                      type="button"
                      onClick={() => {
                        setRenamingId(s.id)
                        setRenameValue(s.title)
                      }}
                      disabled={busy}
                    >
                      Umbenennen<span className="visually-hidden">: {s.title}</span>
                    </button>
                    <button
                      type="button"
                      onClick={() => run(s.id, () => moveSubtask(taskId, s.id, 'UP'))}
                      disabled={busy || index === 0}
                    >
                      Nach oben<span className="visually-hidden">: {s.title}</span>
                    </button>
                    <button
                      type="button"
                      onClick={() => run(s.id, () => moveSubtask(taskId, s.id, 'DOWN'))}
                      disabled={busy || index === subtasks.length - 1}
                    >
                      Nach unten<span className="visually-hidden">: {s.title}</span>
                    </button>
                    <button
                      type="button"
                      className="danger"
                      onClick={() => run(s.id, () => deleteSubtask(taskId, s.id))}
                      disabled={busy}
                    >
                      Löschen<span className="visually-hidden">: {s.title}</span>
                    </button>
                  </div>
                )}
              </li>
            )
          })}
        </ul>
      )}
      <form onSubmit={onAddSubmit} noValidate>
        <div className="field">
          <label htmlFor="new-subtask-title">Neue Unteraufgabe</label>
          {addError && (
            <p role="alert" className="field-error">
              {addError}
            </p>
          )}
          <input id="new-subtask-title" value={title} onChange={(e) => setTitle(e.target.value)} />
        </div>
        <button type="submit" disabled={adding}>
          {adding ? 'Wird hinzugefügt …' : 'Hinzufügen'}
        </button>
      </form>
    </section>
  )
}
