import { useCallback, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { fetchCurrentUser } from '../../api/auth'
import { Dialog } from '../../components/Dialog'
import { useAuth } from './AuthContext'
import { useSessionTimeoutWarning } from './sessionTimeout'

/**
 * US-023: Warnt vor dem Ablauf der Sitzung wegen Inaktivität. "Angemeldet bleiben" löst eine echte Anfrage aus,
 * damit auch der Server die Inaktivität zurücksetzt (nicht nur der lokale Zähler). Reagiert niemand rechtzeitig,
 * übernimmt SessionExpiredDialog — ohne Navigation, damit ein ungesendeter Formularentwurf erhalten bleibt.
 */
export function SessionTimeoutWarning() {
  const { user, setUser, setNotice, markSessionExpired } = useAuth()
  const navigate = useNavigate()
  const stayButtonRef = useRef<HTMLButtonElement>(null)

  const onExpired = useCallback(() => markSessionExpired(), [markSessionExpired])

  const { warning } = useSessionTimeoutWarning(user?.sessionTimeoutSeconds, onExpired)

  if (!user || !warning) return null

  async function stay() {
    try {
      await fetchCurrentUser()
    } catch {
      // Bleibt die Anfrage erfolglos, bleibt die Sitzung wie sie ist; der nächste Prüfzyklus zeigt die Warnung erneut.
    }
  }

  /** Ausdrücklicher Wunsch des Benutzers (nicht die Sitzung, die abläuft): sofortige Navigation ist hier angemessen. */
  function logout() {
    setUser(null)
    setNotice('Du wurdest abgemeldet.')
    navigate('/anmelden', { replace: true })
  }

  return (
    <Dialog titleId="session-warning-title" title="Deine Sitzung läuft in Kürze ab" initialFocusRef={stayButtonRef} onClose={stay}>
      <p>Ohne weitere Aktion wirst du in Kürze wegen Inaktivität abgemeldet.</p>
      <div className="actions">
        <button type="button" ref={stayButtonRef} onClick={stay}>
          Angemeldet bleiben
        </button>
        <button type="button" className="secondary" onClick={logout}>
          Jetzt abmelden
        </button>
      </div>
    </Dialog>
  )
}
