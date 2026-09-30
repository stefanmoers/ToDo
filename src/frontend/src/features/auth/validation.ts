/** Clientseitige Prüfung zur schnellen Rückmeldung. Maßgeblich ist immer die Prüfung im Backend (gleiche Regeln). */

export const PASSWORD_MIN = 15
export const PASSWORD_MAX = 128

const codepoints = (s: string) => Array.from(s).length

export function validateEmail(value: string): string | undefined {
  const v = value.trim()
  if (v.length === 0) return 'Bitte gib deine E-Mail-Adresse ein.'
  if (v.length > 254 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v)) return 'Bitte gib eine gültige E-Mail-Adresse an.'
  return undefined
}

export function validateDisplayName(value: string): string | undefined {
  const n = codepoints(value.trim())
  if (n < 1 || n > 100) return 'Der Anzeigename muss 1 bis 100 Zeichen lang sein.'
  return undefined
}

export function validateNewPassword(value: string): string | undefined {
  const n = codepoints(value)
  if (n < PASSWORD_MIN || n > PASSWORD_MAX)
    return `Das Passwort muss zwischen ${PASSWORD_MIN} und ${PASSWORD_MAX} Zeichen lang sein.`
  return undefined
}

export function validateRequired(value: string, message: string): string | undefined {
  return value.length === 0 ? message : undefined
}
