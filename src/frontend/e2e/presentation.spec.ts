import AxeBuilder from '@axe-core/playwright'
import { expect, test } from '@playwright/test'

const TAGS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa']
const FOLIEN = 18

test('Präsentation: öffentlich erreichbar und mit Pfeilen und Tasten bedienbar (US-182)', async ({ page }) => {
  // Kein Login: die Seite darf weder zur Anmeldung umleiten noch die API brauchen.
  await page.goto('/präsentation')
  await expect(page.getByRole('heading', { level: 2, name: 'UltimateTodo' })).toBeVisible()
  await expect(page.getByRole('group', { name: `Folie 1 von ${FOLIEN}: UltimateTodo` })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Vorherige Folie' })).toHaveAttribute('aria-disabled', 'true')

  await page.getByRole('button', { name: 'Nächste Folie' }).click()
  await expect(page).toHaveURL(/\/pr(ä|%C3%A4)sentation\/2$/)
  await expect(page.getByRole('group', { name: new RegExp(`^Folie 2 von ${FOLIEN}`) })).toBeVisible()

  await page.keyboard.press('ArrowRight')
  await expect(page.getByRole('group', { name: new RegExp(`^Folie 3 von ${FOLIEN}`) })).toBeVisible()
  await page.keyboard.press('ArrowLeft')
  await page.getByRole('button', { name: 'Vorherige Folie' }).click()
  await expect(page.getByRole('group', { name: new RegExp(`^Folie 1 von ${FOLIEN}`) })).toBeVisible()

  await page.keyboard.press('End')
  await expect(page.getByRole('heading', { level: 2, name: 'Fragen und Diskussion' })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Nächste Folie' })).toHaveAttribute('aria-disabled', 'true')
  await page.keyboard.press('Home')
  await expect(page.getByRole('group', { name: new RegExp(`^Folie 1 von ${FOLIEN}`) })).toBeVisible()

  // Direkter Aufruf einer Folie, auch über die ASCII-Schreibweise.
  await page.goto('/praesentation/5')
  await expect(page.getByRole('group', { name: new RegExp(`^Folie 5 von ${FOLIEN}`) })).toBeVisible()

  // Notizen
  await page.keyboard.press('n')
  await expect(page.getByRole('button', { name: 'Sprechernotizen' })).toHaveAttribute('aria-expanded', 'true')
  await expect(page.getByText('Das ist das ganze System auf einer Folie.')).toBeVisible()
})

test('axe: jede Folie der Präsentation ohne Verstöße, auch mit offenen Sprechernotizen', async ({ page }) => {
  await page.goto('/präsentation')
  await page.keyboard.press('n')
  for (let i = 1; i <= FOLIEN; i++) {
    await expect(page.getByRole('group', { name: new RegExp(`^Folie ${i} von ${FOLIEN}`) })).toBeVisible()
    const results = await new AxeBuilder({ page }).withTags(TAGS).analyze()
    expect(results.violations, `Folie ${i}`).toEqual([])
    if (i < FOLIEN) await page.keyboard.press('ArrowRight')
  }
})
