import { api } from './client'
import { DEFAULT_PAGE_SIZE, type Page } from './pagination'

/** US-014: Konten aus Administrator-Sicht. Enthält keine Aufgaben- oder Projektinhalte. */
export interface AdminUser {
  id: string
  email: string
  displayName: string
  status: 'ACTIVE' | 'INACTIVE'
  role: 'USER' | 'ADMIN'
  createdAt: string
}

export const listAdminUsers = (page: number, size = DEFAULT_PAGE_SIZE) =>
  api<Page<AdminUser>>('GET', `/api/v1/admin/users?page=${page}&size=${size}`)

export const deactivateAdminUser = (id: string) =>
  api<AdminUser>('POST', `/api/v1/admin/users/${encodeURIComponent(id)}/deactivate`)

export const reactivateAdminUser = (id: string) =>
  api<AdminUser>('POST', `/api/v1/admin/users/${encodeURIComponent(id)}/reactivate`)
