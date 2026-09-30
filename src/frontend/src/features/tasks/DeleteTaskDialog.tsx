import { useRef, useState, type RefObject } from 'react'
import { deleteTask, type Task } from '../../api/tasks'
import { Dialog } from '../../components/Dialog'
import { isConflict, isNotFound } from './errors'

/** Löschbestätigung (US-033) nach dem WAI-ARIA-Dialogmuster; baut auf dem nativen Dialog-Baustein auf. */
export function DeleteTaskDialog({
  task,
  onClose,
  onDeleted,
  onConflict,
  onGone,
  returnFocusRef,
}: {
  task: Task
  onClose: () => void
  onDeleted: () => void
  onConflict: () => void
  onGone: () => void
  returnFocusRef: RefObject<HTMLElement | null>
}) {
  const cancelRef = useRef<HTMLButtonElement>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string>()

  async function onConfirm() {
    setBusy(true)
    setError(undefined)
    try {
      await deleteTask(task.id, task.version)
      onDeleted()
    } catch (err) {
      if (isConflict(err)) {
        onClose()
        onConflict()
      } else if (isNotFound(err)) {
        onClose()
        onGone()
      } else {
        setError(err instanceof Error ? err.message : 'Es ist ein unerwarteter Fehler aufgetreten.')
      }
    } finally {
      setBusy(false)
    }
  }

  return (
    <Dialog
      titleId="delete-dialog-title"
      title="Aufgabe löschen?"
      describedById="delete-dialog-description"
      initialFocusRef={cancelRef}
      returnFocusRef={returnFocusRef}
      onClose={onClose}
    >
      <p id="delete-dialog-description">„{task.title}“ wird endgültig gelöscht. Das kann nicht rückgängig gemacht werden.</p>
      {error && (
        <p role="alert" className="field-error">
          {error}
        </p>
      )}
      <div className="actions">
        <button type="button" ref={cancelRef} className="secondary" onClick={onClose} disabled={busy}>
          Abbrechen
        </button>
        <button type="button" className="danger" onClick={onConfirm} disabled={busy}>
          {busy ? 'Wird gelöscht …' : 'Endgültig löschen'}
        </button>
      </div>
    </Dialog>
  )
}
