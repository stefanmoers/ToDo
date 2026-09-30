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
      <div className="task-card-top">
        <Link to={`/aufgaben/${task.id}`} className="task-title">
          {task.title}
        </Link>
        <span className={`meta-pill status-${task.status.toLowerCase()}`}>Status: {STATUS_LABELS[task.status]}</span>
      </div>
      <div className="task-meta">
        <span className={`meta-pill priority-${task.priority.toLowerCase()}`}>Priorität: {PRIORITY_LABELS[task.priority]}</span>
        {task.dueDate && <span>Fällig: {formatDue(task, viewerTimeZone)}</span>}
        {task.overdue && (
          <span className="badge overdue">
            <span aria-hidden="true">⚠ </span>Überfällig
          </span>
        )}
        {task.assigneeId && <span>Bearbeiter: {task.assigneeDisplayName ?? 'Unbekannt'}</span>}
        {task.tags.length > 0 && <span>Tags: {task.tags.map((t) => t.name).join(', ')}</span>}
        {task.subtasks.total > 0 && (
          <span>Unteraufgaben: {task.subtasks.done} von {task.subtasks.total} erledigt</span>
        )}
      </div>
      <StatusToggleButton task={task} onChanged={onChanged} onConflict={onConflict} onGone={onGone} />
    </li>
  )
}
