import { Link } from 'react-router-dom'

/** Barrierefreie Seiten-Navigation (0-basiert); rendert nichts bei nur einer Seite. */
export function Pagination({
  page,
  totalPages,
  label,
  toHref,
}: {
  page: number
  totalPages: number
  label: string
  toHref: (page: number) => string
}) {
  if (totalPages <= 1) return null
  return (
    <nav aria-label={label} className="pagination">
      <ul>
        <li>{page > 0 && <Link to={toHref(page - 1)}>Vorherige Seite</Link>}</li>
        <li>
          Seite {page + 1} von {totalPages}
        </li>
        <li>{page < totalPages - 1 && <Link to={toHref(page + 1)}>Nächste Seite</Link>}</li>
      </ul>
    </nav>
  )
}
