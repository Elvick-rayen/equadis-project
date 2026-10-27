import { HttpClient, HttpParams } from '@angular/common/http';
import { computed, effect, inject, Injectable, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { Observable, tap } from 'rxjs';
import { AuthService } from '@shared/auth/auth.service';
import { API_BASE, Page } from '@shared/http/api';
import { NotificationService } from '@shared/notifications/notification.service';
import { Supplier, SupplierDraft, SupplierSearch, SupplierSummary } from '../models/supplier.model';

const SUPPLIERS_URL = `${API_BASE}/suppliers`;

/**
 * Talks to /api/suppliers and holds the supplier state as signals:
 * - the current table page, re-fetched whenever the search parameters change;
 * - the id/name list used by the "Acting as" selector and the BR-01 form hint.
 */
@Injectable({ providedIn: 'root' })
export class SupplierService {
  private readonly _http = inject(HttpClient);
  private readonly _auth = inject(AuthService);
  private readonly _notifications = inject(NotificationService);

  /** Set by the suppliers table; nothing is fetched until it is. */
  private readonly _search = signal<SupplierSearch | undefined>(undefined);
  readonly search = this._search.asReadonly();

  private readonly pageResource = rxResource({
    params: () => this._search(),
    stream: ({ params }) => this._http.get<Page<Supplier>>(SUPPLIERS_URL, { params: toHttpParams(params) }),
  });

  /** The list is administrator-only, so it is only fetched for sessions that can switch persona. */
  private readonly summariesResource = rxResource({
    params: () => (this._auth.canActAs() ? true : undefined),
    stream: () => this._http.get<SupplierSummary[]>(`${SUPPLIERS_URL}/summaries`),
  });

  readonly suppliers = computed<Supplier[]>(() => (this.pageResource.hasValue() ? this.pageResource.value().content : []));
  readonly totalRecords = computed(() => (this.pageResource.hasValue() ? this.pageResource.value().totalElements : 0));
  readonly loading = this.pageResource.isLoading;

  readonly summaries = computed(() => (this.summariesResource.hasValue() ? this.summariesResource.value() : []));
  readonly summariesLoaded = computed(() => this.summariesResource.hasValue());
  readonly summaryById = computed(() => new Map(this.summaries().map((s) => [s.id, s])));

  constructor() {
    effect(() => this.reportError(this.pageResource.error()));
    effect(() => this.reportError(this.summariesResource.error()));
  }

  setSearch(search: SupplierSearch): void {
    this._search.set(search);
  }

  create(draft: SupplierDraft): Observable<Supplier> {
    return this._http.post<Supplier>(SUPPLIERS_URL, draft).pipe(tap(() => this.refresh()));
  }

  update(id: number, draft: SupplierDraft): Observable<Supplier> {
    return this._http.put<Supplier>(`${SUPPLIERS_URL}/${id}`, draft).pipe(tap(() => this.refresh()));
  }

  delete(id: number): Observable<void> {
    return this._http.delete<void>(`${SUPPLIERS_URL}/${id}`).pipe(
      tap(() => {
        const search = this._search();
        // Deleting the last row of a page: step back one page instead of showing an empty one.
        if (search && search.page > 0 && this.suppliers().length === 1) {
          this._search.set({ ...search, page: search.page - 1 });
          this.summariesResource.reload();
        } else {
          this.refresh();
        }
      }),
    );
  }

  /** BR-01 hint for the form; the API stays the real guard. */
  isNameTaken(name: string, exceptId?: number | null): boolean {
    const normalized = name.trim().toLocaleLowerCase();
    return (
      normalized.length > 0 &&
      this.summaries().some((s) => s.id !== exceptId && s.companyName.trim().toLocaleLowerCase() === normalized)
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
function toHttpParams(search: SupplierSearch): HttpParams {
  let params = new HttpParams().set('page', search.page).set('size', search.size);
  if (search.query?.trim()) {
    params = params.set('q', search.query.trim());
  }
  if (search.sort) {
    params = params.set('sort', `${search.sort.field},${search.sort.direction}`);
  }
  return params;
}
