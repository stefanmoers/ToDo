import { useState, type FormEvent } from 'react'
import { changePassword, updateProfile } from '../api/auth'
import { ApiError } from '../api/client'
import { ErrorSummary, Field, type FormErrors } from '../components/FormParts'
import { useAuth } from '../features/auth/AuthContext'
import { listTimeZones } from '../features/auth/timeZones'
import { PASSWORD_MAX, PASSWORD_MIN, validateDisplayName, validateNewPassword, validateRequired } from '../features/auth/validation'
import { usePageTitle } from '../hooks/usePageTitle'

function mapFieldErrors(err: unknown): { errors: FormErrors; general?: string } {
  if (err instanceof ApiError && err.fieldErrors.length > 0) {
    const errors: FormErrors = {}
    for (const f of err.fieldErrors) errors[f.field] = f.message
    return { errors }
  }
  return { errors: {}, general: err instanceof ApiError ? err.message : 'Es ist ein unerwarteter Fehler aufgetreten.' }
}

export default function ProfilePage() {
  usePageTitle('Mein Profil')
  const { user } = useAuth()
  if (!user) return null
  return (
    <>
      <h1>Mein Profil</h1>
      <ProfileForm />
      <PasswordForm />
    </>
  )
}

function ProfileForm() {
  const { user, setUser } = useAuth()
  const [displayName, setDisplayName] = useState(user!.displayName)
  const [timeZone, setTimeZone] = useState(user!.timeZone)
  const [errors, setErrors] = useState<FormErrors>({})
  const [general, setGeneral] = useState<string>()
  const [saved, setSaved] = useState(false)
  const [focusToken, setFocusToken] = useState(0)
  const [busy, setBusy] = useState(false)

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setSaved(false)
    setGeneral(undefined)
    const found: FormErrors = { displayName: validateDisplayName(displayName) }
    if (found.displayName) {
      setErrors(found)
      setFocusToken((n) => n + 1)
      return
    }
    setErrors({})
    setBusy(true)
    try {
      setUser(await updateProfile(displayName, timeZone))
      setSaved(true)
    } catch (err) {
      const m = mapFieldErrors(err)
      setErrors(m.errors)
      setGeneral(m.general)
      setFocusToken((n) => n + 1)
    } finally {
      setBusy(false)
    }
  }

  return (
    <section aria-labelledby="profile-heading">
      <h2 id="profile-heading">Profil</h2>
      <dl className="facts">
        <dt>E-Mail-Adresse (Login)</dt>
        <dd>{user!.email}</dd>
        <dt>Konto erstellt</dt>
        <dd>{new Date(user!.createdAt).toLocaleString('de-DE', { dateStyle: 'long', timeStyle: 'short' })}</dd>
      </dl>
      <p className="hint">Die E-Mail-Adresse kann in dieser Version nicht geändert werden.</p>
      <ErrorSummary errors={errors} labels={{ displayName: 'Anzeigename', timeZone: 'Zeitzone' }} general={general} focusToken={focusToken} />
      <form onSubmit={onSubmit} noValidate>
        <Field id="displayName" label="Anzeigename" error={errors.displayName}>
          {(p) => (
            <input {...p} type="text" name="displayName" autoComplete="name" value={displayName}
              onChange={(e) => setDisplayName(e.target.value)} />
          )}
        </Field>
        <Field id="timeZone" label="Zeitzone"
          hint="Ändert nur, wie „Heute“ berechnet wird. Bereits gespeicherte Fälligkeiten werden nicht verschoben."
          error={errors.timeZone}>
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
          Profil speichern
        </button>
        {saved && (
          <p role="status" className="success">
            Das Profil wurde gespeichert.
          </p>
        )}
      </form>
    </section>
  )
}

function PasswordForm() {
  const { setUser, setNotice } = useAuth()
  const [current, setCurrent] = useState('')
  const [next, setNext] = useState('')
  const [errors, setErrors] = useState<FormErrors>({})
  const [general, setGeneral] = useState<string>()
  const [focusToken, setFocusToken] = useState(0)
  const [busy, setBusy] = useState(false)

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setGeneral(undefined)
    const found: FormErrors = {
      currentPassword: validateRequired(current, 'Bitte gib dein bisheriges Passwort ein.'),
      newPassword: validateNewPassword(next),
    }
    if (found.currentPassword || found.newPassword) {
      setErrors(found)
      setFocusToken((n) => n + 1)
      return
    }
    setErrors({})
    setBusy(true)
    try {
      await changePassword(current, next)
      // Alle Sitzungen sind beendet; eine neue Anmeldung ist erforderlich.
      setNotice('Dein Passwort wurde geändert. Bitte melde dich mit dem neuen Passwort an.')
      setUser(null)
    } catch (err) {
      const m = mapFieldErrors(err)
      setErrors(m.errors)
      setGeneral(m.general)
      setCurrent('')
      setFocusToken((n) => n + 1)
    } finally {
      setBusy(false)
    }
  }

  return (
    <section aria-labelledby="password-heading">
      <h2 id="password-heading">Passwort ändern</h2>
      <p>Nach der Änderung wirst du überall abgemeldet und musst dich neu anmelden.</p>
      <ErrorSummary errors={errors} labels={{ currentPassword: 'Bisheriges Passwort', newPassword: 'Neues Passwort' }} general={general} focusToken={focusToken} />
      <form onSubmit={onSubmit} noValidate>
        <Field id="currentPassword" label="Bisheriges Passwort" error={errors.currentPassword}>
          {(p) => (
            <input {...p} type="password" name="currentPassword" autoComplete="current-password" value={current}
              onChange={(e) => setCurrent(e.target.value)} />
          )}
        </Field>
        <Field id="newPassword" label="Neues Passwort" hint={`${PASSWORD_MIN} bis ${PASSWORD_MAX} Zeichen.`} error={errors.newPassword}>
          {(p) => (
            <input {...p} type="password" name="newPassword" autoComplete="new-password" value={next}
              onChange={(e) => setNext(e.target.value)} />
          )}
        </Field>
        <button type="submit" disabled={busy}>
          Passwort ändern
        </button>
      </form>
    </section>
  )
}
