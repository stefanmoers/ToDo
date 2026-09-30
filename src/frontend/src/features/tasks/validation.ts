/** Clientseitige Prüfung zur schnellen Rückmeldung. Maßgeblich ist immer die Prüfung im Backend (gleiche Regeln). */
import type { FormErrors } from '../../components/FormParts'
import type { TaskDraft } from './draft'
import { possibleOffsets } from './dueTime'

export const TITLE_MAX = 200
export const DESCRIPTION_MAX = 10_000

const codepoints = (s: string) => Array.from(s).length

export function validateTitle(value: string): string | undefined {
  const n = codepoints(value.trim())
  if (n < 1 || n > TITLE_MAX) return `Der Titel muss 1 bis ${TITLE_MAX} Zeichen lang sein.`
  return undefined
}

export function validateDescription(value: string): string | undefined {
  if (value.trim().length === 0) return undefined
  const n = codepoints(value)
  if (n > DESCRIPTION_MAX) return `Die Beschreibung darf höchstens ${DESCRIPTION_MAX} Zeichen lang sein.`
  return undefined
}

/** Prüft den gesamten Formularentwurf, inklusive der Fälligkeits-Randfälle (Sommerzeit). */
export function validateTaskDraft(draft: TaskDraft): FormErrors {
  const errors: FormErrors = {
    title: validateTitle(draft.title),
    description: validateDescription(draft.description),
  }
  const hasDate = draft.dueDate.trim().length > 0
  const hasTime = draft.dueTime.trim().length > 0

  if (draft.dateBadInput) {
    errors.dueDate = 'Bitte gib ein gültiges Datum ein.'
  }
  if (hasTime && !hasDate) {
    errors.dueTime = 'Eine Uhrzeit ist nur zusammen mit einem Datum möglich.'
  } else if (hasDate && hasTime && !errors.dueDate) {
    const offsets = possibleOffsets(draft.dueDate, draft.dueTime, draft.dueTimeZone)
    if (offsets.length === 0) {
      errors.dueTime = 'Diese Uhrzeit gibt es am gewählten Tag wegen der Zeitumstellung nicht.'
    } else if (offsets.length > 1 && draft.dueOffset.trim().length === 0) {
      errors.dueOffset = 'Bitte wähle, welcher Zeitpunkt gemeint ist.'
    }
  }
  return errors
}
