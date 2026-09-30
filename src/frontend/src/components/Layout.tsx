import { useEffect } from 'react'
import { Outlet, useLocation } from 'react-router-dom'

/** Seitenrahmen: Sprunglink, Landmarken und Fokusführung bei Seitenwechsel (WCAG 2.4.1, 2.4.3). */
export default function Layout() {
  const { pathname } = useLocation()

  useEffect(() => {
    // Nach einem Routenwechsel den Fokus auf den Hauptinhalt setzen, damit Screenreader den Wechsel bemerken.
    document.getElementById('main')?.focus()
  }, [pathname])

  return (
    <>
      <a className="skip-link" href="#main">
        Zum Hauptinhalt springen
      </a>
      <header>
        <p className="brand">UltimateTodo</p>
      </header>
      <main id="main" tabIndex={-1}>
        <Outlet />
      </main>
      <footer>
        <p>Referenzprojekt – Entwicklungsstand M0</p>
      </footer>
    </>
  )
}
