import { describe, expect, it } from 'vitest'
import type { Task } from '../../api/tasks'
import { formatDue, formatTimestamp } from './format'

const base: Task = {
  id: '1',
  title: 'X',
  status: 'OPEN',
  priority: 'NORMAL',
  overdue: false,
  creatorId: 'u1',
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
  version: 0,
  tags: [],
  subtasks: { done: 0, total: 0 },
}

describe('formatDue', () => {
  it('zeigt "Keine Fälligkeit" ohne Datum', () => {
    expect(formatDue(base, 'Europe/Berlin')).toBe('Keine Fälligkeit')
  })

  it('zeigt ein reines Datum ohne Zeitverschiebung, egal in welcher Betrachter-Zeitzone', () => {
    const task: Task = { ...base, dueDate: '2026-07-15' }
    const berlin = formatDue(task, 'Europe/Berlin')
    const kiritimati = formatDue(task, 'Pacific/Kiritimati')
    expect(berlin).toContain('15. Juli 2026')
    expect(kiritimati).toContain('15. Juli 2026')
  })

  it('zeigt bei abweichender Betrachter-Zeitzone eine Zusatzangabe', () => {
    const task: Task = {
      ...base,
      dueDate: '2026-07-15',
      dueTime: '09:00',
      dueTimeZone: 'Europe/Berlin',
      dueOffset: '+02:00',
      dueAt: '2026-07-15T07:00:00Z',
    }
    expect(formatDue(task, 'Europe/Berlin')).not.toContain('bei dir')
    expect(formatDue(task, 'America/New_York')).toContain('bei dir')
  })
})

describe('formatTimestamp', () => {
  it('formatiert in der angegebenen Zeitzone', () => {
    expect(formatTimestamp('2026-01-01T12:00:00Z', 'UTC')).toContain('2026')
  })
})
