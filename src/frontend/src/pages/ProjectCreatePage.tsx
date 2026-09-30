import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { createProject } from '../api/projects'
import { emptyDraft, toInput, type ProjectDraft } from '../features/projects/draft'
import { mapProjectApiError } from '../features/projects/errors'
import { ProjectForm } from '../features/projects/ProjectForm'
import { validateProjectDraft } from '../features/projects/validation'
import { usePageTitle } from '../hooks/usePageTitle'
import type { FormErrors } from '../components/FormParts'

export default function ProjectCreatePage() {
  usePageTitle('Neues Projekt')
  const navigate = useNavigate()
  const [draft, setDraft] = useState<ProjectDraft>(emptyDraft)
  const [errors, setErrors] = useState<FormErrors>({})
  const [general, setGeneral] = useState<string>()
  const [focusToken, setFocusToken] = useState(0)
  const [busy, setBusy] = useState(false)

  function onChange(patch: Partial<ProjectDraft>) {
    setDraft((d) => ({ ...d, ...patch }))
  }

  async function onSubmit() {
    setGeneral(undefined)
    const found = validateProjectDraft(draft)
    if (Object.values(found).some(Boolean)) {
      setErrors(found)
      setFocusToken((n) => n + 1)
      return
    }
    setErrors({})
    setBusy(true)
    try {
      const project = await createProject(toInput(draft))
      navigate(`/projekte/${project.id}`, { state: { notice: 'Das Projekt wurde angelegt.' } })
    } catch (err) {
      const m = mapProjectApiError(err)
      setErrors(m.errors)
      setGeneral(m.general)
      setFocusToken((n) => n + 1)
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <h1>Neues Projekt</h1>
      <p>Nur der Name ist Pflicht.</p>
      <ProjectForm
        draft={draft}
        onChange={onChange}
        errors={errors}
        general={general}
        focusToken={focusToken}
        busy={busy}
        busyLabel="Projekt wird angelegt …"
        submitLabel="Projekt anlegen"
        onSubmit={onSubmit}
        cancelTo="/projekte"
      />
    </>
  )
}
