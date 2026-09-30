import { useState, type FormEvent } from 'react'
import { createTag, type Tag } from '../../api/tags'
import { useAllProjects } from '../projects/useProjectList'
import { mapTagApiError } from './errors'
import { validateTagName } from './validation'

export function TagCreateForm({ onCreated }: { onCreated: (tag: Tag) => void }) {
  const projects = useAllProjects()
  const [name, setName] = useState('')
  const [projectId, setProjectId] = useState('')
  const [error, setError] = useState<string>()
  const [busy, setBusy] = useState(false)

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    const validation = validateTagName(name)
    if (validation) {
      setError(validation)
      return
    }
    setBusy(true)
    setError(undefined)
    try {
      const tag = await createTag({ name: name.trim(), projectId: projectId || null })
      setName('')
      setProjectId('')
      onCreated(tag)
    } catch (err) {
      setError(mapTagApiError(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <section aria-labelledby="tag-create-heading">
      <h2 id="tag-create-heading">Neuen Tag anlegen</h2>
      <form onSubmit={onSubmit} noValidate>
        {error && (
          <p role="alert" className="field-error">
            {error}
          </p>
        )}
        <div className="field">
          <label htmlFor="tag-name">Name</label>
          <input id="tag-name" value={name} onChange={(e) => setName(e.target.value)} />
        </div>
        <div className="field">
          <label htmlFor="tag-scope">Geltungsbereich</label>
          <select id="tag-scope" value={projectId} onChange={(e) => setProjectId(e.target.value)}>
            <option value="">Persönlich</option>
            {projects.map((p) => (
              <option key={p.id} value={p.id}>
                {p.name}
              </option>
            ))}
          </select>
        </div>
        <button type="submit" disabled={busy}>
          {busy ? 'Wird angelegt …' : 'Tag anlegen'}
        </button>
      </form>
    </section>
  )
}
