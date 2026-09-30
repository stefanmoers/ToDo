/** Clientseitige Prüfung zur schnellen Rückmeldung. Maßgeblich ist immer die Prüfung im Backend (gleiche Regeln). */
import type { FormErrors } from '../../components/FormParts'
import type { ProjectDraft } from './draft'

export const NAME_MAX = 100
export const DESCRIPTION_MAX = 10_000

const codepoints = (s: string) => Array.from(s).length

export function validateName(value: string): string | undefined {
  const n = codepoints(value.trim())
  if (n < 1 || n > NAME_MAX) return `Der Projektname muss 1 bis ${NAME_MAX} Zeichen lang sein.`
  return undefined
}

export function validateDescription(value: string): string | undefined {
  if (value.trim().length === 0) return undefined
  const n = codepoints(value)
  if (n > DESCRIPTION_MAX) return `Die Beschreibung darf höchstens ${DESCRIPTION_MAX} Zeichen lang sein.`
  return undefined
}

export function validateProjectDraft(draft: ProjectDraft): FormErrors {
  return {
    name: validateName(draft.name),
    description: validateDescription(draft.description),
  }
}
