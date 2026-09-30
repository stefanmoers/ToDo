import { useEffect, useRef, type ReactNode, type RefObject } from 'react'

/**
 * Modaler Dialog auf Basis des nativen `<dialog>`-Elements (`showModal()`): der Browser übernimmt Fokus-Falle,
 * Top-Layer und Escape-Behandlung. Diese Komponente wird nur gerendert, solange sie offen ist.
 */
export function Dialog({
  titleId,
  title,
  describedById,
  initialFocusRef,
  returnFocusRef,
  onClose,
  children,
}: {
  titleId: string
  title: string
  describedById?: string
  initialFocusRef: RefObject<HTMLElement | null>
  returnFocusRef?: RefObject<HTMLElement | null>
  onClose: () => void
  children: ReactNode
}) {
  const dialogRef = useRef<HTMLDialogElement>(null)

  useEffect(() => {
    const dialog = dialogRef.current
    if (!dialog) return
    if (!dialog.open) dialog.showModal()
    initialFocusRef.current?.focus()
    const returnEl = returnFocusRef?.current
    return () => {
      dialog.close()
      returnEl?.focus()
    }
    // Läuft bewusst nur beim Mounten/Unmounten: die Komponente existiert nur, solange der Dialog offen ist.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  return (
    <dialog
      ref={dialogRef}
      aria-labelledby={titleId}
      aria-describedby={describedById}
      onCancel={(e) => {
        e.preventDefault()
        onClose()
      }}
      onKeyDown={(e) => {
        if (e.key === 'Escape') {
          e.preventDefault()
          onClose()
        }
      }}
    >
      <h2 id={titleId}>{title}</h2>
      {children}
    </dialog>
  )
}
