import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { mockApi, pageOf, renderAt, testTask, testUser } from '../testUtils'
import type { TagSummary } from '../api/tagSummary'

afterEach(() => vi.restoreAllMocks())

const loggedIn = { 'GET /api/v1/auth/me': () => ({ body: testUser }) }
const TASK_ID = '22222222-2222-2222-2222-222222222222'
const TAG: TagSummary = { id: 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', name: 'Wichtig' }

describe('Aufgabenliste', () => {
  it('zeigt einen leeren Zustand mit Link zum Anlegen', async () => {
    mockApi({ ...loggedIn, 'GET /api/v1/tasks?page=0&size=25': () => ({ body: pageOf([]) }) })
    renderAt('/')
    expect(await screen.findByText('Du hast noch keine Aufgaben.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Erste Aufgabe anlegen' })).toBeInTheDocument()
  })

  it('zeigt Titel, Status, Priorität und Überfällig-Hinweis', async () => {
    const task = testTask({ priority: 'HIGH', status: 'DONE', dueDate: '2000-01-01', overdue: true })
    mockApi({ ...loggedIn, 'GET /api/v1/tasks?page=0&size=25': () => ({ body: pageOf([task], { totalElements: 1 }) }) })
    renderAt('/')
    expect(await screen.findByRole('link', { name: 'Einkaufen' })).toBeInTheDocument()
    expect(screen.getByText(/Status: Erledigt/)).toBeInTheDocument()
    expect(screen.getByText(/Priorität: Hoch/)).toBeInTheDocument()
    expect(screen.getByText(/Überfällig/, { selector: '.badge' })).toBeInTheDocument()
  })

  it('zeigt einen Ladefehler mit Wiederholungsmöglichkeit', async () => {
    let attempt = 0
    mockApi({
      ...loggedIn,
      'GET /api/v1/tasks?page=0&size=25': () => {
        attempt++
        return attempt === 1 ? { status: 500, body: { code: 'INTERNAL_ERROR', message: 'Kaputt.' } } : { body: pageOf([]) }
      },
    })
    renderAt('/')
    expect(await screen.findByRole('alert')).toHaveTextContent('Kaputt.')
    await userEvent.click(screen.getByRole('button', { name: 'Erneut versuchen' }))
    expect(await screen.findByText('Du hast noch keine Aufgaben.')).toBeInTheDocument()
  })

  it('erledigt eine Aufgabe direkt in der Liste', async () => {
    const task = testTask({ version: 3 })
    const done = { ...task, status: 'DONE' as const, completedAt: '2026-01-01T00:00:00Z', version: 4 }
    const { calls } = mockApi({
      ...loggedIn,
      'GET /api/v1/tasks?page=0&size=25': () => ({ body: pageOf([task], { totalElements: 1 }) }),
      [`POST /api/v1/tasks/${TASK_ID}/complete`]: () => ({ body: done }),
    })
    renderAt('/')
    await userEvent.click(await screen.findByRole('button', { name: /Als erledigt markieren/ }))
    expect(await screen.findByRole('button', { name: /Wieder öffnen/ })).toBeInTheDocument()
    expect(screen.getByText(/Status: Erledigt/)).toBeInTheDocument()
    expect(calls.find((c) => c.key === `POST /api/v1/tasks/${TASK_ID}/complete`)?.body).toEqual({ version: 3 })
  })

  it('blättert zur nächsten Seite und setzt den Fokus auf die Überschrift', async () => {
    mockApi({
      ...loggedIn,
      'GET /api/v1/tasks?page=0&size=25': () => ({ body: pageOf([testTask()], { totalElements: 26, totalPages: 2 }) }),
      'GET /api/v1/tasks?page=1&size=25': () => ({
        body: pageOf([testTask({ id: '333', title: 'Zweite Seite' })], { page: 1, totalElements: 26, totalPages: 2 }),
      }),
    })
    renderAt('/')
    await userEvent.click(await screen.findByRole('link', { name: 'Nächste Seite' }))
    expect(await screen.findByRole('link', { name: 'Zweite Seite' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { level: 2, name: /^Aufgaben/ })).toHaveFocus()
  })

  it('zeigt Tags einer Aufgabe in der Liste an', async () => {
    const task = testTask({ tags: [TAG] })
    mockApi({
      ...loggedIn,
      'GET /api/v1/tags': () => ({ body: [] }),
      'GET /api/v1/tasks?page=0&size=25': () => ({ body: pageOf([task], { totalElements: 1 }) }),
    })
    renderAt('/')
    await screen.findByRole('link', { name: 'Einkaufen' })
    expect(screen.getByText(/Tags: Wichtig/)).toBeInTheDocument()
  })

  it('filtert die Liste über die Tag-Auswahl', async () => {
    const task = testTask({ tags: [TAG] })
    mockApi({
      ...loggedIn,
      'GET /api/v1/tags': () => ({ body: [{ id: TAG.id, name: TAG.name, scope: 'PERSONAL', createdAt: '2026-09-30T05:00:00Z' }] }),
      'GET /api/v1/tasks?page=0&size=25': () => ({ body: pageOf([task], { totalElements: 1 }) }),
      [`GET /api/v1/tasks?page=0&size=25&tagId=${TAG.id}`]: () => ({ body: pageOf([task], { totalElements: 1 }) }),
    })
    renderAt('/')
    const select = await screen.findByLabelText('Nach Tag filtern')
    await userEvent.selectOptions(select, TAG.name)
    expect(await screen.findByRole('link', { name: 'Einkaufen' })).toBeInTheDocument()
  })

  it('zeigt bei aktivem Tag-Filter ohne Treffer einen Rücksetzen-Link', async () => {
    mockApi({
      ...loggedIn,
      'GET /api/v1/tags': () => ({ body: [{ id: TAG.id, name: TAG.name, scope: 'PERSONAL', createdAt: '2026-09-30T05:00:00Z' }] }),
      [`GET /api/v1/tasks?page=0&size=25&tagId=${TAG.id}`]: () => ({ body: pageOf([]) }),
      'GET /api/v1/tasks?page=0&size=25': () => ({ body: pageOf([]) }),
    })
    renderAt(`/?tag=${TAG.id}`)
    expect(await screen.findByText(/Keine Aufgaben für diese Auswahl\./)).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Filter zurücksetzen' }))
    expect(await screen.findByText('Du hast noch keine Aufgaben.')).toBeInTheDocument()
  })

  it('sucht über die Suchleiste', async () => {
    const task = testTask({ title: 'Steuererklärung' })
    mockApi({
      ...loggedIn,
      'GET /api/v1/tasks?page=0&size=25': () => ({ body: pageOf([testTask()], { totalElements: 1 }) }),
      'GET /api/v1/tasks?page=0&size=25&q=steuer': () => ({ body: pageOf([task], { totalElements: 1 }) }),
    })
    renderAt('/')
    await screen.findByRole('link', { name: 'Einkaufen' })
    await userEvent.type(screen.getByLabelText('Suche'), 'steuer')
    await userEvent.click(screen.getByRole('button', { name: 'Suchen' }))
    expect(await screen.findByRole('link', { name: 'Steuererklärung' })).toBeInTheDocument()
  })

  it('filtert über eine Status-Checkbox', async () => {
    const done = testTask({ status: 'DONE' })
    mockApi({
      ...loggedIn,
      'GET /api/v1/tasks?page=0&size=25': () => ({ body: pageOf([testTask()], { totalElements: 1 }) }),
      'GET /api/v1/tasks?page=0&size=25&status=DONE': () => ({ body: pageOf([done], { totalElements: 1 }) }),
    })
    renderAt('/')
    await screen.findByRole('link', { name: 'Einkaufen' })
    await userEvent.click(screen.getByLabelText('Erledigt', { selector: 'input' }))
    expect(await screen.findByText(/Status: Erledigt/)).toBeInTheDocument()
  })

  it('öffnet die Standardansicht „Überfällig“ über den Kurzverweis', async () => {
    const overdue = testTask({ overdue: true, dueDate: '2000-01-01' })
    mockApi({
      ...loggedIn,
      'GET /api/v1/tasks?page=0&size=25': () => ({ body: pageOf([testTask()], { totalElements: 1 }) }),
      'GET /api/v1/tasks?page=0&size=25&overdue=true': () => ({ body: pageOf([overdue], { totalElements: 1 }) }),
    })
    renderAt('/')
    await screen.findByRole('link', { name: 'Einkaufen' })
    await userEvent.click(screen.getByRole('link', { name: 'Überfällig' }))
    await screen.findByRole('link', { name: 'Einkaufen' })
    expect(screen.getByText(/Überfällig/, { selector: '.badge' })).toBeInTheDocument()
  })

  it('sortiert über die Sortierauswahl', async () => {
    const { calls } = mockApi({
      ...loggedIn,
      'GET /api/v1/tasks?page=0&size=25': () => ({ body: pageOf([testTask()], { totalElements: 1 }) }),
      'GET /api/v1/tasks?page=0&size=25&sortBy=priority&sortDirection=asc': () => ({
        body: pageOf([testTask()], { totalElements: 1 }),
      }),
    })
    renderAt('/')
    await screen.findByRole('link', { name: 'Einkaufen' })
    await userEvent.selectOptions(screen.getByLabelText('Sortierung'), 'Priorität (niedrig zuerst)')
    await screen.findByRole('link', { name: 'Einkaufen' })
    expect(calls.some((c) => c.key === 'GET /api/v1/tasks?page=0&size=25&sortBy=priority&sortDirection=asc')).toBe(true)
  })
})

describe('Aufgabe anlegen', () => {
  it('zeigt einen Fehler bei leerem Titel ohne Serveraufruf', async () => {
    const { calls } = mockApi({ ...loggedIn })
    renderAt('/aufgaben/neu')
    await userEvent.click(await screen.findByRole('button', { name: 'Aufgabe anlegen' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Titel')
    expect(calls.filter((c) => c.key.startsWith('POST'))).toHaveLength(0)
  })

  it('legt eine Aufgabe mit Standardwerten an', async () => {
    const created = testTask({ id: '999', title: 'Neue Aufgabe' })
    const { calls } = mockApi({
      ...loggedIn,
      'POST /api/v1/tasks': () => ({ status: 201, body: created }),
      'GET /api/v1/tasks/999': () => ({ body: created }),
    })
    renderAt('/aufgaben/neu')
    await userEvent.type(await screen.findByLabelText('Titel'), 'Neue Aufgabe')
    await userEvent.click(screen.getByRole('button', { name: 'Aufgabe anlegen' }))
    expect(await screen.findByRole('heading', { level: 1, name: 'Neue Aufgabe' })).toBeInTheDocument()
    expect(calls.find((c) => c.key === 'POST /api/v1/tasks')?.body).toEqual({
      projectId: null,
      title: 'Neue Aufgabe',
      description: null,
      priority: 'NORMAL',
      dueDate: null,
      dueTime: null,
      dueTimeZone: null,
      dueOffset: null,
    })
  })

  it('zeigt einen Serverfehler und erhält die Eingaben', async () => {
    mockApi({
      ...loggedIn,
      'POST /api/v1/tasks': () => ({ status: 500, body: { code: 'INTERNAL_ERROR', message: 'Es ist ein Fehler passiert.' } }),
    })
    renderAt('/aufgaben/neu')
    await userEvent.type(await screen.findByLabelText('Titel'), 'X')
    await userEvent.click(screen.getByRole('button', { name: 'Aufgabe anlegen' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Es ist ein Fehler passiert.')
    expect(screen.getByLabelText('Titel')).toHaveValue('X')
  })
})

describe('Aufgabe ansehen', () => {
  it('zeigt „Aufgabe nicht gefunden" bei 404', async () => {
    mockApi({
      ...loggedIn,
      [`GET /api/v1/tasks/${TASK_ID}`]: () => ({ status: 404, body: { code: 'TASK_NOT_FOUND', message: 'x' } }),
    })
    renderAt(`/aufgaben/${TASK_ID}`)
    expect(await screen.findByRole('heading', { level: 1, name: 'Aufgabe nicht gefunden' })).toBeInTheDocument()
  })

  it('zeigt die Beschreibung als reinen Text, nicht als HTML', async () => {
    const task = testTask({ description: '<b>fett</b>' })
    mockApi({ ...loggedIn, [`GET /api/v1/tasks/${TASK_ID}`]: () => ({ body: task }) })
    renderAt(`/aufgaben/${TASK_ID}`)
    expect(await screen.findByText('<b>fett</b>')).toBeInTheDocument()
  })

  it('zeigt zugeordnete Tags und „Keine“ ohne Zuordnung', async () => {
    const task = testTask({ tags: [TAG] })
    mockApi({
      ...loggedIn,
      [`GET /api/v1/tasks/${TASK_ID}`]: () => ({ body: task }),
      'GET /api/v1/tags': () => ({ body: [] }),
    })
    renderAt(`/aufgaben/${TASK_ID}`)
    await screen.findByRole('heading', { level: 1, name: 'Einkaufen' })
    expect(screen.getByText('Wichtig')).toBeInTheDocument()
  })

  it('ordnet einen Tag über den Editor zu, ohne die Version zu ändern', async () => {
    const task = testTask({ tags: [] })
    const tag = { id: TAG.id, name: TAG.name, scope: 'PERSONAL' as const, createdAt: '2026-09-30T05:00:00Z' }
    const { calls } = mockApi({
      ...loggedIn,
      [`GET /api/v1/tasks/${TASK_ID}`]: () => ({ body: task }),
      'GET /api/v1/tags': () => ({ body: [tag] }),
      [`PUT /api/v1/tasks/${TASK_ID}/tags`]: () => ({ body: [TAG] }),
    })
    renderAt(`/aufgaben/${TASK_ID}`)
    await screen.findByRole('heading', { level: 1, name: 'Einkaufen' })
    await userEvent.click(await screen.findByLabelText('Wichtig'))
    await userEvent.click(screen.getByRole('button', { name: 'Tags speichern' }))
    expect(await screen.findByRole('status')).toHaveTextContent('Die Tag-Zuordnung wurde gespeichert.')
    expect(calls.find((c) => c.key === `PUT /api/v1/tasks/${TASK_ID}/tags`)?.body).toEqual({ tagIds: [TAG.id] })
  })
})

describe('Aufgabe bearbeiten', () => {
  it('speichert Änderungen mit der geladenen Version', async () => {
    let current = testTask({ version: 2 })
    const { calls } = mockApi({
      ...loggedIn,
      [`GET /api/v1/tasks/${TASK_ID}`]: () => ({ body: current }),
      [`PUT /api/v1/tasks/${TASK_ID}`]: () => {
        current = { ...current, title: 'Geändert', version: 3 }
        return { body: current }
      },
    })
    renderAt(`/aufgaben/${TASK_ID}/bearbeiten`)
    const title = await screen.findByLabelText('Titel')
    await userEvent.clear(title)
    await userEvent.type(title, 'Geändert')
    await userEvent.click(screen.getByRole('button', { name: 'Änderungen speichern' }))
    expect(await screen.findByRole('heading', { level: 1, name: 'Geändert' })).toBeInTheDocument()
    expect(calls.find((c) => c.key === `PUT /api/v1/tasks/${TASK_ID}`)?.body).toMatchObject({
      title: 'Geändert',
      status: 'OPEN',
      priority: 'NORMAL',
      version: 2,
    })
  })

  it('zeigt bei 409 einen Konflikt, behält die eigene Eingabe und bietet die Serverfassung an', async () => {
    const task = testTask({ version: 2 })
    const serverVersion = { ...task, title: 'Von A geändert', version: 3 }
    let getCount = 0
    mockApi({
      ...loggedIn,
      [`GET /api/v1/tasks/${TASK_ID}`]: () => {
        getCount++
        return { body: getCount === 1 ? task : serverVersion }
      },
      [`PUT /api/v1/tasks/${TASK_ID}`]: () => ({ status: 409, body: { code: 'VERSION_CONFLICT', message: 'Konflikt' } }),
    })
    renderAt(`/aufgaben/${TASK_ID}/bearbeiten`)
    const title = await screen.findByLabelText('Titel')
    await userEvent.clear(title)
    await userEvent.type(title, 'Von B')
    await userEvent.click(screen.getByRole('button', { name: 'Änderungen speichern' }))

    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent('zwischenzeitlich geändert')
    expect(alert).toHaveFocus()
    expect(alert).toHaveTextContent('Von A geändert')
    expect(screen.getByLabelText('Titel')).toHaveValue('Von B')

    await userEvent.click(screen.getByRole('button', { name: 'Meine Eingaben behalten' }))
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
    expect(screen.getByLabelText('Titel')).toHaveValue('Von B')
  })
})

describe('Aufgabe löschen', () => {
  it('öffnet den Löschdialog; Abbrechen gibt den Fokus zurück', async () => {
    const task = testTask()
    mockApi({ ...loggedIn, [`GET /api/v1/tasks/${TASK_ID}`]: () => ({ body: task }) })
    renderAt(`/aufgaben/${TASK_ID}`)
    const deleteButton = await screen.findByRole('button', { name: 'Löschen' })
    await userEvent.click(deleteButton)
    expect(screen.getByRole('button', { name: 'Abbrechen' })).toHaveFocus()
    await userEvent.click(screen.getByRole('button', { name: 'Abbrechen' }))
    expect(deleteButton).toHaveFocus()
  })

  it('löscht endgültig und zeigt die Bestätigung auf der Liste', async () => {
    const task = testTask()
    mockApi({
      ...loggedIn,
      [`GET /api/v1/tasks/${TASK_ID}`]: () => ({ body: task }),
      [`DELETE /api/v1/tasks/${TASK_ID}?version=0`]: () => ({ status: 204 }),
      'GET /api/v1/tasks?page=0&size=25': () => ({ body: pageOf([]) }),
    })
    renderAt(`/aufgaben/${TASK_ID}`)
    await userEvent.click(await screen.findByRole('button', { name: 'Löschen' }))
    await userEvent.click(screen.getByRole('button', { name: 'Endgültig löschen' }))
    expect(await screen.findByText(/wurde gelöscht/)).toBeInTheDocument()
  })

  it('zeigt bei 409 einen Alarm statt eines stillen Fehlschlags', async () => {
    const task = testTask()
    mockApi({
      ...loggedIn,
      [`GET /api/v1/tasks/${TASK_ID}`]: () => ({ body: task }),
      [`DELETE /api/v1/tasks/${TASK_ID}?version=0`]: () => ({ status: 409, body: { code: 'VERSION_CONFLICT', message: 'x' } }),
    })
    renderAt(`/aufgaben/${TASK_ID}`)
    await userEvent.click(await screen.findByRole('button', { name: 'Löschen' }))
    await userEvent.click(screen.getByRole('button', { name: 'Endgültig löschen' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Nicht gelöscht')
  })
})
