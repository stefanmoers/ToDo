export interface Page<T> {
  items: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export const DEFAULT_PAGE_SIZE = 25
