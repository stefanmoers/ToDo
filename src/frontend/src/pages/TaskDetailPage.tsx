import { useEffect, useRef, useState } from 'react'
import { useLocation, useNavigate, useParams, Link } from 'react-router-dom'
import { useAuth } from '../features/auth/AuthContext'
import { useAllProjects } from '../features/projects/useProjectList'
import { DeleteTaskDialog } from '../features/tasks/DeleteTaskDialog'
import { formatDue, formatTimestamp } from '../features/tasks/format'
import { PRIORITY_LABELS, STATUS_LABELS } from '../features/tasks/labels'
import { StatusToggleButton } from '../features/tasks/StatusToggleButton'
import { TaskNotFound } from '../features/tasks/TaskNotFound'
import { useTask } from '../features/tasks/useTask'
import { TaskTagsEditor } from '../features/tags/TaskTagsEditor'
import { usePageTitle } from '../hooks/usePageTitle'

export default function TaskDetailPage() {
  const { taskId } = useParams<{ taskId: string }>()
  return <TaskDetail key={taskId} taskId={taskId!} />
}

function TaskDetail({ taskId }: { taskId: string }) {
  const { user } = useAuth()
  const { state, reload, replace } = useTask(taskId)
  const projects = useAllProjects()
  const navigate = useNavigate()
  const location = useLocation()
  const [dialogOpen, setDialogOpen] = useState(false)
  const [deleteConflict, setDeleteConflict] = useState(false)
  const deleteButtonRef = useRef<HTMLButtonElement>(null)
  const conflictRef = useRef<HTMLDivElement>(null)
  const notice = (location.state as { notice?: string } | null)?.notice

  usePageTitle(state.kind === 'loaded' ? `Aufgabe: ${state.task.title}` : 'Aufgabe')

  useEffect(() => {
    if (deleteConflict) conflictRef.current?.focus()
  }, [deleteConflict])

  if (state.kind === 'loading') return <p role="status">Aufgabe wird geladen …</p>
  if (state.kind === 'notFound') return <TaskNotFound />
  if (state.kind === 'error') {
    return (
      <div role="alert">
        <p>Die Aufgabe konnte nicht geladen werden. {state.message}</p>
        <button type="button" onClick={reload}>
          Erneut versuchen
        </button>
      </div>
    )
  }

  const task = state.task

  return (
    <>
      <h1 className="task-title">{task.title}</h1>
      {notice && (
        <p role="status" className="notice">
          {notice}
        </p>
      )}
      {deleteConflict && (
        <div ref={conflictRef} tabIndex={-1} role="alert" className="conflict">
          <p>
            Nicht gelöscht: Die Aufgabe wurde zwischenzeitlich geändert. Die aktuelle Fassung wird angezeigt. Du
            kannst sie jetzt erneut löschen.
          </p>
        </div>
      )}
      <dl className="facts">
        {task.projectId && (
          <>
            <dt>Projekt</dt>
            <dd>
              {(() => {
                const project = projects.find((p) => p.id === task.projectId)
                return project ? <Link to={`/projekte/${project.id}`}>{project.name}</Link> : 'Wird geladen …'
              })()}
            </dd>
          </>
        )}
        <dt>Status</dt>
        <dd>{STATUS_LABELS[task.status]}</dd>
        <dt>Priorität</dt>
        <dd>{PRIORITY_LABELS[task.priority]}</dd>
        <dt>Fälligkeit</dt>
        <dd>
          {task.dueDate ? formatDue(task, user!.timeZone) : 'Keine Fälligkeit'}
          {task.overdue && ' – Überfällig'}
        </dd>
        <dt>Bearbeiter</dt>
        <dd>{task.assigneeId ? (task.assigneeId === user!.id ? 'Du' : task.assigneeDisplayName ?? 'Unbekannt') : 'Keine Zuweisung'}</dd>
        <dt>Tags</dt>
        <dd>{task.tags.length > 0 ? task.tags.map((t) => t.name).join(', ') : 'Keine'}</dd>
        <dt>Beschreibung</dt>
        <dd className="description">{task.description ?? 'Keine Beschreibung'}</dd>
        <dt>Erstellt von</dt>
        <dd>{task.creatorId === user!.id ? 'Du' : task.creatorDisplayName ?? 'Unbekannt'}</dd>
        <dt>Erstellt am</dt>
        <dd>{formatTimestamp(task.createdAt, user!.timeZone)}</dd>
        <dt>Zuletzt geändert</dt>
        <dd>{formatTimestamp(task.updatedAt, user!.timeZone)}</dd>
        {task.status === 'DONE' && task.completedAt && (
          <>
            <dt>Erledigt am</dt>
            <dd>{formatTimestamp(task.completedAt, user!.timeZone)}</dd>
          </>
        )}
      </dl>
      <div className="actions">
        <StatusToggleButton task={task} onChanged={replace} onConflict={reload} onGone={reload} />
        <Link to={`/aufgaben/${task.id}/bearbeiten`} className="button-link secondary">
          Bearbeiten
        </Link>
        <button
          type="button"
          ref={deleteButtonRef}
          className="danger"
          onClick={() => {
            setDeleteConflict(false)
            setDialogOpen(true)
          }}
        >
          Löschen
        </button>
        <Link to="/">Zurück zur Aufgabenliste</Link>
      </div>
      <TaskTagsEditor taskId={task.id} currentTags={task.tags} onChanged={(tags) => replace({ ...task, tags })} />
      {dialogOpen && (
        <DeleteTaskDialog
          task={task}
          onClose={() => setDialogOpen(false)}
          onDeleted={() => navigate('/', { state: { notice: `„${task.title}“ wurde gelöscht.` } })}
          onConflict={() => {
            setDeleteConflict(true)
            reload()
          }}
          onGone={() => navigate('/', { state: { notice: 'Die Aufgabe war bereits gelöscht.' } })}
          returnFocusRef={deleteButtonRef}
        />
      )}
    </>
  )
}
