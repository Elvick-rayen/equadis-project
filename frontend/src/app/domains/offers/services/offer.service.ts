import { HttpClient, HttpParams } from '@angular/common/http';
import { computed, effect, inject, Injectable, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { Observable, tap } from 'rxjs';
import { API_BASE, Page } from '@shared/http/api';
import { NotificationService } from '@shared/notifications/notification.service';
import { PersonaStore } from '@shared/persona/persona.store';
import { Offer, OfferDraft, OfferSearch } from '../models/offer.model';

/** The signed-in supplier's offers (/api/suppliers/{supplierId}/offers), as signals. */
@Injectable({ providedIn: 'root' })
export class OfferService {
  private readonly _http = inject(HttpClient);
  private readonly _personas = inject(PersonaStore);
  private readonly _notifications = inject(NotificationService);

  /** Set by the offers table; nothing is fetched until it is. */
  private readonly _search = signal<OfferSearch | undefined>(undefined);
  readonly search = this._search.asReadonly();

  private readonly pageResource = rxResource({
    params: () => this._search(),
    stream: ({ params }) => this._http.get<Page<Offer>>(offersUrl(params.supplierId), { params: toHttpParams(params) }),
  });

  readonly offers = computed<Offer[]>(() => (this.pageResource.hasValue() ? this.pageResource.value().content : []));
  readonly totalRecords = computed(() => (this.pageResource.hasValue() ? this.pageResource.value().totalElements : 0));
  readonly loading = this.pageResource.isLoading;
  /** The supplier acted as; null for other personas. */
  readonly supplierId = this._personas.supplierId;

  constructor() {
    effect(() => {
      const error = this.pageResource.error();
      if (error) this._notifications.error(error);
    });
  }

  setSearch(search: Omit<OfferSearch, 'supplierId'>): void {
    const supplierId = this.supplierId();
    if (supplierId !== null) {
      this._search.set({ ...search, supplierId });
    }
  }

  create(draft: OfferDraft): Observable<Offer> {
    return this._http.post<Offer>(offersUrl(this.requireSupplier()), draft).pipe(tap(() => this.pageResource.reload()));
  }

  edit(offerId: number, draft: OfferDraft): Observable<Offer> {
    return this._http
      .put<Offer>(`${offersUrl(this.requireSupplier())}/${offerId}`, draft)
      .pipe(tap(() => this.pageResource.reload()));
  }

  publish(offerId: number): Observable<Offer> {
    return this._http
      .post<Offer>(`${offersUrl(this.requireSupplier())}/${offerId}/publish`, {})
      .pipe(tap(() => this.pageResource.reload()));
  }

  /** BR-08: also cancels the offer's subscriptions (done by the backend). */
  close(offerId: number): Observable<Offer> {
    return this._http
      .post<Offer>(`${offersUrl(this.requireSupplier())}/${offerId}/close`, {})
      .pipe(tap(() => this.pageResource.reload()));
  }

  private requireSupplier(): number {
    const supplierId = this.supplierId();
    if (supplierId === null) {
      throw new Error('Offers are managed while acting as a supplier');
    }
    return supplierId;
  }
}

function offersUrl(supplierId: number): string {
  return `${API_BASE}/suppliers/${supplierId}/offers`;
}

function toHttpParams(search: OfferSearch): HttpParams {
  let params = new HttpParams().set('page', search.page).set('size', search.size);
  if (search.status) {
    params = params.set('status', search.status);
  }
  if (search.sort) {
    params = params.set('sort', `${search.sort.field},${search.sort.direction}`);
  }
  return params;
}
