import { useCallback, useEffect, useState } from 'react'
import { listTags, type Tag } from '../../api/tags'

type ListState = { kind: 'loading' } | { kind: 'loaded'; tags: Tag[] } | { kind: 'error'; message: string }

export function useTagList() {
  const [state, setState] = useState<ListState>({ kind: 'loading' })
  const [reloadToken, setReloadToken] = useState(0)

  useEffect(() => {
    let active = true
    listTags()
      .then((tags) => {
        if (active) setState({ kind: 'loaded', tags })
      })
      .catch((err) => {
        if (active) setState({ kind: 'error', message: err instanceof Error ? err.message : 'Unbekannter Fehler.' })
      })
    return () => {
      active = false
    }
  }, [reloadToken])

  const reload = useCallback(() => {
    setState({ kind: 'loading' })
    setReloadToken((n) => n + 1)
  }, [])

  return { state, reload }
}

/** Für Auswahllisten (Tag-Editor an einer Aufgabe, Filter in der Aufgabenliste): leer statt Fehler bei Problemen. */
export function useAllTags() {
  const [tags, setTags] = useState<Tag[]>([])

  useEffect(() => {
    let active = true
    listTags()
      .then((result) => {
        if (active) setTags(result)
      })
      .catch(() => {
        /* Tag-Auswahl bleibt dann leer; Formulare funktionieren weiterhin ohne Tags. */
      })
    return () => {
      active = false
    }
  }, [])

  return tags
}
