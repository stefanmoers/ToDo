import { api } from './client'
import { DEFAULT_PAGE_SIZE, type Page } from './pagination'

export interface Project {
  id: string
  name: string
  description?: string
  ownerId: string
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
