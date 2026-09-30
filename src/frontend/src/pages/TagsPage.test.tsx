import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { mockApi, pageOf, renderAt, testUser } from '../testUtils'
import type { Tag } from '../api/tags'

afterEach(() => vi.restoreAllMocks())

const loggedIn = {
  'GET /api/v1/auth/me': () => ({ body: testUser }),
  'GET /api/v1/projects?page=0&size=100': () => ({ body: pageOf([]) }),
}

function testTag(overrides: Partial<Tag> = {}): Tag {
  return {
    id: 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    name: 'Wichtig',
    scope: 'PERSONAL',
    createdAt: '2026-09-30T05:00:00Z',
    ...overrides,
  }
}

describe('Tags', () => {
  it('zeigt einen leeren Zustand ohne Tags', async () => {
    mockApi({ ...loggedIn, 'GET /api/v1/tags': () => ({ body: [] }) })
    renderAt('/tags')
    expect(await screen.findByText('Noch keine persönlichen Tags.')).toBeInTheDocument()
  })

  it('legt einen persönlichen Tag an und zeigt ihn in der Liste', async () => {
    const created = testTag({ id: '999', name: 'Dringend' })
    let tags: Tag[] = []
    const { calls } = mockApi({
      ...loggedIn,
      'GET /api/v1/tags': () => ({ body: tags }),
      'POST /api/v1/tags': () => {
        tags = [created]
        return { status: 201, body: created }
      },
    })
    renderAt('/tags')
    await userEvent.type(await screen.findByLabelText('Name'), 'Dringend')
    await userEvent.click(screen.getByRole('button', { name: 'Tag anlegen' }))
    expect(await screen.findByText('Dringend')).toBeInTheDocument()
    expect(await screen.findByText('Der Tag „Dringend“ wurde angelegt.')).toBeInTheDocument()
    expect(calls.find((c) => c.key === 'POST /api/v1/tags')?.body).toEqual({ name: 'Dringend', projectId: null })
  })

  it('zeigt einen Fehler bei leerem Namen ohne Serveraufruf', async () => {
    const { calls } = mockApi({ ...loggedIn, 'GET /api/v1/tags': () => ({ body: [] }) })
    renderAt('/tags')
    await screen.findByText('Noch keine persönlichen Tags.')
    await userEvent.click(screen.getByRole('button', { name: 'Tag anlegen' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Tag-Name')
    expect(calls.filter((c) => c.key.startsWith('POST'))).toHaveLength(0)
  })

  it('benennt einen Tag um', async () => {
    let tag = testTag()
    mockApi({
      ...loggedIn,
      'GET /api/v1/tags': () => ({ body: [tag] }),
      [`PUT /api/v1/tags/${tag.id}`]: () => {
        tag = { ...tag, name: 'Umbenannt' }
        return { body: tag }
      },
    })
    renderAt('/tags')
    await userEvent.click(await screen.findByRole('button', { name: /Umbenennen/ }))
    const name = screen.getByLabelText(/Name für/)
    await userEvent.clear(name)
    await userEvent.type(name, 'Umbenannt')
    await userEvent.click(screen.getByRole('button', { name: 'Speichern' }))
    expect(await screen.findByText('Umbenannt')).toBeInTheDocument()
    expect(await screen.findByText('Der Tag wurde in „Umbenannt“ umbenannt.')).toBeInTheDocument()
  })

  it('löscht einen Tag über den Bestätigungsdialog', async () => {
    const tag = testTag()
    let tags = [tag]
    mockApi({
      ...loggedIn,
      'GET /api/v1/tags': () => ({ body: tags }),
      [`DELETE /api/v1/tags/${tag.id}`]: () => {
        tags = []
        return { status: 204 }
      },
    })
    renderAt('/tags')
    await userEvent.click(await screen.findByRole('button', { name: /^Löschen/ }))
    await userEvent.click(screen.getByRole('button', { name: 'Endgültig löschen' }))
    expect(await screen.findByText('Noch keine persönlichen Tags.')).toBeInTheDocument()
    expect(await screen.findByText('Der Tag wurde gelöscht.')).toBeInTheDocument()
  })

  it('zeigt projektbezogene Tags getrennt von persönlichen Tags', async () => {
    const project = { id: 'p1', name: 'Umzug', ownerId: testUser.id, createdAt: '2026-09-30T05:00:00Z', updatedAt: '2026-09-30T05:00:00Z', version: 0 }
    const personal = testTag({ id: 't1', name: 'Persönlich' })
    const projectTag = testTag({ id: 't2', name: 'Projekt-Tag', scope: 'PROJECT', projectId: 'p1' })
    mockApi({
      'GET /api/v1/auth/me': () => ({ body: testUser }),
      'GET /api/v1/projects?page=0&size=100': () => ({ body: pageOf([project], { totalElements: 1 }) }),
      'GET /api/v1/tags': () => ({ body: [personal, projectTag] }),
    })
    renderAt('/tags')
    expect(await screen.findByText('Persönlich')).toBeInTheDocument()
    expect(await screen.findByRole('heading', { level: 2, name: 'Tags in „Umzug“' })).toBeInTheDocument()
    expect(screen.getByText('Projekt-Tag')).toBeInTheDocument()
  })

  it('nennt bei Projekten anderer Eigentümer den Eigentümer in der Überschrift (US-181)', async () => {
    const mine = { id: 'p1', name: 'Kaffee', ownerId: testUser.id, ownerDisplayName: 'Max', createdAt: '2026-09-30T05:00:00Z', updatedAt: '2026-09-30T05:00:00Z', version: 0 }
    const foreign = { id: 'p2', name: 'Kaffee', ownerId: '99999999-9999-9999-9999-999999999999', ownerDisplayName: 'Uli', createdAt: '2026-09-30T05:00:00Z', updatedAt: '2026-09-30T05:00:00Z', version: 0 }
    mockApi({
      'GET /api/v1/auth/me': () => ({ body: testUser }),
      'GET /api/v1/projects?page=0&size=100': () => ({ body: pageOf([mine, foreign], { totalElements: 2 }) }),
      'GET /api/v1/tags': () => ({
        body: [
          testTag({ id: 't1', name: 'Backen', scope: 'PROJECT', projectId: 'p1' }),
          testTag({ id: 't2', name: 'Backen', scope: 'PROJECT', projectId: 'p2' }),
        ],
      }),
    })
    renderAt('/tags')
    expect(await screen.findByRole('heading', { level: 2, name: 'Tags in „Kaffee“' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { level: 2, name: 'Tags in „Kaffee“ (von Uli)' })).toBeInTheDocument()
  })
})
