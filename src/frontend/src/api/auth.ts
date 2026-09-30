import { api, resetCsrfToken } from './client'

export interface User {
  id: string
  email: string
  displayName: string
  timeZone: string
  role: 'USER' | 'ADMIN'
  createdAt: string
  /** US-023: Inaktivitäts-Timeout der Sitzung in Sekunden; Grundlage der Sitzungs-Timeout-Warnung. */
  sessionTimeoutSeconds: number
}

export interface RegisterInput {
  email: string
  displayName: string
  password: string
  timeZone: string
}

/** Liefert das angemeldete Konto oder null, wenn keine gültige Sitzung besteht. */
export async function fetchCurrentUser(): Promise<User | null> {
  try {
    return await api<User>('GET', '/api/v1/auth/me', undefined, { expectUnauthorized: true })
  } catch (e) {
    if (e instanceof Error && 'status' in e && e.status === 401) return null
    throw e
  }
}

export const register = (input: RegisterInput) => api<User>('POST', '/api/v1/auth/register', input)

export async function login(email: string, password: string): Promise<User> {
  const user = await api<User>('POST', '/api/v1/auth/login', { email, password }, { expectUnauthorized: true })
  resetCsrfToken() // Sitzung und Token wechseln bei der Anmeldung
  return user
}

export async function logout(): Promise<void> {
  try {
    await api('POST', '/api/v1/auth/logout', undefined, { expectUnauthorized: true })
  } finally {
    resetCsrfToken()
  }
}

export const updateProfile = (displayName: string, timeZone: string) =>
  api<User>('PUT', '/api/v1/users/me', { displayName, timeZone })

/** Nach Erfolg ist die Sitzung serverseitig beendet. */
export async function changePassword(currentPassword: string, newPassword: string): Promise<void> {
  await api('POST', '/api/v1/users/me/password', { currentPassword, newPassword })
  resetCsrfToken()
}
