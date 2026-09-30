import { render } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { vi } from 'vitest'
import App from './App'
import type { Page } from './api/pagination'
import type { Task } from './api/tasks'

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
  sessionTimeoutSeconds: 1800,
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

export function testTask(overrides: Partial<Task> = {}): Task {
  return {
    id: '22222222-2222-2222-2222-222222222222',
    title: 'Einkaufen',
    status: 'OPEN',
    priority: 'NORMAL',
    overdue: false,
    creatorId: testUser.id,
    createdAt: '2026-09-30T05:00:00Z',
    updatedAt: '2026-09-30T05:00:00Z',
    version: 0,
    tags: [],
    subtasks: { done: 0, total: 0 },
    ...overrides,
  }
}

export function pageOf<T>(items: T[], overrides: Partial<Page<T>> = {}): Page<T> {
  return { items, page: 0, size: 25, totalElements: items.length, totalPages: items.length === 0 ? 0 : 1, ...overrides }
}

export function renderAt(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <App />
    </MemoryRouter>,
  )
}
