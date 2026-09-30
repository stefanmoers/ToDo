import { describe, expect, it } from 'vitest'
import { addDays, matchesStandardView, standardViewParams, todayInZone } from './standardViews'

describe('todayInZone', () => {
  it('liefert das Datum im Format JJJJ-MM-TT', () => {
    expect(todayInZone('UTC')).toMatch(/^\d{4}-\d{2}-\d{2}$/)
  })
})

describe('addDays', () => {
  it('addiert Tage über Monatsgrenzen hinweg', () => {
    expect(addDays('2026-01-30', 5)).toBe('2026-02-04')
  })

  it('erlaubt auch negative Werte', () => {
    expect(addDays('2026-03-01', -1)).toBe('2026-02-28')
  })
})

describe('standardViewParams', () => {
  it('Alle hat keine Einschränkung', () => {
    expect(standardViewParams('all', 'UTC').toString()).toBe('')
  })

  it('Heute grenzt auf den heutigen Kalendertag ein und schließt Erledigte aus', () => {
    const params = standardViewParams('today', 'UTC')
    const today = todayInZone('UTC')
    expect(params.getAll('status')).toEqual(['OPEN', 'IN_PROGRESS'])
    expect(params.get('dueDateFrom')).toBe(today)
    expect(params.get('dueDateTo')).toBe(today)
  })

  it('Demnächst deckt die nächsten sieben Tage ohne heute ab', () => {
    const params = standardViewParams('upcoming', 'UTC')
    const today = todayInZone('UTC')
    expect(params.get('dueDateFrom')).toBe(addDays(today, 1))
    expect(params.get('dueDateTo')).toBe(addDays(today, 7))
  })

  it('Überfällig setzt nur das overdue-Flag', () => {
    const params = standardViewParams('overdue', 'UTC')
    expect(params.get('overdue')).toBe('true')
    expect(params.has('status')).toBe(false)
  })

  it('Erledigt filtert auf Status DONE', () => {
    const params = standardViewParams('done', 'UTC')
    expect(params.getAll('status')).toEqual(['DONE'])
  })
})

describe('matchesStandardView', () => {
  it('erkennt die aktive Ansicht unabhängig von Suche, Projekt, Tag oder Sortierung', () => {
    const current = new URLSearchParams('overdue=true&q=steuer&sortBy=title')
    expect(matchesStandardView(current, 'overdue', 'UTC')).toBe(true)
    expect(matchesStandardView(current, 'done', 'UTC')).toBe(false)
  })

  it('erkennt "Alle" nur ohne aktive Standardansicht-Parameter', () => {
    expect(matchesStandardView(new URLSearchParams(''), 'all', 'UTC')).toBe(true)
    expect(matchesStandardView(new URLSearchParams('status=DONE'), 'all', 'UTC')).toBe(false)
  })
})
