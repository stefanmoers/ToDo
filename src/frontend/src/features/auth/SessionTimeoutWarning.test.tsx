import { fireEvent, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { mockApi, pageOf, renderAt, testUser } from '../../testUtils'

describe('Sitzungs-Timeout-Warnung (US-023)', () => {
  beforeEach(() => vi.useFakeTimers({ shouldAdvanceTime: true }))
  afterEach(() => {
    vi.useRealTimers()
    vi.restoreAllMocks()
  })

  it('zeigt die Warnung rechtzeitig vor Ablauf und verlängert bei "Angemeldet bleiben"', async () => {
    const user = { ...testUser, sessionTimeoutSeconds: 130 }
    let meCalls = 0
    mockApi({
      'GET /api/v1/auth/me': () => {
        meCalls++
        return { body: user }
      },
      'GET /api/v1/tasks?page=0&size=25': () => ({ body: pageOf([]) }),
    })
    renderAt('/')
    await screen.findByRole('heading', { level: 1, name: 'Meine Aufgaben' })
    expect(screen.queryByRole('heading', { name: 'Deine Sitzung läuft in Kürze ab' })).not.toBeInTheDocument()

    // Nach 20s verbleiben noch 110s (< 120s Warnschwelle).
    await vi.advanceTimersByTimeAsync(20_000)
    expect(await screen.findByRole('heading', { level: 2, name: 'Deine Sitzung läuft in Kürze ab' })).toBeInTheDocument()

    // "Angemeldet bleiben" verlängert nicht nur lokal, sondern löst eine echte Anfrage aus (der Server muss
    // die Inaktivität ebenfalls zurücksetzen) — das unterscheidet es von einem stillen Ausblenden im Browser.
    fireEvent.click(screen.getByRole('button', { name: 'Angemeldet bleiben' }))
    await vi.waitFor(() => expect(meCalls).toBeGreaterThanOrEqual(2))
  })

  it('meldet nach Ablauf ohne weitere Aktivität automatisch ab', async () => {
    const user = { ...testUser, sessionTimeoutSeconds: 20 }
    mockApi({
      'GET /api/v1/auth/me': () => ({ body: user }),
      'GET /api/v1/tasks?page=0&size=25': () => ({ body: pageOf([]) }),
    })
    renderAt('/')
    await screen.findByRole('heading', { level: 1, name: 'Meine Aufgaben' })

    await vi.advanceTimersByTimeAsync(30_000)
    expect(await screen.findByRole('heading', { level: 1, name: 'Anmelden' })).toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent('wegen Inaktivität abgemeldet')
  })

  it('schließt bei Escape wie "Angemeldet bleiben" (echte Anfrage statt stillem Ausblenden)', async () => {
    const user = { ...testUser, sessionTimeoutSeconds: 130 }
    let meCalls = 0
    mockApi({
      'GET /api/v1/auth/me': () => {
        meCalls++
        return { body: user }
      },
      'GET /api/v1/tasks?page=0&size=25': () => ({ body: pageOf([]) }),
    })
    renderAt('/')
    await screen.findByRole('heading', { level: 1, name: 'Meine Aufgaben' })
    await vi.advanceTimersByTimeAsync(20_000)
    const dialog = await screen.findByRole('dialog')
    fireEvent.keyDown(dialog, { key: 'Escape' })
    await vi.waitFor(() => expect(meCalls).toBeGreaterThanOrEqual(2))
  })
})
