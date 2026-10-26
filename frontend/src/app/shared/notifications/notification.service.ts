import { inject, Injectable } from '@angular/core';
import { TranslocoService } from '@jsverse/transloco';
import { MessageService } from 'primeng/api';
import { toApiError } from '../errors/api-error';

/** Translated success/error toasts shown by the shell's <p-toast>. */
@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly messages = inject(MessageService);
  private readonly transloco = inject(TranslocoService);

  success(key: string, params?: Record<string, unknown>): void {
    this.messages.add({
      severity: 'success',
      summary: this.transloco.translate('common.success'),
      detail: this.transloco.translate(key, params),
    });
  }

  /** Shows the translated message for an API business error code. */
  error(error: unknown): void {
    const { code } = toApiError(error);
    const key = `errors.${code}`;
    const detail = this.transloco.translate(key);
    this.messages.add({
      severity: 'error',
      summary: this.transloco.translate('common.error'),
      detail: detail === key ? this.transloco.translate('errors.UNKNOWN') : detail,
    });
  }
}
