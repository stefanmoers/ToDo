import { useCallback, useEffect, useState } from 'react'
import { listProjects, type Project } from '../../api/projects'
import type { Page } from '../../api/pagination'

type ListState =
  | { kind: 'loading' }
  | { kind: 'loaded'; page: Page<Project> }
  | { kind: 'error'; message: string }

export function useProjectList(page: number) {
  const [state, setState] = useState<ListState>({ kind: 'loading' })
  const [reloadToken, setReloadToken] = useState(0)

  useEffect(() => {
    let active = true
    listProjects(page)
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

/** Alle eigenen Projekte für Auswahllisten (Aufgaben zuordnen); bis zu 100, ohne Paginierung in der UI. */
export function useAllProjects() {
  const [projects, setProjects] = useState<Project[]>([])

  useEffect(() => {
    let active = true
    listProjects(0, 100)
      .then((result) => {
        if (active) setProjects(result.items)
      })
      .catch(() => {
        /* Projektauswahl bleibt dann leer; das Formular funktioniert weiterhin ohne Projektzuordnung. */
      })
    return () => {
      active = false
    }
  }, [])

  return projects
}
