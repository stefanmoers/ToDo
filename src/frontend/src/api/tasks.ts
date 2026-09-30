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
  createdAt: string
  updatedAt: string
  completedAt?: string
  version: number
  /** Immer vorhanden (leeres Array ohne Zuordnung); ändert nicht die Version (US-043). */
  tags: TagSummary[]
}

export interface TaskInput {
  projectId: string | null
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

export const listTasks = (page: number, size = DEFAULT_PAGE_SIZE, tagId?: string) =>
  api<Page<Task>>('GET', `/api/v1/tasks?page=${page}&size=${size}${tagId ? `&tagId=${encodeURIComponent(tagId)}` : ''}`)

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
