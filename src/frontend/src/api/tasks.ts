import { api } from './client'
import { DEFAULT_PAGE_SIZE, type Page } from './pagination'
import type { TagSummary } from './tagSummary'

export type TaskStatus = 'OPEN' | 'IN_PROGRESS' | 'DONE'
export type TaskPriority = 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT'

/** Reihenfolge entspricht der fachlichen Rangfolge bzw. dem üblichen Ablauf; für Auswahllisten. */
export const TASK_PRIORITIES: TaskPriority[] = ['LOW', 'NORMAL', 'HIGH', 'URGENT']
export const TASK_STATUSES: TaskStatus[] = ['OPEN', 'IN_PROGRESS', 'DONE']

export interface Task {
  id: string
  /** Nur ein eigenes Projekt; ohne Angabe ist die Aufgabe persönlich (US-040ff). */
  projectId?: string
  title: string
  description?: string
  status: TaskStatus
  priority: TaskPriority
  dueDate?: string
  dueTime?: string
  dueTimeZone?: string
  dueOffset?: string
  /** Nur bei Uhrzeit gesetzt: exakter Fälligkeitszeitpunkt in UTC (ISO-Instant). */
  dueAt?: string
  /** Vom Server berechnet (Fachregel bleibt im Backend). */
  overdue: boolean
  creatorId: string
  creatorDisplayName?: string
  /** Ohne Projekt nur der Ersteller selbst; mit Projekt nur ein aktuelles Mitglied oder der Eigentümer (US-062). */
  assigneeId?: string
  assigneeDisplayName?: string
  createdAt: string
  updatedAt: string
  completedAt?: string
  version: number
  /** Immer vorhanden (leeres Array ohne Zuordnung); ändert nicht die Version (US-043). */
  tags: TagSummary[]
  /** US-071: done=total=0 ohne Unteraufgaben. */
  subtasks: SubtaskProgress
}

export interface SubtaskProgress {
  done: number
  total: number
}

export interface Subtask {
  id: string
  title: string
  done: boolean
  sortOrder: number
}

export interface TaskInput {
  projectId: string | null
  assigneeId: string | null
  title: string
  description: string | null
  priority: TaskPriority
  dueDate: string | null
  dueTime: string | null
  dueTimeZone: string | null
  dueOffset?: string | null
}

export interface TaskUpdate extends TaskInput {
  status: TaskStatus
  version: number
}

export type TaskSortField = 'createdAt' | 'updatedAt' | 'due' | 'priority' | 'title'
export type SortDirection = 'asc' | 'desc'

/** US-050 bis US-052: unterschiedliche Filterarten werden mit UND verknüpft, mehrere Werte je Art mit ODER. */
export interface TaskListFilters {
  q?: string
  status?: TaskStatus[]
  priority?: TaskPriority[]
  projectId?: string
  tagId?: string
  assigneeId?: string
  overdue?: boolean
  dueDateFrom?: string
  dueDateTo?: string
  sortBy?: TaskSortField
  sortDirection?: SortDirection
}

export const listTasks = (page: number, size = DEFAULT_PAGE_SIZE, filters: TaskListFilters = {}) => {
  const params = new URLSearchParams({ page: String(page), size: String(size) })
  if (filters.q) params.set('q', filters.q)
  for (const s of filters.status ?? []) params.append('status', s)
  for (const p of filters.priority ?? []) params.append('priority', p)
  if (filters.projectId) params.set('projectId', filters.projectId)
  if (filters.tagId) params.set('tagId', filters.tagId)
  if (filters.assigneeId) params.set('assigneeId', filters.assigneeId)
  if (filters.overdue) params.set('overdue', 'true')
  if (filters.dueDateFrom) params.set('dueDateFrom', filters.dueDateFrom)
  if (filters.dueDateTo) params.set('dueDateTo', filters.dueDateTo)
  if (filters.sortBy) params.set('sortBy', filters.sortBy)
  if (filters.sortDirection) params.set('sortDirection', filters.sortDirection)
  return api<Page<Task>>('GET', `/api/v1/tasks?${params.toString()}`)
}

export interface TaskSummary {
  openCount: number
  dueTodayCount: number
  overdueCount: number
  doneCount: number
  highPriorityOpenCount: number
}

/** US-110: today ist Pflicht, da der Server die Kontozeitzone nicht kennt; der Client löst „heute" selbst auf. */
export const getTaskSummary = (today: string) =>
  api<TaskSummary>('GET', `/api/v1/tasks/summary?today=${encodeURIComponent(today)}`)

export const getTask = (id: string) => api<Task>('GET', `/api/v1/tasks/${encodeURIComponent(id)}`)

export const createTask = (input: TaskInput) => api<Task>('POST', '/api/v1/tasks', input)

export const updateTask = (id: string, update: TaskUpdate) =>
  api<Task>('PUT', `/api/v1/tasks/${encodeURIComponent(id)}`, update)

export const deleteTask = (id: string, version: number) =>
  api('DELETE', `/api/v1/tasks/${encodeURIComponent(id)}?version=${version}`)

export const completeTask = (id: string, version: number) =>
  api<Task>('POST', `/api/v1/tasks/${encodeURIComponent(id)}/complete`, { version })

export const reopenTask = (id: string, version: number) =>
  api<Task>('POST', `/api/v1/tasks/${encodeURIComponent(id)}/reopen`, { version })

/** Ersetzt die Tag-Zuordnung vollständig; ändert nicht die Version der Aufgabe (US-043). */
export const setTaskTags = (id: string, tagIds: string[]) =>
  api<TagSummary[]>('PUT', `/api/v1/tasks/${encodeURIComponent(id)}/tags`, { tagIds })

/** US-070/071: Unteraufgaben. Jede Mutation liefert die vollständige, neu geordnete Liste zurück. */
export const listSubtasks = (taskId: string) =>
  api<Subtask[]>('GET', `/api/v1/tasks/${encodeURIComponent(taskId)}/subtasks`)

export const createSubtask = (taskId: string, title: string) =>
  api<Subtask[]>('POST', `/api/v1/tasks/${encodeURIComponent(taskId)}/subtasks`, { title })

export const renameSubtask = (taskId: string, subtaskId: string, title: string) =>
  api<Subtask[]>('PUT', `/api/v1/tasks/${encodeURIComponent(taskId)}/subtasks/${encodeURIComponent(subtaskId)}`, { title })

export const setSubtaskDone = (taskId: string, subtaskId: string, done: boolean) =>
  api<Subtask[]>('PUT', `/api/v1/tasks/${encodeURIComponent(taskId)}/subtasks/${encodeURIComponent(subtaskId)}/done`, {
    done,
  })

export const moveSubtask = (taskId: string, subtaskId: string, direction: 'UP' | 'DOWN') =>
  api<Subtask[]>('POST', `/api/v1/tasks/${encodeURIComponent(taskId)}/subtasks/${encodeURIComponent(subtaskId)}/move`, {
    direction,
  })

export const deleteSubtask = (taskId: string, subtaskId: string) =>
  api<Subtask[]>('DELETE', `/api/v1/tasks/${encodeURIComponent(taskId)}/subtasks/${encodeURIComponent(subtaskId)}`)
