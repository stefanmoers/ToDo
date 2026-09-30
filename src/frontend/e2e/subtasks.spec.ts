import AxeBuilder from '@axe-core/playwright'
import { expect, test } from '@playwright/test'
import { createTaskViaUi, registerViaUi } from './helpers'

const TAGS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa']

test('vollständiger Lebenszyklus: anlegen, erledigen, umbenennen, verschieben, löschen (US-070/071)', async ({ page }) => {
  await registerViaUi(page)
  await createTaskViaUi(page, 'Umzug')

  await page.getByLabel('Neue Unteraufgabe').fill('Kartons besorgen')
  await page.getByRole('button', { name: 'Hinzufügen' }).click()
  await expect(page.getByText('Kartons besorgen', { exact: true })).toBeVisible()
  await expect(page.getByText('0 von 1 erledigt')).toBeVisible()

  await page.getByLabel('Neue Unteraufgabe').fill('Möbel abbauen')
  await page.getByRole('button', { name: 'Hinzufügen' }).click()
  await expect(page.getByText('0 von 2 erledigt')).toBeVisible()

  const first = page.locator('li', { hasText: 'Kartons besorgen' })
  const second = page.locator('li', { hasText: 'Möbel abbauen' })

  // Erledigen bleibt nach Neuladen erhalten und ändert die Hauptaufgabe nicht (US-071).
  await first.getByRole('checkbox').check()
  await expect(page.getByText('1 von 2 erledigt')).toBeVisible()
  await page.reload()
  await expect(first.getByRole('checkbox')).toBeChecked()
  await expect(page.getByText('Offen')).toBeVisible()

  // Rückgängig änderbar.
  await first.getByRole('checkbox').uncheck()
  await expect(page.getByText('0 von 2 erledigt')).toBeVisible()

  // Umbenennen.
  await first.getByRole('button', { name: 'Umbenennen' }).click()
  const renameInput = page.getByLabel('Titel von „Kartons besorgen“ ändern')
  await renameInput.fill('Kartons kaufen')
  await first.getByRole('button', { name: 'Speichern' }).click()
  await expect(page.getByText('Kartons kaufen', { exact: true })).toBeVisible()
  await expect(page.getByText('Kartons besorgen', { exact: true })).toHaveCount(0)
  const firstRenamed = page.locator('li', { hasText: 'Kartons kaufen' })

  // Verschieben ohne Ziehbewegung: Ränder sind wirkungslos, dazwischen wird vertauscht.
  await expect(firstRenamed.getByRole('button', { name: 'Nach oben' })).toBeDisabled()
  await expect(second.getByRole('button', { name: 'Nach unten' })).toBeDisabled()
  await second.getByRole('button', { name: 'Nach oben' }).click()
  const subtaskTitles = page.locator('.task-list[aria-label="Unteraufgaben"] .task-title')
  await expect(subtaskTitles.first()).toHaveText('Möbel abbauen')
  expect(await subtaskTitles.allTextContents()).toEqual(['Möbel abbauen', 'Kartons kaufen'])

  // Löschen entfernt nur die Unteraufgabe, nicht die Hauptaufgabe.
  await page.locator('li', { hasText: 'Möbel abbauen' }).getByRole('button', { name: 'Löschen' }).click()
  await expect(page.getByText('Möbel abbauen', { exact: true })).toHaveCount(0)
  await expect(page.getByRole('heading', { level: 1, name: 'Umzug' })).toBeVisible()

  // Leerer Titel wird am Feld gemeldet, ohne die Liste zu ändern.
  await page.getByRole('button', { name: 'Hinzufügen' }).click()
  await expect(page.getByRole('alert')).toBeVisible()
  await expect(page.getByText('Kartons kaufen', { exact: true })).toBeVisible()
})

test('axe: Unteraufgaben leer, gefüllt, mit Fehler und beim Umbenennen', async ({ page }) => {
  async function scan() {
    const results = await new AxeBuilder({ page }).withTags(TAGS).analyze()
    expect(results.violations).toEqual([])
  }

  await registerViaUi(page)
  await createTaskViaUi(page, 'Für axe-Scan')
  await expect(page.getByText('Noch keine Unteraufgaben.')).toBeVisible()
  await scan()

  await page.getByRole('button', { name: 'Hinzufügen' }).click()
  await expect(page.getByRole('alert')).toBeVisible()
  await scan()

  await page.getByLabel('Neue Unteraufgabe').fill('Erste Unteraufgabe')
  await page.getByRole('button', { name: 'Hinzufügen' }).click()
  await expect(page.getByText('Erste Unteraufgabe', { exact: true })).toBeVisible()
  await scan()

  await page.locator('li', { hasText: 'Erste Unteraufgabe' }).getByRole('button', { name: 'Umbenennen' }).click()
  await scan()
})
