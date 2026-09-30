import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { mockApi, renderAt, testUser } from '../../testUtils'

describe('SessionExpiredDialog (US-023)', () => {
  afterEach(() => vi.restoreAllMocks())

  it('bewahrt einen ungesendeten Formularentwurf, wenn die Sitzung während einer Aktion abläuft', async () => {
    const { calls } = mockApi({
      'GET /api/v1/auth/me': () => ({ body: testUser }),
      'POST /api/v1/tasks': () => ({ status: 401, body: { code: 'UNAUTHENTICATED', message: 'Bitte melde dich an.' } }),
      'POST /api/v1/auth/login': () => ({ body: testUser }),
    })
    renderAt('/aufgaben/neu')

    await userEvent.type(await screen.findByLabelText('Titel'), 'Mein Entwurf')
    await userEvent.click(screen.getByRole('button', { name: 'Aufgabe anlegen' }))

    // Kein Sprung zur Anmeldeseite: die Seite mit dem Entwurf bleibt sichtbar, ein Dialog verlangt die erneute Anmeldung.
    expect(await screen.findByRole('heading', { level: 2, name: 'Sitzung abgelaufen' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { level: 1, name: 'Neue Aufgabe' })).toBeInTheDocument()
    expect(screen.getByLabelText('Titel')).toHaveValue('Mein Entwurf')

    await userEvent.type(screen.getByLabelText('E-Mail-Adresse'), testUser.email)
    await userEvent.type(screen.getByLabelText('Passwort'), 'ein-beliebiges-passwort-15')
    await userEvent.click(screen.getByRole('button', { name: 'Anmelden' }))

    expect(await screen.findByLabelText('Titel')).toHaveValue('Mein Entwurf')
    expect(screen.queryByRole('heading', { name: 'Sitzung abgelaufen' })).not.toBeInTheDocument()
    expect(calls.some((c) => c.key === 'POST /api/v1/auth/login')).toBe(true)
  })

  it('bricht auf Wunsch ab und meldet endgültig ab', async () => {
    mockApi({
      'GET /api/v1/auth/me': () => ({ body: testUser }),
      'POST /api/v1/tasks': () => ({ status: 401, body: { code: 'UNAUTHENTICATED', message: 'x' } }),
    })
    renderAt('/aufgaben/neu')
    await userEvent.type(await screen.findByLabelText('Titel'), 'X')
    await userEvent.click(screen.getByRole('button', { name: 'Aufgabe anlegen' }))
    await screen.findByRole('heading', { level: 2, name: 'Sitzung abgelaufen' })

    await userEvent.click(screen.getByRole('button', { name: 'Abmelden' }))
    expect(await screen.findByRole('heading', { level: 1, name: 'Anmelden' })).toBeInTheDocument()
  })
})
