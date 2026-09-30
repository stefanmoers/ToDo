import type { Task, TaskInput, TaskPriority, TaskStatus } from '../../api/tasks'

/** Formularentwurf: alle Felder als Strings, damit die Eingabeelemente kontrolliert bleiben können. */
export interface TaskDraft {
  title: string
  description: string
  priority: TaskPriority
  status: TaskStatus
  dueDate: string
  /** true, wenn der Browser das Datum als unvollständig/ungültig meldet (input.validity.badInput). */
  dateBadInput: boolean
  dueTime: string
  dueTimeZone: string
  dueOffset: string
}

export function emptyDraft(defaultTimeZone: string): TaskDraft {
  return {
    title: '',
    description: '',
    priority: 'NORMAL',
    status: 'OPEN',
    dueDate: '',
    dateBadInput: false,
    dueTime: '',
    dueTimeZone: defaultTimeZone,
    dueOffset: '',
  }
}

export function toDraft(task: Task, defaultTimeZone: string): TaskDraft {
  return {
    title: task.title,
    description: task.description ?? '',
    priority: task.priority,
    status: task.status,
    dueDate: task.dueDate ?? '',
    dateBadInput: false,
    dueTime: task.dueTime ?? '',
    dueTimeZone: task.dueTimeZone ?? defaultTimeZone,
    dueOffset: task.dueOffset ?? '',
  }
}

export function toInput(draft: TaskDraft): TaskInput {
  const title = draft.title.trim()
  const description = draft.description.trim().length === 0 ? null : draft.description
  const hasDate = draft.dueDate.trim().length > 0
  const hasTime = hasDate && draft.dueTime.trim().length > 0
  return {
    title,
    description,
    priority: draft.priority,
    dueDate: hasDate ? draft.dueDate : null,
    dueTime: hasTime ? draft.dueTime : null,
    dueTimeZone: hasDate ? draft.dueTimeZone : null,
    dueOffset: hasTime && draft.dueOffset.trim().length > 0 ? draft.dueOffset : null,
  }
}

const COMPARABLE_FIELDS: (keyof TaskDraft)[] = [
  'title',
  'description',
  'priority',
  'status',
  'dueDate',
  'dueTime',
  'dueTimeZone',
]

/** Liefert die Felder, in denen sich zwei Entwürfe unterscheiden (für die Konfliktanzeige, US-120). */
export function diffDrafts(a: TaskDraft, b: TaskDraft): string[] {
  return COMPARABLE_FIELDS.filter((field) => a[field] !== b[field])
}
