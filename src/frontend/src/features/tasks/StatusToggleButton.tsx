import { useState } from 'react'
import { completeTask, reopenTask, type Task } from '../../api/tasks'
import { isConflict, isNotFound } from './errors'

/** Erledigen/Wieder-öffnen (US-034, US-035). Bei 409 wird der Elternkomponente signalisiert, neu zu laden. */
export function StatusToggleButton({
  task,
  onChanged,
  onConflict,
  onGone,
}: {
  task: Task
  onChanged: (task: Task) => void
  onConflict: () => void
  onGone: () => void
}) {
  const [busy, setBusy] = useState(false)
  const done = task.status === 'DONE'

  async function onClick() {
    if (busy) return
    setBusy(true)
    try {
      const updated = done ? await reopenTask(task.id, task.version) : await completeTask(task.id, task.version)
      onChanged(updated)
    } catch (err) {
      if (isConflict(err)) onConflict()
      else if (isNotFound(err)) onGone()
    } finally {
      setBusy(false)
    }
  }

  return (
    <button type="button" aria-disabled={busy} className="secondary" onClick={onClick}>
      {done ? 'Wieder öffnen' : 'Als erledigt markieren'}
      <span className="visually-hidden">: {task.title}</span>
    </button>
  )
}
