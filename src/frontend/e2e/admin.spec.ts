import AxeBuilder from '@axe-core/playwright'
import { expect, test } from '@playwright/test'
import { registerViaUi } from './helpers'

const TAGS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa']

/**
 * US-014: nur die Rolle ADMIN erreicht die Kontenverwaltung. Der Admin-Erstbezug selbst läuft über einen
 * beim Anwendungsstart gelesenen Umgebungswert (ADMIN_BOOTSTRAP_EMAIL, siehe docs/SECURITY.md) — ein zur
 * Laufzeit von Playwright registriertes Konto kann diesen Weg hier nicht durchlaufen, ohne den Compose-Stack
 * mitten im Testlauf neu zu starten. Die Admin-Handlungen selbst (auflisten/deaktivieren/reaktivieren) sind
 * stattdessen durch UserAdminApiIT (Backend) und AdminPages.test.tsx (Frontend) abgedeckt; hier wird die
 * Zugriffsbeschränkung für normale Benutzer geprüft.
 */
test('normale Benutzer sehen weder den Administration-Link noch erreichen sie die Kontenverwaltung', async ({ page }) => {
  await registerViaUi(page)
  await expect(page.getByRole('link', { name: 'Administration' })).toHaveCount(0)

  await page.goto('/administration')
  await expect(page.getByText('Diese Aktion ist nicht erlaubt.')).toBeVisible()
})

test('axe: Kontenverwaltung ohne Berechtigung', async ({ page }) => {
  await registerViaUi(page)
  await page.goto('/administration')
  await expect(page.getByText('Diese Aktion ist nicht erlaubt.')).toBeVisible()
  const results = await new AxeBuilder({ page }).withTags(TAGS).analyze()
  expect(results.violations).toEqual([])
})
