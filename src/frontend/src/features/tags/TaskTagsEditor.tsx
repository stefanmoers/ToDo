import { useState } from 'react'
import type { TagSummary } from '../../api/tagSummary'
import { setTaskTags } from '../../api/tasks'
import { mapTagApiError } from './errors'
import { useAllTags } from './useTagList'

/**
 * US-043: Tags einer Aufgabe hinzufügen/entfernen; ändert bewusst nicht die Aufgaben-Version.
 * `currentTags` wird nur als Ausgangswert gelesen (lazy initial state) — die Aufgaben-Detailseite mountet diese
 * Komponente über `key={taskId}` neu, wenn eine andere Aufgabe angezeigt wird.
 */
export function TaskTagsEditor({
  taskId,
  currentTags,
  onChanged,
}: {
  taskId: string
  currentTags: TagSummary[]
  onChanged: (tags: TagSummary[]) => void
}) {
  const allTags = useAllTags()
  const [selected, setSelected] = useState<Set<string>>(() => new Set(currentTags.map((t) => t.id)))
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string>()
  const [notice, setNotice] = useState<string>()

  function toggle(id: string) {
    setNotice(undefined)
    setSelected((prev) => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

  async function save() {
    setBusy(true)
    setError(undefined)
    setNotice(undefined)
    try {
      const tags = await setTaskTags(taskId, [...selected])
      onChanged(tags)
      setNotice('Die Tag-Zuordnung wurde gespeichert.')
    } catch (err) {
      setError(mapTagApiError(err))
    } finally {
      setBusy(false)
    }
  }

  if (allTags.length === 0) return null

  return (
    <section aria-labelledby="task-tags-heading">
      <h2 id="task-tags-heading">Tags</h2>
      {error && (
        <p role="alert" className="field-error">
          {error}
        </p>
      )}
      {notice && <p role="status">{notice}</p>}
      <fieldset>
        <legend className="visually-hidden">Zugeordnete Tags</legend>
        {allTags.map((tag) => (
          <p key={tag.id} className="radio-option">
            <input
              type="checkbox"
              id={`task-tag-${tag.id}`}
              checked={selected.has(tag.id)}
              onChange={() => toggle(tag.id)}
            />{' '}
            <label htmlFor={`task-tag-${tag.id}`}>{tag.name}</label>
          </p>
        ))}
      </fieldset>
      <button type="button" onClick={save} disabled={busy}>
        {busy ? 'Wird gespeichert …' : 'Tags speichern'}
      </button>
    </section>
  )
}
