import { useEffect, useRef } from 'react'
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../features/auth/AuthContext'
import { SessionExpiredDialog } from '../features/auth/SessionExpiredDialog'
import { SessionTimeoutWarning } from '../features/auth/SessionTimeoutWarning'

/** Seitenrahmen: Sprunglink, Landmarken und Fokusführung bei Seitenwechsel (WCAG 2.4.1, 2.4.3). */
export default function Layout() {
  const { pathname } = useLocation()
  const { user, signOut } = useAuth()
  const navigate = useNavigate()
  const first = useRef(true)

  useEffect(() => {
    // Nach einem Routenwechsel den Fokus auf den Hauptinhalt setzen, damit Screenreader den Wechsel bemerken.
    if (first.current) {
      first.current = false
      return
    }
    document.getElementById('main')?.focus()
  }, [pathname])

  async function onLogout() {
    await signOut()
    navigate('/anmelden', { replace: true })
  }

  return (
    <>
      <a className="skip-link" href="#main">
        Zum Hauptinhalt springen
      </a>
      <header className="site-header">
        <div className="site-header-inner">
          <p className="brand">
            <Link to="/"><span className="brand-mark" aria-hidden="true">✓</span>UltimateTodo</Link>
          </p>
          <nav className="main-nav" aria-label="Hauptnavigation">
          {user ? (
            <ul>
              <li>
                <NavLink to="/uebersicht">Übersicht</NavLink>
              </li>
              <li>
                <NavLink to="/" end>
                  Meine Aufgaben
                </NavLink>
              </li>
              <li>
                <NavLink to="/projekte">Projekte</NavLink>
              </li>
              <li>
                <NavLink to="/tags">Tags</NavLink>
              </li>
              <li>
                <NavLink to="/profil">Mein Profil</NavLink>
              </li>
              {user.role === 'ADMIN' && (
                <li>
                  <NavLink to="/administration">Administration</NavLink>
                </li>
              )}
              <li>
                <button type="button" className="link-button" onClick={onLogout}>
                  Abmelden ({user.displayName})
                </button>
              </li>
            </ul>
          ) : user === null ? (
            <ul>
              <li>
                <NavLink to="/anmelden">Anmelden</NavLink>
              </li>
              <li>
                <NavLink to="/registrieren">Registrieren</NavLink>
              </li>
            </ul>
          ) : null}
          </nav>
        </div>
      </header>
      <main id="main" className="site-main" tabIndex={-1}>
        <Outlet />
      </main>
      <footer className="site-footer">
        <p>Referenzprojekt – Entwicklungsstand M2</p>
      </footer>
      <SessionTimeoutWarning />
      <SessionExpiredDialog />
    </>
  )
}
