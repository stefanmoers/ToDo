import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { mockApi, renderAt, testUser } from '../testUtils'

afterEach(() => vi.restoreAllMocks())

const unauth = { status: 401, body: { code: 'UNAUTHENTICATED', message: 'Bitte melde dich an.' } }

describe('Anmeldung', () => {
  it('meldet leere Felder an der Fehlerübersicht und am Feld, ohne die API aufzurufen', async () => {
    const { calls } = mockApi({ 'GET /api/v1/auth/me': () => unauth })
    renderAt('/anmelden')
    await userEvent.click(await screen.findByRole('button', { name: 'Anmelden' }))
    const summary = await screen.findByRole('alert')
    expect(summary).toHaveTextContent('Bitte gib deine E-Mail-Adresse ein.')
    expect(summary).toHaveFocus()
    const email = screen.getByLabelText('E-Mail-Adresse')
    expect(email).toHaveAttribute('aria-invalid', 'true')
    expect(email).toHaveAccessibleDescription(/Fehler:\s*Bitte gib deine E-Mail-Adresse ein/)
    expect(calls.filter((c) => c.key.startsWith('POST'))).toHaveLength(0)
  })

  it('zeigt bei falschen Zugangsdaten die allgemeine Meldung, behält die E-Mail und leert das Passwort', async () => {
    mockApi({
      'GET /api/v1/auth/me': () => unauth,
      'POST /api/v1/auth/login': () => ({
        status: 401,
        body: { code: 'INVALID_CREDENTIALS', message: 'E-Mail-Adresse oder Passwort ist nicht korrekt.' },
      }),
    })
    renderAt('/anmelden')
    await userEvent.type(await screen.findByLabelText('E-Mail-Adresse'), 'max@example.test')
    await userEvent.type(screen.getByLabelText('Passwort'), 'irgendwas')
    await userEvent.click(screen.getByRole('button', { name: 'Anmelden' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('E-Mail-Adresse oder Passwort ist nicht korrekt.')
    expect(screen.getByLabelText('E-Mail-Adresse')).toHaveValue('max@example.test')
    expect(screen.getByLabelText('Passwort')).toHaveValue('')
  })

  it('meldet erfolgreich an und zeigt die Startseite', async () => {
    let loggedIn = false
    const { calls } = mockApi({
      'GET /api/v1/auth/me': () => (loggedIn ? { body: testUser } : unauth),
      'POST /api/v1/auth/login': () => {
        loggedIn = true
        return { body: testUser }
      },
      'GET /api/v1/tasks?page=0&size=25': () => ({ body: { items: [], page: 0, size: 25, totalElements: 0, totalPages: 0 } }),
    })
    renderAt('/anmelden')
    await userEvent.type(await screen.findByLabelText('E-Mail-Adresse'), 'max@example.test')
    await userEvent.type(screen.getByLabelText('Passwort'), 'ein langes passwort')
    await userEvent.click(screen.getByRole('button', { name: 'Anmelden' }))
    expect(await screen.findByText(/Hallo Max!/)).toBeInTheDocument()
    expect(calls.find((c) => c.key === 'POST /api/v1/auth/login')?.body).toEqual({
      email: 'max@example.test',
      password: 'ein langes passwort',
    })
  })
})

describe('Registrierung', () => {
  it('prüft die Passwortlänge in Codepoints und zeigt die Regel', async () => {
    mockApi({ 'GET /api/v1/auth/me': () => unauth })
    renderAt('/registrieren')
    await userEvent.type(await screen.findByLabelText('E-Mail-Adresse'), 'neu@example.test')
    await userEvent.type(screen.getByLabelText('Anzeigename'), 'Neu')
    await userEvent.type(screen.getByLabelText('Passwort'), 'zu kurz')
    await userEvent.click(screen.getByRole('button', { name: 'Konto anlegen' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('zwischen 15 und 128 Zeichen')
  })

  it('ordnet die Serverantwort „E-Mail bereits registriert“ dem Feld zu und erhält den Entwurf', async () => {
    mockApi({
      'GET /api/v1/auth/me': () => unauth,
      'POST /api/v1/auth/register': () => ({
        status: 409,
        body: {
          code: 'EMAIL_ALREADY_REGISTERED',
          message: 'Diese E-Mail-Adresse ist bereits registriert.',
          fieldErrors: [{ field: 'email', code: 'TAKEN', message: 'Diese E-Mail-Adresse ist bereits registriert.' }],
        },
      }),
    })
    renderAt('/registrieren')
    await userEvent.type(await screen.findByLabelText('E-Mail-Adresse'), 'da@example.test')
    await userEvent.type(screen.getByLabelText('Anzeigename'), 'Da')
    await userEvent.type(screen.getByLabelText('Passwort'), 'ein langes passwort')
    await userEvent.click(screen.getByRole('button', { name: 'Konto anlegen' }))
    await waitFor(() => expect(screen.getByLabelText('E-Mail-Adresse')).toHaveAttribute('aria-invalid', 'true'))
    expect(screen.getByLabelText('E-Mail-Adresse')).toHaveValue('da@example.test')
    expect(screen.getByLabelText('Anzeigename')).toHaveValue('Da')
  })
})

describe('Profil', () => {
  it('nach Passwortänderung wird zur Anmeldung mit Hinweis geleitet', async () => {
    let changed = false
    mockApi({
      'GET /api/v1/auth/me': () => (changed ? unauth : { body: testUser }),
      'POST /api/v1/users/me/password': () => {
        changed = true
        return { status: 204 }
      },
    })
    renderAt('/profil')
    await userEvent.type(await screen.findByLabelText('Bisheriges Passwort'), 'altes passwort 1234')
    await userEvent.type(screen.getByLabelText('Neues Passwort'), 'ein neues langes passwort')
    await userEvent.click(screen.getByRole('button', { name: 'Passwort ändern' }))
    expect(await screen.findByRole('heading', { level: 1, name: 'Anmelden' })).toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent('Dein Passwort wurde geändert')
  })

  it('zeigt E-Mail als nicht änderbar an', async () => {
    mockApi({ 'GET /api/v1/auth/me': () => ({ body: testUser }) })
    renderAt('/profil')
    expect(await screen.findByText('max@example.test')).toBeInTheDocument()
    expect(screen.queryByLabelText('E-Mail-Adresse')).not.toBeInTheDocument()
  })
})
