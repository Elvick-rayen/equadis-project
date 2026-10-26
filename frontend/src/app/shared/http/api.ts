/** Base path of the backend API. The dev server proxies it to Spring Boot (see proxy.conf.json). */
export const API_BASE = '/api';

export function isApiRequest(url: string): boolean {
  return url === API_BASE || url.startsWith(`${API_BASE}/`);
}

/** Paginated result, as returned by the backend's PageResponse. */
export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export type SortDirection = 'asc' | 'desc';

export interface PageRequest<S extends string = string> {
  page: number;
  size: number;
  sort?: { field: S; direction: SortDirection };
}
