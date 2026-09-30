import { useEffect, useRef, type ReactNode } from 'react'

export interface FormErrors {
  [field: string]: string | undefined
}

/**
 * Fehlerübersicht am Formularanfang: erhält nach fehlgeschlagenem Absenden den Fokus und
 * verlinkt jedes Feld mit seinem Fehler (WCAG 3.3.1, 3.3.3).
 */
export function ErrorSummary({
  errors,
  labels,
  general,
  focusToken,
}: {
  errors: FormErrors
  labels: Record<string, string>
  general?: string
  focusToken: number
}) {
  const ref = useRef<HTMLDivElement>(null)
  const entries = Object.entries(errors).filter(([, m]) => m)
  useEffect(() => {
    if (focusToken > 0) ref.current?.focus()
  }, [focusToken])
  if (entries.length === 0 && !general) return null
  return (
    <div ref={ref} tabIndex={-1} role="alert" className="error-summary" aria-labelledby="error-summary-title">
      <h2 id="error-summary-title">Das Formular enthält Fehler</h2>
      {general && <p>{general}</p>}
      {entries.length > 0 && (
        <ul>
          {entries.map(([field, message]) => (
            <li key={field}>
              <a
                href={`#${field}`}
                onClick={(e) => {
                  e.preventDefault()
                  document.getElementById(field)?.focus()
                }}
              >
                {labels[field] ?? field}: {message}
              </a>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

export function Field({
  id,
  label,
  hint,
  error,
  children,
}: {
  id: string
  label: string
  hint?: string
  error?: string
  children: (props: { id: string; 'aria-describedby'?: string; 'aria-invalid'?: boolean }) => ReactNode
}) {
  const hintId = hint ? `${id}-hint` : undefined
  const errorId = error ? `${id}-error` : undefined
  const describedBy = [hintId, errorId].filter(Boolean).join(' ') || undefined
  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      {hint && (
        <p id={hintId} className="hint">
          {hint}
        </p>
      )}
      {error && (
        <p id={errorId} className="field-error">
          <span aria-hidden="true">⚠ </span>
          <span className="visually-hidden">Fehler: </span>
          {error}
        </p>
      )}
      {children({ id, 'aria-describedby': describedBy, 'aria-invalid': error ? true : undefined })}
    </div>
  )
}
