import { describe, expect, it } from 'vitest'
import { emptyDraft } from './draft'
import { validateDescription, validateTaskDraft, validateTitle } from './validation'

describe('validateTitle', () => {
  it('zählt Codepoints, nicht UTF-16-Einheiten', () => {
    expect(validateTitle('x'.repeat(200))).toBeUndefined()
    expect(validateTitle('x'.repeat(201))).toBeDefined()
    expect(validateTitle('😀'.repeat(200))).toBeUndefined()
  })

  it('lehnt leeren oder nur aus Leerzeichen bestehenden Titel ab', () => {
    expect(validateTitle('')).toBeDefined()
    expect(validateTitle('    ')).toBeDefined()
  })
})

describe('validateDescription', () => {
  it('erlaubt bis zu 10.000 Zeichen, lehnt mehr ab', () => {
    expect(validateDescription('x'.repeat(10_000))).toBeUndefined()
    expect(validateDescription('x'.repeat(10_001))).toBeDefined()
  })

  it('ist optional', () => {
    expect(validateDescription('')).toBeUndefined()
  })
})

describe('validateTaskDraft', () => {
  it('meldet eine Uhrzeit ohne Datum am Feld dueTime', () => {
    const draft = { ...emptyDraft('Europe/Berlin'), title: 'X', dueTime: '09:00' }
    expect(validateTaskDraft(draft).dueTime).toBeDefined()
  })

  it('meldet ein ungültiges Datum am Feld dueDate', () => {
    const draft = { ...emptyDraft('Europe/Berlin'), title: 'X', dueDate: '2026-07-15', dateBadInput: true }
    expect(validateTaskDraft(draft).dueDate).toBeDefined()
  })

  it('meldet eine Sommerzeit-Lücke am Feld dueTime', () => {
    const draft = {
      ...emptyDraft('Europe/Berlin'),
      title: 'X',
      dueDate: '2026-03-29',
      dueTime: '02:30',
    }
    expect(validateTaskDraft(draft).dueTime).toMatch(/Zeitumstellung/)
  })

  it('meldet eine doppelte Uhrzeit ohne Auswahl am Feld dueOffset', () => {
    const draft = {
      ...emptyDraft('Europe/Berlin'),
      title: 'X',
      dueDate: '2026-10-25',
      dueTime: '02:30',
    }
    expect(validateTaskDraft(draft).dueOffset).toBeDefined()
  })

  it('ist fehlerfrei bei nur einem Titel', () => {
    const draft = { ...emptyDraft('Europe/Berlin'), title: 'Einkaufen' }
    const errors = validateTaskDraft(draft)
    expect(Object.values(errors).some(Boolean)).toBe(false)
  })
})
