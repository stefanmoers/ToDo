import { useCallback, useEffect, useState } from 'react'
import { listSubtasks, type Subtask } from '../../api/tasks'

type SubtasksState =
  | { kind: 'loading' }
  | { kind: 'loaded'; subtasks: Subtask[] }
  | { kind: 'error'; message: string }

/**
 * US-070/071: Unteraufgaben einer Aufgabe. Jede Mutation liefert die vollständige, neu geordnete Liste zurück
 * ({@code applyList}); ein erneuter Ladevorgang ist dafür nicht nötig.
 */
export function useSubtasks(taskId: string) {
  const [state, setState] = useState<SubtasksState>({ kind: 'loading' })

  useEffect(() => {
    let active = true
    listSubtasks(taskId)
      .then((subtasks) => {
        if (active) setState({ kind: 'loaded', subtasks })
      })
      .catch((err) => {
        if (active) setState({ kind: 'error', message: err instanceof Error ? err.message : 'Unbekannter Fehler.' })
      })
    return () => {
      active = false
    }
  }, [taskId])

  const applyList = useCallback((subtasks: Subtask[]) => {
    setState({ kind: 'loaded', subtasks })
  }, [])

  return { state, applyList }
}
