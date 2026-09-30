import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { AdminUser } from '../api/admin'
import { mockApi, pageOf, renderAt, testUser } from '../testUtils'

afterEach(() => vi.restoreAllMocks())

const admin = { ...testUser, role: 'ADMIN' as const }
const loggedInAsAdmin = { 'GET /api/v1/auth/me': () => ({ body: admin }) }

function testAccount(overrides: Partial<AdminUser> = {}): AdminUser {
  return {
    id: 'mem-1',
    email: 'bruno@example.test',
    displayName: 'Bruno Bearbeiter',
    status: 'ACTIVE',
    role: 'USER',
    createdAt: '2026-09-30T05:00:00Z',
    ...overrides,
  }
}

describe('Konten administrieren (US-014)', () => {
  it('listet Konten mit Status und Rolle auf', async () => {
    mockApi({
      ...loggedInAsAdmin,
      'GET /api/v1/admin/users?page=0&size=25': () => ({ body: pageOf([testAccount()]) }),
    })
    renderAt('/administration')
    expect(await screen.findByText('bruno@example.test')).toBeInTheDocument()
    expect(screen.getByText('Bruno Bearbeiter')).toBeInTheDocument()
    expect(screen.getByText('Benutzer')).toBeInTheDocument()
    expect(screen.getByText('Aktiv')).toBeInTheDocument()
  })

  it('deaktiviert ein Konto über den Bestätigungsdialog', async () => {
    let account = testAccount()
    mockApi({
      ...loggedInAsAdmin,
      'GET /api/v1/admin/users?page=0&size=25': () => ({ body: pageOf([account]) }),
      'POST /api/v1/admin/users/mem-1/deactivate': () => {
        account = { ...account, status: 'INACTIVE' }
        return { body: account }
      },
    })
    renderAt('/administration')
    await userEvent.click(await screen.findByRole('button', { name: /^Deaktivieren/ }))
    await userEvent.click(screen.getByRole('button', { name: 'Endgültig deaktivieren' }))
    expect(await screen.findByText('„Bruno Bearbeiter“ wurde deaktiviert.')).toBeInTheDocument()
  })

  it('zeigt bei letztem aktivem Administratorkonto einen Alarm statt eines stillen Fehlschlags', async () => {
    mockApi({
      ...loggedInAsAdmin,
      'GET /api/v1/admin/users?page=0&size=25': () => ({ body: pageOf([testAccount({ role: 'ADMIN' })]) }),
      'POST /api/v1/admin/users/mem-1/deactivate': () => ({
        status: 409,
        body: { code: 'LAST_ACTIVE_ADMIN', message: 'Das letzte aktive Administratorkonto kann nicht deaktiviert werden.' },
      }),
    })
    renderAt('/administration')
    await userEvent.click(await screen.findByRole('button', { name: /^Deaktivieren/ }))
    await userEvent.click(screen.getByRole('button', { name: 'Endgültig deaktivieren' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Das letzte aktive Administratorkonto kann nicht deaktiviert werden.')
  })

  it('reaktiviert ein deaktiviertes Konto ohne Bestätigungsdialog', async () => {
    let account = testAccount({ status: 'INACTIVE' })
    mockApi({
      ...loggedInAsAdmin,
      'GET /api/v1/admin/users?page=0&size=25': () => ({ body: pageOf([account]) }),
      'POST /api/v1/admin/users/mem-1/reactivate': () => {
        account = { ...account, status: 'ACTIVE' }
        return { body: account }
      },
    })
    renderAt('/administration')
    await userEvent.click(await screen.findByRole('button', { name: /^Reaktivieren/ }))
    expect(await screen.findByText('„Bruno Bearbeiter“ wurde reaktiviert.')).toBeInTheDocument()
  })
})
