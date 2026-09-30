import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { mockApi, pageOf, renderAt, testTask, testUser } from '../testUtils'
import type { TagSummary } from '../api/tagSummary'

afterEach(() => vi.restoreAllMocks())

const loggedIn = { 'GET /api/v1/auth/me': () => ({ body: testUser }) }
const TASK_ID = '22222222-2222-2222-2222-222222222222'
const TAG: TagSummary = { id: 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', name: 'Wichtig' }
const noSubtasks = {
  [`GET /api/v1/tasks/${TASK_ID}/subtasks`]: () => ({ body: [] }),
  [`GET /api/v1/tasks/${TASK_ID}/activities`]: () => ({ body: [] }),
  [`GET /api/v1/tasks/${TASK_ID}/comments`]: () => ({ body: [] }),
}

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

  it('filtert über „Nur mir zugewiesene Aufgaben“ (US-062)', async () => {
    const assignedToMe = testTask({ assigneeId: testUser.id, assigneeDisplayName: testUser.displayName })
    mockApi({
      ...loggedIn,
      'GET /api/v1/tasks?page=0&size=25': () => ({ body: pageOf([testTask()], { totalElements: 1 }) }),
      [`GET /api/v1/tasks?page=0&size=25&assigneeId=${testUser.id}`]: () => ({
        body: pageOf([assignedToMe], { totalElements: 1 }),
      }),
    })
    renderAt('/')
    await screen.findByRole('link', { name: 'Einkaufen' })
    await userEvent.click(screen.getByLabelText('Nur mir zugewiesene Aufgaben'))
    await screen.findByRole('link', { name: 'Einkaufen' })
    expect(screen.getByLabelText('Nur mir zugewiesene Aufgaben')).toBeChecked()
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
      assigneeId: null,
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

  it('bietet nach Projektauswahl den Eigentümer und Mitglieder als Bearbeiter an (US-062)', async () => {
    const project = {
      id: 'proj-1',
      name: 'Team-Projekt',
      ownerId: 'owner-1',
      ownerDisplayName: 'Frau Eigentümer',
      createdAt: '2026-09-30T05:00:00Z',
      updatedAt: '2026-09-30T05:00:00Z',
      version: 0,
    }
    const created = testTask({ id: '999', title: 'Team-Aufgabe', projectId: project.id })
    const { calls } = mockApi({
      ...loggedIn,
      'GET /api/v1/projects?page=0&size=100': () => ({ body: pageOf([project], { totalElements: 1 }) }),
      [`GET /api/v1/projects/${project.id}`]: () => ({ body: project }),
      [`GET /api/v1/projects/${project.id}/members`]: () => ({
        body: [{ userId: 'mem-1', displayName: 'Herr Mitglied', addedAt: '2026-09-30T05:00:00Z' }],
      }),
      'POST /api/v1/tasks': () => ({ status: 201, body: created }),
      'GET /api/v1/tasks/999': () => ({ body: created }),
    })
    renderAt('/aufgaben/neu')
    await userEvent.selectOptions(await screen.findByLabelText('Projekt'), 'Team-Projekt')
    await screen.findByRole('option', { name: 'Frau Eigentümer' })
    expect(screen.getByRole('option', { name: 'Herr Mitglied' })).toBeInTheDocument()
    await userEvent.selectOptions(screen.getByLabelText('Bearbeiter'), 'Herr Mitglied')
    await userEvent.type(screen.getByLabelText('Titel'), 'Team-Aufgabe')
    await userEvent.click(screen.getByRole('button', { name: 'Aufgabe anlegen' }))
    expect(await screen.findByRole('heading', { level: 1, name: 'Team-Aufgabe' })).toBeInTheDocument()
    expect(calls.find((c) => c.key === 'POST /api/v1/tasks')?.body).toMatchObject({
      projectId: project.id,
      assigneeId: 'mem-1',
    })
  })
})

describe('Aufgabe ansehen', () => {
  it('zeigt den zugewiesenen Bearbeiter mit Namen (US-062)', async () => {
    const task = testTask({ assigneeId: 'mem-1', assigneeDisplayName: 'Herr Mitglied' })
    mockApi({
      ...loggedIn,
      ...noSubtasks,
      [`GET /api/v1/tasks/${TASK_ID}`]: () => ({ body: task }),
      'GET /api/v1/tags': () => ({ body: [] }),
    })
    renderAt(`/aufgaben/${TASK_ID}`)
    await screen.findByRole('heading', { level: 1, name: 'Einkaufen' })
    expect(screen.getByText('Herr Mitglied')).toBeInTheDocument()
  })

  it('zeigt „Keine Zuweisung“ ohne Bearbeiter', async () => {
    const task = testTask()
    mockApi({
      ...loggedIn,
      ...noSubtasks,
      [`GET /api/v1/tasks/${TASK_ID}`]: () => ({ body: task }),
      'GET /api/v1/tags': () => ({ body: [] }),
    })
    renderAt(`/aufgaben/${TASK_ID}`)
    await screen.findByRole('heading', { level: 1, name: 'Einkaufen' })
    expect(screen.getByText('Keine Zuweisung')).toBeInTheDocument()
  })

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
    mockApi({ ...loggedIn, ...noSubtasks, [`GET /api/v1/tasks/${TASK_ID}`]: () => ({ body: task }) })
    renderAt(`/aufgaben/${TASK_ID}`)
    expect(await screen.findByText('<b>fett</b>')).toBeInTheDocument()
  })

  it('zeigt zugeordnete Tags und „Keine“ ohne Zuordnung', async () => {
    const task = testTask({ tags: [TAG] })
    mockApi({
      ...loggedIn,
      ...noSubtasks,
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
      ...noSubtasks,
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

describe('Gleichnamige Tags (US-181)', () => {
  const MINE = {
    id: 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    name: 'Backen',
    scope: 'PROJECT' as const,
    projectId: 'p-mine',
    projectName: 'Kaffee',
    projectOwnerId: testUser.id,
    projectOwnerDisplayName: testUser.displayName,
    createdAt: '2026-09-30T05:00:00Z',
  }
  const FOREIGN = {
    id: 'cccccccc-cccc-cccc-cccc-cccccccccccc',
    name: 'Backen',
    scope: 'PROJECT' as const,
    projectId: 'p-foreign',
    projectName: 'Kaffee',
    projectOwnerId: '99999999-9999-9999-9999-999999999999',
    projectOwnerDisplayName: 'Uli',
    createdAt: '2026-09-30T05:00:00Z',
  }

  it('unterscheidet gleichnamige Tags im Editor über den zugänglichen Namen', async () => {
    const task = testTask({ tags: [] })
    const { calls } = mockApi({
      ...loggedIn,
      ...noSubtasks,
      [`GET /api/v1/tasks/${TASK_ID}`]: () => ({ body: task }),
      'GET /api/v1/tags': () => ({ body: [MINE, FOREIGN] }),
      [`PUT /api/v1/tasks/${TASK_ID}/tags`]: () => ({ body: [{ id: FOREIGN.id, name: FOREIGN.name }] }),
    })
    renderAt(`/aufgaben/${TASK_ID}`)
    await screen.findByRole('heading', { level: 1, name: 'Einkaufen' })

    const mine = await screen.findByRole('checkbox', { name: 'Backen · Projekt „Kaffee“ (du)' })
    const foreign = screen.getByRole('checkbox', { name: 'Backen · Projekt „Kaffee“ (Uli)' })
    expect(mine).not.toBe(foreign)

    await userEvent.click(foreign)
    await userEvent.click(screen.getByRole('button', { name: 'Tags speichern' }))
    expect(await screen.findByRole('status')).toHaveTextContent('Die Tag-Zuordnung wurde gespeichert.')
    expect(calls.find((c) => c.key === `PUT /api/v1/tasks/${TASK_ID}/tags`)?.body).toEqual({ tagIds: [FOREIGN.id] })
  })

  it('zeigt bei eindeutigen Tag-Namen keine Herkunft', async () => {
    const task = testTask({ tags: [] })
    mockApi({
      ...loggedIn,
      ...noSubtasks,
      [`GET /api/v1/tasks/${TASK_ID}`]: () => ({ body: task }),
      'GET /api/v1/tags': () => ({ body: [MINE] }),
    })
    renderAt(`/aufgaben/${TASK_ID}`)
    await screen.findByRole('heading', { level: 1, name: 'Einkaufen' })
    expect(await screen.findByRole('checkbox', { name: 'Backen' })).toBeInTheDocument()
  })

  it('beschriftet gleichnamige Tags im Filter der Aufgabenliste mit ihrer Herkunft', async () => {
    mockApi({
      ...loggedIn,
      'GET /api/v1/tags': () => ({ body: [MINE, FOREIGN] }),
      'GET /api/v1/tasks?page=0&size=25': () => ({ body: pageOf([]) }),
    })
    renderAt('/')
    const select = await screen.findByLabelText('Nach Tag filtern')
    expect(within(select).getByRole('option', { name: 'Backen · Projekt „Kaffee“ (du)' })).toBeInTheDocument()
    expect(within(select).getByRole('option', { name: 'Backen · Projekt „Kaffee“ (Uli)' })).toBeInTheDocument()
  })
})

describe('Unteraufgaben (US-070/071)', () => {
  const subtasksUrl = `GET /api/v1/tasks/${TASK_ID}/subtasks`
  const historyUrls = {
    [`GET /api/v1/tasks/${TASK_ID}/activities`]: () => ({ body: [] }),
    [`GET /api/v1/tasks/${TASK_ID}/comments`]: () => ({ body: [] }),
  }

  it('zeigt „Noch keine Unteraufgaben.“ ohne Einträge', async () => {
    mockApi({
      ...loggedIn,
      ...historyUrls,
      [`GET /api/v1/tasks/${TASK_ID}`]: () => ({ body: testTask() }),
      [subtasksUrl]: () => ({ body: [] }),
    })
    renderAt(`/aufgaben/${TASK_ID}`)
    expect(await screen.findByText('Noch keine Unteraufgaben.')).toBeInTheDocument()
  })

  it('legt eine Unteraufgabe an und zeigt den Fortschritt', async () => {
    mockApi({
      ...loggedIn,
      ...historyUrls,
      [`GET /api/v1/tasks/${TASK_ID}`]: () => ({ body: testTask() }),
      [subtasksUrl]: () => ({ body: [] }),
      [`POST /api/v1/tasks/${TASK_ID}/subtasks`]: () => ({
        status: 201,
        body: [{ id: 's1', title: 'Kartons besorgen', done: false, sortOrder: 0 }],
      }),
    })
    renderAt(`/aufgaben/${TASK_ID}`)
    await screen.findByText('Noch keine Unteraufgaben.')
    await userEvent.type(screen.getByLabelText('Neue Unteraufgabe'), 'Kartons besorgen')
    await userEvent.click(screen.getByRole('button', { name: 'Hinzufügen' }))
    expect(await screen.findByText('Kartons besorgen')).toBeInTheDocument()
    expect(screen.getByText('0 von 1 erledigt')).toBeInTheDocument()
  })

  it('zeigt einen Feldfehler bei leerem Titel ohne die Liste zu ändern', async () => {
    mockApi({
      ...loggedIn,
      ...historyUrls,
      [`GET /api/v1/tasks/${TASK_ID}`]: () => ({ body: testTask() }),
      [subtasksUrl]: () => ({ body: [] }),
      [`POST /api/v1/tasks/${TASK_ID}/subtasks`]: () => ({
        status: 400,
        body: { code: 'VALIDATION_FAILED', message: 'x', fieldErrors: [{ field: 'title', code: 'INVALID', message: 'Bitte gib einen Titel an.' }] },
      }),
    })
    renderAt(`/aufgaben/${TASK_ID}`)
    await screen.findByText('Noch keine Unteraufgaben.')
    await userEvent.click(screen.getByRole('button', { name: 'Hinzufügen' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Bitte gib einen Titel an.')
    expect(screen.getByText('Noch keine Unteraufgaben.')).toBeInTheDocument()
  })

  it('setzt den Erledigt-Zustand und aktualisiert den Fortschritt', async () => {
    mockApi({
      ...loggedIn,
      ...historyUrls,
      [`GET /api/v1/tasks/${TASK_ID}`]: () => ({ body: testTask() }),
      [subtasksUrl]: () => ({ body: [{ id: 's1', title: 'Kartons besorgen', done: false, sortOrder: 0 }] }),
      [`PUT /api/v1/tasks/${TASK_ID}/subtasks/s1/done`]: () => ({
        body: [{ id: 's1', title: 'Kartons besorgen', done: true, sortOrder: 0 }],
      }),
    })
    renderAt(`/aufgaben/${TASK_ID}`)
    const checkbox = await screen.findByRole('checkbox', { name: 'Kartons besorgen' })
    await userEvent.click(checkbox)
    expect(await screen.findByText('1 von 1 erledigt')).toBeInTheDocument()
  })

  it('benennt eine Unteraufgabe um', async () => {
    const { calls } = mockApi({
      ...loggedIn,
      ...historyUrls,
      [`GET /api/v1/tasks/${TASK_ID}`]: () => ({ body: testTask() }),
      [subtasksUrl]: () => ({ body: [{ id: 's1', title: 'Alter Titel', done: false, sortOrder: 0 }] }),
      [`PUT /api/v1/tasks/${TASK_ID}/subtasks/s1`]: () => ({
        body: [{ id: 's1', title: 'Neuer Titel', done: false, sortOrder: 0 }],
      }),
    })
    renderAt(`/aufgaben/${TASK_ID}`)
    await screen.findByText('Alter Titel')
    await userEvent.click(screen.getByRole('button', { name: 'Umbenennen: Alter Titel' }))
    const input = screen.getByLabelText('Titel von „Alter Titel“ ändern')
    await userEvent.clear(input)
    await userEvent.type(input, 'Neuer Titel')
    await userEvent.click(screen.getByRole('button', { name: 'Speichern' }))
    expect(await screen.findByText('Neuer Titel')).toBeInTheDocument()
    expect(calls.find((c) => c.key === `PUT /api/v1/tasks/${TASK_ID}/subtasks/s1`)?.body).toEqual({ title: 'Neuer Titel' })
  })

  it('verschiebt eine Unteraufgabe nach oben; am Rand ist der Button deaktiviert', async () => {
    mockApi({
      ...loggedIn,
      ...historyUrls,
      [`GET /api/v1/tasks/${TASK_ID}`]: () => ({ body: testTask() }),
      [subtasksUrl]: () => ({
        body: [
          { id: 's1', title: 'A', done: false, sortOrder: 0 },
          { id: 's2', title: 'B', done: false, sortOrder: 1 },
        ],
      }),
      [`POST /api/v1/tasks/${TASK_ID}/subtasks/s2/move`]: () => ({
        body: [
          { id: 's2', title: 'B', done: false, sortOrder: 0 },
          { id: 's1', title: 'A', done: false, sortOrder: 1 },
        ],
      }),
    })
    renderAt(`/aufgaben/${TASK_ID}`)
    await screen.findByText('A')
    expect(screen.getByRole('button', { name: 'Nach oben: A' })).toBeDisabled()
    await userEvent.click(screen.getByRole('button', { name: 'Nach oben: B' }))
    const titles = await screen.findAllByText(/^[AB]$/)
    expect(titles.map((t) => t.textContent)).toEqual(['B', 'A'])
  })

  it('löscht eine Unteraufgabe', async () => {
    mockApi({
      ...loggedIn,
      ...historyUrls,
      [`GET /api/v1/tasks/${TASK_ID}`]: () => ({ body: testTask() }),
      [subtasksUrl]: () => ({ body: [{ id: 's1', title: 'Kartons besorgen', done: false, sortOrder: 0 }] }),
      [`DELETE /api/v1/tasks/${TASK_ID}/subtasks/s1`]: () => ({ body: [] }),
    })
    renderAt(`/aufgaben/${TASK_ID}`)
    await screen.findByText('Kartons besorgen')
    await userEvent.click(screen.getByRole('button', { name: 'Löschen: Kartons besorgen' }))
    expect(await screen.findByText('Noch keine Unteraufgaben.')).toBeInTheDocument()
  })
})

describe('Verlauf (US-080/081)', () => {
  const noSubtasksUrl = { [`GET /api/v1/tasks/${TASK_ID}/subtasks`]: () => ({ body: [] }) }

  it('zeigt „Noch keine Einträge.“ ohne Historie', async () => {
    mockApi({
      ...loggedIn,
      ...noSubtasksUrl,
      [`GET /api/v1/tasks/${TASK_ID}`]: () => ({ body: testTask() }),
      [`GET /api/v1/tasks/${TASK_ID}/activities`]: () => ({ body: [] }),
      [`GET /api/v1/tasks/${TASK_ID}/comments`]: () => ({ body: [] }),
    })
    renderAt(`/aufgaben/${TASK_ID}`)
    expect(await screen.findByText('Noch keine Einträge.')).toBeInTheDocument()
  })

  it('zeigt verständliche deutsche Texte für Erstellen, Titel- und Statusänderung', async () => {
    mockApi({
      ...loggedIn,
      ...noSubtasksUrl,
      [`GET /api/v1/tasks/${TASK_ID}`]: () => ({ body: testTask() }),
      [`GET /api/v1/tasks/${TASK_ID}/activities`]: () => ({
        body: [
          { id: 'a1', type: 'CREATED', actorId: testUser.id, actorDisplayName: 'Test Person', createdAt: '2026-09-30T05:00:00Z' },
          {
            id: 'a2', type: 'TITLE_CHANGED', fromValue: 'Alt', toValue: 'Neu', actorId: testUser.id,
            actorDisplayName: 'Test Person', createdAt: '2026-09-30T05:01:00Z',
          },
          {
            id: 'a3', type: 'STATUS_CHANGED', fromValue: 'OPEN', toValue: 'DONE', actorId: testUser.id,
            actorDisplayName: 'Test Person', createdAt: '2026-09-30T05:02:00Z',
          },
        ],
      }),
      [`GET /api/v1/tasks/${TASK_ID}/comments`]: () => ({ body: [] }),
    })
    renderAt(`/aufgaben/${TASK_ID}`)
    expect(await screen.findByText('Aufgabe angelegt')).toBeInTheDocument()
    expect(screen.getByText('Titel geändert: von „Alt“ zu „Neu“')).toBeInTheDocument()
    expect(screen.getByText('Status geändert: von Offen zu Erledigt')).toBeInTheDocument()
  })

  it('fügt einen Kommentar hinzu und zeigt ihn chronologisch nach den Aktivitäten', async () => {
    mockApi({
      ...loggedIn,
      ...noSubtasksUrl,
      [`GET /api/v1/tasks/${TASK_ID}`]: () => ({ body: testTask() }),
      [`GET /api/v1/tasks/${TASK_ID}/activities`]: () => ({
        body: [{ id: 'a1', type: 'CREATED', actorId: testUser.id, actorDisplayName: 'Test Person', createdAt: '2026-09-30T05:00:00Z' }],
      }),
      [`GET /api/v1/tasks/${TASK_ID}/comments`]: () => ({ body: [] }),
      [`POST /api/v1/tasks/${TASK_ID}/comments`]: () => ({
        status: 201,
        body: {
          id: 'c1', authorId: testUser.id, authorDisplayName: 'Test Person', content: 'Mein Kommentar',
          createdAt: '2026-09-30T05:05:00Z',
        },
      }),
    })
    renderAt(`/aufgaben/${TASK_ID}`)
    await screen.findByText('Aufgabe angelegt')
    await userEvent.type(screen.getByLabelText('Kommentar hinzufügen'), 'Mein Kommentar')
    await userEvent.click(screen.getByRole('button', { name: 'Kommentieren' }))
    expect(await screen.findByText('Mein Kommentar')).toBeInTheDocument()
  })

  it('zeigt einen Feldfehler bei leerem Kommentar', async () => {
    mockApi({
      ...loggedIn,
      ...noSubtasksUrl,
      [`GET /api/v1/tasks/${TASK_ID}`]: () => ({ body: testTask() }),
      [`GET /api/v1/tasks/${TASK_ID}/activities`]: () => ({ body: [] }),
      [`GET /api/v1/tasks/${TASK_ID}/comments`]: () => ({ body: [] }),
      [`POST /api/v1/tasks/${TASK_ID}/comments`]: () => ({
        status: 400,
        body: { code: 'VALIDATION_FAILED', message: 'x', fieldErrors: [{ field: 'content', code: 'INVALID', message: 'Bitte gib einen Text an.' }] },
      }),
    })
    renderAt(`/aufgaben/${TASK_ID}`)
    await screen.findByText('Noch keine Einträge.')
    await userEvent.click(screen.getByRole('button', { name: 'Kommentieren' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Bitte gib einen Text an.')
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
    mockApi({ ...loggedIn, ...noSubtasks, [`GET /api/v1/tasks/${TASK_ID}`]: () => ({ body: task }) })
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
      ...noSubtasks,
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
      ...noSubtasks,
      [`GET /api/v1/tasks/${TASK_ID}`]: () => ({ body: task }),
      [`DELETE /api/v1/tasks/${TASK_ID}?version=0`]: () => ({ status: 409, body: { code: 'VERSION_CONFLICT', message: 'x' } }),
    })
    renderAt(`/aufgaben/${TASK_ID}`)
    await userEvent.click(await screen.findByRole('button', { name: 'Löschen' }))
    await userEvent.click(screen.getByRole('button', { name: 'Endgültig löschen' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Nicht gelöscht')
  })
})
