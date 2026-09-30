import { Link } from 'react-router-dom'
import { usePageTitle } from '../../hooks/usePageTitle'

export function TaskNotFound() {
  usePageTitle('Aufgabe nicht gefunden')
  return (
    <>
      <h1>Aufgabe nicht gefunden</h1>
      <p>Diese Aufgabe gibt es nicht (mehr) oder du hast keinen Zugriff darauf.</p>
      <p>
        <Link to="/">Zur Aufgabenliste</Link>
      </p>
    </>
  )
}
