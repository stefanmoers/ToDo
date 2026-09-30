import { describe, expect, it } from 'vitest'
import type { Task } from '../../api/tasks'
import { diffDrafts, emptyDraft, toDraft, toInput } from './draft'

const task: Task = {
  id: '1',
  title: 'Einkaufen',
  description: 'Milch, Brot',
  status: 'OPEN',
  priority: 'HIGH',
  dueDate: '2026-07-15',
  dueTime: '09:00',
  dueTimeZone: 'Europe/Berlin',
  dueOffset: '+02:00',
  dueAt: '2026-07-15T07:00:00Z',
  overdue: false,
  creatorId: 'u1',
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
  version: 3,
  tags: [],
}

describe('toInput', () => {
  it('trimmt den Titel und wandelt leere Beschreibung in null', () => {
    const draft = { ...emptyDraft('Europe/Berlin'), title: '  Einkaufen  ', description: '   ' }
    const input = toInput(draft)
    expect(input.title).toBe('Einkaufen')
    expect(input.description).toBeNull()
  })

  it('setzt Uhrzeit, Zone und Offset auf null, wenn kein Datum gesetzt ist', () => {
    const draft = { ...emptyDraft('Europe/Berlin'), title: 'X', dueTime: '09:00', dueOffset: '+02:00' }
    const input = toInput(draft)
    expect(input.dueDate).toBeNull()
    expect(input.dueTime).toBeNull()
    expect(input.dueTimeZone).toBeNull()
    expect(input.dueOffset).toBeNull()
  })

  it('behält gesetzte Fälligkeitsfelder', () => {
    const draft = toDraft(task, 'Europe/Berlin')
    const input = toInput(draft)
    expect(input).toEqual({
      projectId: null,
      title: 'Einkaufen',
      description: 'Milch, Brot',
      priority: 'HIGH',
      dueDate: '2026-07-15',
      dueTime: '09:00',
      dueTimeZone: 'Europe/Berlin',
      dueOffset: '+02:00',
    })
  })
})

describe('toDraft und toInput', () => {
  it('ergeben hin und zurück wieder denselben Input', () => {
    const draft = toDraft(task, 'Europe/Berlin')
    expect(toInput(draft).title).toBe(task.title)
    expect(toInput(draft).description).toBe(task.description)
  })
})

describe('diffDrafts', () => {
  it('findet nur die tatsächlich abweichenden Felder', () => {
    const a = emptyDraft('Europe/Berlin')
    const b = { ...a, title: 'Anders', priority: 'HIGH' as const }
    expect(diffDrafts(a, b)).toEqual(['title', 'priority'])
  })

  it('liefert eine leere Liste bei identischen Entwürfen', () => {
    const a = emptyDraft('Europe/Berlin')
    expect(diffDrafts(a, { ...a })).toEqual([])
  })
})
