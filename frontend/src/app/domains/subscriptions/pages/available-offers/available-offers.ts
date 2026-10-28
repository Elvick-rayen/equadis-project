import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { TranslocoDirective } from '@jsverse/transloco';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { LanguageStore } from '@shared/i18n/language.store';
import { NotificationService } from '@shared/notifications/notification.service';
import { DiscountPipe } from '@shared/ui/discount.pipe';
import { AvailableOffer } from '../../models/subscription.model';
import { SubscriptionService } from '../../services/subscription.service';

/** Retailer → Available offers: offers from active partners (BR-10), with Subscribe / Unsubscribe. */
@Component({
  selector: 'app-available-offers',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoDirective, DatePipe, TableModule, ButtonModule, DiscountPipe],
  templateUrl: './available-offers.html',
})
export class AvailableOffers {
  protected readonly subscriptionService = inject(SubscriptionService);
  protected readonly language = inject(LanguageStore).language;
  private readonly _notifications = inject(NotificationService);

  /** Offer whose button is waiting for the API. */
  protected readonly busyOfferId = signal<number | null>(null);

  protected subscribe(offer: AvailableOffer): void {
    this.busyOfferId.set(offer.id);
    this.subscriptionService.subscribe(offer.id).subscribe({
      next: () => {
        this.busyOfferId.set(null);
        this._notifications.success('subscriptions.subscribed', { title: offer.title });
      },
      error: (error) => {
        this.busyOfferId.set(null);
        this._notifications.error(error);
      },
    });
  }

  protected unsubscribe(offer: AvailableOffer): void {
    const subscription = this.subscriptionService.currentByOffer().get(offer.id);
    if (!subscription) return;
    this.busyOfferId.set(offer.id);
    this.subscriptionService.unsubscribe(subscription.id).subscribe({
      next: () => {
        this.busyOfferId.set(null);
        this._notifications.success('subscriptions.unsubscribed', { title: offer.title });
      },
      error: (error) => {
        this.busyOfferId.set(null);
        this._notifications.error(error);
      },
    });
  }
}
