import { useCallback, useEffect, useState } from 'react'
import { getProject, type Project } from '../../api/projects'
import { isNotFound } from './errors'

type ProjectState =
  | { kind: 'loading' }
  | { kind: 'loaded'; project: Project }
  | { kind: 'notFound' }
  | { kind: 'error'; message: string }

export function useProject(id: string) {
  const [state, setState] = useState<ProjectState>({ kind: 'loading' })
  const [reloadToken, setReloadToken] = useState(0)

  useEffect(() => {
    let active = true
    getProject(id)
      .then((project) => {
        if (active) setState({ kind: 'loaded', project })
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

  const reload = useCallback(() => {
    setState({ kind: 'loading' })
    setReloadToken((n) => n + 1)
  }, [])

  return { state, reload }
}
