import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import * as authApi from '../../api/auth'
import { setUnauthorizedHandler } from '../../api/client'

interface AuthState {
  /** undefined = wird noch geladen */
  user: authApi.User | null | undefined
  /** Hinweis für die Anmeldeseite (abgemeldet, Sitzung abgelaufen, Passwort geändert) */
  notice: string | undefined
  signIn: (email: string, password: string) => Promise<void>
  signUp: (input: authApi.RegisterInput) => Promise<void>
  signOut: () => Promise<void>
  setUser: (user: authApi.User | null) => void
  setNotice: (notice: string | undefined) => void
}

const AuthContext = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<authApi.User | null | undefined>(undefined)
  const [notice, setNotice] = useState<string>()

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
        if (current) setNotice('Deine Sitzung ist abgelaufen. Bitte melde dich erneut an.')
        return null
      })
    })
    return () => setUnauthorizedHandler(null)
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
      setNotice('Du wurdest abgemeldet.')
    }
  }, [])

  const value = useMemo(
    () => ({ user, notice, signIn, signUp, signOut, setUser, setNotice }),
    [user, notice, signIn, signUp, signOut],
  )
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth braucht einen AuthProvider')
  return ctx
}
