import { useEffect, useRef, useState } from 'react'
import { setActivityHandler } from '../../api/client'

/** Wie viele Sekunden vor Ablauf gewarnt wird (US-023). */
export const WARNING_SECONDS = 120
const CHECK_INTERVAL_MS = 15_000

/**
 * Beobachtet die Inaktivität der Sitzung. Jede erfolgreiche API-Antwort (siehe `api/client.ts`) zählt als
 * Aktivität und setzt die Uhr zurück. Ohne weitere Aktivität wird `onExpired` aufgerufen.
 */
export function useSessionTimeoutWarning(timeoutSeconds: number | undefined, onExpired: () => void) {
  const [warning, setWarning] = useState(false)
  // 0 als reiner Anfangswert (Date.now() darf nicht während des Renderns aufgerufen werden); der erste Effekt setzt ihn.
  const lastActivity = useRef(0)

  useEffect(() => {
    lastActivity.current = Date.now()
    setActivityHandler(() => {
      lastActivity.current = Date.now()
      setWarning(false)
    })
    return () => setActivityHandler(null)
  }, [])

  useEffect(() => {
    if (!timeoutSeconds) return
    const interval = setInterval(() => {
      const elapsedSeconds = (Date.now() - lastActivity.current) / 1000
      const remaining = timeoutSeconds - elapsedSeconds
      if (remaining <= 0) onExpired()
      else if (remaining <= WARNING_SECONDS) setWarning(true)
    }, CHECK_INTERVAL_MS)
    return () => clearInterval(interval)
  }, [timeoutSeconds, onExpired])

  return { warning }
}
