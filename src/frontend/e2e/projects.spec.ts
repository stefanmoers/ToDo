import AxeBuilder from '@axe-core/playwright'
import { expect, test } from '@playwright/test'
import { createProjectViaApi, createProjectViaUi, PASSWORD, registerViaUi, uniqueEmail } from './helpers'

const TAGS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa']

test('vollständiger Lebenszyklus: anlegen, ansehen, bearbeiten, löschen', async ({ page }) => {
  await registerViaUi(page)
  await page.goto('/projekte')
  await expect(page.getByText('Du hast noch keine Projekte.')).toBeVisible()

  await createProjectViaUi(page, 'Hausbau')
  await page.getByRole('link', { name: 'Bearbeiten' }).click()
  await page.getByLabel('Beschreibung').fill('Grundstück, Bauplan, Handwerker')
  await page.getByRole('button', { name: 'Änderungen speichern' }).click()
  await expect(page.getByText('Die Änderungen wurden gespeichert.')).toBeVisible()
  await expect(page.getByText('Grundstück, Bauplan, Handwerker')).toBeVisible()

  await page.getByRole('button', { name: 'Löschen' }).click()
  await expect(page.getByRole('dialog')).toBeVisible()
  await page.getByRole('button', { name: 'Abbrechen' }).click()
  await expect(page.getByRole('button', { name: 'Löschen' })).toBeFocused()

  await page.getByRole('button', { name: 'Löschen' }).click()
  await page.getByRole('button', { name: 'Endgültig löschen' }).click()
  await expect(page.getByText(/wurde gelöscht/)).toBeVisible()
  await expect(page.getByText('Du hast noch keine Projekte.')).toBeVisible()
})

test('eine Aufgabe kann einem eigenen Projekt zugeordnet werden; Löschen des Projekts löst sie', async ({ page }) => {
  await registerViaUi(page)
  await createProjectViaUi(page, 'Projekt mit Aufgabe')
  const projectUrl = page.url()

  await page.getByRole('link', { name: 'Meine Aufgaben' }).click()
  await page.getByRole('link', { name: 'Neue Aufgabe anlegen' }).click()
  await page.getByLabel('Projekt').selectOption({ label: 'Projekt mit Aufgabe' })
  await page.getByLabel('Titel').fill('Projektaufgabe')
  await page.getByRole('button', { name: 'Aufgabe anlegen' }).click()
  await expect(page.getByRole('heading', { level: 1, name: 'Projektaufgabe' })).toBeVisible()
  await expect(page.getByRole('link', { name: 'Projekt mit Aufgabe' })).toBeVisible()
  const taskUrl = page.url()

  await page.goto(projectUrl)
  await page.getByRole('button', { name: 'Löschen' }).click()
  await page.getByRole('button', { name: 'Endgültig löschen' }).click()
  await expect(page.getByText(/wurde gelöscht/)).toBeVisible()

  // Die Aufgabe bleibt erhalten und wird wieder zur persönlichen Aufgabe (US-042).
  await page.goto(taskUrl)
  await expect(page.getByRole('heading', { level: 1, name: 'Projektaufgabe' })).toBeVisible()
  await expect(page.getByText('Projekt mit Aufgabe')).toHaveCount(0)
})

test('zwei Konten sehen nur ihre eigenen Projekte', async ({ page, browser }) => {
  await registerViaUi(page, uniqueEmail(), PASSWORD, 'Person A')
  await createProjectViaUi(page, 'Geheimprojekt A')
  const projectUrl = page.url()

  const other = await browser.newContext()
  const second = await other.newPage()
  await registerViaUi(second, uniqueEmail(), PASSWORD, 'Person B')
  await second.goto('/projekte')
  await expect(second.getByText('Geheimprojekt A')).toHaveCount(0)

  await second.goto(projectUrl)
  await expect(second.getByRole('heading', { level: 1, name: 'Projekt nicht gefunden' })).toBeVisible()

  // Ein fremdes Projekt kann auch keiner eigenen Aufgabe zugewiesen werden.
  const projectId = projectUrl.split('/').pop()
  const csrf = await second.request.get('/api/v1/auth/csrf')
  const { headerName, token } = await csrf.json()
  const response = await second.request.post('/api/v1/tasks', {
    headers: { [headerName]: token },
    data: { projectId, title: 'Versuch' },
  })
  expect(response.status()).toBe(400)
  await other.close()
})

test('axe: Projektliste leer und gefüllt, Anlegen mit Fehler, Details und Löschdialog', async ({ page }) => {
  async function scan() {
    const results = await new AxeBuilder({ page }).withTags(TAGS).analyze()
    expect(results.violations).toEqual([])
  }

  await registerViaUi(page)
  await page.goto('/projekte')
  await scan()

  await page.getByRole('link', { name: 'Neues Projekt anlegen' }).click()
  await page.getByRole('button', { name: 'Projekt anlegen' }).click()
  await expect(page.getByRole('alert')).toBeVisible()
  await scan()

  await createProjectViaApi(page, 'Für axe-Scan')
  await page.goto('/projekte')
  await scan()

  await page.getByRole('link', { name: 'Für axe-Scan' }).click()
  await scan()
  await page.getByRole('button', { name: 'Löschen' }).click()
  await expect(page.getByRole('dialog')).toBeVisible()
  await scan()
})
