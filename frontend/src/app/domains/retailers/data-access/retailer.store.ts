import { computed, inject, Injectable, signal } from '@angular/core';
import { NotificationService } from '../../../shared/notifications/notification.service';
import { Retailer } from '../models/retailer.model';
import { RetailerApi } from './retailer.api';

type LoadStatus = 'idle' | 'loading' | 'loaded' | 'error';

/** Signal-based state for retailers. */
@Injectable({ providedIn: 'root' })
export class RetailerStore {
  private readonly api = inject(RetailerApi);
  private readonly notifications = inject(NotificationService);

  private readonly _retailers = signal<Retailer[]>([]);
  private readonly _status = signal<LoadStatus>('idle');

  readonly retailers = this._retailers.asReadonly();
  readonly loading = computed(() => this._status() === 'loading');
  readonly loaded = computed(() => this._status() === 'loaded');
  readonly byId = computed(() => new Map(this._retailers().map((r) => [r.id, r])));

  /** Loads the list once; pass `force` to refresh. */
  load(force = false): void {
    const status = this._status();
    if (!force && (status === 'loading' || status === 'loaded')) return;

    this._status.set('loading');
    this.api.list().subscribe({
      next: (retailers) => {
        this._retailers.set(retailers);
        this._status.set('loaded');
      },
      error: (error) => {
        this._status.set('error');
        this.notifications.error(error);
      },
    });
  }
}
