import type { FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { ErrorSummary, Field, type FormErrors } from '../../components/FormParts'
import type { ProjectDraft } from './draft'
import { PROJECT_FIELD_LABELS } from './labels'

export function ProjectForm({
  draft,
  onChange,
  errors,
  general,
  focusToken,
  busy,
  busyLabel,
  submitLabel,
  onSubmit,
  cancelTo,
}: {
  draft: ProjectDraft
  onChange: (patch: Partial<ProjectDraft>) => void
  errors: FormErrors
  general?: string
  focusToken: number
  busy: boolean
  busyLabel: string
  submitLabel: string
  onSubmit: () => void
  cancelTo: string
}) {
  function handleSubmit(e: FormEvent) {
    e.preventDefault()
    onSubmit()
  }

  return (
    <>
      <ErrorSummary errors={errors} labels={PROJECT_FIELD_LABELS} general={general} focusToken={focusToken} />
      <form onSubmit={handleSubmit} noValidate>
        <Field id="name" label="Name" hint="Pflichtfeld, höchstens 100 Zeichen." error={errors.name}>
          {(p) => <input {...p} type="text" name="name" value={draft.name} onChange={(e) => onChange({ name: e.target.value })} />}
        </Field>
        <Field
          id="description"
          label="Beschreibung"
          hint="Optional, höchstens 10.000 Zeichen."
          error={errors.description}
        >
          {(p) => (
            <textarea
              {...p}
              name="description"
              rows={4}
              value={draft.description}
              onChange={(e) => onChange({ description: e.target.value })}
            />
          )}
        </Field>
        <div className="actions">
          <button type="submit" disabled={busy}>
            {busy ? busyLabel : submitLabel}
          </button>
          <Link to={cancelTo} className="button-link secondary">
            Abbrechen
          </Link>
        </div>
      </form>
    </>
  )
}
