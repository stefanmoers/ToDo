import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { createTask } from '../api/tasks'
import { useAuth } from '../features/auth/AuthContext'
import { useAllProjects } from '../features/projects/useProjectList'
import { emptyDraft, toInput, type TaskDraft } from '../features/tasks/draft'
import { mapTaskApiError } from '../features/tasks/errors'
import { TaskForm } from '../features/tasks/TaskForm'
import { validateTaskDraft } from '../features/tasks/validation'
import { usePageTitle } from '../hooks/usePageTitle'
import type { FormErrors } from '../components/FormParts'

export default function TaskCreatePage() {
  usePageTitle('Neue Aufgabe')
  const { user } = useAuth()
  const navigate = useNavigate()
  const projects = useAllProjects()
  const [draft, setDraft] = useState<TaskDraft>(() => emptyDraft(user!.timeZone))
  const [errors, setErrors] = useState<FormErrors>({})
  const [general, setGeneral] = useState<string>()
  const [focusToken, setFocusToken] = useState(0)
  const [busy, setBusy] = useState(false)

  function onChange(patch: Partial<TaskDraft>) {
    setDraft((d) => ({ ...d, ...patch }))
  }

  async function onSubmit() {
    setGeneral(undefined)
    const found = validateTaskDraft(draft)
    if (Object.values(found).some(Boolean)) {
      setErrors(found)
      setFocusToken((n) => n + 1)
      return
    }
    setErrors({})
    setBusy(true)
    try {
      const task = await createTask(toInput(draft))
      navigate(`/aufgaben/${task.id}`, { state: { notice: 'Die Aufgabe wurde angelegt.' } })
    } catch (err) {
      const m = mapTaskApiError(err)
      setErrors(m.errors)
      setGeneral(m.general)
      setFocusToken((n) => n + 1)
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <h1>Neue Aufgabe</h1>
      <p>Nur der Titel ist Pflicht.</p>
      <TaskForm
        draft={draft}
        onChange={onChange}
        errors={errors}
        general={general}
        focusToken={focusToken}
        busy={busy}
        busyLabel="Aufgabe wird angelegt …"
        submitLabel="Aufgabe anlegen"
        showStatus={false}
        projects={projects}
        onSubmit={onSubmit}
        cancelTo="/"
      />
    </>
  )
}
