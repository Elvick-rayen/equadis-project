import { HttpClient } from '@angular/common/http';
import { computed, effect, inject, Injectable } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { Observable, tap } from 'rxjs';
import { API_BASE } from '@shared/http/api';
import { NotificationService } from '@shared/notifications/notification.service';
import { PersonaStore } from '@shared/persona/persona.store';
import { AvailableOffer, Subscription } from '../models/subscription.model';

/**
 * The signed-in retailer's available offers and subscriptions, as signals. Both lists are
 * reloaded after subscribing or unsubscribing, so the two pages always agree.
 */
@Injectable({ providedIn: 'root' })
export class SubscriptionService {
  private readonly _http = inject(HttpClient);
  private readonly _personas = inject(PersonaStore);
  private readonly _notifications = inject(NotificationService);

  /** The retailer acted as; nothing is fetched for other personas. */
  readonly retailerId = this._personas.retailerId;

  private readonly availableResource = rxResource({
    params: () => this.retailerId() ?? undefined,
    stream: ({ params: retailerId }) =>
      this._http.get<AvailableOffer[]>(`${API_BASE}/retailers/${retailerId}/available-offers`),
  });

  private readonly subscriptionsResource = rxResource({
    params: () => this.retailerId() ?? undefined,
    stream: ({ params: retailerId }) => this._http.get<Subscription[]>(subscriptionsUrl(retailerId)),
  });

  readonly availableOffers = computed(() => (this.availableResource.hasValue() ? this.availableResource.value() : []));
  readonly subscriptions = computed(() => (this.subscriptionsResource.hasValue() ? this.subscriptionsResource.value() : []));
  readonly loadingOffers = this.availableResource.isLoading;
  readonly loadingSubscriptions = this.subscriptionsResource.isLoading;

  /** Not-cancelled subscription per offer id, to show Subscribe or Unsubscribe. */
  readonly currentByOffer = computed(
    () => new Map(this.subscriptions().filter((s) => s.state !== 'CANCELLED').map((s) => [s.offerId, s])),
  );

  constructor() {
    effect(() => this.reportError(this.availableResource.error()));
    effect(() => this.reportError(this.subscriptionsResource.error()));
  }

  /** BR-10 and BR-11 are checked by the API. */
  subscribe(offerId: number): Observable<Subscription> {
    return this._http
      .post<Subscription>(subscriptionsUrl(this.requireRetailer()), { offerId })
      .pipe(tap(() => this.reload()));
  }

  unsubscribe(subscriptionId: number): Observable<void> {
    return this._http
      .delete<void>(`${subscriptionsUrl(this.requireRetailer())}/${subscriptionId}`)
      .pipe(tap(() => this.reload()));
  }

  /**
   * Also called when a page opens: offers, partnerships and statuses change while other
   * personas are used (BR-10, BR-12), and the retailer id alone would not trigger a fetch.
   */
  reload(): void {
    this.availableResource.reload();
    this.subscriptionsResource.reload();
  }

  private requireRetailer(): number {
    const retailerId = this.retailerId();
    if (retailerId === null) {
      throw new Error('Subscriptions are managed while acting as a retailer');
    }
    return retailerId;
  }

  private reportError(error: unknown): void {
    if (error) {
      this._notifications.error(error);
    }
  }
}

function subscriptionsUrl(retailerId: number): string {
  return `${API_BASE}/retailers/${retailerId}/subscriptions`;
}
