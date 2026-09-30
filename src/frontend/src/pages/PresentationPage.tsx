import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { slides } from '../features/presentation/slides'
import '../features/presentation/presentation.css'

const TOTAL = slides.length

function parseIndex(param: string | undefined): number {
  const n = Number.parseInt(param ?? '1', 10)
  if (!Number.isFinite(n)) return 0
  return Math.min(Math.max(n, 1), TOTAL) - 1
}

function isTypingTarget(target: EventTarget | null): boolean {
  if (!(target instanceof HTMLElement)) return false
  return target.isContentEditable || ['INPUT', 'TEXTAREA', 'SELECT'].includes(target.tagName)
}

/**
 * Projektpräsentation unter /präsentation. Die Seite ist öffentlich und braucht keine API.
 * Bedienung: Pfeil-Schaltflächen, Pfeiltasten, Pos1/Ende, N (Notizen), F (Vollbild).
 */
export default function PresentationPage() {
  const { folie } = useParams()
  const navigate = useNavigate()
  const index = parseIndex(folie)
  const slide = slides[index]
  const [notesOpen, setNotesOpen] = useState(false)
  const [fullscreen, setFullscreen] = useState(false)
  const canFullscreen = typeof document !== 'undefined' && document.fullscreenEnabled === true

  const go = useCallback(
    (target: number) => {
      const next = Math.min(Math.max(target, 0), TOTAL - 1)
      if (next !== index) navigate(`/präsentation/${next + 1}`, { replace: true })
    },
    [index, navigate],
  )

  const toggleFullscreen = useCallback(() => {
    if (!canFullscreen) return
    if (document.fullscreenElement) void document.exitFullscreen()
    else void document.documentElement.requestFullscreen().catch(() => undefined)
  }, [canFullscreen])

  useEffect(() => {
    document.title = `${slide.title} (Folie ${index + 1} von ${TOTAL}) – UltimateTodo`
  }, [slide.title, index])

  useEffect(() => {
    const onChange = () => setFullscreen(document.fullscreenElement !== null)
    document.addEventListener('fullscreenchange', onChange)
    return () => document.removeEventListener('fullscreenchange', onChange)
  }, [])

  useEffect(() => {
    function onKey(e: KeyboardEvent) {
      if (e.defaultPrevented || e.altKey || e.ctrlKey || e.metaKey || isTypingTarget(e.target)) return
      switch (e.key) {
        case 'ArrowRight':
        case 'PageDown':
          go(index + 1)
          break
        case 'ArrowLeft':
        case 'PageUp':
          go(index - 1)
          break
        case 'Home':
          go(0)
          break
        case 'End':
          go(TOTAL - 1)
          break
        case 'n':
        case 'N':
          setNotesOpen((open) => !open)
          break
        case 'f':
        case 'F':
          toggleFullscreen()
          break
        default:
          return
      }
      e.preventDefault()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [go, index, toggleFullscreen])

  const atStart = index === 0
  const atEnd = index === TOTAL - 1
  const label = `Folie ${index + 1} von ${TOTAL}: ${slide.title}`

  return (
    <main className="pres" id="main" aria-label="Präsentation">
      <h1 className="visually-hidden">UltimateTodo – Projektvorstellung</h1>
      <div className="pres-viewport">
        <button
          type="button"
          className="pres-nav pres-nav--prev"
          aria-label="Vorherige Folie"
          aria-disabled={atStart}
          onClick={() => go(index - 1)}
        >
          <span aria-hidden="true">←</span>
        </button>
        <div className="pres-stage">
          <div
            className={slide.dark ? 'slide slide--dark' : 'slide slide--light'}
            role="group"
            aria-roledescription="Folie"
            aria-label={label}
          >
            {slide.body}
          </div>
        </div>
        <button
          type="button"
          className="pres-nav pres-nav--next"
          aria-label="Nächste Folie"
          aria-disabled={atEnd}
          onClick={() => go(index + 1)}
        >
          <span aria-hidden="true">→</span>
        </button>
      </div>

      <div className="pres-bar">
        <p className="pres-counter">
          Folie {index + 1} von {TOTAL}
        </p>
        <p className="pres-hint">← → Folie wechseln · Pos1/Ende · N Notizen · F Vollbild</p>
        <div className="pres-actions">
          <button
            type="button"
            className="pres-btn"
            aria-expanded={notesOpen}
            aria-controls="pres-notes"
            onClick={() => setNotesOpen((open) => !open)}
          >
            Sprechernotizen
          </button>
          {canFullscreen && (
            <button type="button" className="pres-btn" onClick={toggleFullscreen}>
              {fullscreen ? 'Vollbild beenden' : 'Vollbild'}
            </button>
          )}
          <Link className="pres-btn pres-btn--link" to="/">
            Zur Anwendung
          </Link>
        </div>
      </div>

      <div id="pres-notes" className="pres-notes" hidden={!notesOpen}>
        <h2 className="pres-notes-title">Sprechernotizen</h2>
        <p>{slide.notes}</p>
      </div>

      <p className="visually-hidden" role="status">
        {label}
      </p>
    </main>
  )
}
