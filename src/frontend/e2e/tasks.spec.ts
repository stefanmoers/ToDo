import { expect, test } from '@playwright/test'
import { createTaskViaApi, createTaskViaUi, PASSWORD, registerViaUi, uniqueEmail } from './helpers'

test('vollständiger Lebenszyklus: anlegen, ansehen, bearbeiten, erledigen, wieder öffnen, löschen', async ({ page }) => {
  await registerViaUi(page)
  await expect(page.getByText('Du hast noch keine Aufgaben.')).toBeVisible()

  await createTaskViaUi(page, 'Steuererklärung')
  await page.getByRole('link', { name: 'Bearbeiten' }).click()
  await page.getByLabel('Beschreibung').fill('Belege sortieren')
  await page.getByLabel('Priorität').selectOption('HIGH')
  await page.getByLabel('Fälligkeitsdatum').fill('2026-12-24')
  await page.getByLabel('Uhrzeit').fill('18:00')
  await page.getByRole('button', { name: 'Änderungen speichern' }).click()
  await expect(page.getByText('Die Änderungen wurden gespeichert.')).toBeVisible()
  await expect(page.getByText('Hoch')).toBeVisible()
  await expect(page.getByText(/24\. Dezember 2026/)).toBeVisible()

  // Persistenz nach Neuladen (US-002).
  await page.reload()
  await expect(page.getByRole('heading', { level: 1, name: 'Steuererklärung' })).toBeVisible()
  await expect(page.getByText(/24\. Dezember 2026/)).toBeVisible()

  await page.getByRole('button', { name: /Als erledigt markieren/ }).click()
  await expect(page.getByText('Status')).toBeVisible()
  await expect(page.getByRole('button', { name: /Wieder öffnen/ })).toBeVisible()
  await expect(page.getByText('Erledigt', { exact: true })).toBeVisible()

  await page.getByRole('button', { name: /Wieder öffnen/ }).click()
  await expect(page.getByText('Offen', { exact: true })).toBeVisible()

  await page.getByRole('button', { name: 'Löschen' }).click();
  await expect(page.getByRole('dialog')).toBeVisible()
  await page.getByRole('button', { name: 'Abbrechen' }).click()
  await expect(page.getByRole('button', { name: 'Löschen' })).toBeFocused()

  await page.getByRole('button', { name: 'Löschen' }).click()
  await page.getByRole('button', { name: 'Endgültig löschen' }).click()
  await expect(page.getByText(/wurde gelöscht/)).toBeVisible()

  await page.goto(page.url()) // aktuelle Liste
  await expect(page.getByText('Du hast noch keine Aufgaben.')).toBeVisible()
})

test('Fälligkeit lässt sich wieder entfernen', async ({ page }) => {
  await registerViaUi(page)
  await createTaskViaUi(page, 'Mit Fälligkeit')
  await page.getByRole('link', { name: 'Bearbeiten' }).click()
  await page.getByLabel('Fälligkeitsdatum').fill('2026-08-01')
  await page.getByRole('button', { name: 'Änderungen speichern' }).click()
  await expect(page.getByText(/1\. August 2026/)).toBeVisible()

  await page.getByRole('link', { name: 'Bearbeiten' }).click()
  await page.getByRole('button', { name: 'Fälligkeit entfernen' }).click()
  await page.getByRole('button', { name: 'Änderungen speichern' }).click()
  await expect(page.getByText('Keine Fälligkeit')).toBeVisible()
})

test('Validierung: leerer oder zu langer Titel wird an der Fehlerübersicht gemeldet', async ({ page }) => {
  await registerViaUi(page)
  await page.getByRole('link', { name: 'Neue Aufgabe anlegen' }).click()
  await page.getByRole('button', { name: 'Aufgabe anlegen' }).click()
  await expect(page.getByRole('alert')).toBeFocused()
  await expect(page.getByRole('alert')).toContainText('Titel')

  await page.getByLabel('Titel').fill('x'.repeat(201))
  await page.getByRole('button', { name: 'Aufgabe anlegen' }).click()
  await expect(page.getByRole('alert')).toContainText('200 Zeichen')
})

test('zwei Konten sehen nur ihre eigenen Aufgaben', async ({ page, browser }) => {
  await registerViaUi(page, uniqueEmail(), PASSWORD, 'Person A')
  await createTaskViaUi(page, 'Geheim A')
  const taskUrl = page.url()

  const other = await browser.newContext()
  const second = await other.newPage()
  await registerViaUi(second, uniqueEmail(), PASSWORD, 'Person B')
  await expect(second.getByText('Geheim A')).toHaveCount(0)

  await second.goto(taskUrl)
  await expect(second.getByRole('heading', { level: 1, name: 'Aufgabe nicht gefunden' })).toBeVisible()

  const taskId = taskUrl.split('/').pop()
  const directApi = await second.request.get(`/api/v1/tasks/${taskId}`)
  expect(directApi.status()).toBe(404)
  await other.close()
})

test('Bearbeitungskonflikt: eigene Eingaben bleiben erhalten, Serverfassung kann übernommen werden', async ({
  page,
  context,
}) => {
  await registerViaUi(page)
  await createTaskViaUi(page, 'Konflikttest')
  const taskUrl = page.url()

  const second = await context.newPage()
  await second.goto(`${taskUrl}/bearbeiten`)
  await page.goto(`${taskUrl}/bearbeiten`)

  await page.getByLabel('Titel').fill('Fassung A')
  await page.getByRole('button', { name: 'Änderungen speichern' }).click()
  await expect(page.getByRole('heading', { level: 1, name: 'Fassung A' })).toBeVisible()

  await second.getByLabel('Titel').fill('Fassung B')
  await second.getByRole('button', { name: 'Änderungen speichern' }).click()
  const alert = second.getByRole('alert')
  await expect(alert).toBeVisible()
  await expect(alert).toContainText('zwischenzeitlich geändert')
  await expect(alert).toContainText('Fassung A')
  await expect(second.getByLabel('Titel')).toHaveValue('Fassung B')

  await second.getByRole('button', { name: 'Meine Eingaben behalten' }).click()
  await second.getByRole('button', { name: 'Änderungen speichern' }).click()
  await expect(second.getByRole('heading', { level: 1, name: 'Fassung B' })).toBeVisible()
})

test('Löschkonflikt: Löschen scheitert mit Alarm, zweiter Versuch danach erfolgreich', async ({ page, context }) => {
  await registerViaUi(page)
  await createTaskViaUi(page, 'Löschkonflikt')
  const taskUrl = page.url()

  const second = await context.newPage()
  await second.goto(taskUrl)

  await page.goto(`${taskUrl}/bearbeiten`)
  await page.getByLabel('Titel').fill('Geändert von A')
  await page.getByRole('button', { name: 'Änderungen speichern' }).click()

  await second.getByRole('button', { name: 'Löschen' }).click()
  await second.getByRole('button', { name: 'Endgültig löschen' }).click()
  await expect(second.getByRole('alert')).toContainText('Nicht gelöscht')
  await expect(second.getByRole('heading', { level: 1, name: 'Geändert von A' })).toBeVisible()

  await second.getByRole('button', { name: 'Löschen' }).click()
  await second.getByRole('button', { name: 'Endgültig löschen' }).click()
  await expect(second.getByText(/wurde gelöscht/)).toBeVisible()
})

test('Paginierung: 26 Aufgaben werden auf zwei Seiten aufgeteilt', async ({ page }) => {
  await registerViaUi(page)
  for (let i = 1; i <= 26; i++) {
    await createTaskViaApi(page, { title: `Aufgabe ${i}` })
  }
  await page.goto('/')
  await expect(page.locator('.task-list li')).toHaveCount(25)
  await page.getByRole('link', { name: 'Nächste Seite' }).click()
  await expect(page).toHaveURL(/seite=2/)
  await expect(page.getByRole('heading', { level: 2, name: /^Aufgaben/ })).toBeFocused()
  await page.getByRole('link', { name: 'Vorherige Seite' }).click()
  await expect(page).toHaveURL(/seite=1/)
})
