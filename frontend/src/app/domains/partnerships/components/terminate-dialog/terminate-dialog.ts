import { ChangeDetectionStrategy, Component, effect, inject, input, model, signal, untracked } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslocoDirective } from '@jsverse/transloco';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { MessageModule } from 'primeng/message';
import { TextareaModule } from 'primeng/textarea';
import { NotificationService } from '@shared/notifications/notification.service';
import { Partnership } from '../../models/partnership.model';
import { PartnershipService } from '../../services/partnership.service';

/** BR-07: terminating needs a reason; the retailer's subscriptions to the supplier's offers are cancelled. */
@Component({
  selector: 'app-terminate-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, TranslocoDirective, DialogModule, ButtonModule, TextareaModule, MessageModule],
  templateUrl: './terminate-dialog.html',
})
export class TerminateDialog {
  private readonly _partnershipService = inject(PartnershipService);
  private readonly _notifications = inject(NotificationService);

  readonly visible = model(false);
  readonly partnership = input<Partnership | null>(null);

  protected readonly saving = signal(false);
  protected readonly reason = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required, Validators.maxLength(500), Validators.pattern(/\S/)],
  });

  constructor() {
    effect(() => {
      if (this.visible()) untracked(() => this.reason.reset(''));
    });
  }

  protected save(): void {
    const partnership = this.partnership();
    if (!partnership || this.reason.invalid) {
      this.reason.markAsTouched();
      return;
    }
    this.saving.set(true);
    this._partnershipService.terminate(partnership.id, this.reason.value.trim()).subscribe({
      next: (terminated) => {
        this.saving.set(false);
        this._notifications.success('partnerships.terminated', {
          supplier: terminated.supplierName,
          retailer: terminated.retailerName,
        });
        this.visible.set(false);
      },
      error: (error) => {
        this.saving.set(false);
        this._notifications.error(error);
      },
    });
  }
}
