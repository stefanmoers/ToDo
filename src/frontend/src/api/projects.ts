import { api } from './client'
import { DEFAULT_PAGE_SIZE, type Page } from './pagination'

export interface Project {
  id: string
  name: string
  description?: string
  ownerId: string
  ownerDisplayName?: string
  createdAt: string
  updatedAt: string
  version: number
}

export interface ProjectInput {
  name: string
  description: string | null
}

export interface ProjectUpdate extends ProjectInput {
  version: number
}

export const listProjects = (page: number, size = DEFAULT_PAGE_SIZE) =>
  api<Page<Project>>('GET', `/api/v1/projects?page=${page}&size=${size}`)

export const getProject = (id: string) => api<Project>('GET', `/api/v1/projects/${encodeURIComponent(id)}`)

export const createProject = (input: ProjectInput) => api<Project>('POST', '/api/v1/projects', input)

export const updateProject = (id: string, update: ProjectUpdate) =>
  api<Project>('PUT', `/api/v1/projects/${encodeURIComponent(id)}`, update)

export const deleteProject = (id: string, version: number) =>
  api('DELETE', `/api/v1/projects/${encodeURIComponent(id)}?version=${version}`)

export interface ProjectMember {
  userId: string
  displayName: string
  addedAt: string
}

/** US-060: für Eigentümer und aktuelle Mitglieder gleichermaßen sichtbar. */
export const listMembers = (projectId: string) =>
  api<ProjectMember[]>('GET', `/api/v1/projects/${encodeURIComponent(projectId)}/members`)

/** US-060: nur der Eigentümer, über die exakte Login-E-Mail eines aktiven Kontos. */
export const addMember = (projectId: string, email: string) =>
  api<ProjectMember>('POST', `/api/v1/projects/${encodeURIComponent(projectId)}/members`, { email })

/** US-061: nur der Eigentümer; entfernt zugleich Zuweisungen dieses Mitglieds in diesem Projekt. */
export const removeMember = (projectId: string, userId: string) =>
  api('DELETE', `/api/v1/projects/${encodeURIComponent(projectId)}/members/${encodeURIComponent(userId)}`)
