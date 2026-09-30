import { useState, type FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { ApiError } from '../api/client'
import { ErrorSummary, Field, type FormErrors } from '../components/FormParts'
import { useAuth } from '../features/auth/AuthContext'
import { validateEmail, validateRequired } from '../features/auth/validation'
import { usePageTitle } from '../hooks/usePageTitle'

const LABELS = { email: 'E-Mail-Adresse', password: 'Passwort' }

export default function LoginPage() {
  usePageTitle('Anmelden')
  const { user, signIn, notice } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [errors, setErrors] = useState<FormErrors>({})
  const [general, setGeneral] = useState<string>()
  const [focusToken, setFocusToken] = useState(0)
  const [busy, setBusy] = useState(false)

  if (user) return <Navigate to="/" replace />

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    const found: FormErrors = {
      email: validateEmail(email),
      password: validateRequired(password, 'Bitte gib dein Passwort ein.'),
    }
    setGeneral(undefined)
    if (found.email || found.password) {
      setErrors(found)
      setFocusToken((n) => n + 1)
      return
    }
    setErrors({})
    setBusy(true)
    try {
      await signIn(email, password)
      navigate('/', { replace: true })
    } catch (err) {
      // Der Entwurf (E-Mail) bleibt erhalten; das Passwort wird bewusst geleert.
      setPassword('')
      setGeneral(err instanceof ApiError ? err.message : 'Es ist ein unerwarteter Fehler aufgetreten.')
      setFocusToken((n) => n + 1)
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <h1>Anmelden</h1>
      {notice && (
        <p role="status" className="notice">
          {notice}
        </p>
      )}
      <ErrorSummary errors={errors} labels={LABELS} general={general} focusToken={focusToken} />
      <form onSubmit={onSubmit} noValidate>
        <Field id="email" label="E-Mail-Adresse" error={errors.email}>
          {(p) => (
            <input {...p} type="email" name="email" autoComplete="username" value={email}
              onChange={(e) => setEmail(e.target.value)} />
          )}
        </Field>
        <Field id="password" label="Passwort" error={errors.password}>
          {(p) => (
            <input {...p} type="password" name="password" autoComplete="current-password" value={password}
              onChange={(e) => setPassword(e.target.value)} />
          )}
        </Field>
        <button type="submit" disabled={busy}>
          {busy ? 'Anmeldung läuft …' : 'Anmelden'}
        </button>
      </form>
      <p>
        <Link to="/passwort-vergessen">Passwort vergessen?</Link>
      </p>
      <p>
        Noch kein Konto? <Link to="/registrieren">Jetzt registrieren</Link>
      </p>
    </>
  )
}
