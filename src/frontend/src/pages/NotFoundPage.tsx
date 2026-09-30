import { Link } from 'react-router-dom'

export default function NotFoundPage() {
  return (
    <>
      <h1>Seite nicht gefunden</h1>
      <p>
        Diese Seite gibt es nicht. <Link to="/">Zur Startseite</Link>
      </p>
    </>
  )
}
