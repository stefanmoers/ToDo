import { api } from './client'

export type TagScope = 'PERSONAL' | 'PROJECT'

export interface Tag {
  id: string
  name: string
  scope: TagScope
  projectId?: string
  createdAt: string
}

export interface TagInput {
  name: string
  projectId: string | null
}

export const listTags = () => api<Tag[]>('GET', '/api/v1/tags')

export const createTag = (input: TagInput) => api<Tag>('POST', '/api/v1/tags', input)

export const renameTag = (id: string, name: string) => api<Tag>('PUT', `/api/v1/tags/${encodeURIComponent(id)}`, { name })

export const deleteTag = (id: string) => api('DELETE', `/api/v1/tags/${encodeURIComponent(id)}`)
