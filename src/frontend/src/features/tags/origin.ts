import type { Tag } from '../../api/tags'

/**
 * US-181: Herkunft eines Tags in Worten, z. B. „persönlich“ oder „Projekt „Kaffee“ (Stefan Moers)“. Die Angabe
 * steht im Text und ist damit auch ohne Farbe und für Screenreader lesbar.
 */
export function tagOrigin(tag: Tag, currentUserId: string | undefined): string {
  if (tag.scope === 'PERSONAL') return 'persönlich'
  const project = tag.projectName ? `Projekt „${tag.projectName}“` : 'Projekt'
  const owner =
    tag.projectOwnerId && tag.projectOwnerId === currentUserId ? 'du' : (tag.projectOwnerDisplayName ?? 'unbekannt')
  return `${project} (${owner})`
}

const nameKey = (name: string) => name.trim().toLocaleLowerCase('de')

/** Namen, die in der Liste mehrfach vorkommen (ohne Beachtung der Großschreibung). */
function ambiguousNames(tags: Tag[]): Set<string> {
  const seen = new Set<string>()
  const ambiguous = new Set<string>()
  for (const tag of tags) {
    const key = nameKey(tag.name)
    if (seen.has(key)) ambiguous.add(key)
    seen.add(key)
  }
  return ambiguous
}

export interface TagLabel {
  name: string
  /** Nur bei mehrfach vorkommenden Namen gesetzt; sonst bleibt der Eintrag kurz. */
  origin?: string
  /** Name, bei Bedarf mit Herkunft: „Backen · Projekt „Kaffee“ (Stefan Moers)“. */
  text: string
}

/** Beschriftungen für Auswahllisten: Die Herkunft erscheint nur dort, wo gleichnamige Tags vorkommen. */
export function tagLabels(tags: Tag[], currentUserId: string | undefined): Map<string, TagLabel> {
  const ambiguous = ambiguousNames(tags)
  return new Map(
    tags.map((tag) => {
      const origin = ambiguous.has(nameKey(tag.name)) ? tagOrigin(tag, currentUserId) : undefined
      return [tag.id, { name: tag.name, origin, text: origin ? `${tag.name} · ${origin}` : tag.name }]
    }),
  )
}
