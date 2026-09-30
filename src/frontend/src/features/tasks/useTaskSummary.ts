import { useCallback, useEffect, useState } from 'react'
import { getTaskSummary, type TaskSummary } from '../../api/tasks'

type SummaryState = { kind: 'loading' } | { kind: 'loaded'; summary: TaskSummary } | { kind: 'error'; message: string }

export function useTaskSummary(today: string) {
  const [state, setState] = useState<SummaryState>({ kind: 'loading' })
  const [reloadToken, setReloadToken] = useState(0)

  useEffect(() => {
    let active = true
    getTaskSummary(today)
      .then((summary) => {
        if (active) setState({ kind: 'loaded', summary })
      })
      .catch((err) => {
        if (active) setState({ kind: 'error', message: err instanceof Error ? err.message : 'Unbekannter Fehler.' })
      })
    return () => {
      active = false
    }
  }, [today, reloadToken])

  const reload = useCallback(() => {
    setState({ kind: 'loading' })
    setReloadToken((n) => n + 1)
  }, [])

  return { state, reload }
}
