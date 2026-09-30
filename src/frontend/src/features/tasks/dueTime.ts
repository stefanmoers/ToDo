/** Ermittelt den tatsächlichen UTC-Offset einer Zeitzone zu einem gegebenen realen UTC-Zeitpunkt. */
function offsetMinutesAt(timeZone: string, utcMs: number): number {
  const parts = new Intl.DateTimeFormat('en-US', {
    timeZone,
    hourCycle: 'h23',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  }).formatToParts(new Date(utcMs))
  const map: Record<string, string> = {}
  for (const p of parts) if (p.type !== 'literal') map[p.type] = p.value
  const asUtc = Date.UTC(
    Number(map.year),
    Number(map.month) - 1,
    Number(map.day),
    Number(map.hour),
    Number(map.minute),
    Number(map.second),
  )
  return Math.round((asUtc - utcMs) / 60_000)
}

export function formatOffset(minutes: number): string {
  const sign = minutes < 0 ? '-' : '+'
  const abs = Math.abs(minutes)
  const hh = String(Math.floor(abs / 60)).padStart(2, '0')
  const mm = String(abs % 60).padStart(2, '0')
  return `${sign}${hh}:${mm}`
}

/**
 * Ermittelt, welche(r) UTC-Offset(s) zu einer lokalen Uhrzeit (Datum+Zeit als Strings) in dieser Zeitzone passen.
 * Leeres Ergebnis: die Uhrzeit existiert an diesem Tag nicht (Sommerzeit-Lücke). Zwei Einträge: die Uhrzeit
 * kommt zweimal vor (Herbst-Zeitumstellung) und braucht eine Auswahl.
 */
export function possibleOffsets(dateStr: string, timeStr: string, timeZone: string): string[] {
  const [y, m, d] = dateStr.split('-').map(Number)
  const [h, mi] = timeStr.split(':').map(Number)
  if (!y || !m || !d || Number.isNaN(h) || Number.isNaN(mi)) return []
  // "Naive" Interpretation der lokalen Werte als UTC-Zahlen; dient nur als stabiler Bezugspunkt für die Suche.
  const naiveUtc = Date.UTC(y, m - 1, d, h, mi)
  const before = offsetMinutesAt(timeZone, naiveUtc - 12 * 3_600_000)
  const after = offsetMinutesAt(timeZone, naiveUtc + 12 * 3_600_000)
  const candidates = [...new Set([before, after])]
  const valid = candidates.filter((offsetMin) => {
    const candidateUtc = naiveUtc - offsetMin * 60_000
    return offsetMinutesAt(timeZone, candidateUtc) === offsetMin
  })
  return valid.sort((a, b) => b - a).map(formatOffset)
}
