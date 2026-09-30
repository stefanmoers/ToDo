export interface FieldError {
  field: string
  code: string
  message: string
}

/** Fehler im einheitlichen API-Format (siehe ADR-005). */
export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly code: string,
    message: string,
    public readonly fieldErrors: FieldError[] = [],
    public readonly requestId?: string,
  ) {
    super(message)
  }
}

interface CsrfToken {
  headerName: string
  token: string
}

let csrf: CsrfToken | null = null
let onUnauthorized: (() => void) | null = null

/** Der Auth-Kontext registriert hier, was bei einer abgelaufenen Sitzung (401) geschehen soll. */
export function setUnauthorizedHandler(handler: (() => void) | null) {
  onUnauthorized = handler
}

/** Nach Login/Logout wechselt die Sitzung; der Token muss dann neu geholt werden. */
export function resetCsrfToken() {
  csrf = null
}

async function loadCsrf(): Promise<CsrfToken> {
  if (csrf) return csrf
  const response = await fetch('/api/v1/auth/csrf', { credentials: 'same-origin' })
  if (!response.ok) throw await toApiError(response)
  csrf = (await response.json()) as CsrfToken
  return csrf
}

async function toApiError(response: Response): Promise<ApiError> {
  try {
    const body = (await response.json()) as {
      code?: string
      message?: string
      fieldErrors?: FieldError[]
      requestId?: string
    }
    return new ApiError(
      response.status,
      body.code ?? 'UNKNOWN',
      body.message ?? 'Es ist ein Fehler aufgetreten.',
      body.fieldErrors ?? [],
      body.requestId,
    )
  } catch {
    return new ApiError(response.status, 'UNKNOWN', 'Es ist ein unerwarteter Fehler aufgetreten.')
  }
}

const SAFE_METHODS = new Set(['GET', 'HEAD'])

/**
 * Ruft die API auf. Schreibende Anfragen erhalten den CSRF-Header; bei einem ungültigen Token
 * wird er einmal neu geholt und die Anfrage wiederholt. Netzwerkfehler werden zu ApiError NETWORK.
 */
export async function api<T = void>(
  method: 'GET' | 'POST' | 'PUT' | 'DELETE',
  path: string,
  body?: unknown,
  options: { expectUnauthorized?: boolean } = {},
): Promise<T> {
  for (let attempt = 0; attempt < 2; attempt++) {
    const headers: Record<string, string> = { Accept: 'application/json' }
    if (body !== undefined) headers['Content-Type'] = 'application/json'
    try {
      if (!SAFE_METHODS.has(method)) {
        const token = await loadCsrf()
        headers[token.headerName] = token.token
      }
      const response = await fetch(path, {
        method,
        headers,
        credentials: 'same-origin',
        body: body === undefined ? undefined : JSON.stringify(body),
      })
      if (response.ok) {
        return response.status === 204 ? (undefined as T) : ((await response.json()) as T)
      }
      const error = await toApiError(response)
      if (error.code === 'CSRF_INVALID' && attempt === 0) {
        csrf = null
        continue
      }
      if (error.status === 401 && !options.expectUnauthorized) onUnauthorized?.()
      throw error
    } catch (e) {
      if (e instanceof ApiError) throw e
      throw new ApiError(0, 'NETWORK', 'Der Server ist nicht erreichbar. Bitte prüfe deine Verbindung und versuche es erneut.')
    }
  }
  throw new ApiError(403, 'CSRF_INVALID', 'Die Sicherheitsprüfung ist fehlgeschlagen. Bitte lade die Seite neu.')
}
