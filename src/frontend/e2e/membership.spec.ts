import AxeBuilder from '@axe-core/playwright'
import { expect, test } from '@playwright/test'
import { addMemberViaApi, createProjectViaApi, PASSWORD, registerViaUi, uniqueEmail } from './helpers'

const TAGS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa']

test('vollständiger Lebenszyklus: teilen, zuweisen, entfernen (US-060 bis US-062)', async ({ page, browser }) => {
  await registerViaUi(page, uniqueEmail(), PASSWORD, 'Eigentümerin')
  await createProjectViaApi(page, 'Geteiltes Projekt')
  await page.goto('/projekte')
  await page.getByRole('link', { name: 'Geteiltes Projekt' }).click()
  const projectUrl = page.url()

  const memberContext = await browser.newContext()
  const memberPage = await memberContext.newPage()
  const memberEmail = await registerViaUi(memberPage, uniqueEmail(), PASSWORD, 'Herr Mitglied')

  // Vor der Aufnahme kein Zugriff.
  await memberPage.goto(projectUrl)
  await expect(memberPage.getByRole('heading', { level: 1, name: 'Projekt nicht gefunden' })).toBeVisible()

  // Eigentümerin nimmt das Mitglied über die Oberfläche auf.
  await page.goto(projectUrl)
  await page.getByLabel('Mitglied per E-Mail aufnehmen').fill(memberEmail)
  await page.getByRole('button', { name: 'Aufnehmen' }).click()
  await expect(page.getByText('„Herr Mitglied“ wurde aufgenommen.')).toBeVisible()

  // Jetzt sieht das Mitglied das Projekt.
  await memberPage.goto(projectUrl)
  await expect(memberPage.getByRole('heading', { level: 1, name: 'Geteiltes Projekt' })).toBeVisible()

  // Eigentümerin legt eine Aufgabe an und weist sie dem Mitglied zu.
  await page.getByRole('link', { name: 'Meine Aufgaben' }).click()
  await page.getByRole('link', { name: 'Neue Aufgabe anlegen' }).click()
  await expect(page.getByRole('heading', { level: 1, name: 'Neue Aufgabe' })).toBeVisible()
  await page.getByLabel('Projekt').selectOption({ label: 'Geteiltes Projekt' })
  await page.getByLabel('Bearbeiter').selectOption({ label: 'Herr Mitglied' })
  await page.getByLabel('Titel').fill('Für das Mitglied')
  await page.getByRole('button', { name: 'Aufgabe anlegen' }).click()
  await expect(page.getByRole('heading', { level: 1, name: 'Für das Mitglied' })).toBeVisible()
  const taskUrl = page.url()

  // Die Eigentümerin sieht den Namen des Mitglieds; das Mitglied selbst sieht "Du".
  await expect(page.locator('dd').filter({ hasText: 'Herr Mitglied' })).toBeVisible()
  await memberPage.goto(taskUrl)
  await expect(memberPage.locator('dd').filter({ hasText: 'Du' })).toBeVisible()

  // Eigentümerin entfernt das Mitglied wieder; die Zuweisung verschwindet, die Aufgabe bleibt.
  await page.goto(projectUrl)
  await page.getByRole('button', { name: /^Entfernen/ }).click()
  await page.getByRole('button', { name: 'Endgültig entfernen' }).click()
  await expect(page.getByText('„Herr Mitglied“ wurde entfernt.')).toBeVisible()

  await page.goto(taskUrl)
  await expect(page.locator('dd').filter({ hasText: 'Keine Zuweisung' })).toBeVisible()

  await memberPage.goto(projectUrl)
  await expect(memberPage.getByRole('heading', { level: 1, name: 'Projekt nicht gefunden' })).toBeVisible()

  await memberContext.close()
})

test('Nicht-Eigentümer sieht Mitglieder, aber keine Verwaltung', async ({ page, browser }) => {
  const ownerPage = page
  await registerViaUi(ownerPage, uniqueEmail(), PASSWORD, 'Eigentümerin B')
  const projectId = await createProjectViaApi(ownerPage, 'Projekt B')

  const memberContext = await browser.newContext()
  const memberPage = await memberContext.newPage()
  const memberEmail = await registerViaUi(memberPage, uniqueEmail(), PASSWORD, 'Mitglied B')
  await addMemberViaApi(ownerPage, projectId, memberEmail)

  await memberPage.goto(`/projekte/${projectId}`)
  await expect(memberPage.getByRole('heading', { level: 1, name: 'Projekt B' })).toBeVisible()
  await expect(memberPage.getByText('Mitglied B', { exact: true })).toBeVisible()
  await expect(memberPage.getByRole('button', { name: /Entfernen/ })).toHaveCount(0)
  await expect(memberPage.getByLabel('Mitglied per E-Mail aufnehmen')).toHaveCount(0)

  await memberContext.close()
})

test('axe: Mitgliederverwaltung leer, gefüllt, mit Fehler und geöffnetem Entfernen-Dialog', async ({ page, browser }) => {
  async function scan() {
    const results = await new AxeBuilder({ page }).withTags(TAGS).analyze()
    expect(results.violations).toEqual([])
  }

  await registerViaUi(page)
  const projectId = await createProjectViaApi(page, 'Für axe-Scan')
  await page.goto(`/projekte/${projectId}`)
  await scan()

  await page.getByRole('button', { name: 'Aufnehmen' }).click()
  await expect(page.getByRole('alert')).toBeVisible()
  await scan()

  await page.getByLabel('Mitglied per E-Mail aufnehmen').fill('nie-registriert@example.test')
  await page.getByRole('button', { name: 'Aufnehmen' }).click()
  await expect(page.getByRole('alert')).toBeVisible()
  await scan()

  // Registrierung des künftigen Mitglieds in einem eigenen Browser-Kontext, damit die eigene Sitzung erhalten bleibt.
  const otherContext = await browser.newContext()
  const memberEmail = await registerViaUi(await otherContext.newPage(), uniqueEmail(), PASSWORD, 'Für Scan')
  await otherContext.close()

  await page.getByLabel('Mitglied per E-Mail aufnehmen').fill(memberEmail)
  await page.getByRole('button', { name: 'Aufnehmen' }).click()
  await expect(page.getByText('„Für Scan“ wurde aufgenommen.')).toBeVisible()
  await scan()

  await page.getByRole('button', { name: /^Entfernen/ }).click()
  await expect(page.getByRole('dialog')).toBeVisible()
  await scan()
})
