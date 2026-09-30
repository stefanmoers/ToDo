import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import * as authApi from '../../api/auth'
import { setUnauthorizedHandler } from '../../api/client'

interface AuthState {
  /** undefined = wird noch geladen */
  user: authApi.User | null | undefined
  /** Hinweis für die Anmeldeseite (abgemeldet, Sitzung abgelaufen, Passwort geändert) */
  notice: string | undefined
  /**
   * US-023: true, sobald die Sitzung erkennbar abgelaufen ist (401 bei einer geschützten Anfrage oder
   * ausgelaufene Timeout-Warnung). Bewusst getrennt von `user`: `user` bleibt gesetzt, damit die aktuelle
   * Seite (und ein ungesendeter Formularentwurf) nicht durch eine Navigation zur Anmeldeseite verloren geht.
   */
  sessionExpired: boolean
  signIn: (email: string, password: string) => Promise<void>
  signUp: (input: authApi.RegisterInput) => Promise<void>
  signOut: () => Promise<void>
  setUser: (user: authApi.User | null) => void
  setNotice: (notice: string | undefined) => void
  markSessionExpired: () => void
  /** Meldet innerhalb der laufenden Seite erneut an, ohne die Seite zu verlassen. */
  resumeSession: (email: string, password: string) => Promise<void>
  /** Bricht die abgelaufene Sitzung endgültig ab (Nutzerwunsch); erst danach erfolgt die Navigation zur Anmeldung. */
  abandonSession: () => void
}

const AuthContext = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<authApi.User | null | undefined>(undefined)
  const [notice, setNotice] = useState<string>()
  const [sessionExpired, setSessionExpired] = useState(false)

  useEffect(() => {
    let active = true
    authApi
      .fetchCurrentUser()
      .then((u) => active && setUser(u))
      .catch(() => active && setUser(null))
    return () => {
      active = false
    }
  }, [])

  useEffect(() => {
    setUnauthorizedHandler(() => {
      setUser((current) => {
        // Bewusst nicht auf null setzen: das würde die geschützte Seite verlassen und einen
        // ungesendeten Formularentwurf zerstören. Stattdessen zeigt SessionExpiredDialog eine
        // Anmeldung innerhalb der laufenden Seite (US-023).
        if (current) setSessionExpired(true)
        return current
      })
    })
    return () => setUnauthorizedHandler(null)
  }, [])

  const markSessionExpired = useCallback(() => setSessionExpired(true), [])

  const resumeSession = useCallback(async (email: string, password: string) => {
    const u = await authApi.login(email, password)
    setUser(u)
    setSessionExpired(false)
  }, [])

  const abandonSession = useCallback(() => {
    setSessionExpired(false)
    setUser(null)
    setNotice('Du wurdest abgemeldet.')
  }, [])

  const signIn = useCallback(async (email: string, password: string) => {
    const u = await authApi.login(email, password)
    setNotice(undefined)
    setUser(u)
  }, [])

  const signUp = useCallback(
    async (input: authApi.RegisterInput) => {
      await authApi.register(input)
      await signIn(input.email, input.password)
    },
    [signIn],
  )

  const signOut = useCallback(async () => {
    try {
      await authApi.logout()
    } finally {
      // Nach Logout dürfen keine Daten der bisherigen Sitzung im Browserzustand bleiben.
      setUser(null)
      setSessionExpired(false)
      setNotice('Du wurdest abgemeldet.')
    }
  }, [])

  const value = useMemo(
    () => ({
      user,
      notice,
      sessionExpired,
      signIn,
      signUp,
      signOut,
      setUser,
      setNotice,
      markSessionExpired,
      resumeSession,
      abandonSession,
    }),
    [user, notice, sessionExpired, signIn, signUp, signOut, markSessionExpired, resumeSession, abandonSession],
  )
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth braucht einen AuthProvider')
  return ctx
}
