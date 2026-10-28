import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TranslocoDirective, TranslocoService } from '@jsverse/transloco';
import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { debounceTime, distinctUntilChanged, Subject } from 'rxjs';
import { CountryNamePipe } from '@shared/countries/country-name.pipe';
import { LanguageStore } from '@shared/i18n/language.store';
import { NotificationService } from '@shared/notifications/notification.service';
import { RetailerFormDialog } from '../../components/retailer-form-dialog/retailer-form-dialog';
import { Retailer, RetailerSortField } from '../../models/retailer.model';
import { RetailerService } from '../../services/retailer.service';

const PAGE_SIZE = 10;
const SORT_FIELDS: readonly RetailerSortField[] = ['storeName', 'storeType', 'contactEmail', 'location.city', 'location.countryCode'];

/** Administrator → Retailers: server-side paged table, "New" button, Edit and Delete per row. */
@Component({
  selector: 'app-retailer-list',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    TranslocoDirective,
    TableModule,
    TagModule,
    ButtonModule,
    IconFieldModule,
    InputIconModule,
    InputTextModule,
    CountryNamePipe,
    RetailerFormDialog,
  ],
  templateUrl: './retailer-list.html',
})
export class RetailerList {
  protected readonly retailerService = inject(RetailerService);
  protected readonly language = inject(LanguageStore).language;
  private readonly _confirmation = inject(ConfirmationService);
  private readonly _notifications = inject(NotificationService);
  private readonly _transloco = inject(TranslocoService);

  protected readonly pageSize = PAGE_SIZE;
  protected readonly first = signal(0);
  protected readonly dialogOpen = signal(false);
  protected readonly editing = signal<Retailer | null>(null);

  private readonly query = signal('');
  private readonly queryInput = new Subject<string>();

  constructor() {
    this.queryInput
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(inject(DestroyRef)))
      .subscribe((query) => {
        this.query.set(query);
        this.first.set(0);
        const search = this.retailerService.search();
        this.retailerService.setSearch({ ...(search ?? { size: PAGE_SIZE }), page: 0, query });
      });
  }

  /** Called by p-table on first display, page change and sort change. */
  protected onLazyLoad(event: TableLazyLoadEvent): void {
    const size = event.rows ?? PAGE_SIZE;
    const first = event.first ?? 0;
    const field = typeof event.sortField === 'string' ? (event.sortField as RetailerSortField) : undefined;
    this.first.set(first);
    this.retailerService.setSearch({
      page: Math.floor(first / size),
      size,
      query: this.query(),
      sort: field && SORT_FIELDS.includes(field) ? { field, direction: event.sortOrder === -1 ? 'desc' : 'asc' } : undefined,
    });
  }

  protected onSearch(value: string): void {
    this.queryInput.next(value);
  }

  protected openNew(): void {
    this.editing.set(null);
    this.dialogOpen.set(true);
  }

  protected openEdit(retailer: Retailer): void {
    this.editing.set(retailer);
    this.dialogOpen.set(true);
  }

  protected confirmDelete(retailer: Retailer): void {
    const t = (key: string, params?: Record<string, unknown>) => this._transloco.translate(key, params);
    this._confirmation.confirm({
      header: t('retailers.deleteTitle'),
      message: t('retailers.deleteConfirm', { name: retailer.storeName }),
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: t('common.delete'),
      rejectLabel: t('common.cancel'),
      acceptButtonProps: { severity: 'danger' },
      rejectButtonProps: { severity: 'secondary', text: true },
      accept: () =>
        this.retailerService.delete(retailer.id).subscribe({
          next: () => this._notifications.success('retailers.deleted', { name: retailer.storeName }),
          error: (error) => this._notifications.error(error),
        }),
    });
  }
}
