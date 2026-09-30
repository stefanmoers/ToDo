import AxeBuilder from '@axe-core/playwright'
import { expect, test, type Page } from '@playwright/test'
import { registerViaUi } from './helpers'

const TAGS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa']

async function scan(page: Page) {
  const results = await new AxeBuilder({ page }).withTags(TAGS).analyze()
  expect(results.violations).toEqual([])
}

test('axe: Anmeldung normal und mit Fehlern', async ({ page }) => {
  await page.goto('/anmelden')
  await expect(page.getByRole('heading', { level: 1, name: 'Anmelden' })).toBeVisible()
  await scan(page)
  await page.getByRole('button', { name: 'Anmelden' }).click()
  await expect(page.getByRole('alert')).toBeVisible()
  await scan(page)
})

test('axe: Registrierung normal und mit Fehlern', async ({ page }) => {
  await page.goto('/registrieren')
  await expect(page.getByRole('heading', { level: 1, name: 'Registrieren' })).toBeVisible()
  await scan(page)
  await page.getByRole('button', { name: 'Konto anlegen' }).click()
  await expect(page.getByRole('alert')).toBeVisible()
  await scan(page)
})

test('axe: Profil und Passwortformular mit Fehlern, Startseite, Nicht gefunden', async ({ page }) => {
  await registerViaUi(page)
  await scan(page)
  await page.goto('/profil')
  await expect(page.getByRole('heading', { level: 1, name: 'Mein Profil' })).toBeVisible()
  await scan(page)
  await page.getByRole('button', { name: 'Passwort ändern' }).click()
  await expect(page.getByRole('alert')).toBeVisible()
  await scan(page)
  await page.goto('/gibt-es-nicht')
  await expect(page.getByRole('heading', { level: 1 })).toBeVisible()
  await scan(page)
})

test('Tastatur: Anmeldeformular vollständig ohne Maus bedienbar, Fehlerübersicht erhält den Fokus', async ({ page }) => {
  await page.goto('/anmelden')
  await page.getByLabel('E-Mail-Adresse').focus()
  await page.keyboard.press('Enter')
  await expect(page.getByRole('alert')).toBeFocused()
  await page.getByRole('link', { name: /E-Mail-Adresse: / }).press('Enter')
  await expect(page.getByLabel('E-Mail-Adresse')).toBeFocused()
})

test('Reflow: bei 320 px Breite kein horizontales Scrollen', async ({ page }) => {
  await page.setViewportSize({ width: 320, height: 640 })
  for (const path of ['/anmelden', '/registrieren']) {
    await page.goto(path)
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible()
    const overflow = await page.evaluate(() => document.documentElement.scrollWidth > document.documentElement.clientWidth)
    expect(overflow, path).toBe(false)
  }
})
