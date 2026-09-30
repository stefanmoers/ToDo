import { render } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { vi } from 'vitest'
import App from './App'

type Handler = (body: unknown, init?: RequestInit) => { status?: number; body?: unknown }

export const jsonResponse = (status: number, body?: unknown) =>
  new Response(body === undefined ? null : JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })

export const testUser = {
  id: '11111111-1111-1111-1111-111111111111',
  email: 'max@example.test',
  displayName: 'Max',
  timeZone: 'Europe/Berlin',
  role: 'USER',
  createdAt: '2026-09-30T05:00:00Z',
}

/** Ersetzt fetch durch eine Tabelle aus "METHODE /pfad" → Antwort. Unbekannte Aufrufe schlagen sichtbar fehl. */
export function mockApi(routes: Record<string, Handler>) {
  const calls: { key: string; body: unknown }[] = []
  const spy = vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
    const method = (init?.method ?? 'GET').toUpperCase()
    const key = `${method} ${String(input)}`
    if (key === 'GET /api/v1/auth/csrf') return jsonResponse(200, { headerName: 'X-CSRF-TOKEN', token: 't' })
    const handler = routes[key]
    if (!handler) throw new Error(`Unerwarteter Aufruf: ${key}`)
    const body = init?.body ? JSON.parse(String(init.body)) : undefined
    calls.push({ key, body })
    const r = handler(body, init)
    return jsonResponse(r.status ?? 200, r.body)
  })
  return { calls, spy }
}

export function renderAt(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <App />
    </MemoryRouter>,
  )
}
