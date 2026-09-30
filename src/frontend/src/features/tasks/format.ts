import type { Task } from '../../api/tasks'

/** Zeigt die Fälligkeit an; bei reinem Datum ohne Verschiebung, bei Uhrzeit in der Aufgabenzeitzone. */
export function formatDue(task: Task, viewerTimeZone: string): string {
  if (!task.dueDate) return 'Keine Fälligkeit'
  if (!task.dueTime || !task.dueAt) {
    const [y, m, d] = task.dueDate.split('-').map(Number)
    const date = new Date(Date.UTC(y, m - 1, d))
    return date.toLocaleDateString('de-DE', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric', timeZone: 'UTC' })
  }
  const instant = new Date(task.dueAt)
  const inTaskZone = instant.toLocaleString('de-DE', { dateStyle: 'long', timeStyle: 'short', timeZone: task.dueTimeZone })
  if (!task.dueTimeZone || task.dueTimeZone === viewerTimeZone) return inTaskZone
  const inViewerZone = instant.toLocaleString('de-DE', { dateStyle: 'long', timeStyle: 'short', timeZone: viewerTimeZone })
  return `${inTaskZone} (bei dir: ${inViewerZone})`
}

export function formatTimestamp(iso: string, viewerTimeZone: string): string {
  return new Date(iso).toLocaleString('de-DE', { dateStyle: 'long', timeStyle: 'short', timeZone: viewerTimeZone })
}
