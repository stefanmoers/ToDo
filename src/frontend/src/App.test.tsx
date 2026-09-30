import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from './App'

afterEach(() => vi.restoreAllMocks())

function renderAt(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <App />
    </MemoryRouter>,
  )
}

describe('App', () => {
  it('zeigt Überschrift, Sprunglink und meldet den Systemstatus', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(JSON.stringify({ status: 'UP' })))
    renderAt('/')
    expect(screen.getByRole('heading', { level: 1, name: 'UltimateTodo' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Zum Hauptinhalt springen' })).toHaveAttribute('href', '#main')
    expect(await screen.findByText('Das System ist bereit.')).toBeInTheDocument()
  })

  it('meldet einen nicht erreichbaren Server verständlich', async () => {
    vi.spyOn(globalThis, 'fetch').mockRejectedValue(new TypeError('Netzwerkfehler'))
    renderAt('/')
    expect(await screen.findByText('Der Systemstatus konnte nicht abgefragt werden.')).toBeInTheDocument()
  })

  it('zeigt für unbekannte Adressen eine Nicht-gefunden-Seite', () => {
    renderAt('/gibt-es-nicht')
    expect(screen.getByRole('heading', { level: 1, name: 'Seite nicht gefunden' })).toBeInTheDocument()
  })
})
