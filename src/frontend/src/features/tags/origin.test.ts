import { describe, expect, it } from 'vitest'
import type { Tag } from '../../api/tags'
import { tagLabels, tagOrigin } from './origin'

const ME = '11111111-1111-1111-1111-111111111111'
const OTHER = '22222222-2222-2222-2222-222222222222'

function tag(overrides: Partial<Tag>): Tag {
  return { id: 't', name: 'Backen', scope: 'PERSONAL', createdAt: '2026-09-30T05:00:00Z', ...overrides }
}

const ownProject = tag({
  id: 'own',
  scope: 'PROJECT',
  projectId: 'p1',
  projectName: 'Kaffee',
  projectOwnerId: ME,
  projectOwnerDisplayName: 'Max',
})
const foreignProject = tag({
  id: 'foreign',
  scope: 'PROJECT',
  projectId: 'p2',
  projectName: 'Kaffee',
  projectOwnerId: OTHER,
  projectOwnerDisplayName: 'Uli',
})

describe('tagOrigin (US-181)', () => {
  it('nennt persönliche Tags „persönlich“', () => {
    expect(tagOrigin(tag({}), ME)).toBe('persönlich')
  })

  it('nennt bei Projekt-Tags Projekt und Eigentümer', () => {
    expect(tagOrigin(foreignProject, ME)).toBe('Projekt „Kaffee“ (Uli)')
  })

  it('nennt den eigenen Eigentümer „du“', () => {
    expect(tagOrigin(ownProject, ME)).toBe('Projekt „Kaffee“ (du)')
  })

  it('fällt ohne Angaben auf neutrale Wörter zurück', () => {
    expect(tagOrigin(tag({ scope: 'PROJECT', projectId: 'p9' }), ME)).toBe('Projekt (unbekannt)')
  })
})

describe('tagLabels (US-181)', () => {
  it('lässt eindeutige Namen kurz', () => {
    const labels = tagLabels([tag({ id: 'a', name: 'Wichtig' }), tag({ id: 'b', name: 'Eilig' })], ME)
    expect(labels.get('a')).toEqual({ name: 'Wichtig', origin: undefined, text: 'Wichtig' })
    expect(labels.get('b')?.text).toBe('Eilig')
  })

  it('ergänzt die Herkunft bei gleichnamigen Tags und macht sie dadurch unterscheidbar', () => {
    const personal = tag({ id: 'personal' })
    const labels = tagLabels([personal, ownProject, foreignProject], ME)
    const texts = [...labels.values()].map((l) => l.text)
    expect(texts).toEqual([
      'Backen · persönlich',
      'Backen · Projekt „Kaffee“ (du)',
      'Backen · Projekt „Kaffee“ (Uli)',
    ])
    expect(new Set(texts).size).toBe(3)
  })

  it('vergleicht Namen ohne Beachtung der Großschreibung', () => {
    const labels = tagLabels([tag({ id: 'a', name: 'backen' }), foreignProject], ME)
    expect(labels.get('a')?.origin).toBe('persönlich')
    expect(labels.get('foreign')?.origin).toBe('Projekt „Kaffee“ (Uli)')
  })

  it('kennzeichnet nur die mehrfach vorkommenden Namen', () => {
    const labels = tagLabels([tag({ id: 'a', name: 'Wichtig' }), ownProject, foreignProject], ME)
    expect(labels.get('a')?.origin).toBeUndefined()
    expect(labels.get('own')?.origin).toBeDefined()
  })
})
