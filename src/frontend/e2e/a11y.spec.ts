import AxeBuilder from '@axe-core/playwright'
import { expect, test, type Page } from '@playwright/test'
import { createTaskViaApi, createTaskViaUi, registerViaUi } from './helpers'

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

test('axe: Aufgabenliste leer, gefüllt und mit Paginierung', async ({ page }) => {
  await registerViaUi(page)
  await scan(page)
  await createTaskViaApi(page, { title: 'Offene Aufgabe' })
  await createTaskViaApi(page, {
    title: 'Überfällige dringende Aufgabe',
    priority: 'URGENT',
    dueDate: '2000-01-01',
    dueTimeZone: 'Europe/Berlin',
  })
  for (let i = 1; i <= 24; i++) await createTaskViaApi(page, { title: `Aufgabe ${i}` })
  await page.goto('/')
  await expect(page.getByRole('heading', { level: 2, name: /^Aufgaben/ })).toBeVisible()
  await scan(page)
})

test('axe: Aufgabe anlegen normal, mit Fehlern und mit Fälligkeitsauswahl bei Zeitumstellung', async ({ page }) => {
  await registerViaUi(page)
  await page.getByRole('link', { name: 'Neue Aufgabe anlegen' }).click()
  await scan(page)
  await page.getByRole('button', { name: 'Aufgabe anlegen' }).click()
  await expect(page.getByRole('alert')).toBeVisible()
  await scan(page)

  await page.getByLabel('Titel').fill('Doppelte Uhrzeit')
  await page.getByLabel('Fälligkeitsdatum').fill('2026-10-25')
  await page.getByLabel('Uhrzeit').fill('02:30')
  await expect(page.getByRole('group', { name: 'Welcher Zeitpunkt ist gemeint?' })).toBeVisible()
  await scan(page)
})

test('axe: Aufgabendetails und geöffneter Löschdialog', async ({ page }) => {
  await registerViaUi(page)
  await createTaskViaUi(page, 'Details ansehen')
  await scan(page)
  await page.getByRole('button', { name: 'Löschen' }).click()
  await expect(page.getByRole('dialog')).toBeVisible()
  await scan(page)
})

test('axe: Konflikthinweis beim Bearbeiten', async ({ page, context }) => {
  await registerViaUi(page)
  await createTaskViaUi(page, 'Konflikt für axe')
  const taskUrl = page.url()
  const second = await context.newPage()
  await second.goto(`${taskUrl}/bearbeiten`)
  await page.goto(`${taskUrl}/bearbeiten`)
  await page.getByLabel('Titel').fill('Zuerst gespeichert')
  await page.getByRole('button', { name: 'Änderungen speichern' }).click()
  await second.getByLabel('Titel').fill('Danach versucht')
  await second.getByRole('button', { name: 'Änderungen speichern' }).click()
  await expect(second.getByRole('alert')).toBeVisible()
  await scan(second)
})

test('axe: Aufgabe nicht gefunden', async ({ page }) => {
  await registerViaUi(page)
  await page.goto('/aufgaben/00000000-0000-0000-0000-000000000000')
  await expect(page.getByRole('heading', { level: 1, name: 'Aufgabe nicht gefunden' })).toBeVisible()
  await scan(page)
})

test('Tastatur: Aufgabe ohne Maus anlegen, erledigen und löschen; der Dialog hält den Fokus', async ({ page }) => {
  await registerViaUi(page)
  await page.getByRole('link', { name: 'Neue Aufgabe anlegen' }).click()
  await page.getByLabel('Titel').fill('Nur mit Tastatur')
  await page.getByRole('button', { name: 'Aufgabe anlegen' }).click()
  await expect(page.getByRole('heading', { level: 1, name: 'Nur mit Tastatur' })).toBeVisible()

  await page.getByRole('button', { name: /Als erledigt markieren/ }).focus()
  await page.keyboard.press('Enter')
  await expect(page.getByRole('button', { name: /Wieder öffnen/ })).toBeVisible()

  await page.getByRole('button', { name: 'Löschen' }).focus()
  await page.keyboard.press('Enter')
  const dialog = page.getByRole('dialog')
  await expect(dialog).toBeVisible()
  for (let i = 0; i < 5; i++) {
    await page.keyboard.press('Tab')
    const withinDialog = await page.evaluate(() => document.activeElement?.closest('dialog') !== null)
    expect(withinDialog).toBe(true)
  }
  await page.keyboard.press('Escape')
  await expect(dialog).toBeHidden()
  await expect(page.getByRole('button', { name: 'Löschen' })).toBeFocused()
})

test('Reflow: Aufgaben-Seiten bei 320 px ohne horizontales Scrollen', async ({ page }) => {
  await registerViaUi(page)
  const longTitle = 'X'.repeat(200)
  await createTaskViaApi(page, { title: longTitle, description: 'Y'.repeat(500) })
  await page.setViewportSize({ width: 320, height: 640 })
  for (const path of ['/', `/aufgaben/neu`]) {
    await page.goto(path)
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible()
    const overflow = await page.evaluate(() => document.documentElement.scrollWidth > document.documentElement.clientWidth)
    expect(overflow, path).toBe(false)
  }
  await page.goto('/')
  await page.getByRole('link', { name: longTitle }).click()
  const overflowDetail = await page.evaluate(() => document.documentElement.scrollWidth > document.documentElement.clientWidth)
  expect(overflowDetail).toBe(false)
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
