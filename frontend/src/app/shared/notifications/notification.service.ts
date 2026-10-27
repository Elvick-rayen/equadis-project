import { inject, Injectable } from '@angular/core';
import { TranslocoService } from '@jsverse/transloco';
import { MessageService } from 'primeng/api';
import { toApiError } from '../errors/api-error';

/** Translated success/error toasts shown by the shell's <p-toast>. */
@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly _messages = inject(MessageService);
  private readonly _transloco = inject(TranslocoService);

  success(key: string, params?: Record<string, unknown>): void {
    this._messages.add({
      severity: 'success',
      summary: this._transloco.translate('common.success'),
      detail: this._transloco.translate(key, params),
    });
  }

  /** Shows the translated message for an API business error code. */
  error(error: unknown): void {
    const { code } = toApiError(error);
    // The auth interceptor already signs out and explains it (SESSION_EXPIRED).
    if (code === 'UNAUTHENTICATED') return;
    const key = `errors.${code}`;
    const detail = this._transloco.translate(key);
    this._messages.add({
      severity: 'error',
      summary: this._transloco.translate('common.error'),
      detail: detail === key ? this._transloco.translate('errors.UNKNOWN') : detail,
    });
  }
}
