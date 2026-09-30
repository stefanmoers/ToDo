import type { FormEvent } from 'react'
import { Link } from 'react-router-dom'
import type { Project } from '../../api/projects'
import { TASK_PRIORITIES, TASK_STATUSES } from '../../api/tasks'
import { ErrorSummary, Field, type FormErrors } from '../../components/FormParts'
import { DueFields } from './DueFields'
import type { TaskDraft } from './draft'
import { PRIORITY_LABELS, STATUS_LABELS, TASK_FIELD_LABELS } from './labels'
import { useAssignableUsers } from './useAssignableUsers'

export function TaskForm({
  draft,
  onChange,
  errors,
  general,
  focusToken,
  busy,
  busyLabel,
  submitLabel,
  showStatus,
  projects,
  onSubmit,
  cancelTo,
}: {
  draft: TaskDraft
  onChange: (patch: Partial<TaskDraft>) => void
  errors: FormErrors
  general?: string
  focusToken: number
  busy: boolean
  busyLabel: string
  submitLabel: string
  showStatus: boolean
  projects: Project[]
  onSubmit: () => void
  cancelTo: string
}) {
  const assignableUsers = useAssignableUsers(draft.projectId)

  function handleSubmit(e: FormEvent) {
    e.preventDefault()
    onSubmit()
  }

  return (
    <>
      <ErrorSummary errors={errors} labels={TASK_FIELD_LABELS} general={general} focusToken={focusToken} />
      <form onSubmit={handleSubmit} noValidate>
        {projects.length > 0 && (
          <Field id="projectId" label="Projekt" hint="Optional. Ohne Auswahl ist die Aufgabe persönlich." error={errors.projectId}>
            {(p) => (
              <select
                {...p}
                name="projectId"
                value={draft.projectId}
                onChange={(e) => onChange({ projectId: e.target.value, assigneeId: '' })}
              >
                <option value="">Keine (persönliche Aufgabe)</option>
                {projects.map((project) => (
                  <option key={project.id} value={project.id}>
                    {project.name}
                  </option>
                ))}
              </select>
            )}
          </Field>
        )}
        {assignableUsers.length > 0 && (
          <Field
            id="assigneeId"
            label="Bearbeiter"
            hint={draft.projectId ? 'Optional. Eigentümer oder ein Mitglied dieses Projekts.' : 'Optional. Ohne Projekt nur du selbst.'}
            error={errors.assigneeId}
          >
            {(p) => (
              <select
                {...p}
                name="assigneeId"
                value={draft.assigneeId}
                onChange={(e) => onChange({ assigneeId: e.target.value })}
              >
                <option value="">Keine Zuweisung</option>
                {assignableUsers.map((u) => (
                  <option key={u.id} value={u.id}>
                    {u.displayName}
                  </option>
                ))}
              </select>
            )}
          </Field>
        )}
        <Field id="title" label="Titel" hint="Pflichtfeld, höchstens 200 Zeichen." error={errors.title}>
          {(p) => (
            <input {...p} type="text" name="title" value={draft.title} onChange={(e) => onChange({ title: e.target.value })} />
          )}
        </Field>
        <Field
          id="description"
          label="Beschreibung"
          hint="Optional, höchstens 10.000 Zeichen. Wird als reiner Text gespeichert."
          error={errors.description}
        >
          {(p) => (
            <textarea
              {...p}
              name="description"
              rows={6}
              value={draft.description}
              onChange={(e) => onChange({ description: e.target.value })}
            />
          )}
        </Field>
        <Field id="priority" label="Priorität" error={errors.priority}>
          {(p) => (
            <select
              {...p}
              name="priority"
              value={draft.priority}
              onChange={(e) => onChange({ priority: e.target.value as TaskDraft['priority'] })}
            >
              {TASK_PRIORITIES.map((value) => (
                <option key={value} value={value}>
                  {PRIORITY_LABELS[value]}
                </option>
              ))}
            </select>
          )}
        </Field>
        {showStatus && (
          <Field id="status" label="Status" error={errors.status}>
            {(p) => (
              <select
                {...p}
                name="status"
                value={draft.status}
                onChange={(e) => onChange({ status: e.target.value as TaskDraft['status'] })}
              >
                {TASK_STATUSES.map((value) => (
                  <option key={value} value={value}>
                    {STATUS_LABELS[value]}
                  </option>
                ))}
              </select>
            )}
          </Field>
        )}
        <DueFields draft={draft} onChange={onChange} errors={errors} />
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
