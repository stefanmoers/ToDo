import { useState, type FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { ApiError } from '../api/client'
import { ErrorSummary, Field, type FormErrors } from '../components/FormParts'
import { useAuth } from '../features/auth/AuthContext'
import { detectTimeZone, listTimeZones } from '../features/auth/timeZones'
import {
  PASSWORD_MAX,
  PASSWORD_MIN,
  validateDisplayName,
  validateEmail,
  validateNewPassword,
} from '../features/auth/validation'
import { usePageTitle } from '../hooks/usePageTitle'

const LABELS = { email: 'E-Mail-Adresse', displayName: 'Anzeigename', password: 'Passwort', timeZone: 'Zeitzone' }

export default function RegisterPage() {
  usePageTitle('Registrieren')
  const { user, signUp } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [password, setPassword] = useState('')
  const [timeZone, setTimeZone] = useState(detectTimeZone)
  const [errors, setErrors] = useState<FormErrors>({})
  const [general, setGeneral] = useState<string>()
  const [focusToken, setFocusToken] = useState(0)
  const [busy, setBusy] = useState(false)

  if (user) return <Navigate to="/" replace />

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    const found: FormErrors = {
      email: validateEmail(email),
      displayName: validateDisplayName(displayName),
      password: validateNewPassword(password),
    }
    setGeneral(undefined)
    if (Object.values(found).some(Boolean)) {
      setErrors(found)
      setFocusToken((n) => n + 1)
      return
    }
    setErrors({})
    setBusy(true)
    try {
      await signUp({ email, displayName, password, timeZone })
      navigate('/', { replace: true })
    } catch (err) {
      if (err instanceof ApiError && err.fieldErrors.length > 0) {
        const mapped: FormErrors = {}
        for (const f of err.fieldErrors) mapped[f.field] = f.message
        setErrors(mapped)
      } else {
        setGeneral(err instanceof ApiError ? err.message : 'Es ist ein unerwarteter Fehler aufgetreten.')
      }
      setFocusToken((n) => n + 1)
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <h1>Registrieren</h1>
      <p>Alle Angaben sind Pflicht.</p>
      <ErrorSummary errors={errors} labels={LABELS} general={general} focusToken={focusToken} />
      <form onSubmit={onSubmit} noValidate>
        <Field id="email" label="E-Mail-Adresse" hint="Mit dieser Adresse meldest du dich an." error={errors.email}>
          {(p) => (
            <input {...p} type="email" name="email" autoComplete="username" value={email}
              onChange={(e) => setEmail(e.target.value)} />
          )}
        </Field>
        <Field id="displayName" label="Anzeigename" error={errors.displayName}>
          {(p) => (
            <input {...p} type="text" name="displayName" autoComplete="name" value={displayName}
              onChange={(e) => setDisplayName(e.target.value)} />
          )}
        </Field>
        <Field id="password" label="Passwort"
          hint={`${PASSWORD_MIN} bis ${PASSWORD_MAX} Zeichen. Leerzeichen sind erlaubt; ein Passwortmanager wird unterstützt.`}
          error={errors.password}>
          {(p) => (
            <input {...p} type="password" name="password" autoComplete="new-password" value={password}
              onChange={(e) => setPassword(e.target.value)} />
          )}
        </Field>
        <Field id="timeZone" label="Zeitzone" hint="Fälligkeiten und „Heute“ richten sich nach dieser Zeitzone." error={errors.timeZone}>
          {(p) => (
            <select {...p} name="timeZone" value={timeZone} onChange={(e) => setTimeZone(e.target.value)}>
              {listTimeZones(timeZone).map((z) => (
                <option key={z} value={z}>
                  {z}
                </option>
              ))}
            </select>
          )}
        </Field>
        <button type="submit" disabled={busy}>
          {busy ? 'Konto wird angelegt …' : 'Konto anlegen'}
        </button>
      </form>
      <p>
        Bereits registriert? <Link to="/anmelden">Zur Anmeldung</Link>
      </p>
    </>
  )
}
