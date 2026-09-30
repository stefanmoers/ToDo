import { expect, test } from '@playwright/test'
import { PASSWORD, registerViaUi, uniqueEmail } from './helpers'

test('Registrieren, Neuladen, Abmelden und erneut Anmelden', async ({ page }) => {
  const email = await registerViaUi(page)
  await expect(page.getByText('Hallo E2E Person!')).toBeVisible()

  // Anmeldestatus bleibt beim Neuladen erhalten (US-020).
  await page.reload()
  await expect(page.getByText('Hallo E2E Person!')).toBeVisible()

  await page.getByRole('button', { name: /Abmelden/ }).click()
  await expect(page.getByRole('heading', { level: 1, name: 'Anmelden' })).toBeVisible()
  await expect(page.getByRole('status')).toHaveText('Du wurdest abgemeldet.')

  // Nach Logout führt die geschützte Seite nicht mehr zu Daten (US-021).
  await page.goto('/profil')
  await expect(page).toHaveURL(/\/anmelden$/)

  await page.getByLabel('E-Mail-Adresse').fill(email)
  await page.getByLabel('Passwort').fill(PASSWORD)
  await page.getByRole('button', { name: 'Anmelden' }).click()
  await expect(page.getByText('Hallo E2E Person!')).toBeVisible()
})

test('Falsche Zugangsdaten zeigen eine allgemeine Meldung und behalten die E-Mail', async ({ page }) => {
  await page.goto('/anmelden')
  await page.getByLabel('E-Mail-Adresse').fill('unbekannt@example.test')
  await page.getByLabel('Passwort').fill('falsches passwort')
  await page.getByRole('button', { name: 'Anmelden' }).click()
  await expect(page.getByRole('alert')).toContainText('E-Mail-Adresse oder Passwort ist nicht korrekt.')
  await expect(page.getByLabel('E-Mail-Adresse')).toHaveValue('unbekannt@example.test')
})

test('Doppelte E-Mail wird am Feld gemeldet', async ({ page, context }) => {
  const email = await registerViaUi(page)
  await context.clearCookies()
  await page.goto('/registrieren')
  await page.getByLabel('E-Mail-Adresse').fill(email)
  await page.getByLabel('Anzeigename').fill('Zweite Person')
  await page.getByLabel('Passwort', { exact: true }).fill(PASSWORD)
  await page.getByRole('button', { name: 'Konto anlegen' }).click()
  await expect(page.getByRole('alert')).toContainText('bereits registriert')
  await expect(page.getByLabel('E-Mail-Adresse')).toHaveAttribute('aria-invalid', 'true')
})

test('Profil ändern und Passwort ändern entwertet die Sitzung, auch in einem zweiten Browser', async ({ page, browser }) => {
  const email = await registerViaUi(page)
  await page.getByRole('link', { name: 'Mein Profil' }).click()
  await page.getByLabel('Anzeigename').fill('Neuer Name')
  await page.getByRole('button', { name: 'Profil speichern' }).click()
  await expect(page.getByText('Das Profil wurde gespeichert.')).toBeVisible()

  // Zweite Sitzung desselben Kontos.
  const other = await browser.newContext()
  const second = await other.newPage()
  await second.goto('/anmelden')
  await second.getByLabel('E-Mail-Adresse').fill(email)
  await second.getByLabel('Passwort').fill(PASSWORD)
  await second.getByRole('button', { name: 'Anmelden' }).click()
  await expect(second.getByText('Hallo Neuer Name!')).toBeVisible()

  const newPassword = 'ein-ganz-neues-passwort-99'
  await page.getByLabel('Bisheriges Passwort').fill(PASSWORD)
  await page.getByLabel('Neues Passwort').fill(newPassword)
  await page.getByRole('button', { name: 'Passwort ändern' }).click()
  await expect(page.getByRole('heading', { level: 1, name: 'Anmelden' })).toBeVisible()
  await expect(page.getByRole('status')).toContainText('Dein Passwort wurde geändert')

  // Die zweite Sitzung ist entwertet: nächste geschützte Anfrage führt zur Anmeldung.
  await second.reload()
  await expect(second.getByRole('heading', { level: 1, name: 'Anmelden' })).toBeVisible()
  await other.close()

  // Altes Passwort funktioniert nicht mehr, neues schon.
  await page.getByLabel('E-Mail-Adresse').fill(email)
  await page.getByLabel('Passwort').fill(PASSWORD)
  await page.getByRole('button', { name: 'Anmelden' }).click()
  await expect(page.getByRole('alert')).toContainText('nicht korrekt')
  await page.getByLabel('Passwort').fill(newPassword)
  await page.getByRole('button', { name: 'Anmelden' }).click()
  await expect(page.getByText('Hallo Neuer Name!')).toBeVisible()
})

test('Zwei Konten sehen nur ihre eigenen Profildaten', async ({ page, browser }) => {
  const a = await registerViaUi(page, uniqueEmail(), PASSWORD, 'Person A')
  const other = await browser.newContext()
  const second = await other.newPage()
  const b = await registerViaUi(second, uniqueEmail(), PASSWORD, 'Person B')
  await page.goto('/profil')
  await expect(page.getByText(a)).toBeVisible()
  await expect(page.getByText(b)).toHaveCount(0)
  await other.close()
})
