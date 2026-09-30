import { useEffect, useRef } from 'react'
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { Pagination } from '../components/Pagination'
import { ProjectListItem } from '../features/projects/ProjectListItem'
import { useProjectList } from '../features/projects/useProjectList'
import { usePageTitle } from '../hooks/usePageTitle'

export default function ProjectListPage() {
  usePageTitle('Meine Projekte')
  const navigate = useNavigate()
  const location = useLocation()
  const notice = (location.state as { notice?: string } | null)?.notice
  const [searchParams] = useSearchParams()
  const pageParam = Number.parseInt(searchParams.get('seite') ?? '1', 10)
  const page = Number.isFinite(pageParam) && pageParam > 0 ? pageParam - 1 : 0
  const { state, reload } = useProjectList(page)
  const headingRef = useRef<HTMLHeadingElement>(null)
  const loadedOnce = useRef(false)

  useEffect(() => {
    if (state.kind === 'loaded') {
      if (loadedOnce.current) headingRef.current?.focus()
      loadedOnce.current = true
    }
  }, [state.kind, page])

  useEffect(() => {
    if (state.kind === 'loaded' && state.page.items.length === 0 && page > 0 && state.page.totalPages > 0) {
      navigate(`/projekte?seite=${state.page.totalPages}`, { replace: true })
    }
  }, [state, page, navigate])

  return (
    <>
      <h1>Meine Projekte</h1>
      {notice && (
        <p role="status" className="notice">
          {notice}
        </p>
      )}
      <p>
        <Link to="/projekte/neu" className="button-link">
          Neues Projekt anlegen
        </Link>
      </p>
      {state.kind === 'loading' && <p role="status">Projekte werden geladen …</p>}
      {state.kind === 'error' && (
        <div role="alert">
          <p>Die Projekte konnten nicht geladen werden. {state.message}</p>
          <button type="button" onClick={reload}>
            Erneut versuchen
          </button>
        </div>
      )}
      {state.kind === 'loaded' && state.page.totalElements === 0 && (
        <p>
          Du hast noch keine Projekte. <Link to="/projekte/neu">Erstes Projekt anlegen</Link>
        </p>
      )}
      {state.kind === 'loaded' && state.page.totalElements > 0 && (
        <>
          <h2 ref={headingRef} tabIndex={-1} id="project-list-heading">
            Projekte – Seite {state.page.page + 1} von {state.page.totalPages}
          </h2>
          <ul className="task-list" aria-labelledby="project-list-heading">
            {state.page.items.map((project) => (
              <ProjectListItem key={project.id} project={project} />
            ))}
          </ul>
          <Pagination
            page={state.page.page}
            totalPages={state.page.totalPages}
            label="Seiten der Projektliste"
            toHref={(p) => `/projekte?seite=${p + 1}`}
          />
        </>
      )}
    </>
  )
}
