import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { mockApi, pageOf, renderAt, testUser } from '../testUtils'
import type { Project } from '../api/projects'

afterEach(() => vi.restoreAllMocks())

const loggedIn = { 'GET /api/v1/auth/me': () => ({ body: testUser }) }
const PROJECT_ID = '44444444-4444-4444-4444-444444444444'

function testProject(overrides: Partial<Project> = {}): Project {
  return {
    id: PROJECT_ID,
    name: 'Umzug',
    ownerId: testUser.id,
    createdAt: '2026-09-30T05:00:00Z',
    updatedAt: '2026-09-30T05:00:00Z',
    version: 0,
    ...overrides,
  }
}

describe('Projektliste', () => {
  it('zeigt einen leeren Zustand mit Link zum Anlegen', async () => {
    mockApi({ ...loggedIn, 'GET /api/v1/projects?page=0&size=25': () => ({ body: pageOf([]) }) })
    renderAt('/projekte')
    expect(await screen.findByText('Du hast noch keine Projekte.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Erstes Projekt anlegen' })).toBeInTheDocument()
  })

  it('zeigt vorhandene Projekte', async () => {
    mockApi({
      ...loggedIn,
      'GET /api/v1/projects?page=0&size=25': () => ({ body: pageOf([testProject()], { totalElements: 1 }) }),
    })
    renderAt('/projekte')
    expect(await screen.findByRole('link', { name: 'Umzug' })).toBeInTheDocument()
  })
})

describe('Projekt anlegen', () => {
  it('zeigt einen Fehler bei leerem Namen ohne Serveraufruf', async () => {
    const { calls } = mockApi({ ...loggedIn })
    renderAt('/projekte/neu')
    await userEvent.click(await screen.findByRole('button', { name: 'Projekt anlegen' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Name')
    expect(calls.filter((c) => c.key.startsWith('POST'))).toHaveLength(0)
  })

  it('legt ein Projekt an und zeigt die Detailseite', async () => {
    const created = testProject({ id: '999', name: 'Neues Projekt' })
    mockApi({
      ...loggedIn,
      'POST /api/v1/projects': () => ({ status: 201, body: created }),
      'GET /api/v1/projects/999': () => ({ body: created }),
    })
    renderAt('/projekte/neu')
    await userEvent.type(await screen.findByLabelText('Name'), 'Neues Projekt')
    await userEvent.click(screen.getByRole('button', { name: 'Projekt anlegen' }))
    expect(await screen.findByRole('heading', { level: 1, name: 'Neues Projekt' })).toBeInTheDocument()
  })
})

describe('Projekt ansehen', () => {
  it('zeigt „Projekt nicht gefunden" bei 404', async () => {
    mockApi({
      ...loggedIn,
      [`GET /api/v1/projects/${PROJECT_ID}`]: () => ({ status: 404, body: { code: 'PROJECT_NOT_FOUND', message: 'x' } }),
    })
    renderAt(`/projekte/${PROJECT_ID}`)
    expect(await screen.findByRole('heading', { level: 1, name: 'Projekt nicht gefunden' })).toBeInTheDocument()
  })
})

describe('Projekt bearbeiten', () => {
  it('speichert Änderungen mit der geladenen Version', async () => {
    let current = testProject({ version: 2 })
    mockApi({
      ...loggedIn,
      [`GET /api/v1/projects/${PROJECT_ID}`]: () => ({ body: current }),
      [`PUT /api/v1/projects/${PROJECT_ID}`]: () => {
        current = { ...current, name: 'Geändert', version: 3 }
        return { body: current }
      },
    })
    renderAt(`/projekte/${PROJECT_ID}/bearbeiten`)
    const name = await screen.findByLabelText('Name')
    await userEvent.clear(name)
    await userEvent.type(name, 'Geändert')
    await userEvent.click(screen.getByRole('button', { name: 'Änderungen speichern' }))
    expect(await screen.findByRole('heading', { level: 1, name: 'Geändert' })).toBeInTheDocument()
  })

  it('zeigt bei 409 einen Konflikt und behält die eigene Eingabe', async () => {
    const project = testProject({ version: 2 })
    const serverVersion = { ...project, name: 'Von A geändert', version: 3 }
    let getCount = 0
    mockApi({
      ...loggedIn,
      [`GET /api/v1/projects/${PROJECT_ID}`]: () => {
        getCount++
        return { body: getCount === 1 ? project : serverVersion }
      },
      [`PUT /api/v1/projects/${PROJECT_ID}`]: () => ({ status: 409, body: { code: 'VERSION_CONFLICT', message: 'x' } }),
    })
    renderAt(`/projekte/${PROJECT_ID}/bearbeiten`)
    const name = await screen.findByLabelText('Name')
    await userEvent.clear(name)
    await userEvent.type(name, 'Von B')
    await userEvent.click(screen.getByRole('button', { name: 'Änderungen speichern' }))

    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent('zwischenzeitlich geändert')
    expect(screen.getByLabelText('Name')).toHaveValue('Von B')
  })
})

describe('Projekt löschen', () => {
  it('löscht endgültig und zeigt die Bestätigung auf der Liste', async () => {
    const project = testProject()
    mockApi({
      ...loggedIn,
      [`GET /api/v1/projects/${PROJECT_ID}`]: () => ({ body: project }),
      [`DELETE /api/v1/projects/${PROJECT_ID}?version=0`]: () => ({ status: 204 }),
      'GET /api/v1/projects?page=0&size=25': () => ({ body: pageOf([]) }),
    })
    renderAt(`/projekte/${PROJECT_ID}`)
    await userEvent.click(await screen.findByRole('button', { name: 'Löschen' }))
    await userEvent.click(screen.getByRole('button', { name: 'Endgültig löschen' }))
    expect(await screen.findByText(/wurde gelöscht/)).toBeInTheDocument()
  })
})

describe('Mitglieder', () => {
  it('Eigentümer nimmt ein Mitglied per E-Mail auf', async () => {
    const project = testProject()
    let members: { userId: string; displayName: string; addedAt: string }[] = []
    mockApi({
      ...loggedIn,
      [`GET /api/v1/projects/${PROJECT_ID}`]: () => ({ body: project }),
      [`GET /api/v1/projects/${PROJECT_ID}/members`]: () => ({ body: members }),
      [`POST /api/v1/projects/${PROJECT_ID}/members`]: () => {
        const member = { userId: 'mem-1', displayName: 'Bruno Bearbeiter', addedAt: '2026-09-30T05:00:00Z' }
        members = [member]
        return { status: 201, body: member }
      },
    })
    renderAt(`/projekte/${PROJECT_ID}`)
    expect(await screen.findByText('Noch keine Mitglieder außer dir.')).toBeInTheDocument()
    await userEvent.type(screen.getByLabelText('Mitglied per E-Mail aufnehmen'), 'bruno@example.test')
    await userEvent.click(screen.getByRole('button', { name: 'Aufnehmen' }))
    expect(await screen.findByText('„Bruno Bearbeiter“ wurde aufgenommen.')).toBeInTheDocument()
    expect(screen.getByText('Bruno Bearbeiter')).toBeInTheDocument()
  })

  it('zeigt einen Feldfehler bei unbekannter E-Mail', async () => {
    const project = testProject()
    mockApi({
      ...loggedIn,
      [`GET /api/v1/projects/${PROJECT_ID}`]: () => ({ body: project }),
      [`GET /api/v1/projects/${PROJECT_ID}/members`]: () => ({ body: [] }),
      [`POST /api/v1/projects/${PROJECT_ID}/members`]: () => ({
        status: 400,
        body: { code: 'VALIDATION_FAILED', message: 'x', fieldErrors: [{ field: 'email', code: 'NOT_FOUND', message: 'Kein Konto mit dieser E-Mail-Adresse gefunden.' }] },
      }),
    })
    renderAt(`/projekte/${PROJECT_ID}`)
    await userEvent.type(await screen.findByLabelText('Mitglied per E-Mail aufnehmen'), 'unbekannt@example.test')
    await userEvent.click(screen.getByRole('button', { name: 'Aufnehmen' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Kein Konto mit dieser E-Mail-Adresse gefunden.')
  })

  it('Nicht-Eigentümer sieht Mitglieder, aber keine Verwaltung', async () => {
    const project = testProject({ ownerId: 'anderer-eigentuemer' })
    mockApi({
      ...loggedIn,
      [`GET /api/v1/projects/${PROJECT_ID}`]: () => ({ body: project }),
      [`GET /api/v1/projects/${PROJECT_ID}/members`]: () => ({
        body: [{ userId: testUser.id, displayName: 'Test Person', addedAt: '2026-09-30T05:00:00Z' }],
      }),
    })
    renderAt(`/projekte/${PROJECT_ID}`)
    expect(await screen.findByText('Test Person')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Entfernen/ })).not.toBeInTheDocument()
    expect(screen.queryByLabelText('Mitglied per E-Mail aufnehmen')).not.toBeInTheDocument()
  })

  it('Eigentümer entfernt ein Mitglied über den Bestätigungsdialog', async () => {
    const project = testProject()
    let members = [{ userId: 'mem-1', displayName: 'Bruno Bearbeiter', addedAt: '2026-09-30T05:00:00Z' }]
    mockApi({
      ...loggedIn,
      [`GET /api/v1/projects/${PROJECT_ID}`]: () => ({ body: project }),
      [`GET /api/v1/projects/${PROJECT_ID}/members`]: () => ({ body: members }),
      [`DELETE /api/v1/projects/${PROJECT_ID}/members/mem-1`]: () => {
        members = []
        return { status: 204 }
      },
    })
    renderAt(`/projekte/${PROJECT_ID}`)
    await userEvent.click(await screen.findByRole('button', { name: /^Entfernen/ }))
    await userEvent.click(screen.getByRole('button', { name: 'Endgültig entfernen' }))
    expect(await screen.findByText('„Bruno Bearbeiter“ wurde entfernt.')).toBeInTheDocument()
    expect(await screen.findByText('Noch keine Mitglieder außer dir.')).toBeInTheDocument()
  })
})
