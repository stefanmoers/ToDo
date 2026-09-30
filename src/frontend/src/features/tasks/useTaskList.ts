import { useCallback, useEffect, useState } from 'react'
import { listTasks, type Task, type TaskListFilters } from '../../api/tasks'
import type { Page } from '../../api/pagination'

type ListState =
  | { kind: 'loading' }
  | { kind: 'loaded'; page: Page<Task> }
  | { kind: 'error'; message: string }

/** `filters` sollte vom Aufrufer memoisiert werden (z. B. via useMemo), sonst löst jedes Rendern einen Refetch aus. */
export function useTaskList(page: number, filters: TaskListFilters = {}) {
  const [state, setState] = useState<ListState>({ kind: 'loading' })
  const [reloadToken, setReloadToken] = useState(0)

  useEffect(() => {
    let active = true
    listTasks(page, undefined, filters)
      .then((result) => {
        if (active) setState({ kind: 'loaded', page: result })
      })
      .catch((err) => {
        if (active) setState({ kind: 'error', message: err instanceof Error ? err.message : 'Unbekannter Fehler.' })
      })
    return () => {
      active = false
    }
  }, [page, filters, reloadToken])

  // Setzt sofort auf "loading" (Event-Handler, nicht im Effekt) und löst über reloadToken den Refetch aus.
  const reload = useCallback(() => {
    setState({ kind: 'loading' })
    setReloadToken((n) => n + 1)
  }, [])
  const replaceItem = useCallback((task: Task) => {
    setState((current) => {
      if (current.kind !== 'loaded') return current
      return { kind: 'loaded', page: { ...current.page, items: current.page.items.map((t) => (t.id === task.id ? task : t)) } }
    })
  }, [])

  return { state, reload, replaceItem }
}
