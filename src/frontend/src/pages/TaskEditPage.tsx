import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { getTask, updateTask, type Task } from '../api/tasks'
import { useAuth } from '../features/auth/AuthContext'
import { ConflictNotice } from '../features/tasks/ConflictNotice'
import { toDraft, toInput, type TaskDraft } from '../features/tasks/draft'
import { isConflict, isNotFound, mapTaskApiError } from '../features/tasks/errors'
import { TaskForm } from '../features/tasks/TaskForm'
import { TaskNotFound } from '../features/tasks/TaskNotFound'
import { useTask } from '../features/tasks/useTask'
import { validateTaskDraft } from '../features/tasks/validation'
import { usePageTitle } from '../hooks/usePageTitle'
import type { FormErrors } from '../components/FormParts'

export default function TaskEditPage() {
  const { taskId } = useParams<{ taskId: string }>()
  return <TaskEdit key={taskId} taskId={taskId!} />
}

function TaskEdit({ taskId }: { taskId: string }) {
  const { user } = useAuth()
  const { state } = useTask(taskId)

  if (state.kind === 'loading') return <p role="status">Aufgabe wird geladen …</p>
  if (state.kind === 'notFound') return <TaskNotFound />
  if (state.kind === 'error') {
    return (
      <p role="alert">Die Aufgabe konnte nicht geladen werden. {state.message}</p>
    )
  }
  return <TaskEditForm initial={state.task} viewerTimeZone={user!.timeZone} />
}

function TaskEditForm({ initial, viewerTimeZone }: { initial: Task; viewerTimeZone: string }) {
  const navigate = useNavigate()
  const [base, setBase] = useState(initial)
  const [draft, setDraft] = useState<TaskDraft>(() => toDraft(initial, viewerTimeZone))
  const [errors, setErrors] = useState<FormErrors>({})
  const [general, setGeneral] = useState<string>()
  const [focusToken, setFocusToken] = useState(0)
  const [busy, setBusy] = useState(false)
  const [conflict, setConflict] = useState<{ server?: Task; deleted?: boolean } | null>(null)
  const [conflictFocusToken, setConflictFocusToken] = useState(0)

  usePageTitle(`Aufgabe bearbeiten: ${base.title}`)

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
      const updated = await updateTask(base.id, { ...toInput(draft), status: draft.status, version: base.version })
      navigate(`/aufgaben/${updated.id}`, { state: { notice: 'Die Änderungen wurden gespeichert.' } })
    } catch (err) {
      if (isConflict(err)) {
        try {
          const server = await getTask(base.id)
          setConflict({ server })
        } catch (e) {
          setConflict({ deleted: isNotFound(e) })
        }
        setConflictFocusToken((n) => n + 1)
      } else if (isNotFound(err)) {
        setConflict({ deleted: true })
        setConflictFocusToken((n) => n + 1)
      } else {
        const m = mapTaskApiError(err)
        setErrors(m.errors)
        setGeneral(m.general)
        setFocusToken((n) => n + 1)
      }
    } finally {
      setBusy(false)
    }
  }

  function useServer(server: Task) {
    setBase(server)
    setDraft(toDraft(server, viewerTimeZone))
    setConflict(null)
  }

  function keepMine(server: Task) {
    setBase(server)
    setConflict(null)
  }

  return (
    <>
      <h1>Aufgabe bearbeiten</h1>
      {conflict && (
        <ConflictNotice
          draft={draft}
          server={conflict.server}
          deleted={conflict.deleted}
          viewerTimeZone={viewerTimeZone}
          onUseServer={useServer}
          onKeepMine={keepMine}
          focusToken={conflictFocusToken}
        />
      )}
      <TaskForm
        draft={draft}
        onChange={onChange}
        errors={errors}
        general={general}
        focusToken={focusToken}
        busy={busy || conflict?.deleted === true}
        busyLabel="Änderungen werden gespeichert …"
        submitLabel="Änderungen speichern"
        showStatus
        onSubmit={onSubmit}
        cancelTo={`/aufgaben/${base.id}`}
      />
    </>
  )
}
