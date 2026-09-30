import { useEffect, useRef, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { DeleteProjectDialog } from '../features/projects/DeleteProjectDialog'
import { ProjectNotFound } from '../features/projects/ProjectNotFound'
import { useProject } from '../features/projects/useProject'
import { formatTimestamp } from '../features/tasks/format'
import { useAuth } from '../features/auth/AuthContext'
import { usePageTitle } from '../hooks/usePageTitle'

export default function ProjectDetailPage() {
  const { projectId } = useParams<{ projectId: string }>()
  return <ProjectDetail key={projectId} projectId={projectId!} />
}

function ProjectDetail({ projectId }: { projectId: string }) {
  const { user } = useAuth()
  const { state, reload } = useProject(projectId)
  const navigate = useNavigate()
  const location = useLocation()
  const [dialogOpen, setDialogOpen] = useState(false)
  const [deleteConflict, setDeleteConflict] = useState(false)
  const deleteButtonRef = useRef<HTMLButtonElement>(null)
  const conflictRef = useRef<HTMLDivElement>(null)
  const notice = (location.state as { notice?: string } | null)?.notice

  usePageTitle(state.kind === 'loaded' ? `Projekt: ${state.project.name}` : 'Projekt')

  useEffect(() => {
    if (deleteConflict) conflictRef.current?.focus()
  }, [deleteConflict])

  if (state.kind === 'loading') return <p role="status">Projekt wird geladen …</p>
  if (state.kind === 'notFound') return <ProjectNotFound />
  if (state.kind === 'error') {
    return (
      <div role="alert">
        <p>Das Projekt konnte nicht geladen werden. {state.message}</p>
        <button type="button" onClick={reload}>
          Erneut versuchen
        </button>
      </div>
    )
  }

  const project = state.project

  return (
    <>
      <h1 className="task-title">{project.name}</h1>
      {notice && (
        <p role="status" className="notice">
          {notice}
        </p>
      )}
      {deleteConflict && (
        <div ref={conflictRef} tabIndex={-1} role="alert" className="conflict">
          <p>
            Nicht gelöscht: Das Projekt wurde zwischenzeitlich geändert. Die aktuelle Fassung wird angezeigt. Du
            kannst es jetzt erneut löschen.
          </p>
        </div>
      )}
      <dl className="facts">
        <dt>Beschreibung</dt>
        <dd className="description">{project.description ?? 'Keine Beschreibung'}</dd>
        <dt>Erstellt am</dt>
        <dd>{formatTimestamp(project.createdAt, user!.timeZone)}</dd>
        <dt>Zuletzt geändert</dt>
        <dd>{formatTimestamp(project.updatedAt, user!.timeZone)}</dd>
      </dl>
      <div className="actions">
        <Link to={`/projekte/${project.id}/bearbeiten`} className="button-link secondary">
          Bearbeiten
        </Link>
        <button
          type="button"
          ref={deleteButtonRef}
          className="danger"
          onClick={() => {
            setDeleteConflict(false)
            setDialogOpen(true)
          }}
        >
          Löschen
        </button>
        <Link to="/projekte">Zurück zur Projektliste</Link>
      </div>
      {dialogOpen && (
        <DeleteProjectDialog
          project={project}
          onClose={() => setDialogOpen(false)}
          onDeleted={() => navigate('/projekte', { state: { notice: `„${project.name}“ wurde gelöscht.` } })}
          onConflict={() => {
            setDeleteConflict(true)
            reload()
          }}
          onGone={() => navigate('/projekte', { state: { notice: 'Das Projekt war bereits gelöscht.' } })}
          returnFocusRef={deleteButtonRef}
        />
      )}
    </>
  )
}
