import { useEffect, useRef } from 'react'
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { Pagination } from '../components/Pagination'
import { useAuth } from '../features/auth/AuthContext'
import { useAllTags } from '../features/tags/useTagList'
import { pluralTasks } from '../features/tasks/labels'
import { TaskListItem } from '../features/tasks/TaskListItem'
import { useTaskList } from '../features/tasks/useTaskList'
import { usePageTitle } from '../hooks/usePageTitle'

export default function TaskListPage() {
  usePageTitle('Meine Aufgaben')
  const { user } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const pageParam = Number.parseInt(searchParams.get('seite') ?? '1', 10)
  const page = Number.isFinite(pageParam) && pageParam > 0 ? pageParam - 1 : 0
  const tagId = searchParams.get('tag') ?? undefined
  const tags = useAllTags()
  const { state, reload, replaceItem } = useTaskList(page, tagId)
  const headingRef = useRef<HTMLHeadingElement>(null)
  const loadedOnce = useRef(false)
  const notice = (location.state as { notice?: string } | null)?.notice

  function onFilterChange(newTagId: string) {
    const next = new URLSearchParams()
    if (newTagId) next.set('tag', newTagId)
    setSearchParams(next)
  }

  useEffect(() => {
    if (state.kind === 'loaded') {
      if (loadedOnce.current) headingRef.current?.focus()
      loadedOnce.current = true
    }
  }, [state.kind, page])

  useEffect(() => {
    if (state.kind === 'loaded' && state.page.items.length === 0 && page > 0 && state.page.totalPages > 0) {
      const query = tagId ? `?seite=${state.page.totalPages}&tag=${encodeURIComponent(tagId)}` : `?seite=${state.page.totalPages}`
      navigate(`/${query}`, { replace: true })
    }
  }, [state, page, tagId, navigate])

  return (
    <>
      <h1>Meine Aufgaben</h1>
      <p>Hallo {user?.displayName}!</p>
      {notice && (
        <p role="status" className="notice">
          {notice}
        </p>
      )}
      <p>
        <Link to="/aufgaben/neu" className="button-link">
          Neue Aufgabe anlegen
        </Link>
      </p>
      {tags.length > 0 && (
        <div className="field">
          <label htmlFor="tag-filter">Nach Tag filtern</label>
          <select id="tag-filter" value={tagId ?? ''} onChange={(e) => onFilterChange(e.target.value)}>
            <option value="">Alle Aufgaben</option>
            {tags.map((tag) => (
              <option key={tag.id} value={tag.id}>
                {tag.name}
              </option>
            ))}
          </select>
        </div>
      )}
      {state.kind === 'loading' && <p role="status">Aufgaben werden geladen …</p>}
      {state.kind === 'error' && (
        <div role="alert">
          <p>Die Aufgaben konnten nicht geladen werden. {state.message}</p>
          <button type="button" onClick={reload}>
            Erneut versuchen
          </button>
        </div>
      )}
      {state.kind === 'loaded' && state.page.totalElements === 0 && tagId && (
        <p>
          Keine Aufgaben mit diesem Tag. <button type="button" className="link-button" onClick={() => onFilterChange('')}>Filter zurücksetzen</button>
        </p>
      )}
      {state.kind === 'loaded' && state.page.totalElements === 0 && !tagId && (
        <p>
          Du hast noch keine Aufgaben. <Link to="/aufgaben/neu">Erste Aufgabe anlegen</Link>
        </p>
      )}
      {state.kind === 'loaded' && state.page.totalElements > 0 && (
        <>
          <h2 ref={headingRef} tabIndex={-1} id="task-list-heading">
            Aufgaben – Seite {state.page.page + 1} von {state.page.totalPages} ({pluralTasks(state.page.totalElements)})
          </h2>
          <ul className="task-list" aria-labelledby="task-list-heading">
            {state.page.items.map((task) => (
              <TaskListItem
                key={task.id}
                task={task}
                viewerTimeZone={user!.timeZone}
                onChanged={replaceItem}
                onConflict={reload}
                onGone={reload}
              />
            ))}
          </ul>
          <Pagination
            page={state.page.page}
            totalPages={state.page.totalPages}
            label="Seiten der Aufgabenliste"
            toHref={(p) => (tagId ? `/?seite=${p + 1}&tag=${encodeURIComponent(tagId)}` : `/?seite=${p + 1}`)}
          />
        </>
      )}
    </>
  )
}
