import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, effect, inject, input, model, signal, untracked } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslocoDirective } from '@jsverse/transloco';
import { ButtonModule } from 'primeng/button';
import { DatePickerModule } from 'primeng/datepicker';
import { DialogModule } from 'primeng/dialog';
import { addDays, addMonths, fromIsoDate, today, toDisplayDate, toIsoDate } from '@shared/dates/local-date';
import { NotificationService } from '@shared/notifications/notification.service';
import { Partnership, Renewal } from '../../models/partnership.model';
import { PartnershipService } from '../../services/partnership.service';

/** BR-06: pick a new end date; shows the renewal history kept so far. */
@Component({
  selector: 'app-renew-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, TranslocoDirective, DialogModule, ButtonModule, DatePickerModule, DatePipe],
  templateUrl: './renew-dialog.html',
})
export class RenewDialog {
  private readonly _partnershipService = inject(PartnershipService);
  private readonly _notifications = inject(NotificationService);

  readonly visible = model(false);
  readonly partnership = input<Partnership | null>(null);

  protected readonly saving = signal(false);
  protected readonly history = signal<Renewal[]>([]);
  protected readonly minDate = signal(today());
  protected readonly newEndDate = new FormControl<Date | null>(null, Validators.required);

  constructor() {
    effect(() => {
      const partnership = this.partnership();
      if (!this.visible() || !partnership) return;
      untracked(() => {
        const currentEnd = fromIsoDate(partnership.endDate);
        // The new end must be after the current one; suggest one more year.
        this.minDate.set(addDays(currentEnd, 1));
        this.newEndDate.reset(addMonths(currentEnd > today() ? currentEnd : today(), 12));
        this.history.set([]);
        this._partnershipService.get(partnership.id).subscribe({
          next: (detail) => this.history.set(detail.renewals ?? []),
          error: (error) => this._notifications.error(error),
        });
      });
    });
  }

  protected save(): void {
    const partnership = this.partnership();
    if (!partnership || this.newEndDate.invalid) {
      this.newEndDate.markAsTouched();
      return;
    }
    this.saving.set(true);
    this._partnershipService.renew(partnership.id, toIsoDate(this.newEndDate.value!)).subscribe({
      next: (renewed) => {
        this.saving.set(false);
        this._notifications.success('partnerships.renewed', { end: toDisplayDate(renewed.endDate) });
        this.visible.set(false);
      },
      error: (error) => {
        this.saving.set(false);
        this._notifications.error(error);
      },
    });
  }
}
