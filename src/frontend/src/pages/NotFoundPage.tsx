import { Link } from 'react-router-dom'

export default function NotFoundPage() {
  return (
    <div className="content-card auth-card">
      <h1>Seite nicht gefunden</h1>
      <p>
        Diese Seite gibt es nicht. <Link to="/">Zur Startseite</Link>
      </p>
    </div>
  )
}
