import { HttpClient, HttpParams } from '@angular/common/http';
import { computed, effect, inject, Injectable, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { Observable, tap } from 'rxjs';
import { AuthService } from '@shared/auth/auth.service';
import { API_BASE, Page } from '@shared/http/api';
import { NotificationService } from '@shared/notifications/notification.service';
import { Retailer, RetailerDraft, RetailerSearch, RetailerSummary } from '../models/retailer.model';

const RETAILERS_URL = `${API_BASE}/retailers`;

/**
 * Talks to /api/retailers and holds the retailer state as signals:
 * - the current table page, re-fetched whenever the search parameters change;
 * - the id/name list used by the administrator's "Acting as" selector.
 */
@Injectable({ providedIn: 'root' })
export class RetailerService {
  private readonly _http = inject(HttpClient);
  private readonly _auth = inject(AuthService);
  private readonly _notifications = inject(NotificationService);

  /** Set by the retailers table; nothing is fetched until it is. */
  private readonly _search = signal<RetailerSearch | undefined>(undefined);
  readonly search = this._search.asReadonly();

  private readonly pageResource = rxResource({
    params: () => this._search(),
    stream: ({ params }) => this._http.get<Page<Retailer>>(RETAILERS_URL, { params: toHttpParams(params) }),
  });

  /** The list is administrator-only, so it is only fetched for sessions that can switch persona. */
  private readonly summariesResource = rxResource({
    params: () => (this._auth.canActAs() ? true : undefined),
    stream: () => this._http.get<RetailerSummary[]>(`${RETAILERS_URL}/summaries`),
  });

  readonly retailers = computed<Retailer[]>(() => (this.pageResource.hasValue() ? this.pageResource.value().content : []));
  readonly totalRecords = computed(() => (this.pageResource.hasValue() ? this.pageResource.value().totalElements : 0));
  readonly loading = this.pageResource.isLoading;

  readonly summaries = computed(() => (this.summariesResource.hasValue() ? this.summariesResource.value() : []));

  constructor() {
    effect(() => this.reportError(this.pageResource.error()));
    effect(() => this.reportError(this.summariesResource.error()));
  }

  setSearch(search: RetailerSearch): void {
    this._search.set(search);
  }

  create(draft: RetailerDraft): Observable<Retailer> {
    return this._http.post<Retailer>(RETAILERS_URL, draft).pipe(tap(() => this.refresh()));
  }

  update(id: number, draft: RetailerDraft): Observable<Retailer> {
    return this._http.put<Retailer>(`${RETAILERS_URL}/${id}`, draft).pipe(tap(() => this.refresh()));
  }

  delete(id: number): Observable<void> {
    return this._http.delete<void>(`${RETAILERS_URL}/${id}`).pipe(
      tap(() => {
        const search = this._search();
        // Deleting the last row of a page: step back one page instead of showing an empty one.
        if (search && search.page > 0 && this.retailers().length === 1) {
          this._search.set({ ...search, page: search.page - 1 });
          this.summariesResource.reload();
        } else {
          this.refresh();
        }
      }),
    );
  }

  private refresh(): void {
    this.pageResource.reload();
    this.summariesResource.reload();
  }

  private reportError(error: unknown): void {
    if (error) {
      this._notifications.error(error);
    }
  }
}

/** page, size, optional q and sort=field,direction, as the API expects. */
function toHttpParams(search: RetailerSearch): HttpParams {
  let params = new HttpParams().set('page', search.page).set('size', search.size);
  if (search.query?.trim()) {
    params = params.set('q', search.query.trim());
  }
  if (search.sort) {
    params = params.set('sort', `${search.sort.field},${search.sort.direction}`);
  }
  return params;
}
