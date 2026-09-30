import { useRef, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { deactivateAdminUser, reactivateAdminUser, type AdminUser } from '../api/admin'
import { ApiError } from '../api/client'
import { Dialog } from '../components/Dialog'
import { Pagination } from '../components/Pagination'
import { useAdminUsers } from '../features/admin/useAdminUsers'
import { usePageTitle } from '../hooks/usePageTitle'

const STATUS_LABELS: Record<AdminUser['status'], string> = { ACTIVE: 'Aktiv', INACTIVE: 'Deaktiviert' }
const ROLE_LABELS: Record<AdminUser['role'], string> = { USER: 'Benutzer', ADMIN: 'Administrator' }

/**
 * US-014: Konten auflisten, deaktivieren, reaktivieren. Zeigt bewusst keine Aufgaben- oder Projektinhalte —
 * Administration ist von deren Berechtigungen unabhängig (docs/DOMAIN_MODEL.md).
 */
export default function AdminUsersPage() {
  usePageTitle('Konten administrieren')
  const [searchParams] = useSearchParams()
  const pageParam = Number.parseInt(searchParams.get('seite') ?? '1', 10)
  const page = Number.isFinite(pageParam) && pageParam > 0 ? pageParam - 1 : 0
  const { state, reload } = useAdminUsers(page)
  const [notice, setNotice] = useState<string>()
  const [rowError, setRowError] = useState<{ id: string; message: string } | undefined>()
  const [busyId, setBusyId] = useState<string | null>(null)
  const [pendingDeactivation, setPendingDeactivation] = useState<AdminUser | null>(null)
  const lastActionButtonRef = useRef<HTMLButtonElement | null>(null)
  const cancelRef = useRef<HTMLButtonElement>(null)

  async function onReactivate(target: AdminUser) {
    setBusyId(target.id)
    setRowError(undefined)
    try {
      await reactivateAdminUser(target.id)
      setNotice(`„${target.displayName}“ wurde reaktiviert.`)
      reload()
    }
    catch (err) {
      setRowError({ id: target.id, message: err instanceof ApiError ? err.message : 'Es ist ein Fehler aufgetreten.' })
    }
    finally {
      setBusyId(null)
    }
  }

  async function onConfirmDeactivate() {
    if (!pendingDeactivation) return
    setBusyId(pendingDeactivation.id)
    setRowError(undefined)
    try {
      await deactivateAdminUser(pendingDeactivation.id)
      setNotice(`„${pendingDeactivation.displayName}“ wurde deaktiviert.`)
      setPendingDeactivation(null)
      reload()
    }
    catch (err) {
      setRowError({
        id: pendingDeactivation.id,
        message: err instanceof ApiError ? err.message : 'Es ist ein Fehler aufgetreten.',
      })
      setPendingDeactivation(null)
    }
    finally {
      setBusyId(null)
    }
  }

  return (
    <>
      <div className="page-heading">
        <div>
          <p className="eyebrow">Verwaltung</p>
          <h1>Konten administrieren</h1>
        </div>
      </div>
      {notice && (
        <p role="status" className="notice">
          {notice}
        </p>
      )}
      {state.kind === 'loading' && <p role="status">Konten werden geladen …</p>}
      {state.kind === 'error' && (
        <div role="alert">
          <p>Die Konten konnten nicht geladen werden. {state.message}</p>
        </div>
      )}
      {state.kind === 'loaded' && (
        <>
          <p id="admin-table-hint" className="hint table-scroll-hint">Die Kontenliste ist seitlich scrollbar. Mit Tab erreichst du die Tabelle.</p>
          {/* Horizontales Scrollen der Tabelle muss auch per Tastatur erreichbar sein. */}
          {/* eslint-disable-next-line jsx-a11y/no-noninteractive-tabindex */}
          <div className="table-scroll" role="region" aria-label="Kontenliste" aria-describedby="admin-table-hint" tabIndex={0}>
            <table>
              <caption className="visually-hidden">Konten – Seite {state.page.page + 1} von {state.page.totalPages}</caption>
              <thead>
                <tr>
                  <th scope="col">E-Mail</th>
                  <th scope="col">Anzeigename</th>
                  <th scope="col">Rolle</th>
                  <th scope="col">Status</th>
                  <th scope="col">Aktion</th>
                </tr>
              </thead>
              <tbody>
                {state.page.items.map((u) => {
                  const busy = busyId === u.id
                  return (
                    <tr key={u.id}>
                      <td>{u.email}</td>
                      <td>{u.displayName}</td>
                      <td>{ROLE_LABELS[u.role]}</td>
                      <td>{STATUS_LABELS[u.status]}</td>
                      <td>
                        {rowError?.id === u.id && (
                          <p role="alert" className="field-error">
                            {rowError.message}
                          </p>
                        )}
                        {u.status === 'ACTIVE' ? (
                          <button
                            type="button"
                            className="danger"
                            disabled={busy}
                            onClick={(e) => {
                              lastActionButtonRef.current = e.currentTarget
                              setRowError(undefined)
                              setPendingDeactivation(u)
                            }}
                          >
                            Deaktivieren<span className="visually-hidden">: {u.displayName}</span>
                          </button>
                        ) : (
                          <button type="button" disabled={busy} onClick={() => onReactivate(u)}>
                            Reaktivieren<span className="visually-hidden">: {u.displayName}</span>
                          </button>
                        )}
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
          <Pagination
            page={state.page.page}
            totalPages={state.page.totalPages}
            label="Seiten der Kontenliste"
            toHref={(p) => `/administration?seite=${p + 1}`}
          />
        </>
      )}
      {pendingDeactivation && (
        <Dialog
          titleId="deactivate-user-title"
          title="Konto deaktivieren?"
          describedById="deactivate-user-description"
          initialFocusRef={cancelRef}
          returnFocusRef={lastActionButtonRef}
          onClose={() => setPendingDeactivation(null)}
        >
          <p id="deactivate-user-description">
            „{pendingDeactivation.displayName}“ verliert sofort den Zugriff; laufende Sitzungen enden spätestens
            bei der nächsten Anfrage. Die Daten des Kontos bleiben erhalten und können jederzeit reaktiviert werden.
          </p>
          <div className="actions">
            <button type="button" ref={cancelRef} className="secondary" onClick={() => setPendingDeactivation(null)}>
              Abbrechen
            </button>
            <button type="button" className="danger" onClick={onConfirmDeactivate}>
              Endgültig deaktivieren
            </button>
          </div>
        </Dialog>
      )}
    </>
  )
}
