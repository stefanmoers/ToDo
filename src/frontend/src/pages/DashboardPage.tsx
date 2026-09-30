import { Link } from 'react-router-dom'
import { useAuth } from '../features/auth/AuthContext'
import { standardViewParams, todayInZone } from '../features/tasks/standardViews'
import { useTaskSummary } from '../features/tasks/useTaskSummary'
import { usePageTitle } from '../hooks/usePageTitle'

/** US-110: fünf Zähler, jeder mit Verweis auf dieselbe Liste, die ihn ergäbe (gleiche Rechte-/Zeitregeln). */
export default function DashboardPage() {
  usePageTitle('Übersicht')
  const { user } = useAuth()
  const timeZone = user!.timeZone
  const today = todayInZone(timeZone)
  const { state, reload } = useTaskSummary(today)

  const openParams = new URLSearchParams()
  openParams.set('status', 'OPEN')
  openParams.append('status', 'IN_PROGRESS')

  const highPriorityOpenParams = new URLSearchParams(openParams)
  highPriorityOpenParams.set('priority', 'HIGH')
  highPriorityOpenParams.append('priority', 'URGENT')

  return (
    <>
      <div className="page-heading">
        <div>
          <p className="eyebrow">Dein Arbeitsbereich</p>
          <h1>Übersicht</h1>
          <p>Hallo {user?.displayName}! Jede Zahl führt zur passenden Aufgabenliste.</p>
        </div>
      </div>
      {state.kind === 'loading' && <p role="status">Übersicht wird geladen …</p>}
      {state.kind === 'error' && (
        <div role="alert">
          <p>Die Übersicht konnte nicht geladen werden. {state.message}</p>
          <button type="button" onClick={reload}>
            Erneut versuchen
          </button>
        </div>
      )}
      {state.kind === 'loaded' && (
        <ul className="dashboard-counters">
          <li>
            <Link to={`/?${openParams.toString()}`}>
              <strong>{state.summary.openCount}</strong> nicht erledigte Aufgaben
            </Link>
          </li>
          <li>
            <Link to={`/?${standardViewParams('today', timeZone).toString()}`}>
              <strong>{state.summary.dueTodayCount}</strong> heute fällig
            </Link>
          </li>
          <li>
            <Link to={`/?${standardViewParams('overdue', timeZone).toString()}`}>
              <strong>{state.summary.overdueCount}</strong> überfällig
            </Link>
          </li>
          <li>
            <Link to={`/?${standardViewParams('done', timeZone).toString()}`}>
              <strong>{state.summary.doneCount}</strong> erledigt
            </Link>
          </li>
          <li>
            <Link to={`/?${highPriorityOpenParams.toString()}`}>
              <strong>{state.summary.highPriorityOpenCount}</strong> offen mit hoher oder dringender Priorität
            </Link>
          </li>
        </ul>
      )}
    </>
  )
}
