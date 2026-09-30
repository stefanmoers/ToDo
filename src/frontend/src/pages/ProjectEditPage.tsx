import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { getProject, updateProject, type Project } from '../api/projects'
import { ConflictNotice } from '../features/projects/ConflictNotice'
import { toDraft, toInput, type ProjectDraft } from '../features/projects/draft'
import { isConflict, isNotFound, mapProjectApiError } from '../features/projects/errors'
import { ProjectForm } from '../features/projects/ProjectForm'
import { ProjectNotFound } from '../features/projects/ProjectNotFound'
import { useProject } from '../features/projects/useProject'
import { validateProjectDraft } from '../features/projects/validation'
import { usePageTitle } from '../hooks/usePageTitle'
import type { FormErrors } from '../components/FormParts'

export default function ProjectEditPage() {
  const { projectId } = useParams<{ projectId: string }>()
  return <ProjectEdit key={projectId} projectId={projectId!} />
}

function ProjectEdit({ projectId }: { projectId: string }) {
  const { state } = useProject(projectId)

  if (state.kind === 'loading') return <p role="status">Projekt wird geladen …</p>
  if (state.kind === 'notFound') return <ProjectNotFound />
  if (state.kind === 'error') {
    return <p role="alert">Das Projekt konnte nicht geladen werden. {state.message}</p>
  }
  return <ProjectEditForm initial={state.project} />
}

function ProjectEditForm({ initial }: { initial: Project }) {
  const navigate = useNavigate()
  const [base, setBase] = useState(initial)
  const [draft, setDraft] = useState<ProjectDraft>(() => toDraft(initial))
  const [errors, setErrors] = useState<FormErrors>({})
  const [general, setGeneral] = useState<string>()
  const [focusToken, setFocusToken] = useState(0)
  const [busy, setBusy] = useState(false)
  const [conflict, setConflict] = useState<{ server?: Project; deleted?: boolean } | null>(null)
  const [conflictFocusToken, setConflictFocusToken] = useState(0)

  usePageTitle(`Projekt bearbeiten: ${base.name}`)

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
      const updated = await updateProject(base.id, { ...toInput(draft), version: base.version })
      navigate(`/projekte/${updated.id}`, { state: { notice: 'Die Änderungen wurden gespeichert.' } })
    } catch (err) {
      if (isConflict(err)) {
        try {
          const server = await getProject(base.id)
          setConflict({ server })
        } catch (e) {
          setConflict({ deleted: isNotFound(e) })
        }
        setConflictFocusToken((n) => n + 1)
      } else if (isNotFound(err)) {
        setConflict({ deleted: true })
        setConflictFocusToken((n) => n + 1)
      } else {
        const m = mapProjectApiError(err)
        setErrors(m.errors)
        setGeneral(m.general)
        setFocusToken((n) => n + 1)
      }
    } finally {
      setBusy(false)
    }
  }

  function useServer(server: Project) {
    setBase(server)
    setDraft(toDraft(server))
    setConflict(null)
  }

  function keepMine(server: Project) {
    setBase(server)
    setConflict(null)
  }

  return (
    <div className="content-card form-shell">
      <h1>Projekt bearbeiten</h1>
      {conflict && (
        <ConflictNotice
          draft={draft}
          server={conflict.server}
          deleted={conflict.deleted}
          onUseServer={useServer}
          onKeepMine={keepMine}
          focusToken={conflictFocusToken}
        />
      )}
      <ProjectForm
        draft={draft}
        onChange={onChange}
        errors={errors}
        general={general}
        focusToken={focusToken}
        busy={busy || conflict?.deleted === true}
        busyLabel="Änderungen werden gespeichert …"
        submitLabel="Änderungen speichern"
        onSubmit={onSubmit}
        cancelTo={`/projekte/${base.id}`}
      />
    </div>
  )
}
