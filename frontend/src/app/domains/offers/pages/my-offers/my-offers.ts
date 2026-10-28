import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { translateObjectSignal, TranslocoDirective, TranslocoService } from '@jsverse/transloco';
import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { SelectModule } from 'primeng/select';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { LanguageStore } from '@shared/i18n/language.store';
import { NotificationService } from '@shared/notifications/notification.service';
import { DiscountPipe } from '@shared/ui/discount.pipe';
import { OfferFormDialog } from '../../components/offer-form-dialog/offer-form-dialog';
import { Offer, OFFER_STATUS_SEVERITY, OFFER_STATUSES, OfferSortField, OfferStatus } from '../../models/offer.model';
import { OfferService } from '../../services/offer.service';

const PAGE_SIZE = 10;
const SORT_FIELDS: readonly OfferSortField[] = ['reference', 'title', 'validFrom', 'validTo', 'status'];

type Labels = Record<string, string> | undefined;

/** Supplier → My offers: status tag, create/edit dialog for drafts, Publish and Close (BR-08). */
@Component({
  selector: 'app-my-offers',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoDirective, FormsModule, DatePipe, TableModule, TagModule, ButtonModule, SelectModule, DiscountPipe, OfferFormDialog],
  templateUrl: './my-offers.html',
})
export class MyOffers {
  protected readonly offerService = inject(OfferService);
  protected readonly language = inject(LanguageStore).language;
  private readonly _confirmation = inject(ConfirmationService);
  private readonly _notifications = inject(NotificationService);
  private readonly _transloco = inject(TranslocoService);
  private readonly statusLabels = translateObjectSignal('offers.statuses');

  protected readonly pageSize = PAGE_SIZE;
  protected severityOf(status: OfferStatus) {
    return OFFER_STATUS_SEVERITY[status];
  }
  protected readonly first = signal(0);
  protected readonly status = signal<OfferStatus | null>(null);
  protected readonly dialogOpen = signal(false);
  protected readonly editing = signal<Offer | null>(null);

  protected readonly statusOptions = computed(() => {
    const labels = this.statusLabels() as Labels;
    return [
      { label: this._transloco.translate('offers.allStatuses'), value: null },
      ...OFFER_STATUSES.map((status) => ({ label: labels?.[status] ?? status, value: status as OfferStatus | null })),
    ];
  });

  private lastSort: { field: OfferSortField; direction: 'asc' | 'desc' } | undefined;

  protected onLazyLoad(event: TableLazyLoadEvent): void {
    const size = event.rows ?? PAGE_SIZE;
    const first = event.first ?? 0;
    const field = typeof event.sortField === 'string' ? (event.sortField as OfferSortField) : undefined;
    this.first.set(first);
    this.lastSort = field && SORT_FIELDS.includes(field) ? { field, direction: event.sortOrder === -1 ? 'desc' : 'asc' } : undefined;
    this.offerService.setSearch({ page: Math.floor(first / size), size, status: this.status(), sort: this.lastSort });
  }

  protected onStatusChange(status: OfferStatus | null): void {
    this.status.set(status);
    this.first.set(0);
    const search = this.offerService.search();
    this.offerService.setSearch({ page: 0, size: search?.size ?? PAGE_SIZE, status, sort: this.lastSort });
  }

  protected openNew(): void {
    this.editing.set(null);
    this.dialogOpen.set(true);
  }

  protected openEdit(offer: Offer): void {
    this.editing.set(offer);
    this.dialogOpen.set(true);
  }

  protected publish(offer: Offer): void {
    this.offerService.publish(offer.id).subscribe({
      next: () => this._notifications.success('offers.published', { reference: offer.reference }),
      error: (error) => this._notifications.error(error),
    });
  }

  protected confirmClose(offer: Offer): void {
    const t = (key: string, params?: Record<string, unknown>) => this._transloco.translate(key, params);
    this._confirmation.confirm({
      header: t('offers.closeTitle'),
      message: t('offers.closeConfirm', { reference: offer.reference }),
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: t('offers.close'),
      rejectLabel: t('common.cancel'),
      acceptButtonProps: { severity: 'danger' },
      rejectButtonProps: { severity: 'secondary', text: true },
      accept: () =>
        this.offerService.close(offer.id).subscribe({
          next: () => this._notifications.success('offers.closed', { reference: offer.reference }),
          error: (error) => this._notifications.error(error),
        }),
    });
  }
}
