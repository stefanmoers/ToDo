import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { mockApi, renderAt, testUser } from '../testUtils'
import { todayInZone } from '../features/tasks/standardViews'

afterEach(() => vi.restoreAllMocks())

const loggedIn = { 'GET /api/v1/auth/me': () => ({ body: testUser }) }
const today = todayInZone(testUser.timeZone)

describe('Übersicht', () => {
  it('zeigt die fünf Zähler und verweist von „überfällig“ auf die passende Liste', async () => {
    mockApi({
      ...loggedIn,
      [`GET /api/v1/tasks/summary?today=${today}`]: () => ({
        body: { openCount: 4, dueTodayCount: 1, overdueCount: 2, doneCount: 3, highPriorityOpenCount: 1 },
      }),
    })
    renderAt('/uebersicht')
    expect(await screen.findByRole('link', { name: '4 nicht erledigte Aufgaben' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '1 heute fällig' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '3 erledigt' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '1 offen mit hoher oder dringender Priorität' })).toBeInTheDocument()

    const overdueLink = screen.getByRole('link', { name: '2 überfällig' })
    expect(overdueLink).toHaveAttribute('href', '/?overdue=true')
  })

  it('zeigt einen Ladefehler mit Wiederholungsmöglichkeit', async () => {
    let attempt = 0
    mockApi({
      ...loggedIn,
      [`GET /api/v1/tasks/summary?today=${today}`]: () => {
        attempt++
        return attempt === 1
          ? { status: 500, body: { code: 'INTERNAL_ERROR', message: 'Kaputt.' } }
          : { body: { openCount: 0, dueTodayCount: 0, overdueCount: 0, doneCount: 0, highPriorityOpenCount: 0 } }
      },
    })
    renderAt('/uebersicht')
    expect(await screen.findByRole('alert')).toHaveTextContent('Kaputt.')
    await userEvent.click(screen.getByRole('button', { name: 'Erneut versuchen' }))
    expect(await screen.findByRole('link', { name: '0 nicht erledigte Aufgaben' })).toBeInTheDocument()
  })
})
