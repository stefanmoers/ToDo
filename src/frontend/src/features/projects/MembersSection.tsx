import { useRef, useState, type FormEvent } from 'react'
import { addMember, removeMember, type ProjectMember } from '../../api/projects'
import { Dialog } from '../../components/Dialog'
import { mapProjectApiError } from './errors'
import { useMembers } from './useMembers'

/** US-060 (Projekt teilen) und US-061 (Mitglied entfernen): nur der Eigentümer verwaltet Mitglieder. */
export function MembersSection({ projectId, isOwner }: { projectId: string; isOwner: boolean }) {
  const { state, reload } = useMembers(projectId)
  const [email, setEmail] = useState('')
  const [addError, setAddError] = useState<string>()
  const [adding, setAdding] = useState(false)
  const [notice, setNotice] = useState<string>()
  const [pendingRemoval, setPendingRemoval] = useState<ProjectMember | null>(null)
  const [removeError, setRemoveError] = useState<string>()
  const [removing, setRemoving] = useState(false)
  const lastRemoveButtonRef = useRef<HTMLButtonElement | null>(null)
  const cancelRef = useRef<HTMLButtonElement>(null)

  async function onAddSubmit(e: FormEvent) {
    e.preventDefault()
    setAdding(true)
    setAddError(undefined)
    setNotice(undefined)
    try {
      const member = await addMember(projectId, email.trim())
      setEmail('')
      setNotice(`„${member.displayName}“ wurde aufgenommen.`)
      reload()
    }
    catch (err) {
      const mapped = mapProjectApiError(err)
      setAddError(mapped.errors.email ?? mapped.general ?? 'Es ist ein Fehler aufgetreten.')
    }
    finally {
      setAdding(false)
    }
  }

  async function onConfirmRemove() {
    if (!pendingRemoval) return
    setRemoving(true)
    setRemoveError(undefined)
    try {
      await removeMember(projectId, pendingRemoval.userId)
      setNotice(`„${pendingRemoval.displayName}“ wurde entfernt.`)
      setPendingRemoval(null)
      reload()
    }
    catch (err) {
      setRemoveError(mapProjectApiError(err).general ?? 'Es ist ein Fehler aufgetreten.')
    }
    finally {
      setRemoving(false)
    }
  }

  return (
    <section aria-labelledby="members-heading">
      <h2 id="members-heading">Mitglieder</h2>
      {notice && (
        <p role="status" className="notice">
          {notice}
        </p>
      )}
      {state.kind === 'loading' && <p role="status">Mitglieder werden geladen …</p>}
      {state.kind === 'error' && (
        <div role="alert">
          <p>Die Mitglieder konnten nicht geladen werden. {state.message}</p>
        </div>
      )}
      {state.kind === 'loaded' && (
        state.members.length === 0 ? (
          <p>Noch keine Mitglieder außer dir.</p>
        ) : (
          <ul className="task-list" aria-label="Mitglieder">
            {state.members.map((m) => (
              <li key={m.userId} className="task">
                <span className="task-title">{m.displayName}</span>
                {isOwner && (
                  <div className="actions">
                    <button
                      type="button"
                      className="danger"
                      onClick={(e) => {
                        lastRemoveButtonRef.current = e.currentTarget
                        setRemoveError(undefined)
                        setPendingRemoval(m)
                      }}
                    >
                      Entfernen<span className="visually-hidden">: {m.displayName}</span>
                    </button>
                  </div>
                )}
              </li>
            ))}
          </ul>
        )
      )}
      {isOwner && (
        <form onSubmit={onAddSubmit} noValidate>
          <div className="field">
            <label htmlFor="member-email">Mitglied per E-Mail aufnehmen</label>
            {addError && (
              <p role="alert" className="field-error">
                {addError}
              </p>
            )}
            <input id="member-email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} />
          </div>
          <button type="submit" disabled={adding}>
            {adding ? 'Wird aufgenommen …' : 'Aufnehmen'}
          </button>
        </form>
      )}
      {pendingRemoval && (
        <Dialog
          titleId="remove-member-title"
          title="Mitglied entfernen?"
          describedById="remove-member-description"
          initialFocusRef={cancelRef}
          returnFocusRef={lastRemoveButtonRef}
          onClose={() => setPendingRemoval(null)}
        >
          <p id="remove-member-description">
            „{pendingRemoval.displayName}“ verliert sofort den Zugriff auf dieses Projekt; eigene Zuweisungen
            darin werden entfernt. Kommentare und Aktivitäten bleiben erhalten.
          </p>
          {removeError && (
            <p role="alert" className="field-error">
              {removeError}
            </p>
          )}
          <div className="actions">
            <button type="button" ref={cancelRef} className="secondary" onClick={() => setPendingRemoval(null)} disabled={removing}>
              Abbrechen
            </button>
            <button type="button" className="danger" onClick={onConfirmRemove} disabled={removing}>
              {removing ? 'Wird entfernt …' : 'Endgültig entfernen'}
            </button>
          </div>
        </Dialog>
      )}
    </section>
  )
}
