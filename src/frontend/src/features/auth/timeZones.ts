/** Liste der IANA-Zeitzonen des Browsers; die erkannte Zone des Geräts wird als Vorschlag verwendet. */
export function detectTimeZone(): string {
  try {
    const zone = Intl.DateTimeFormat().resolvedOptions().timeZone
    return zone && zone.includes('/') ? zone : 'Europe/Berlin'
  } catch {
    return 'Europe/Berlin'
  }
}

export function listTimeZones(current?: string): string[] {
  let zones: string[] = []
  try {
    zones = (Intl as unknown as { supportedValuesOf(key: string): string[] }).supportedValuesOf('timeZone')
  } catch {
    zones = ['Europe/Berlin']
  }
  const set = new Set(zones.filter((z) => z.includes('/')))
  set.add('Europe/Berlin')
  set.add(detectTimeZone())
  if (current) set.add(current)
  return [...set].sort()
}
