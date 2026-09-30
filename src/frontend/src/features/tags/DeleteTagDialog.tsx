import { useRef, useState, type RefObject } from 'react'
import { deleteTag, type Tag } from '../../api/tags'
import { Dialog } from '../../components/Dialog'
import { mapTagApiError } from './errors'

/** Löschbestätigung (US-043): entfernt nur den Tag, keine Aufgabe. */
export function DeleteTagDialog({
  tag,
  onClose,
  onDeleted,
  returnFocusRef,
}: {
  tag: Tag
  onClose: () => void
  onDeleted: () => void
  returnFocusRef: RefObject<HTMLElement | null>
}) {
  const cancelRef = useRef<HTMLButtonElement>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string>()

  async function onConfirm() {
    setBusy(true)
    setError(undefined)
    try {
      await deleteTag(tag.id)
      onDeleted()
    } catch (err) {
      setError(mapTagApiError(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <Dialog
      titleId="delete-tag-dialog-title"
      title="Tag löschen?"
      describedById="delete-tag-dialog-description"
      initialFocusRef={cancelRef}
      returnFocusRef={returnFocusRef}
      onClose={onClose}
    >
      <p id="delete-tag-dialog-description">
        „{tag.name}“ wird endgültig gelöscht. Zugeordnete Aufgaben bleiben erhalten, verlieren aber diese Markierung.
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
