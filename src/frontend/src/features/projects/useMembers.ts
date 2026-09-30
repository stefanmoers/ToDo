import { useCallback, useEffect, useState } from 'react'
import { listMembers, type ProjectMember } from '../../api/projects'

type MembersState =
  | { kind: 'loading' }
  | { kind: 'loaded'; members: ProjectMember[] }
  | { kind: 'error'; message: string }

/** US-060/061: Mitgliederliste eines Projekts, für Eigentümer und Mitglieder gleichermaßen sichtbar. */
export function useMembers(projectId: string) {
  const [state, setState] = useState<MembersState>({ kind: 'loading' })
  const [reloadToken, setReloadToken] = useState(0)

  useEffect(() => {
    let active = true
    listMembers(projectId)
      .then((members) => {
        if (active) setState({ kind: 'loaded', members })
      })
      .catch((err) => {
        if (active) setState({ kind: 'error', message: err instanceof Error ? err.message : 'Unbekannter Fehler.' })
      })
    return () => {
      active = false
    }
  }, [projectId, reloadToken])

  const reload = useCallback(() => {
    setState({ kind: 'loading' })
    setReloadToken((n) => n + 1)
  }, [])

  return { state, reload }
}
