import { ApiError } from '../../api/client'

export function mapTagApiError(err: unknown): string {
  return err instanceof ApiError ? err.message : 'Es ist ein unerwarteter Fehler aufgetreten.'
}

export const isConflict = (err: unknown): boolean => err instanceof ApiError && err.status === 409

export const isNotFound = (err: unknown): boolean => err instanceof ApiError && (err.status === 404 || err.status === 403)
