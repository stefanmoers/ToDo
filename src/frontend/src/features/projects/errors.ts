import { ApiError } from '../../api/client'
import type { FormErrors } from '../../components/FormParts'

export function mapProjectApiError(err: unknown): { errors: FormErrors; general?: string } {
  if (err instanceof ApiError && err.fieldErrors.length > 0) {
    const errors: FormErrors = {}
    for (const f of err.fieldErrors) errors[f.field] = f.message
    return { errors }
  }
  return { errors: {}, general: err instanceof ApiError ? err.message : 'Es ist ein unerwarteter Fehler aufgetreten.' }
}

export const isConflict = (err: unknown): boolean => err instanceof ApiError && err.status === 409

export const isNotFound = (err: unknown): boolean => err instanceof ApiError && (err.status === 404 || err.status === 403)
