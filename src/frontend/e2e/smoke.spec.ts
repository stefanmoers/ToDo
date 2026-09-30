import { expect, test } from '@playwright/test'

test('Startseite leitet Besucher zur Anmeldung', async ({ page }) => {
  await page.goto('/')
  await expect(page).toHaveURL(/\/anmelden$/)
  await expect(page.getByRole('heading', { level: 1, name: 'Anmelden' })).toBeVisible()
})

test('Sprunglink ist per Tastatur erreichbar', async ({ page }) => {
  await page.goto('/anmelden')
  await page.keyboard.press('Tab')
  await expect(page.getByRole('link', { name: 'Zum Hauptinhalt springen' })).toBeFocused()
})

test('interne Betriebsendpunkte sind über den Webserver nicht erreichbar', async ({ request }) => {
  for (const path of ['/actuator/env', '/actuator/configprops', '/actuator/heapdump', '/v3/api-docs']) {
    const response = await request.get(path)
    expect(response.status(), path).toBeGreaterThanOrEqual(400)
  }
})

test('Sicherheitsheader und kein Caching privater API-Antworten', async ({ request }) => {
  const page = await request.get('/')
  expect(page.headers()['content-security-policy']).toContain("default-src 'self'")
  expect(page.headers()['x-content-type-options']).toBe('nosniff')
  const me = await request.get('/api/v1/auth/me')
  expect(me.status()).toBe(401)
  expect(me.headers()['cache-control']).toMatch(/no-store|no-cache/)
  expect((await me.json()).code).toBe('UNAUTHENTICATED')
})
