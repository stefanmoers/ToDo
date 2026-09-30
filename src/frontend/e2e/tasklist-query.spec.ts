import AxeBuilder from '@axe-core/playwright'
import { expect, test } from '@playwright/test'
import { completeTaskViaApi, createTaskViaApi, registerViaUi } from './helpers'

const TAGS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa']

test('Suche findet Titel und Beschreibung ohne Beachtung der Großschreibung', async ({ page }) => {
  await registerViaUi(page)
  await createTaskViaApi(page, { title: 'Steuererklärung abgeben', description: 'Belege sortieren' })
  await createTaskViaApi(page, { title: 'Einkaufen', description: 'Milch und Brot kaufen' })
  await page.goto('/')

  await page.getByLabel('Suche').fill('MILCH');
  await page.getByRole('button', { name: 'Suchen' }).click()
  await expect(page.getByRole('link', { name: 'Einkaufen' })).toBeVisible()
  await expect(page.getByRole('link', { name: 'Steuererklärung abgeben' })).toHaveCount(0)
})

test('Status- und Prioritätsfilter werden kombiniert; verschiedene Filterarten mit UND', async ({ page }) => {
  await registerViaUi(page)
  await createTaskViaApi(page, { title: 'Offen hoch', priority: 'HIGH' })
  await createTaskViaApi(page, { title: 'Offen niedrig', priority: 'LOW' })
  const doneHighId = await createTaskViaApi(page, { title: 'Erledigt hoch', priority: 'HIGH' })
  await completeTaskViaApi(page, doneHighId)
  await page.goto('/')

  // Nur Priorität gefiltert: beide "Hoch"-Aufgaben zählen, unabhängig vom Status (ein Filtertyp allein).
  await page.getByLabel('Hoch').click()
  await expect(page.getByRole('link', { name: 'Offen hoch' })).toBeVisible()
  await expect(page.getByRole('link', { name: 'Erledigt hoch' })).toBeVisible()
  await expect(page.getByRole('link', { name: 'Offen niedrig' })).toHaveCount(0)

  // Zusätzlich Status "Offen": beide Filterarten werden mit UND verknüpft.
  await page.getByLabel('Offen', { exact: true }).click()
  await expect(page.getByRole('link', { name: 'Offen hoch' })).toBeVisible()
  await expect(page.getByRole('link', { name: 'Erledigt hoch' })).toHaveCount(0)
  await expect(page.getByRole('link', { name: 'Offen niedrig' })).toHaveCount(0)
})

test('Sortierung nach Priorität folgt der fachlichen Rangfolge', async ({ page }) => {
  await registerViaUi(page)
  await createTaskViaApi(page, { title: 'P-URGENT', priority: 'URGENT' })
  await createTaskViaApi(page, { title: 'P-LOW', priority: 'LOW' })
  await createTaskViaApi(page, { title: 'P-HIGH', priority: 'HIGH' })
  await page.goto('/')

  await page.getByLabel('Sortierung').selectOption({ label: 'Priorität (hoch zuerst)' })
  await expect(page.locator('.task-list .task-title')).toHaveText(['P-URGENT', 'P-HIGH', 'P-LOW'])
})

test('Standardansichten Heute, Demnächst, Überfällig und Erledigt zeigen jeweils die richtigen Aufgaben', async ({
  page,
}) => {
  await registerViaUi(page)
  const today = new Date().toISOString().slice(0, 10)
  await createTaskViaApi(page, { title: 'Fällig heute', dueDate: today, dueTimeZone: 'Europe/Berlin' })
  await createTaskViaApi(page, { title: 'Verspätete Aufgabe', dueDate: '2000-01-01', dueTimeZone: 'Europe/Berlin' })
  const doneId = await createTaskViaApi(page, { title: 'Fertig' })
  await completeTaskViaApi(page, doneId)
  await page.goto('/')

  await page.getByRole('link', { name: 'Heute', exact: true }).click()
  await expect(page.getByRole('link', { name: 'Fällig heute' })).toBeVisible()
  await expect(page.getByRole('link', { name: 'Verspätete Aufgabe' })).toHaveCount(0)

  await page.getByRole('link', { name: 'Überfällig', exact: true }).click()
  await expect(page.getByRole('link', { name: 'Verspätete Aufgabe' })).toBeVisible()
  await expect(page.getByRole('link', { name: 'Fällig heute' })).toHaveCount(0)

  await page.getByRole('link', { name: 'Erledigt', exact: true }).click()
  await expect(page.getByRole('link', { name: 'Fertig' })).toBeVisible()

  await page.getByRole('link', { name: 'Alle', exact: true }).click()
  await expect(page.getByRole('link', { name: 'Fällig heute' })).toBeVisible()
  await expect(page.getByRole('link', { name: 'Fertig' })).toBeVisible()
})

test('axe: Aufgabenliste mit Suche, Filtern und aktiver Standardansicht', async ({ page }) => {
  async function scan() {
    const results = await new AxeBuilder({ page }).withTags(TAGS).analyze()
    expect(results.violations).toEqual([])
  }

  await registerViaUi(page)
  await createTaskViaApi(page, { title: 'Für axe-Scan', priority: 'HIGH' })
  await page.goto('/')
  await scan()

  await page.getByLabel('Suche').fill('axe')
  await page.getByRole('button', { name: 'Suchen' }).click()
  await scan()

  await page.getByRole('link', { name: 'Überfällig', exact: true }).click()
  await scan()
})
