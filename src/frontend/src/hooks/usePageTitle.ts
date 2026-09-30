import { useEffect } from 'react'

/** Setzt einen aussagekräftigen Seitentitel (WCAG 2.4.2). */
export function usePageTitle(title: string) {
  useEffect(() => {
    document.title = `${title} – UltimateTodo`
  }, [title])
}
