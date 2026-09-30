import { Link } from 'react-router-dom'
import type { Project } from '../../api/projects'

export function ProjectListItem({ project }: { project: Project }) {
  return (
    <li className="task">
      <Link to={`/projekte/${project.id}`} className="task-title">
        {project.name}
      </Link>
      {project.description && <p className="task-meta">{project.description}</p>}
    </li>
  )
}
