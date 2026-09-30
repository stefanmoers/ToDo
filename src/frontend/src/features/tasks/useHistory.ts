import { useEffect, useState } from 'react'
import { listActivities, listComments, type Activity, type Comment } from '../../api/tasks'

export type HistoryEntry =
  | { kind: 'activity'; id: string; createdAt: string; actorId: string; actorDisplayName?: string; activity: Activity }
  | { kind: 'comment'; id: string; createdAt: string; actorId: string; actorDisplayName?: string; comment: Comment }

type HistoryState = { kind: 'loading' } | { kind: 'loaded'; entries: HistoryEntry[] } | { kind: 'error'; message: string }

/**
 * US-081: Aktivitätshistorie und US-080: Kommentare in einer gemeinsamen, chronologischen Zeitleiste.
 * `COMMENTED`-Einträge der Historie werden ausgeblendet — der zugehörige Kommentar selbst zeigt denselben
 * Zeitpunkt bereits mit vollem Text, ein zusätzlicher „Hat kommentiert“-Eintrag wäre redundant.
 */
export function useHistory(taskId: string) {
  const [state, setState] = useState<HistoryState>({ kind: 'loading' })

  useEffect(() => {
    let active = true
    Promise.all([listActivities(taskId), listComments(taskId)])
      .then(([activities, comments]) => {
        if (active) setState({ kind: 'loaded', entries: merge(activities, comments) })
      })
      .catch((err) => {
        if (active) setState({ kind: 'error', message: err instanceof Error ? err.message : 'Unbekannter Fehler.' })
      })
    return () => {
      active = false
    }
  }, [taskId])

  function addComment(comment: Comment) {
    setState((prev) => {
      if (prev.kind !== 'loaded') return prev
      const entry: HistoryEntry = {
        kind: 'comment',
        id: comment.id,
        createdAt: comment.createdAt,
        actorId: comment.authorId,
        actorDisplayName: comment.authorDisplayName,
        comment,
      }
      return { kind: 'loaded', entries: [...prev.entries, entry].sort(byCreatedAt) }
    })
  }

  return { state, addComment }
}

function merge(activities: Activity[], comments: Comment[]): HistoryEntry[] {
  const activityEntries: HistoryEntry[] = activities.filter((a) => a.type !== 'COMMENTED').map((a) => ({
    kind: 'activity',
    id: a.id,
    createdAt: a.createdAt,
    actorId: a.actorId,
    actorDisplayName: a.actorDisplayName,
    activity: a,
  }))
  const commentEntries: HistoryEntry[] = comments.map((c) => ({
    kind: 'comment',
    id: c.id,
    createdAt: c.createdAt,
    actorId: c.authorId,
    actorDisplayName: c.authorDisplayName,
    comment: c,
  }))
  return [...activityEntries, ...commentEntries].sort(byCreatedAt)
}

function byCreatedAt(a: HistoryEntry, b: HistoryEntry): number {
  return a.createdAt.localeCompare(b.createdAt)
}
