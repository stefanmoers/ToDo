import AxeBuilder from '@axe-core/playwright'
import { expect, test } from '@playwright/test'
import { createTaskViaUi, registerViaUi } from './helpers'

const TAGS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa']

test('vollständiger Lebenszyklus: Erstellen, Änderungen, Erledigen und Kommentar erscheinen ohne Neuladen (US-080/081)', async ({
  page,
}) => {
  await registerViaUi(page)
  await createTaskViaUi(page, 'Testaufgabe')

  await expect(page.getByText('Aufgabe angelegt')).toBeVisible()

  // Titel- und Prioritätsänderung erscheinen sofort im Verlauf, ohne dass die Seite neu geladen wird (US-081).
  await page.getByRole('link', { name: 'Bearbeiten' }).click()
  await page.getByLabel('Titel').fill('Testaufgabe Zwei')
  await page.getByLabel('Priorität').selectOption('HIGH')
  await page.getByRole('button', { name: 'Änderungen speichern' }).click()
  await expect(page.getByText('Titel geändert: von „Testaufgabe“ zu „Testaufgabe Zwei“')).toBeVisible()
  await expect(page.getByText('Priorität geändert: von Normal zu Hoch')).toBeVisible()

  // Erledigen erscheint ebenfalls sofort, als eigene Art statt einer allgemeinen Statusänderung.
  await page.getByRole('button', { name: 'Als erledigt markieren' }).click()
  await expect(page.getByText('Als erledigt markiert')).toBeVisible()
  await expect(page.getByText('Status geändert')).toHaveCount(0);

  // Ein Kommentar erscheint mit vollem Text, chronologisch nach den bisherigen Einträgen.
  await page.getByLabel('Kommentar hinzufügen').fill('Das ist ein Testkommentar mit <b>HTML</b>.')
  await page.getByRole('button', { name: 'Kommentieren' }).click()
  await expect(page.getByText('Das ist ein Testkommentar mit <b>HTML</b>.', { exact: true })).toBeVisible()

  const messages = page.locator('.history-list li p:not(.history-meta)')
  await expect(messages).toHaveCount(5)
  await expect(messages.nth(0)).toHaveText('Aufgabe angelegt')
  await expect(messages.nth(1)).toHaveText('Titel geändert: von „Testaufgabe“ zu „Testaufgabe Zwei“')
  await expect(messages.nth(2)).toHaveText('Priorität geändert: von Normal zu Hoch')
  await expect(messages.nth(3)).toHaveText('Als erledigt markiert')
  await expect(messages.nth(4)).toHaveText('Das ist ein Testkommentar mit <b>HTML</b>.')

  // Kein Client-seitiges Rendering von HTML: der Kommentar erscheint als reiner Text, nie als echtes <b>-Element.
  await expect(page.locator('b', { hasText: 'HTML' })).toHaveCount(0)
})

test('leerer Kommentar wird am Feld gemeldet, ohne den Verlauf zu ändern', async ({ page }) => {
  await registerViaUi(page)
  await createTaskViaUi(page, 'X')
  await expect(page.getByText('Noch keine Einträge.')).toHaveCount(0)

  await page.getByRole('button', { name: 'Kommentieren' }).click()
  await expect(page.getByRole('alert')).toBeVisible()
  const messages = page.locator('.history-list li p:not(.history-meta)')
  await expect(messages).toHaveCount(1)
})

test('axe: Verlauf leer, gefüllt und mit Feldfehler', async ({ page }) => {
  async function scan() {
    const results = await new AxeBuilder({ page }).withTags(TAGS).analyze()
    expect(results.violations).toEqual([])
  }

  await registerViaUi(page)
  await createTaskViaUi(page, 'Für axe-Scan')
  await scan()

  await page.getByRole('button', { name: 'Kommentieren' }).click()
  await expect(page.getByRole('alert')).toBeVisible()
  await scan()

  await page.getByLabel('Kommentar hinzufügen').fill('Ein Kommentar')
  await page.getByRole('button', { name: 'Kommentieren' }).click()
  await expect(page.getByText('Ein Kommentar', { exact: true })).toBeVisible()
  await scan()
})
