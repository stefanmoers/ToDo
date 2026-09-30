import { useEffect, useRef } from 'react'
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../features/auth/AuthContext'
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
      <header>
        <p className="brand">
          <Link to="/">UltimateTodo</Link>
        </p>
        <nav aria-label="Hauptnavigation">
          {user ? (
            <ul>
              <li>
                <NavLink to="/" end>
                  Meine Aufgaben
                </NavLink>
              </li>
              <li>
                <NavLink to="/profil">Mein Profil</NavLink>
              </li>
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
      </header>
      <main id="main" tabIndex={-1}>
        <Outlet />
      </main>
      <footer>
        <p>Referenzprojekt – Entwicklungsstand M1b</p>
      </footer>
      <SessionTimeoutWarning />
    </>
  )
}
