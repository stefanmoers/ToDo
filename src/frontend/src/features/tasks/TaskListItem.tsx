import { Link } from 'react-router-dom'
import type { Task } from '../../api/tasks'
import { formatDue } from './format'
import { PRIORITY_LABELS, STATUS_LABELS } from './labels'
import { StatusToggleButton } from './StatusToggleButton'

export function TaskListItem({
  task,
  viewerTimeZone,
  onChanged,
  onConflict,
  onGone,
}: {
  task: Task
  viewerTimeZone: string
  onChanged: (task: Task) => void
  onConflict: () => void
  onGone: () => void
}) {
  return (
    <li className={task.status === 'DONE' ? 'task done' : 'task'}>
      <Link to={`/aufgaben/${task.id}`} className="task-title">
        {task.title}
      </Link>
      <p className="task-meta">
        Status: {STATUS_LABELS[task.status]} · Priorität: {PRIORITY_LABELS[task.priority]}
        {task.dueDate && <> · Fällig: {formatDue(task, viewerTimeZone)}</>}
        {task.overdue && (
          <span className="badge overdue">
            {' '}
            <span aria-hidden="true">⚠ </span>Überfällig
          </span>
        )}
        {task.assigneeId && <> · Bearbeiter: {task.assigneeDisplayName ?? 'Unbekannt'}</>}
        {task.tags.length > 0 && <> · Tags: {task.tags.map((t) => t.name).join(', ')}</>}
        {task.subtasks.total > 0 && (
          <>
            {' '}
            · Unteraufgaben: {task.subtasks.done} von {task.subtasks.total} erledigt
          </>
        )}
      </p>
      <StatusToggleButton task={task} onChanged={onChanged} onConflict={onConflict} onGone={onGone} />
    </li>
  )
}
