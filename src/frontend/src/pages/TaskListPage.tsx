import { useEffect, useMemo, useRef, useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import {
  TASK_PRIORITIES,
  TASK_STATUSES,
  type TaskListFilters,
  type TaskPriority,
  type TaskSortField,
  type TaskStatus,
} from '../api/tasks'
import { Pagination } from '../components/Pagination'
import { useAuth } from '../features/auth/AuthContext'
import { useAllProjects } from '../features/projects/useProjectList'
import { tagLabels } from '../features/tags/origin'
import { useAllTags } from '../features/tags/useTagList'
import { PRIORITY_LABELS, STATUS_LABELS, pluralTasks } from '../features/tasks/labels'
import { matchesStandardView, STANDARD_VIEWS, standardViewParams } from '../features/tasks/standardViews'
import { TaskListItem } from '../features/tasks/TaskListItem'
import { useTaskList } from '../features/tasks/useTaskList'
import { usePageTitle } from '../hooks/usePageTitle'

const SORT_OPTIONS: { value: string; label: string; sortBy: TaskSortField; sortDirection: 'asc' | 'desc' }[] = [
  { value: 'createdAt-desc', label: 'Erstellt (neueste zuerst)', sortBy: 'createdAt', sortDirection: 'desc' },
  { value: 'createdAt-asc', label: 'Erstellt (älteste zuerst)', sortBy: 'createdAt', sortDirection: 'asc' },
  { value: 'updatedAt-desc', label: 'Zuletzt geändert (neueste zuerst)', sortBy: 'updatedAt', sortDirection: 'desc' },
  { value: 'updatedAt-asc', label: 'Zuletzt geändert (älteste zuerst)', sortBy: 'updatedAt', sortDirection: 'asc' },
  { value: 'due-asc', label: 'Fälligkeit (bald zuerst)', sortBy: 'due', sortDirection: 'asc' },
  { value: 'due-desc', label: 'Fälligkeit (spät zuerst)', sortBy: 'due', sortDirection: 'desc' },
  { value: 'priority-desc', label: 'Priorität (hoch zuerst)', sortBy: 'priority', sortDirection: 'desc' },
  { value: 'priority-asc', label: 'Priorität (niedrig zuerst)', sortBy: 'priority', sortDirection: 'asc' },
  { value: 'title-asc', label: 'Titel (A bis Z)', sortBy: 'title', sortDirection: 'asc' },
  { value: 'title-desc', label: 'Titel (Z bis A)', sortBy: 'title', sortDirection: 'desc' },
]

export default function TaskListPage() {
  usePageTitle('Meine Aufgaben')
  const { user } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const searchInputRef = useRef<HTMLInputElement>(null)
  const headingRef = useRef<HTMLHeadingElement>(null)
  const loadedOnce = useRef(false)
  const [filtersOpen, setFiltersOpen] = useState(false)

  const pageParam = Number.parseInt(searchParams.get('seite') ?? '1', 10)
  const page = Number.isFinite(pageParam) && pageParam > 0 ? pageParam - 1 : 0
  const q = searchParams.get('q') ?? undefined
  const statuses = searchParams.getAll('status') as TaskStatus[]
  const priorities = searchParams.getAll('priority') as TaskPriority[]
  const projectId = searchParams.get('projectId') ?? undefined
  const tagId = searchParams.get('tag') ?? undefined
  const assigneeId = searchParams.get('assigneeId') ?? undefined
  const overdue = searchParams.get('overdue') === 'true'
  const dueDateFrom = searchParams.get('dueDateFrom') ?? undefined
  const dueDateTo = searchParams.get('dueDateTo') ?? undefined
  const sortBy = (searchParams.get('sortBy') ?? undefined) as TaskSortField | undefined
  const sortDirection = (searchParams.get('sortDirection') ?? undefined) as 'asc' | 'desc' | undefined

  const hasActiveFilters = Boolean(
    q || statuses.length > 0 || priorities.length > 0 || projectId || tagId || assigneeId || overdue || dueDateFrom
      || dueDateTo,
  )
  const activeFacetCount = statuses.length + priorities.length + Number(Boolean(projectId)) + Number(Boolean(tagId))
    + Number(Boolean(assigneeId)) + Number(overdue) + Number(Boolean(dueDateFrom)) + Number(Boolean(dueDateTo))

  const filters: TaskListFilters = {
    q,
    status: statuses.length > 0 ? statuses : undefined,
    priority: priorities.length > 0 ? priorities : undefined,
    projectId,
    tagId,
    assigneeId,
    overdue: overdue || undefined,
    dueDateFrom,
    dueDateTo,
    sortBy,
    sortDirection,
  }
  // Neues Objekt bei jedem Rendern; über den Inhalt stabilisiert, damit useTaskList nicht unnötig neu lädt.
  const filtersKey = JSON.stringify(filters)
  // eslint-disable-next-line react-hooks/exhaustive-deps -- absichtlich über filtersKey (Inhalt), nicht filters (Referenz) stabilisiert
  const stableFilters = useMemo(() => filters, [filtersKey])

  const { state, reload, replaceItem } = useTaskList(page, stableFilters)
  const tags = useAllTags()
  const tagTexts = tagLabels(tags, user?.id)
  const projects = useAllProjects()
  const notice = (location.state as { notice?: string } | null)?.notice

  function updateParams(mutate: (next: URLSearchParams) => void) {
    const next = new URLSearchParams(searchParams)
    next.delete('seite')
    mutate(next)
    setSearchParams(next)
  }

  function toggleMultiValue(key: string, value: string) {
    updateParams((next) => {
      const selected = new Set(next.getAll(key))
      if (selected.has(value)) selected.delete(value)
      else selected.add(value)
      next.delete(key)
      for (const v of selected) next.append(key, v)
    })
  }

  function onSearchSubmit(e: FormEvent) {
    e.preventDefault()
    const value = searchInputRef.current?.value.trim() ?? ''
    updateParams((next) => {
      if (value) next.set('q', value)
      else next.delete('q')
    })
  }

  function onSingleValueChange(key: string, value: string) {
    updateParams((next) => {
      if (value) next.set(key, value)
      else next.delete(key)
    })
  }

  function onOverdueChange(checked: boolean) {
    updateParams((next) => {
      if (checked) next.set('overdue', 'true')
      else next.delete('overdue')
    })
  }

  function onSortChange(value: string) {
    const option = SORT_OPTIONS.find((o) => o.value === value)
    updateParams((next) => {
      if (option && option.sortBy !== 'createdAt') next.set('sortBy', option.sortBy)
      else next.delete('sortBy')
      if (option && option.sortDirection !== 'desc') next.set('sortDirection', option.sortDirection)
      else next.delete('sortDirection')
    })
  }

  function resetFilters() {
    setSearchParams(new URLSearchParams())
  }

  const currentSortValue = `${sortBy ?? 'createdAt'}-${sortDirection ?? 'desc'}`

  useEffect(() => {
    if (state.kind === 'loaded') {
      if (loadedOnce.current) headingRef.current?.focus()
      loadedOnce.current = true
    }
  }, [state.kind, page])

  useEffect(() => {
    if (state.kind === 'loaded' && state.page.items.length === 0 && page > 0 && state.page.totalPages > 0) {
      const next = new URLSearchParams(searchParams)
      next.set('seite', String(state.page.totalPages))
      navigate(`/?${next.toString()}`, { replace: true })
    }
  }, [state, page, searchParams, navigate])

  function pageHref(p: number): string {
    const next = new URLSearchParams(searchParams)
    next.set('seite', String(p + 1))
    return `/?${next.toString()}`
  }

  return (
    <>
      <div className="page-heading">
        <div>
          <p className="eyebrow">Dein Arbeitsbereich</p>
          <h1>Meine Aufgaben</h1>
          <p>Hallo {user?.displayName}! Hier behältst du den Überblick.</p>
        </div>
        <Link to="/aufgaben/neu" className="button-link">
          Neue Aufgabe anlegen
        </Link>
      </div>
      {notice && (
        <p role="status" className="notice">
          {notice}
        </p>
      )}
      <nav aria-label="Standardansichten" className="standard-views">
        <ul>
          {STANDARD_VIEWS.map(({ view, label }) => {
            const params = standardViewParams(view, user!.timeZone)
            const active = matchesStandardView(searchParams, view, user!.timeZone)
            return (
              <li key={view}>
                <Link to={`/?${params.toString()}`} aria-current={active ? 'page' : undefined}>
                  {label}
                </Link>
              </li>
            )
          })}
        </ul>
      </nav>

      <div className="task-workspace">
        <aside className="filter-panel" aria-labelledby="task-filter-heading">
          <section aria-labelledby="task-filter-heading">
            <div className="filter-heading">
              <h2 id="task-filter-heading">Filter</h2>
              <button
                type="button"
                className="filter-toggle secondary"
                aria-controls="task-filter-fields"
                aria-expanded={filtersOpen}
                onClick={() => setFiltersOpen((open) => !open)}
              >
                {filtersOpen ? 'Filter schließen' : 'Filter anzeigen'}
                {activeFacetCount > 0 && ` (${activeFacetCount} aktiv)`}
              </button>
            </div>
            <div id="task-filter-fields" className={filtersOpen ? 'filter-fields is-open' : 'filter-fields'}>
              <fieldset>
                <legend>Status</legend>
                {TASK_STATUSES.map((s) => (
                  <p key={s} className="radio-option">
                    <input
                      type="checkbox"
                      id={`filter-status-${s}`}
                      checked={statuses.includes(s)}
                      onChange={() => toggleMultiValue('status', s)}
                    />{' '}
                    <label htmlFor={`filter-status-${s}`}>{STATUS_LABELS[s]}</label>
                  </p>
                ))}
              </fieldset>
              <fieldset>
                <legend>Priorität</legend>
                {TASK_PRIORITIES.map((p) => (
                  <p key={p} className="radio-option">
                    <input
                      type="checkbox"
                      id={`filter-priority-${p}`}
                      checked={priorities.includes(p)}
                      onChange={() => toggleMultiValue('priority', p)}
                    />{' '}
                    <label htmlFor={`filter-priority-${p}`}>{PRIORITY_LABELS[p]}</label>
                  </p>
                ))}
              </fieldset>
              {projects.length > 0 && (
                <div className="field">
                  <label htmlFor="filter-project">Projekt</label>
                  <select id="filter-project" value={projectId ?? ''} onChange={(e) => onSingleValueChange('projectId', e.target.value)}>
                    <option value="">Alle Projekte</option>
                    {projects.map((p) => (
                      <option key={p.id} value={p.id}>
                        {p.name}
                      </option>
                    ))}
                  </select>
                </div>
              )}
              {tags.length > 0 && (
                <div className="field">
                  <label htmlFor="tag-filter">Nach Tag filtern</label>
                  <select id="tag-filter" value={tagId ?? ''} onChange={(e) => onSingleValueChange('tag', e.target.value)}>
                    <option value="">Alle Tags</option>
                    {tags.map((tag) => (
                      <option key={tag.id} value={tag.id}>
                        {tagTexts.get(tag.id)?.text ?? tag.name}
                      </option>
                    ))}
                  </select>
                </div>
              )}
              <p className="radio-option">
                <input type="checkbox" id="filter-overdue" checked={overdue} onChange={(e) => onOverdueChange(e.target.checked)} />{' '}
                <label htmlFor="filter-overdue">Nur überfällige Aufgaben</label>
              </p>
              <p className="radio-option">
                <input
                  type="checkbox"
                  id="filter-assignee-me"
                  checked={assigneeId === user!.id}
                  onChange={(e) => onSingleValueChange('assigneeId', e.target.checked ? user!.id : '')}
                />{' '}
                <label htmlFor="filter-assignee-me">Nur mir zugewiesene Aufgaben</label>
              </p>
              {hasActiveFilters && (
                <p>
                  <button type="button" className="link-button" onClick={resetFilters}>
                    Alle Filter zurücksetzen
                  </button>
                </p>
              )}
            </div>
          </section>
        </aside>

        <div className="task-results">
          <form role="search" className="task-search" onSubmit={onSearchSubmit}>
            <div className="field">
              <label htmlFor="task-search">Suche</label>
              <input
                key={q ?? ''}
                id="task-search"
                ref={searchInputRef}
                defaultValue={q ?? ''}
                placeholder="Titel oder Beschreibung"
              />
            </div>
            <button type="submit">Suchen</button>
          </form>

          <div className="field">
            <label htmlFor="sort-select">Sortierung</label>
            <select id="sort-select" value={currentSortValue} onChange={(e) => onSortChange(e.target.value)}>
              {SORT_OPTIONS.map((o) => (
                <option key={o.value} value={o.value}>
                  {o.label}
                </option>
              ))}
            </select>
          </div>

          {state.kind === 'loading' && <p role="status">Aufgaben werden geladen …</p>}
          {state.kind === 'error' && (
            <div role="alert">
              <p>Die Aufgaben konnten nicht geladen werden. {state.message}</p>
              <button type="button" onClick={reload}>
                Erneut versuchen
              </button>
            </div>
          )}
          {state.kind === 'loaded' && state.page.totalElements === 0 && hasActiveFilters && (
            <p className="empty-state">
              Keine Aufgaben für diese Auswahl.{' '}
              <button type="button" className="link-button" onClick={resetFilters}>
                Filter zurücksetzen
              </button>
            </p>
          )}
          {state.kind === 'loaded' && state.page.totalElements === 0 && !hasActiveFilters && (
            <p className="empty-state">
              Du hast noch keine Aufgaben. <Link to="/aufgaben/neu">Erste Aufgabe anlegen</Link>
            </p>
          )}
          {state.kind === 'loaded' && state.page.totalElements > 0 && (
            <>
              <h2 ref={headingRef} tabIndex={-1} id="task-list-heading" className="results-heading">
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
              <Pagination page={state.page.page} totalPages={state.page.totalPages} label="Seiten der Aufgabenliste" toHref={pageHref} />
            </>
          )}
        </div>
      </div>
    </>
  )
}
