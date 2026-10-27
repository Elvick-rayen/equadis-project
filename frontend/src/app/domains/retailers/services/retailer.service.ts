import { HttpClient } from '@angular/common/http';
import { computed, effect, inject, Injectable } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { AuthService } from '@shared/auth/auth.service';
import { API_BASE } from '@shared/http/api';
import { NotificationService } from '@shared/notifications/notification.service';
import { RetailerSummary } from '../models/retailer.model';

const RETAILERS_URL = `${API_BASE}/retailers`;

/** Talks to /api/retailers and holds the retailer list for the administrator's "Acting as" selector. */
@Injectable({ providedIn: 'root' })
export class RetailerService {
  private readonly _http = inject(HttpClient);
  private readonly _auth = inject(AuthService);
  private readonly _notifications = inject(NotificationService);

  /** The list is administrator-only, so it is only fetched for sessions that can switch persona. */
  private readonly summariesResource = rxResource({
    params: () => (this._auth.canActAs() ? true : undefined),
    stream: () => this._http.get<RetailerSummary[]>(`${RETAILERS_URL}/summaries`),
  });

  readonly summaries = computed(() => (this.summariesResource.hasValue() ? this.summariesResource.value() : []));

  constructor() {
    effect(() => {
      const error = this.summariesResource.error();
      if (error) this._notifications.error(error);
    });
  }
}
