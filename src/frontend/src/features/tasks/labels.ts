import type { TaskPriority, TaskStatus } from '../../api/tasks'

export const PRIORITY_LABELS: Record<TaskPriority, string> = {
  LOW: 'Niedrig',
  NORMAL: 'Normal',
  HIGH: 'Hoch',
  URGENT: 'Dringend',
}

export const STATUS_LABELS: Record<TaskStatus, string> = {
  OPEN: 'Offen',
  IN_PROGRESS: 'In Bearbeitung',
  DONE: 'Erledigt',
}

export const TASK_FIELD_LABELS: Record<string, string> = {
  title: 'Titel',
  description: 'Beschreibung',
  priority: 'Priorität',
  status: 'Status',
  dueDate: 'Fälligkeitsdatum',
  dueTime: 'Uhrzeit',
  dueTimeZone: 'Zeitzone der Fälligkeit',
  dueOffset: 'Zeitpunkt bei Zeitumstellung',
}

export function pluralTasks(n: number): string {
  return n === 1 ? '1 Aufgabe' : `${n} Aufgaben`
}
