import { Link } from 'react-router-dom'
import { usePageTitle } from '../hooks/usePageTitle'

/** US-013: Passwort-Wiederherstellung ist im Demo-Projekt bewusst nicht umgesetzt (Auftraggeber-Entscheidung). */
export default function PasswordResetUnavailablePage() {
  usePageTitle('Passwort vergessen')
  return (
    <div className="content-card auth-card">
      <h1>Passwort vergessen</h1>
      <p>Die Passwort-Wiederherstellung ist in diesem Demo-Projekt nicht verfügbar.</p>
      <p>
        <Link to="/anmelden">Zurück zur Anmeldung</Link>
      </p>
    </div>
  )
}
