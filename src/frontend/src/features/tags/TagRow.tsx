import { useEffect, useRef, useState, type FormEvent } from 'react'
import { renameTag, type Tag } from '../../api/tags'
import { DeleteTagDialog } from './DeleteTagDialog'
import { mapTagApiError } from './errors'
import { validateTagName } from './validation'

export function TagRow({
  tag,
  onRenamed,
  onDeleted,
}: {
  tag: Tag
  onRenamed: (tag: Tag) => void
  onDeleted: () => void
}) {
  const [editing, setEditing] = useState(false)
  const [name, setName] = useState(tag.name)
  const [error, setError] = useState<string>()
  const [busy, setBusy] = useState(false)
  const [dialogOpen, setDialogOpen] = useState(false)
  const deleteButtonRef = useRef<HTMLButtonElement>(null)
  const nameInputRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    if (editing) nameInputRef.current?.focus()
  }, [editing])

  function startEditing() {
    setName(tag.name)
    setError(undefined)
    setEditing(true)
  }

  async function save(e: FormEvent) {
    e.preventDefault()
    const validation = validateTagName(name)
    if (validation) {
      setError(validation)
      return
    }
    setBusy(true)
    setError(undefined)
    try {
      const updated = await renameTag(tag.id, name.trim())
      onRenamed(updated)
      setEditing(false)
    } catch (err) {
      setError(mapTagApiError(err))
    } finally {
      setBusy(false)
    }
  }

  if (editing) {
    return (
      <li>
        <form onSubmit={save} noValidate>
          <label htmlFor={`tag-name-${tag.id}`}>
            Name für „{tag.name}“
          </label>
          {error && (
            <p role="alert" className="field-error">
              {error}
            </p>
          )}
          <input id={`tag-name-${tag.id}`} ref={nameInputRef} value={name} onChange={(e) => setName(e.target.value)} />
          <div className="actions">
            <button type="submit" disabled={busy}>
              {busy ? 'Wird gespeichert …' : 'Speichern'}
            </button>
            <button type="button" className="secondary" onClick={() => setEditing(false)}>
              Abbrechen
            </button>
          </div>
        </form>
      </li>
    )
  }

  return (
    <li className="task">
      <span className="task-title">{tag.name}</span>
      <div className="actions">
        <button type="button" className="secondary" onClick={startEditing}>
          Umbenennen<span className="visually-hidden">: {tag.name}</span>
        </button>
        <button type="button" ref={deleteButtonRef} className="danger" onClick={() => setDialogOpen(true)}>
          Löschen<span className="visually-hidden">: {tag.name}</span>
        </button>
      </div>
      {dialogOpen && (
        <DeleteTagDialog
          tag={tag}
          onClose={() => setDialogOpen(false)}
          onDeleted={() => {
            setDialogOpen(false)
            onDeleted()
          }}
          returnFocusRef={deleteButtonRef}
        />
      )}
    </li>
  )
}
