import type { Activity } from '../../api/tasks'
import { PRIORITY_LABELS, STATUS_LABELS } from './labels'
import type { TaskPriority, TaskStatus } from '../../api/tasks'

const NO_ASSIGNEE = 'Niemand'
const NO_PROJECT = 'Kein Projekt'
const NO_DUE = 'Keine Fälligkeit'

function statusLabel(raw: string | undefined): string {
  return raw ? STATUS_LABELS[raw as TaskStatus] ?? raw : raw ?? ''
}

function priorityLabel(raw: string | undefined): string {
  return raw ? PRIORITY_LABELS[raw as TaskPriority] ?? raw : raw ?? ''
}

/** US-081: verständliche deutsche Beschreibung eines Historien-Eintrags. */
export function describeActivity(a: Activity): string {
  switch (a.type) {
    case 'CREATED':
      return 'Aufgabe angelegt'
    case 'TITLE_CHANGED':
      return `Titel geändert: von „${a.fromValue}“ zu „${a.toValue}“`
    case 'STATUS_CHANGED':
      return `Status geändert: von ${statusLabel(a.fromValue)} zu ${statusLabel(a.toValue)}`
    case 'PRIORITY_CHANGED':
      return `Priorität geändert: von ${priorityLabel(a.fromValue)} zu ${priorityLabel(a.toValue)}`
    case 'ASSIGNEE_CHANGED':
      return `Bearbeiter geändert: von ${a.fromValue ?? NO_ASSIGNEE} zu ${a.toValue ?? NO_ASSIGNEE}`
    case 'DUE_CHANGED':
      return `Fälligkeit geändert: von ${a.fromValue ?? NO_DUE} zu ${a.toValue ?? NO_DUE}`
    case 'PROJECT_CHANGED':
      return `Projekt geändert: von ${a.fromValue ?? NO_PROJECT} zu ${a.toValue ?? NO_PROJECT}`
    case 'COMPLETED':
      return 'Als erledigt markiert'
    case 'REOPENED':
      return 'Wieder geöffnet'
    case 'COMMENTED':
      return 'Hat kommentiert'
    default:
      return a.type
  }
}
