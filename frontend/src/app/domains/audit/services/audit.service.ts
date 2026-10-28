import { HttpClient, HttpParams } from '@angular/common/http';
import { computed, effect, inject, Injectable, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { API_BASE, Page } from '@shared/http/api';
import { NotificationService } from '@shared/notifications/notification.service';
import { AuditEntry, AuditSearch } from '../models/audit.model';

/** The audit log (/api/audit-log, administrator only), newest first. */
@Injectable({ providedIn: 'root' })
export class AuditService {
  private readonly _http = inject(HttpClient);
  private readonly _notifications = inject(NotificationService);

  private readonly _search = signal<AuditSearch | undefined>(undefined);
  readonly search = this._search.asReadonly();

  private readonly pageResource = rxResource({
    params: () => this._search(),
    stream: ({ params }) => {
      let httpParams = new HttpParams().set('page', params.page).set('size', params.size);
      if (params.subjectType) {
        httpParams = httpParams.set('subjectType', params.subjectType);
      }
      return this._http.get<Page<AuditEntry>>(`${API_BASE}/audit-log`, { params: httpParams });
    },
  });

  readonly entries = computed<AuditEntry[]>(() => (this.pageResource.hasValue() ? this.pageResource.value().content : []));
  readonly totalRecords = computed(() => (this.pageResource.hasValue() ? this.pageResource.value().totalElements : 0));
  readonly loading = this.pageResource.isLoading;

  constructor() {
    effect(() => {
      const error = this.pageResource.error();
      if (error) this._notifications.error(error);
    });
  }

  setSearch(search: AuditSearch): void {
    this._search.set(search);
  }

  reload(): void {
    this.pageResource.reload();
  }
}
