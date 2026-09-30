/** Clientseitige Prüfung zur schnellen Rückmeldung. Maßgeblich ist immer die Prüfung im Backend (gleiche Regeln). */
export const NAME_MAX = 50

const codepoints = (s: string) => Array.from(s).length

export function validateTagName(value: string): string | undefined {
  const n = codepoints(value.trim())
  if (n < 1 || n > NAME_MAX) return `Der Tag-Name muss 1 bis ${NAME_MAX} Zeichen lang sein.`
  return undefined
}
