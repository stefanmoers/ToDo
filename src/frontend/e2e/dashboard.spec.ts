import AxeBuilder from '@axe-core/playwright'
import { expect, test } from '@playwright/test'
import { completeTaskViaApi, createTaskViaApi, registerViaUi } from './helpers'

const TAGS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa']

test('Übersicht zeigt Zähler und führt zur passenden Liste', async ({ page }) => {
  await registerViaUi(page)
  await createTaskViaApi(page, { title: 'Offene Aufgabe' })
  await createTaskViaApi(page, { title: 'Dringende Aufgabe', priority: 'URGENT' })
  const doneId = await createTaskViaApi(page, { title: 'Erledigte Aufgabe' })
  await completeTaskViaApi(page, doneId)

  await page.goto('/uebersicht')
  await expect(page.getByRole('heading', { level: 1, name: 'Übersicht' })).toBeVisible()
  await expect(page.getByRole('link', { name: /2 nicht erledigte Aufgaben/ })).toBeVisible()
  await expect(page.getByRole('link', { name: /1 erledigt/ })).toBeVisible()
  await expect(page.getByRole('link', { name: /1 offen mit hoher oder dringender Priorität/ })).toBeVisible()

  await page.getByRole('link', { name: /1 erledigt/ }).click()
  await expect(page.getByRole('link', { name: 'Erledigte Aufgabe' })).toBeVisible()
  await expect(page.getByRole('link', { name: 'Offene Aufgabe' })).toHaveCount(0)
})

test('axe: Übersicht mit Zählern', async ({ page }) => {
  await registerViaUi(page)
  await createTaskViaApi(page, { title: 'Für axe-Scan' })
  await page.goto('/uebersicht')
  await expect(page.getByRole('link', { name: /nicht erledigte Aufgaben/ })).toBeVisible()
  const results = await new AxeBuilder({ page }).withTags(TAGS).analyze()
  expect(results.violations).toEqual([])
})
