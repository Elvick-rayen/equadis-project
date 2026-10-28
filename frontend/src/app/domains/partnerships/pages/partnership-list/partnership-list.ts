import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { translateObjectSignal, TranslocoDirective } from '@jsverse/transloco';
import { ButtonModule } from 'primeng/button';
import { SelectModule } from 'primeng/select';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { PartnershipFormDialog } from '../../components/partnership-form-dialog/partnership-form-dialog';
import { RenewDialog } from '../../components/renew-dialog/renew-dialog';
import { TerminateDialog } from '../../components/terminate-dialog/terminate-dialog';
import {
  PARTNERSHIP_STATUSES,
  Partnership,
  PartnershipFilter,
  PartnershipSortField,
  PartnershipStatus,
  STATUS_SEVERITY,
} from '../../models/partnership.model';
import { PartnershipService } from '../../services/partnership.service';

const PAGE_SIZE = 10;
const SORT_FIELDS: readonly PartnershipSortField[] = ['startDate', 'endDate'];

type Labels = Record<string, string> | undefined;

/**
 * Administrator → Partnerships: status tag and filter, "New" dialog, and Renew / Terminate
 * buttons shown only when the backend allows them today.
 */
@Component({
  selector: 'app-partnership-list',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    TranslocoDirective,
    FormsModule,
    DatePipe,
    TableModule,
    TagModule,
    ButtonModule,
    SelectModule,
    PartnershipFormDialog,
    RenewDialog,
    TerminateDialog,
  ],
  templateUrl: './partnership-list.html',
})
export class PartnershipList {
  protected readonly partnershipService = inject(PartnershipService);
  private readonly statusLabels = translateObjectSignal('partnerships.statuses');
  private readonly filterLabels = translateObjectSignal('partnerships.filters');

  protected readonly pageSize = PAGE_SIZE;
  protected severityOf(status: PartnershipStatus) {
    return STATUS_SEVERITY[status];
  }
  protected readonly first = signal(0);
  protected readonly filter = signal<PartnershipFilter | null>(null);

  protected readonly newOpen = signal(false);
  protected readonly renewing = signal<Partnership | null>(null);
  protected readonly renewOpen = signal(false);
  protected readonly terminating = signal<Partnership | null>(null);
  protected readonly terminateOpen = signal(false);

  protected readonly filterOptions = computed(() => {
    const statuses = this.statusLabels() as Labels;
    const filters = this.filterLabels() as Labels;
    return [
      { label: filters?.['all'] ?? 'All', value: null },
      ...PARTNERSHIP_STATUSES.map((status) => ({ label: statuses?.[status] ?? status, value: status as PartnershipFilter })),
      { label: filters?.['expiring'] ?? 'Ending within 30 days', value: 'EXPIRING' as PartnershipFilter },
    ];
  });

  private lastSort: { field: PartnershipSortField; direction: 'asc' | 'desc' } | undefined;

  protected onLazyLoad(event: TableLazyLoadEvent): void {
    const size = event.rows ?? PAGE_SIZE;
    const first = event.first ?? 0;
    const field = typeof event.sortField === 'string' ? (event.sortField as PartnershipSortField) : undefined;
    this.first.set(first);
    this.lastSort = field && SORT_FIELDS.includes(field) ? { field, direction: event.sortOrder === -1 ? 'desc' : 'asc' } : undefined;
    this.partnershipService.setSearch({ page: Math.floor(first / size), size, filter: this.filter(), sort: this.lastSort });
  }

  protected onFilterChange(filter: PartnershipFilter | null): void {
    this.filter.set(filter);
    this.first.set(0);
    const search = this.partnershipService.search();
    this.partnershipService.setSearch({ page: 0, size: search?.size ?? PAGE_SIZE, filter, sort: this.lastSort });
  }

  protected openRenew(partnership: Partnership): void {
    this.renewing.set(partnership);
    this.renewOpen.set(true);
  }

  protected openTerminate(partnership: Partnership): void {
    this.terminating.set(partnership);
    this.terminateOpen.set(true);
  }
}
