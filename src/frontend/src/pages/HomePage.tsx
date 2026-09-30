import { useEffect, useState } from 'react'
import { fetchReadiness, type Readiness } from '../api/health'
import { useAuth } from '../features/auth/AuthContext'
import { usePageTitle } from '../hooks/usePageTitle'

const LABELS: Record<Readiness | 'LOADING', string> = {
  LOADING: 'Systemstatus wird geprüft …',
  UP: 'Das System ist bereit.',
  DOWN: 'Das System ist zurzeit nicht bereit.',
  UNKNOWN: 'Der Systemstatus konnte nicht abgefragt werden.',
}

export default function HomePage() {
  usePageTitle('Start')
  const { user } = useAuth()
  const [status, setStatus] = useState<Readiness | 'LOADING'>('LOADING')

  useEffect(() => {
    const controller = new AbortController()
    fetchReadiness(controller.signal).then((result) => {
      if (!controller.signal.aborted) setStatus(result)
    })
    return () => controller.abort()
  }, [])

  return (
    <>
      <h1>UltimateTodo</h1>
      <p>Hallo {user?.displayName}! Deine Aufgaben folgen im nächsten Meilenstein.</p>
      <p role="status" data-testid="system-status">
        {LABELS[status]}
      </p>
    </>
  )
}
