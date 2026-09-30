import type { Project, ProjectInput } from '../../api/projects'

export interface ProjectDraft {
  name: string
  description: string
}

export function emptyDraft(): ProjectDraft {
  return { name: '', description: '' }
}

export function toDraft(project: Project): ProjectDraft {
  return { name: project.name, description: project.description ?? '' }
}

export function toInput(draft: ProjectDraft): ProjectInput {
  const description = draft.description.trim().length === 0 ? null : draft.description
  return { name: draft.name.trim(), description }
}

const COMPARABLE_FIELDS: (keyof ProjectDraft)[] = ['name', 'description']

/** Liefert die Felder, in denen sich zwei Entwürfe unterscheiden (für die Konfliktanzeige, US-120). */
export function diffDrafts(a: ProjectDraft, b: ProjectDraft): string[] {
  return COMPARABLE_FIELDS.filter((field) => a[field] !== b[field])
}
