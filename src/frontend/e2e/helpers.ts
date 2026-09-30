import { expect, type Page } from '@playwright/test'

export const PASSWORD = 'e2e-test-passwort-lang-genug'

export function uniqueEmail(): string {
  return `e2e-${Date.now()}-${Math.random().toString(36).slice(2, 8)}@example.test`
}

/** Registriert ein neues Konto über die Oberfläche; danach ist man angemeldet. */
export async function registerViaUi(page: Page, email = uniqueEmail(), password = PASSWORD, name = 'E2E Person') {
  await page.goto('/registrieren')
  await page.getByLabel('E-Mail-Adresse').fill(email)
  await page.getByLabel('Anzeigename').fill(name)
  await page.getByLabel('Passwort', { exact: true }).fill(password)
  await page.getByRole('button', { name: 'Konto anlegen' }).click()
  await expect(page.getByRole('heading', { level: 1, name: 'UltimateTodo' })).toBeVisible()
  return email
}
