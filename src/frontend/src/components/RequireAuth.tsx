import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../features/auth/AuthContext'

/** Schützt Seiten im Browser (Komfort). Die eigentliche Zugriffsprüfung passiert im Backend. */
export default function RequireAuth() {
  const { user } = useAuth()
  if (user === undefined) return <p role="status">Anmeldestatus wird geprüft …</p>
  if (user === null) return <Navigate to="/anmelden" replace />
  return <Outlet />
}
