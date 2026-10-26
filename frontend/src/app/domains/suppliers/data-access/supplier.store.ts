import { computed, effect, inject, Injectable, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { Observable, tap } from 'rxjs';
import { NotificationService } from '../../../shared/notifications/notification.service';
import { Supplier, SupplierDraft } from '../models/supplier.model';
import { SupplierApi, SupplierSearch } from './supplier.api';

/**
 * Signal-based supplier state:
 * - the current table page, re-fetched whenever the search parameters change;
 * - the id/name list used by the "Acting as" selector and the BR-01 form hint.
 */
@Injectable({ providedIn: 'root' })
export class SupplierStore {
  private readonly api = inject(SupplierApi);
  private readonly notifications = inject(NotificationService);

  /** Set by the suppliers table; nothing is fetched until it is. */
  private readonly _search = signal<SupplierSearch | undefined>(undefined);
  readonly search = this._search.asReadonly();

  private readonly pageResource = rxResource({
    params: () => this._search(),
    stream: ({ params }) => this.api.search(params),
  });

  private readonly summariesResource = rxResource({
    stream: () => this.api.summaries(),
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
    return this.api.create(draft).pipe(tap(() => this.refresh()));
  }

  update(id: number, draft: SupplierDraft): Observable<Supplier> {
    return this.api.update(id, draft).pipe(tap(() => this.refresh()));
  }

  delete(id: number): Observable<void> {
    return this.api.delete(id).pipe(
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
      this.notifications.error(error);
    }
  }
}
