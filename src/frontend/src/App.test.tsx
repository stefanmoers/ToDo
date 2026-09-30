import { screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { mockApi, renderAt, testUser } from './testUtils'

afterEach(() => vi.restoreAllMocks())

describe('App', () => {
  it('leitet nicht angemeldete Besucher zur Anmeldung', async () => {
    mockApi({ 'GET /api/v1/auth/me': () => ({ status: 401, body: { code: 'UNAUTHENTICATED', message: 'Bitte melde dich an.' } }) })
    renderAt('/')
    expect(await screen.findByRole('heading', { level: 1, name: 'Anmelden' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Zum Hauptinhalt springen' })).toHaveAttribute('href', '#main')
    expect(document.title).toBe('Anmelden – UltimateTodo')
  })

  it('zeigt angemeldeten Benutzern die Aufgabenliste', async () => {
    mockApi({
      'GET /api/v1/auth/me': () => ({ body: testUser }),
      'GET /api/v1/tasks?page=0&size=25': () => ({ body: { items: [], page: 0, size: 25, totalElements: 0, totalPages: 0 } }),
    })
    renderAt('/')
    expect(await screen.findByText(/Hallo Max!/)).toBeInTheDocument()
    expect(screen.getByRole('heading', { level: 1, name: 'Meine Aufgaben' })).toBeInTheDocument()
  })

  it('zeigt für unbekannte Adressen eine Nicht-gefunden-Seite', async () => {
    mockApi({ 'GET /api/v1/auth/me': () => ({ status: 401, body: { code: 'UNAUTHENTICATED', message: 'x' } }) })
    renderAt('/gibt-es-nicht')
    expect(await screen.findByRole('heading', { level: 1, name: 'Seite nicht gefunden' })).toBeInTheDocument()
  })
})
