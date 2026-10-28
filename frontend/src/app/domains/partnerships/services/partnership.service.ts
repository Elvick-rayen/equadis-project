import { HttpClient, HttpParams } from '@angular/common/http';
import { computed, effect, inject, Injectable, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { map, Observable, tap } from 'rxjs';
import { API_BASE, Page } from '@shared/http/api';
import { NotificationService } from '@shared/notifications/notification.service';
import {
  CompanyOption,
  EXPIRING_WITHIN_DAYS,
  Partnership,
  PartnershipDraft,
  PartnershipSearch,
} from '../models/partnership.model';

const PARTNERSHIPS_URL = `${API_BASE}/partnerships`;

/**
 * Talks to /api/partnerships and holds the partnership state as signals. The supplier and
 * retailer pickers call the companies' summary endpoints directly: a domain never imports
 * another domain, but any domain may call any API.
 */
@Injectable({ providedIn: 'root' })
export class PartnershipService {
  private readonly _http = inject(HttpClient);
  private readonly _notifications = inject(NotificationService);

  /** Set by the partnerships table; nothing is fetched until it is. */
  private readonly _search = signal<PartnershipSearch | undefined>(undefined);
  readonly search = this._search.asReadonly();

  private readonly pageResource = rxResource({
    params: () => this._search(),
    stream: ({ params }) => this._http.get<Page<Partnership>>(PARTNERSHIPS_URL, { params: toHttpParams(params) }),
  });

  /** Picker options, loaded the first time the "New" dialog asks for them. */
  private readonly _optionsWanted = signal(false);

  private readonly suppliersResource = rxResource({
    params: () => (this._optionsWanted() ? true : undefined),
    stream: () =>
      this._http
        .get<{ id: number; companyName: string }[]>(`${API_BASE}/suppliers/summaries`)
        .pipe(map((list) => list.map((s): CompanyOption => ({ id: s.id, name: s.companyName })))),
  });

  private readonly retailersResource = rxResource({
    params: () => (this._optionsWanted() ? true : undefined),
    stream: () =>
      this._http
        .get<{ id: number; storeName: string }[]>(`${API_BASE}/retailers/summaries`)
        .pipe(map((list) => list.map((r): CompanyOption => ({ id: r.id, name: r.storeName })))),
  });

  readonly partnerships = computed<Partnership[]>(() =>
    this.pageResource.hasValue() ? this.pageResource.value().content : [],
  );
  readonly totalRecords = computed(() => (this.pageResource.hasValue() ? this.pageResource.value().totalElements : 0));
  readonly loading = this.pageResource.isLoading;

  readonly supplierOptions = computed(() => (this.suppliersResource.hasValue() ? this.suppliersResource.value() : []));
  readonly retailerOptions = computed(() => (this.retailersResource.hasValue() ? this.retailersResource.value() : []));

  constructor() {
    effect(() => this.reportError(this.pageResource.error()));
    effect(() => this.reportError(this.suppliersResource.error()));
    effect(() => this.reportError(this.retailersResource.error()));
  }

  setSearch(search: PartnershipSearch): void {
    this._search.set(search);
  }

  loadCompanyOptions(): void {
    this._optionsWanted.set(true);
  }

  /** With its renewal history. */
  get(id: number): Observable<Partnership> {
    return this._http.get<Partnership>(`${PARTNERSHIPS_URL}/${id}`);
  }

  create(draft: PartnershipDraft): Observable<Partnership> {
    return this._http.post<Partnership>(PARTNERSHIPS_URL, draft).pipe(tap(() => this.pageResource.reload()));
  }

  renew(id: number, newEndDate: string): Observable<Partnership> {
    return this._http
      .post<Partnership>(`${PARTNERSHIPS_URL}/${id}/renewals`, { newEndDate })
      .pipe(tap(() => this.pageResource.reload()));
  }

  /** BR-07: also cancels the retailer's subscriptions to the supplier's offers (done by the backend). */
  terminate(id: number, reason: string): Observable<Partnership> {
    return this._http
      .post<Partnership>(`${PARTNERSHIPS_URL}/${id}/termination`, { reason })
      .pipe(tap(() => this.pageResource.reload()));
  }

  private reportError(error: unknown): void {
    if (error) {
      this._notifications.error(error);
    }
  }
}

function toHttpParams(search: PartnershipSearch): HttpParams {
  let params = new HttpParams().set('page', search.page).set('size', search.size);
  if (search.filter === 'EXPIRING') {
    params = params.set('expiringWithinDays', EXPIRING_WITHIN_DAYS);
  } else if (search.filter) {
    params = params.set('status', search.filter);
  }
  if (search.sort) {
    params = params.set('sort', `${search.sort.field},${search.sort.direction}`);
  }
  return params;
}
