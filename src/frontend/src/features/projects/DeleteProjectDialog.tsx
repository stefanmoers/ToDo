import { useRef, useState, type RefObject } from 'react'
import { deleteProject, type Project } from '../../api/projects'
import { Dialog } from '../../components/Dialog'
import { isConflict, isNotFound } from './errors'

/** Löschbestätigung (US-042): Aufgaben bleiben erhalten und werden persönlich. */
export function DeleteProjectDialog({
  project,
  onClose,
  onDeleted,
  onConflict,
  onGone,
  returnFocusRef,
}: {
  project: Project
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
      await deleteProject(project.id, project.version)
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
      titleId="delete-project-dialog-title"
      title="Projekt löschen?"
      describedById="delete-project-dialog-description"
      initialFocusRef={cancelRef}
      returnFocusRef={returnFocusRef}
      onClose={onClose}
    >
      <p id="delete-project-dialog-description">
        „{project.name}“ wird endgültig gelöscht. Seine Aufgaben bleiben erhalten und werden zu persönlichen Aufgaben
        ihres jeweiligen Erstellers.
      </p>
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
