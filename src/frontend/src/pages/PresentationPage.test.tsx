import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from '../App'
import { slides } from '../features/presentation/slides'
import { mockApi } from '../testUtils'

afterEach(() => vi.restoreAllMocks())

function renderAt(path: string) {
  // Ohne Routen: jeder API-Aufruf würde mit "Unerwarteter Aufruf" scheitern. Die Präsentation braucht keine API.
  const api = mockApi({})
  const view = render(
    <MemoryRouter initialEntries={[path]}>
      <App />
    </MemoryRouter>,
  )
  return { ...view, api }
}

const current = () => screen.getByRole('group', { name: /^Folie \d+ von \d+: / })

describe('Präsentation (US-182)', () => {
  it('enthält 18 Folien mit eindeutiger Kennung, Titel und Sprechernotizen', () => {
    expect(slides).toHaveLength(18)
    expect(new Set(slides.map((s) => s.id)).size).toBe(slides.length)
    for (const s of slides) {
      expect(s.title.length).toBeGreaterThan(0)
      expect(s.notes.length).toBeGreaterThan(0)
    }
  })

  it('zeigt unter /präsentation die erste Folie ohne Anmeldung und ohne API-Aufruf', () => {
    const { api } = renderAt('/präsentation')
    expect(current()).toHaveAccessibleName('Folie 1 von 18: UltimateTodo')
    expect(screen.getByRole('heading', { level: 2, name: 'UltimateTodo' })).toBeInTheDocument()
    expect(screen.getByText('Folie 1 von 18')).toBeInTheDocument()
    expect(document.title).toBe('UltimateTodo (Folie 1 von 18) – UltimateTodo')
    expect(api.spy).not.toHaveBeenCalled()
  })

  it.each(['/pr%C3%A4sentation', '/praesentation', '/präsentation/1'])('ist auch unter %s erreichbar', (path) => {
    renderAt(path)
    expect(current()).toHaveAccessibleName(/^Folie 1 von 18/)
  })

  it('wechselt mit den Pfeil-Schaltflächen vor und zurück', async () => {
    renderAt('/präsentation')
    const user = userEvent.setup()
    const prev = screen.getByRole('button', { name: 'Vorherige Folie' })
    const next = screen.getByRole('button', { name: 'Nächste Folie' })
    expect(prev).toHaveAttribute('aria-disabled', 'true')
    await user.click(next)
    expect(current()).toHaveAccessibleName('Folie 2 von 18: Was ihr in den nächsten Minuten seht')
    expect(prev).toHaveAttribute('aria-disabled', 'false')
    await user.click(prev)
    expect(current()).toHaveAccessibleName('Folie 1 von 18: UltimateTodo')
    // Am Anfang bewirkt "zurück" nichts.
    await user.click(prev)
    expect(current()).toHaveAccessibleName('Folie 1 von 18: UltimateTodo')
  })

  it('bedient sich mit Pfeiltasten, Pos1 und Ende', async () => {
    renderAt('/präsentation')
    const user = userEvent.setup()
    await user.keyboard('{ArrowRight}{ArrowRight}')
    expect(current()).toHaveAccessibleName(/^Folie 3 von 18/)
    await user.keyboard('{ArrowLeft}')
    expect(current()).toHaveAccessibleName(/^Folie 2 von 18/)
    await user.keyboard('{End}')
    expect(current()).toHaveAccessibleName(/^Folie 18 von 18: Fragen und Diskussion/)
    expect(screen.getByRole('button', { name: 'Nächste Folie' })).toHaveAttribute('aria-disabled', 'true')
    await user.keyboard('{ArrowRight}')
    expect(current()).toHaveAccessibleName(/^Folie 18 von 18/)
    await user.keyboard('{Home}')
    expect(current()).toHaveAccessibleName(/^Folie 1 von 18/)
  })

  it('öffnet eine Folie direkt über die Adresse und begrenzt ungültige Nummern', () => {
    const first = renderAt('/präsentation/5')
    expect(current()).toHaveAccessibleName(/^Folie 5 von 18: Drei Bausteine, ein Zugangspunkt/)
    first.unmount()
    const high = renderAt('/präsentation/99')
    expect(current()).toHaveAccessibleName(/^Folie 18 von 18/)
    high.unmount()
    renderAt('/präsentation/abc')
    expect(current()).toHaveAccessibleName(/^Folie 1 von 18/)
  })

  it('blendet Sprechernotizen mit Schaltfläche und Taste N ein und aus', async () => {
    renderAt('/präsentation')
    const user = userEvent.setup()
    const toggle = screen.getByRole('button', { name: 'Sprechernotizen' })
    expect(toggle).toHaveAttribute('aria-expanded', 'false')
    expect(screen.queryByText(/Begrüßung\. Ziel der nächsten/)).not.toBeVisible()
    await user.click(toggle)
    expect(toggle).toHaveAttribute('aria-expanded', 'true')
    expect(screen.getByText(/Begrüßung\. Ziel der nächsten/)).toBeVisible()
    await user.keyboard('n')
    expect(toggle).toHaveAttribute('aria-expanded', 'false')
  })

  it('ignoriert Pfeiltasten mit gedrückter Steuerungstaste (Browser-Kurzbefehle)', async () => {
    renderAt('/präsentation')
    const user = userEvent.setup()
    await user.keyboard('{Control>}{ArrowRight}{/Control}')
    expect(current()).toHaveAccessibleName(/^Folie 1 von 18/)
  })

  it('kündigt den Folienwechsel per Statusmeldung an und führt zurück zur Anwendung', async () => {
    renderAt('/präsentation')
    const user = userEvent.setup()
    await user.keyboard('{ArrowRight}')
    expect(screen.getByRole('status')).toHaveTextContent('Folie 2 von 18: Was ihr in den nächsten Minuten seht')
    expect(screen.getByRole('link', { name: 'Zur Anwendung' })).toHaveAttribute('href', '/')
  })
})
