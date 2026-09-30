export type Readiness = 'UP' | 'DOWN' | 'UNKNOWN'

/** Fragt den öffentlichen Bereitschaftsstatus des Backends ab (nur Gesamtzustand, keine Details). */
export async function fetchReadiness(signal?: AbortSignal): Promise<Readiness> {
  try {
    const response = await fetch('/actuator/health', { signal })
    const body = (await response.json()) as { status?: string }
    return body.status === 'UP' ? 'UP' : 'DOWN'
  } catch {
    return 'UNKNOWN'
  }
}
