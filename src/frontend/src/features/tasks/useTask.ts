import { useCallback, useEffect, useState } from 'react'
import { getTask, type Task } from '../../api/tasks'
import { isNotFound } from './errors'

type TaskState =
  | { kind: 'loading' }
  | { kind: 'loaded'; task: Task }
  | { kind: 'notFound' }
  | { kind: 'error'; message: string }

export function useTask(id: string) {
  const [state, setState] = useState<TaskState>({ kind: 'loading' })
  const [reloadToken, setReloadToken] = useState(0)

  useEffect(() => {
    let active = true
    getTask(id)
      .then((task) => {
        if (active) setState({ kind: 'loaded', task })
      })
      .catch((err) => {
        if (!active) return
        if (isNotFound(err)) setState({ kind: 'notFound' })
        else setState({ kind: 'error', message: err instanceof Error ? err.message : 'Unbekannter Fehler.' })
      })
    return () => {
      active = false
    }
  }, [id, reloadToken])

  // Setzt sofort auf "loading" (Event-Handler, nicht im Effekt) und löst über reloadToken den Refetch aus.
  const reload = useCallback(() => {
    setState({ kind: 'loading' })
    setReloadToken((n) => n + 1)
  }, [])
  const replace = useCallback((task: Task) => setState({ kind: 'loaded', task }), [])

  return { state, reload, replace }
}
