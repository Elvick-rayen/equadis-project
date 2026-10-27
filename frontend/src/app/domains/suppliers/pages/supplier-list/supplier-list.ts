import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TranslocoDirective, TranslocoService } from '@jsverse/transloco';
import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { debounceTime, distinctUntilChanged, Subject } from 'rxjs';
import { CountryNamePipe } from '@shared/countries/country-name.pipe';
import { LanguageStore } from '@shared/i18n/language.store';
import { NotificationService } from '@shared/notifications/notification.service';
import { SupplierFormDialog } from '../../components/supplier-form-dialog/supplier-form-dialog';
import { SupplierService } from '../../services/supplier.service';
import { Supplier, SupplierSortField } from '../../models/supplier.model';

const PAGE_SIZE = 10;
const SORT_FIELDS: readonly SupplierSortField[] = ['companyName', 'contact.name', 'contact.email', 'countryCode'];

/** Administrator → Suppliers: server-side paged table, "New" button, Edit and Delete per row. */
@Component({
  selector: 'app-supplier-list',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    TranslocoDirective,
    TableModule,
    ButtonModule,
    IconFieldModule,
    InputIconModule,
    InputTextModule,
    CountryNamePipe,
    SupplierFormDialog,
  ],
  templateUrl: './supplier-list.html',
  styleUrl: './supplier-list.scss',
})
export class SupplierList {
  protected readonly supplierService = inject(SupplierService);
  protected readonly language = inject(LanguageStore).language;
  private readonly _confirmation = inject(ConfirmationService);
  private readonly _notifications = inject(NotificationService);
  private readonly _transloco = inject(TranslocoService);

  protected readonly pageSize = PAGE_SIZE;
  /** Index of the first row shown; reset to 0 when the search text changes. */
  protected readonly first = signal(0);
  protected readonly dialogOpen = signal(false);
  protected readonly editing = signal<Supplier | null>(null);

  private readonly query = signal('');
  private readonly queryInput = new Subject<string>();

  constructor() {
    this.queryInput
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(inject(DestroyRef)))
      .subscribe((query) => {
        this.query.set(query);
        this.first.set(0);
        const search = this.supplierService.search();
        this.supplierService.setSearch({ ...(search ?? { size: PAGE_SIZE }), page: 0, query });
      });
  }

  /** Called by p-table on first display, page change and sort change. */
  protected onLazyLoad(event: TableLazyLoadEvent): void {
    const size = event.rows ?? PAGE_SIZE;
    const first = event.first ?? 0;
    const field = typeof event.sortField === 'string' ? (event.sortField as SupplierSortField) : undefined;
    this.first.set(first);
    this.supplierService.setSearch({
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

  protected openEdit(supplier: Supplier): void {
    this.editing.set(supplier);
    this.dialogOpen.set(true);
  }

  protected confirmDelete(supplier: Supplier): void {
    const t = (key: string, params?: Record<string, unknown>) => this._transloco.translate(key, params);
    this._confirmation.confirm({
      header: t('suppliers.deleteTitle'),
      message: t('suppliers.deleteConfirm', { name: supplier.companyName }),
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: t('common.delete'),
      rejectLabel: t('common.cancel'),
      acceptButtonProps: { severity: 'danger' },
      rejectButtonProps: { severity: 'secondary', text: true },
      accept: () =>
        this.supplierService.delete(supplier.id).subscribe({
          next: () => this._notifications.success('suppliers.deleted', { name: supplier.companyName }),
          error: (error) => this._notifications.error(error),
        }),
    });
  }
}
