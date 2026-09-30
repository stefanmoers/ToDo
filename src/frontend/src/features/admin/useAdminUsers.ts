import { useCallback, useEffect, useState } from 'react'
import { listAdminUsers, type AdminUser } from '../../api/admin'
import type { Page } from '../../api/pagination'

type ListState =
  | { kind: 'loading' }
  | { kind: 'loaded'; page: Page<AdminUser> }
  | { kind: 'error'; message: string }

/** US-014: Kontenliste für die Administration. */
export function useAdminUsers(page: number) {
  const [state, setState] = useState<ListState>({ kind: 'loading' })
  const [reloadToken, setReloadToken] = useState(0)

  useEffect(() => {
    let active = true
    listAdminUsers(page)
      .then((result) => {
        if (active) setState({ kind: 'loaded', page: result })
      })
      .catch((err) => {
        if (active) setState({ kind: 'error', message: err instanceof Error ? err.message : 'Unbekannter Fehler.' })
      })
    return () => {
      active = false
    }
  }, [page, reloadToken])

  const reload = useCallback(() => {
    setState({ kind: 'loading' })
    setReloadToken((n) => n + 1)
  }, [])

  return { state, reload }
}
