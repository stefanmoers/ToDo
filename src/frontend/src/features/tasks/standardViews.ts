/** "Heute" bezieht sich auf Kalendertage in der Kontozeitzone des Betrachters (docs/DOMAIN_MODEL.md). */
export function todayInZone(timeZone: string): string {
  return new Intl.DateTimeFormat('en-CA', { timeZone, year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date())
}

export function addDays(dateStr: string, days: number): string {
  const [y, m, d] = dateStr.split('-').map(Number)
  return new Date(Date.UTC(y, m - 1, d + days)).toISOString().slice(0, 10)
}

export type StandardView = 'all' | 'today' | 'upcoming' | 'overdue' | 'done'

export const STANDARD_VIEWS: { view: StandardView; label: string }[] = [
  { view: 'all', label: 'Alle' },
  { view: 'today', label: 'Heute' },
  { view: 'upcoming', label: 'Demnächst' },
  { view: 'overdue', label: 'Überfällig' },
  { view: 'done', label: 'Erledigt' },
]

/**
 * Baut die Filter-Parameter einer Standardansicht (US-053). „Heute“/„Demnächst“ schließen erledigte Aufgaben aus
 * und decken Kalendertage in der übergebenen Kontozeitzone ab; „Demnächst“ die nächsten sieben Tage ohne heute.
 */
export function standardViewParams(view: StandardView, timeZone: string): URLSearchParams {
  const params = new URLSearchParams()
  const today = todayInZone(timeZone)
  switch (view) {
    case 'today':
      params.set('status', 'OPEN')
      params.append('status', 'IN_PROGRESS')
      params.set('dueDateFrom', today)
      params.set('dueDateTo', today)
      break
    case 'upcoming':
      params.set('status', 'OPEN')
      params.append('status', 'IN_PROGRESS')
      params.set('dueDateFrom', addDays(today, 1))
      params.set('dueDateTo', addDays(today, 7))
      break
    case 'overdue':
      params.set('overdue', 'true')
      break
    case 'done':
      params.set('status', 'DONE')
      break
    case 'all':
      break
  }
  return params
}

const VIEW_RELEVANT_KEYS = ['status', 'dueDateFrom', 'dueDateTo', 'overdue'] as const

/** Für die Hervorhebung der aktiven Standardansicht; unabhängig von Suche/Projekt/Tag/Sortierung/Seite. */
export function matchesStandardView(current: URLSearchParams, view: StandardView, timeZone: string): boolean {
  const target = standardViewParams(view, timeZone)
  return VIEW_RELEVANT_KEYS.every((key) => current.getAll(key).join(',') === target.getAll(key).join(','))
}
