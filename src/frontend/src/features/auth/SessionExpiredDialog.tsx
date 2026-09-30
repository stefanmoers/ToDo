import { useRef, useState, type FormEvent } from 'react'
import { ApiError } from '../../api/client'
import { Dialog } from '../../components/Dialog'
import { useAuth } from './AuthContext'

/**
 * US-023: Erscheint, sobald die Sitzung erkennbar abgelaufen ist (401 oder ausgelaufene Warnung).
 * Anders als ein Sprung zur Anmeldeseite bleibt die aktuelle Seite gemountet — ein ungesendeter
 * Formularentwurf auf ihr geht dadurch nicht verloren.
 */
export function SessionExpiredDialog() {
  const { sessionExpired, resumeSession, abandonSession } = useAuth()
  const emailRef = useRef<HTMLInputElement>(null)
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string>()
  const [busy, setBusy] = useState(false)

  if (!sessionExpired) return null

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError(undefined)
    try {
      await resumeSession(email, password)
    } catch (err) {
      setPassword('')
      setError(err instanceof ApiError ? err.message : 'Es ist ein unerwarteter Fehler aufgetreten.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <Dialog
      titleId="session-expired-title"
      title="Sitzung abgelaufen"
      describedById="session-expired-description"
      initialFocusRef={emailRef}
      onClose={() => {
        /* Anmeldung ist hier verpflichtend; Escape schließt den Dialog nicht. */
      }}
    >
      <p id="session-expired-description">
        Bitte melde dich erneut an, um fortzufahren. Deine bisherigen Eingaben auf dieser Seite bleiben erhalten.
      </p>
      {error && (
        <p role="alert" className="field-error">
          {error}
        </p>
      )}
      <form onSubmit={onSubmit} noValidate>
        <div className="field">
          <label htmlFor="session-expired-email">E-Mail-Adresse</label>
          <input
            ref={emailRef}
            id="session-expired-email"
            type="email"
            autoComplete="username"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
        </div>
        <div className="field">
          <label htmlFor="session-expired-password">Passwort</label>
          <input
            id="session-expired-password"
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </div>
        <div className="actions">
          <button type="submit" disabled={busy}>
            {busy ? 'Anmeldung läuft …' : 'Anmelden'}
          </button>
          <button type="button" className="secondary" onClick={abandonSession}>
            Abmelden
          </button>
        </div>
      </form>
    </Dialog>
  )
}
