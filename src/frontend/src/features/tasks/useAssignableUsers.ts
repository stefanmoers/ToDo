import { useEffect, useState } from 'react'
import { getProject, listMembers } from '../../api/projects'
import { useAuth } from '../auth/AuthContext'

export interface AssignableUser {
  id: string
  displayName: string
}

/**
 * US-062: wer einer Aufgabe zugewiesen werden darf. Ohne Projekt nur die eigene Person (Arbeitsannahme,
 * docs/DOMAIN_MODEL.md); mit Projekt der Eigentümer und alle aktuellen Mitglieder.
 */
export function useAssignableUsers(projectId: string): AssignableUser[] {
  const { user } = useAuth()
  const [projectUsers, setProjectUsers] = useState<AssignableUser[]>([])
  // Verhindert, dass beim Wechsel des Projekts kurz noch die Mitgliederliste des vorherigen Projekts angezeigt wird.
  const [loadedForProjectId, setLoadedForProjectId] = useState('')

  useEffect(() => {
    if (!projectId) return undefined
    let active = true
    Promise.all([getProject(projectId), listMembers(projectId)])
      .then(([project, members]) => {
        if (!active) return
        const owner: AssignableUser = { id: project.ownerId, displayName: project.ownerDisplayName ?? 'Eigentümer' }
        const rest = members.map((m) => ({ id: m.userId, displayName: m.displayName }))
        setProjectUsers([owner, ...rest])
        setLoadedForProjectId(projectId)
      })
      .catch(() => {
        if (active) {
          setProjectUsers([])
          setLoadedForProjectId(projectId)
        }
      })
    return () => {
      active = false
    }
  }, [projectId])

  if (!projectId) return user ? [{ id: user.id, displayName: 'Ich selbst' }] : []
  return loadedForProjectId === projectId ? projectUsers : []
}
