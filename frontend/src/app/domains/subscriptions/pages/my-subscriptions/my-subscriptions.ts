import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { TranslocoDirective } from '@jsverse/transloco';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { LanguageStore } from '@shared/i18n/language.store';
import { NotificationService } from '@shared/notifications/notification.service';
import { DiscountPipe } from '@shared/ui/discount.pipe';
import { STATE_SEVERITY, Subscription, SubscriptionState } from '../../models/subscription.model';
import { SubscriptionService } from '../../services/subscription.service';

/** Retailer → My subscriptions: state Active, Suspended (BR-12) or Cancelled, with the cause. */
@Component({
  selector: 'app-my-subscriptions',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoDirective, DatePipe, TableModule, TagModule, ButtonModule, DiscountPipe],
  templateUrl: './my-subscriptions.html',
})
export class MySubscriptions {
  protected readonly subscriptionService = inject(SubscriptionService);
  protected readonly language = inject(LanguageStore).language;
  private readonly _notifications = inject(NotificationService);

  protected severityOf(state: SubscriptionState) {
    return STATE_SEVERITY[state];
  }
  protected readonly busyId = signal<number | null>(null);

  protected unsubscribe(subscription: Subscription): void {
    this.busyId.set(subscription.id);
    this.subscriptionService.unsubscribe(subscription.id).subscribe({
      next: () => {
        this.busyId.set(null);
        this._notifications.success('subscriptions.unsubscribed', { title: subscription.offerTitle });
      },
      error: (error) => {
        this.busyId.set(null);
        this._notifications.error(error);
      },
    });
  }
}
