import { Field, type FormErrors } from '../../components/FormParts'
import { listTimeZones } from '../auth/timeZones'
import { possibleOffsets } from './dueTime'
import type { TaskDraft } from './draft'

export function DueFields({
  draft,
  onChange,
  errors,
}: {
  draft: TaskDraft
  onChange: (patch: Partial<TaskDraft>) => void
  errors: FormErrors
}) {
  const hasDate = draft.dueDate.trim().length > 0
  const hasTime = draft.dueTime.trim().length > 0
  const offsets = hasDate && hasTime ? possibleOffsets(draft.dueDate, draft.dueTime, draft.dueTimeZone) : []
  const needsOffsetChoice = offsets.length > 1

  return (
    <fieldset className="due-fields">
      <legend>Fälligkeit (optional)</legend>
      <Field id="dueDate" label="Fälligkeitsdatum" hint="Optional." error={errors.dueDate}>
        {(p) => (
          <input
            {...p}
            type="date"
            name="dueDate"
            value={draft.dueDate}
            onChange={(e) => onChange({ dueDate: e.target.value, dateBadInput: e.target.validity.badInput })}
          />
        )}
      </Field>
      <Field id="dueTime" label="Uhrzeit" hint="Optional. Nur zusammen mit einem Datum." error={errors.dueTime}>
        {(p) => (
          <input
            {...p}
            type="time"
            name="dueTime"
            value={draft.dueTime}
            onChange={(e) => onChange({ dueTime: e.target.value })}
          />
        )}
      </Field>
      <Field
        id="dueTimeZone"
        label="Zeitzone der Fälligkeit"
        hint="Standard ist deine Kontozeitzone."
        error={errors.dueTimeZone}
      >
        {(p) => (
          <select
            {...p}
            name="dueTimeZone"
            value={draft.dueTimeZone}
            onChange={(e) => onChange({ dueTimeZone: e.target.value })}
          >
            {listTimeZones(draft.dueTimeZone).map((z) => (
              <option key={z} value={z}>
                {z}
              </option>
            ))}
          </select>
        )}
      </Field>
      {needsOffsetChoice && (
        <fieldset>
          <legend>Welcher Zeitpunkt ist gemeint?</legend>
          <p className="hint">
            Die Uhrzeit {draft.dueTime} kommt an diesem Tag wegen der Zeitumstellung zweimal vor.
          </p>
          {errors.dueOffset && (
            <p id="dueOffset-error" className="field-error">
              <span aria-hidden="true">⚠ </span>
              <span className="visually-hidden">Fehler: </span>
              {errors.dueOffset}
            </p>
          )}
          {offsets.map((offset, index) => {
            const id = index === 0 ? 'dueOffset' : `dueOffset-${index}`
            return (
              <p key={offset} className="radio-option">
                <input
                  type="radio"
                  id={id}
                  name="dueOffset"
                  value={offset}
                  checked={draft.dueOffset === offset}
                  aria-describedby={errors.dueOffset ? 'dueOffset-error' : undefined}
                  onChange={() => onChange({ dueOffset: offset })}
                />{' '}
                <label htmlFor={id}>
                  {draft.dueTime} Uhr, UTC{offset}
                </label>
              </p>
            )
          })}
        </fieldset>
      )}
      {(hasDate || hasTime) && (
        <button
          type="button"
          className="secondary"
          onClick={() => onChange({ dueDate: '', dateBadInput: false, dueTime: '', dueOffset: '' })}
        >
          Fälligkeit entfernen
        </button>
      )}
    </fieldset>
  )
}
