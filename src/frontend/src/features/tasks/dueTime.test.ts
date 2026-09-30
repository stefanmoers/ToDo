import { describe, expect, it } from 'vitest'
import { possibleOffsets } from './dueTime'

describe('possibleOffsets', () => {
  it('liefert einen Offset für eine normale Uhrzeit', () => {
    expect(possibleOffsets('2026-07-15', '09:00', 'Europe/Berlin')).toEqual(['+02:00'])
  })

  it('liefert keinen Offset für eine Uhrzeit in der Frühjahrs-Lücke', () => {
    // 2026-03-29: Umstellung 02:00 -> 03:00 in Europe/Berlin, 02:30 existiert nicht.
    expect(possibleOffsets('2026-03-29', '02:30', 'Europe/Berlin')).toEqual([])
  })

  it('liefert zwei Offsets für eine doppelte Uhrzeit im Herbst', () => {
    // 2026-10-25: Umstellung 03:00 -> 02:00 in Europe/Berlin, 02:30 kommt zweimal vor.
    const offsets = possibleOffsets('2026-10-25', '02:30', 'Europe/Berlin')
    expect(offsets).toHaveLength(2)
    expect(offsets).toEqual(expect.arrayContaining(['+02:00', '+01:00']))
  })

  it('liefert unterschiedliche Offsets für eine andere Zeitzone ohne Sommerzeit an diesem Tag', () => {
    expect(possibleOffsets('2026-10-25', '02:30', 'America/Los_Angeles')).toEqual(['-07:00'])
  })
})
