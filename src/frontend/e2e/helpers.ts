import { expect, type Page } from '@playwright/test'

export const PASSWORD = 'e2e-test-passwort-lang-genug'

export function uniqueEmail(): string {
  return `e2e-${Date.now()}-${Math.random().toString(36).slice(2, 8)}@example.test`
}

/** Registriert ein neues Konto über die Oberfläche; danach ist man angemeldet. */
export async function registerViaUi(page: Page, email = uniqueEmail(), password = PASSWORD, name = 'E2E Person') {
  await page.goto('/registrieren')
  await page.getByLabel('E-Mail-Adresse').fill(email)
  await page.getByLabel('Anzeigename').fill(name)
  await page.getByLabel('Passwort', { exact: true }).fill(password)
  await page.getByRole('button', { name: 'Konto anlegen' }).click()
  await expect(page.getByRole('heading', { level: 1, name: 'Meine Aufgaben' })).toBeVisible()
  return email
}

interface TaskInputForTest {
  title: string
  description?: string | null
  priority?: 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT'
  dueDate?: string | null
  dueTime?: string | null
  dueTimeZone?: string | null
  dueOffset?: string | null
}

/** Legt eine Aufgabe direkt über die API an (schnell, für Testvorbereitung); teilt sich die Cookies mit `page`. */
export async function createTaskViaApi(page: Page, input: TaskInputForTest): Promise<string> {
  const csrf = await page.request.get('/api/v1/auth/csrf')
  const { headerName, token } = await csrf.json()
  const response = await page.request.post('/api/v1/tasks', {
    headers: { [headerName]: token },
    data: {
      description: null,
      priority: 'NORMAL',
      dueDate: null,
      dueTime: null,
      dueTimeZone: null,
      dueOffset: null,
      ...input,
    },
  })
  const body = await response.json()
  return body.id as string
}

/** Legt eine Aufgabe über die Oberfläche an; danach ist die Detailseite der neuen Aufgabe geöffnet. */
export async function createTaskViaUi(page: Page, title: string) {
  await page.getByRole('link', { name: 'Neue Aufgabe anlegen' }).click()
  await page.getByLabel('Titel').fill(title)
  await page.getByRole('button', { name: 'Aufgabe anlegen' }).click()
  await expect(page.getByRole('heading', { level: 1, name: title })).toBeVisible()
}

/** Legt ein Projekt direkt über die API an (schnell, für Testvorbereitung); teilt sich die Cookies mit `page`. */
export async function createProjectViaApi(page: Page, name: string, description: string | null = null): Promise<string> {
  const csrf = await page.request.get('/api/v1/auth/csrf')
  const { headerName, token } = await csrf.json()
  const response = await page.request.post('/api/v1/projects', { headers: { [headerName]: token }, data: { name, description } })
  const body = await response.json()
  return body.id as string
}

/** Legt ein Projekt über die Oberfläche an; danach ist die Detailseite des neuen Projekts geöffnet. */
export async function createProjectViaUi(page: Page, name: string) {
  await page.goto('/projekte/neu')
  await page.getByLabel('Name').fill(name)
  await page.getByRole('button', { name: 'Projekt anlegen' }).click()
  await expect(page.getByRole('heading', { level: 1, name })).toBeVisible()
}

/** Legt einen Tag direkt über die API an (schnell, für Testvorbereitung); teilt sich die Cookies mit `page`. */
export async function createTagViaApi(page: Page, name: string, projectId: string | null = null): Promise<string> {
  const csrf = await page.request.get('/api/v1/auth/csrf')
  const { headerName, token } = await csrf.json()
  const response = await page.request.post('/api/v1/tags', { headers: { [headerName]: token }, data: { name, projectId } })
  const body = await response.json()
  return body.id as string
}
