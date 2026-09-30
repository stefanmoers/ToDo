import { Link } from 'react-router-dom'
import { usePageTitle } from '../../hooks/usePageTitle'

export function ProjectNotFound() {
  usePageTitle('Projekt nicht gefunden')
  return (
    <>
      <h1>Projekt nicht gefunden</h1>
      <p>Dieses Projekt gibt es nicht (mehr) oder du hast keinen Zugriff darauf.</p>
      <p>
        <Link to="/projekte">Zur Projektliste</Link>
      </p>
    </>
  )
}
