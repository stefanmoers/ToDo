import AxeBuilder from '@axe-core/playwright'
import { expect, test } from '@playwright/test'
import {
  addMemberViaApi,
  createProjectViaApi,
  createProjectViaUi,
  createTagViaApi,
  createTaskViaApi,
  createTaskViaUi,
  PASSWORD,
  registerViaUi,
  uniqueEmail,
} from './helpers'

const TAGS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa']

test('vollständiger Lebenszyklus: anlegen, umbenennen, zuordnen, filtern, löschen', async ({ page }) => {
  await registerViaUi(page)
  await page.getByRole('link', { name: 'Tags' }).click()
  await expect(page.getByText('Noch keine persönlichen Tags.')).toBeVisible()

  await page.getByLabel('Name').fill('Wichtig')
  await page.getByRole('button', { name: 'Tag anlegen' }).click()
  await expect(page.getByText('Der Tag „Wichtig“ wurde angelegt.')).toBeVisible()

  await page.getByRole('button', { name: /Umbenennen/ }).click()
  const nameField = page.getByLabel(/Name für/)
  await nameField.fill('Sehr wichtig')
  await page.getByRole('button', { name: 'Speichern' }).click()
  await expect(page.getByText('Der Tag wurde in „Sehr wichtig“ umbenannt.')).toBeVisible()

  await page.getByRole('link', { name: 'Meine Aufgaben' }).click()
  await createTaskViaUi(page, 'Aufgabe mit Tag')
  await page.getByLabel('Sehr wichtig').check()
  await page.getByRole('button', { name: 'Tags speichern' }).click()
  await expect(page.getByText('Die Tag-Zuordnung wurde gespeichert.')).toBeVisible()
  await expect(page.locator('dd').filter({ hasText: 'Sehr wichtig' })).toBeVisible()

  await page.getByRole('link', { name: 'Meine Aufgaben' }).click()
  await expect(page.getByText(/Tags: Sehr wichtig/)).toBeVisible()
  await page.getByLabel('Nach Tag filtern').selectOption({ label: 'Sehr wichtig' })
  await expect(page.getByRole('link', { name: 'Aufgabe mit Tag' })).toBeVisible()

  await page.getByRole('link', { name: 'Tags' }).click()
  await page.getByRole('button', { name: /^Löschen/ }).click()
  await page.getByRole('button', { name: 'Endgültig löschen' }).click()
  await expect(page.getByText('Der Tag wurde gelöscht.')).toBeVisible()
  await expect(page.getByText('Noch keine persönlichen Tags.')).toBeVisible()

  // Die Aufgabe bleibt erhalten, verliert aber die Markierung (US-043).
  await page.getByRole('link', { name: 'Meine Aufgaben' }).click()
  await expect(page.getByText(/Tags:/)).toHaveCount(0)
})

test('ein projektbezogener Tag erscheint in der Tag-Verwaltung unter seinem Projekt', async ({ page }) => {
  await registerViaUi(page)
  await createProjectViaUi(page, 'Umzug')
  const projectId = page.url().split('/').pop()!
  await createTagViaApi(page, 'Kisten', projectId)

  await page.getByRole('link', { name: 'Tags' }).click()
  await expect(page.getByRole('heading', { level: 2, name: 'Tags in „Umzug“' })).toBeVisible()
  await expect(page.getByText('Kisten', { exact: true })).toBeVisible()
})

test('zwei Konten sehen nur ihre eigenen Tags', async ({ page, browser }) => {
  await registerViaUi(page, uniqueEmail(), PASSWORD, 'Person A')
  await createTagViaApi(page, 'Geheimtag A')

  const other = await browser.newContext()
  const second = await other.newPage()
  await registerViaUi(second, uniqueEmail(), PASSWORD, 'Person B')
  await second.getByRole('link', { name: 'Tags' }).click()
  await expect(second.getByText('Geheimtag A')).toHaveCount(0)
  await expect(second.getByText('Noch keine persönlichen Tags.')).toBeVisible()
  await other.close()
})

test('ein fremder Tag wird beim Zuordnen zu einer Aufgabe serverseitig abgewiesen', async ({ page, browser }) => {
  await registerViaUi(page, uniqueEmail(), PASSWORD, 'Person A')
  const foreignTagId = await createTagViaApi(page, 'Tag von A')

  const other = await browser.newContext()
  const second = await other.newPage()
  await registerViaUi(second, uniqueEmail(), PASSWORD, 'Person B')
  const csrf = await second.request.get('/api/v1/auth/csrf')
  const { headerName, token } = await csrf.json()
  const ownTaskId = await createTaskViaApi(second, { title: 'Aufgabe von B' })
  const response = await second.request.put(`/api/v1/tasks/${ownTaskId}/tags`, {
    headers: { [headerName]: token },
    data: { tagIds: [foreignTagId] },
  })
  expect(response.status()).toBe(400)
  await other.close()
})

test('axe: Tag-Verwaltung leer, mit Fehler, gefüllt und mit geöffnetem Löschdialog', async ({ page }) => {
  async function scan() {
    const results = await new AxeBuilder({ page }).withTags(TAGS).analyze()
    expect(results.violations).toEqual([])
  }

  await registerViaUi(page)
  await page.getByRole('link', { name: 'Tags' }).click()
  await scan()

  await page.getByRole('button', { name: 'Tag anlegen' }).click()
  await expect(page.getByRole('alert')).toBeVisible()
  await scan()

  await page.getByLabel('Name').fill('Für axe-Scan')
  await page.getByRole('button', { name: 'Tag anlegen' }).click()
  await expect(page.getByText('Für axe-Scan', { exact: true })).toBeVisible()
  await scan()

  await page.getByRole('button', { name: /Umbenennen/ }).click()
  await scan()
  await page.getByRole('button', { name: 'Abbrechen' }).click()

  await page.getByRole('button', { name: /^Löschen/ }).click()
  await expect(page.getByRole('dialog')).toBeVisible()
  await scan()
})

test('axe: Tag-Editor auf der Aufgabendetailseite', async ({ page }) => {
  const scan = async () => {
    const results = await new AxeBuilder({ page }).withTags(TAGS).analyze()
    expect(results.violations).toEqual([])
  }

  await registerViaUi(page)
  await createTagViaApi(page, 'Für die Aufgabe')
  await createTaskViaUi(page, 'Aufgabe mit Tag-Editor')
  await expect(page.getByRole('heading', { level: 2, name: 'Tags' })).toBeVisible()
  await scan()
})

test('gleichnamige Tags aus verschiedenen Projekten sind unterscheidbar (US-181)', async ({ page, browser }) => {
  await registerViaUi(page, uniqueEmail(), PASSWORD, 'Anna Eigentümerin')
  const projectA = await createProjectViaApi(page, 'Operation Kaffee')
  await createTagViaApi(page, 'Backen', projectA)
  const taskId = await createTaskViaApi(page, { title: 'Kuchen backen', projectId: projectA })

  const memberContext = await browser.newContext()
  const member = await memberContext.newPage()
  const memberEmail = await registerViaUi(member, uniqueEmail(), PASSWORD, 'Ben Mitglied')
  const projectB = await createProjectViaApi(member, 'Operation Kaffee')
  await createTagViaApi(member, 'Backen', projectB)
  await addMemberViaApi(page, projectA, memberEmail)

  // Tag-Auswahl an der Aufgabe: zwei „Backen“, unterscheidbar über ihren zugänglichen Namen.
  await member.goto(`/aufgaben/${taskId}`)
  await expect(member.getByRole('heading', { level: 1, name: 'Kuchen backen' })).toBeVisible()
  const foreign = member.getByRole('checkbox', { name: 'Backen · Projekt „Operation Kaffee“ (Anna Eigentümerin)' })
  const own = member.getByRole('checkbox', { name: 'Backen · Projekt „Operation Kaffee“ (du)' })
  await expect(foreign).toBeVisible()
  await expect(own).toBeVisible()
  const results = await new AxeBuilder({ page: member }).withTags(TAGS).analyze()
  expect(results.violations).toEqual([])

  // Zuordnen des fremden Tags: nur dieses ist danach gewählt.
  await foreign.check()
  await member.getByRole('button', { name: 'Tags speichern' }).click()
  await expect(member.getByText('Die Tag-Zuordnung wurde gespeichert.')).toBeVisible()
  await member.reload()
  await expect(member.getByRole('checkbox', { name: 'Backen · Projekt „Operation Kaffee“ (Anna Eigentümerin)' })).toBeChecked()
  await expect(member.getByRole('checkbox', { name: 'Backen · Projekt „Operation Kaffee“ (du)' })).not.toBeChecked()

  // Filter der Aufgabenliste: beide Optionen sind beschriftet.
  await member.goto('/')
  const filter = member.getByLabel('Nach Tag filtern')
  await expect(filter.getByRole('option', { name: 'Backen · Projekt „Operation Kaffee“ (Anna Eigentümerin)' })).toHaveCount(1)
  await expect(filter.getByRole('option', { name: 'Backen · Projekt „Operation Kaffee“ (du)' })).toHaveCount(1)

  // Tag-Verwaltung: die fremde Projektüberschrift nennt den Eigentümer.
  await member.getByRole('link', { name: 'Tags' }).click()
  await expect(member.getByRole('heading', { level: 2, name: 'Tags in „Operation Kaffee“ (von Anna Eigentümerin)' })).toBeVisible()

  await memberContext.close()
})
