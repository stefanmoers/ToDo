import { expect, test } from '@playwright/test'

test('Startseite lädt und meldet Systembereitschaft über den Webserver', async ({ page }) => {
  await page.goto('/')
  await expect(page.getByRole('heading', { level: 1, name: 'UltimateTodo' })).toBeVisible()
  await expect(page.getByTestId('system-status')).toHaveText('Das System ist bereit.')
})

test('Sprunglink ist per Tastatur erreichbar', async ({ page }) => {
  await page.goto('/')
  await page.keyboard.press('Tab')
  await expect(page.getByRole('link', { name: 'Zum Hauptinhalt springen' })).toBeFocused()
})

test('interne Betriebsendpunkte sind über den Webserver nicht erreichbar', async ({ request }) => {
  for (const path of ['/actuator/env', '/actuator/configprops', '/actuator/heapdump']) {
    const response = await request.get(path)
    expect(response.status(), path).toBeGreaterThanOrEqual(400)
  }
})
