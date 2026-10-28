import { ChangeDetectionStrategy, Component, effect, inject, model, signal, untracked } from '@angular/core';
import { AbstractControl, FormControl, FormGroup, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { TranslocoDirective } from '@jsverse/transloco';
import { ButtonModule } from 'primeng/button';
import { DatePickerModule } from 'primeng/datepicker';
import { DialogModule } from 'primeng/dialog';
import { MessageModule } from 'primeng/message';
import { SelectModule } from 'primeng/select';
import { addMonths, today, toIsoDate } from '@shared/dates/local-date';
import { NotificationService } from '@shared/notifications/notification.service';
import { PartnershipService } from '../../services/partnership.service';

/** BR-03 hint in the form: end after start, at most 36 months. The API is the real guard. */
const periodValidator: ValidatorFn = (group: AbstractControl): ValidationErrors | null => {
  const start = group.get('startDate')?.value as Date | null;
  const end = group.get('endDate')?.value as Date | null;
  if (!start || !end) return null;
  if (end <= start) return { endBeforeStart: true };
  if (end > addMonths(start, 36)) return { tooLong: true };
  return null;
};

/** "New partnership": supplier, retailer and period (BR-03, BR-04 checked by the API). */
@Component({
  selector: 'app-partnership-form-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, TranslocoDirective, DialogModule, ButtonModule, SelectModule, DatePickerModule, MessageModule],
  templateUrl: './partnership-form-dialog.html',
})
export class PartnershipFormDialog {
  protected readonly partnershipService = inject(PartnershipService);
  private readonly _notifications = inject(NotificationService);

  readonly visible = model(false);

  protected readonly saving = signal(false);
  protected readonly minDate = signal(today());

  protected readonly form = new FormGroup(
    {
      supplierId: new FormControl<number | null>(null, Validators.required),
      retailerId: new FormControl<number | null>(null, Validators.required),
      startDate: new FormControl<Date | null>(null, Validators.required),
      endDate: new FormControl<Date | null>(null, Validators.required),
    },
    { validators: periodValidator },
  );

  constructor() {
    effect(() => {
      if (!this.visible()) return;
      untracked(() => {
        this.partnershipService.loadCompanyOptions();
        this.minDate.set(today());
        this.form.reset({ supplierId: null, retailerId: null, startDate: today(), endDate: addMonths(today(), 12) });
      });
    });
  }

  protected showError(control: AbstractControl): boolean {
    return control.invalid && (control.touched || control.dirty);
  }

  protected save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    this.saving.set(true);
    this.partnershipService
      .create({
        supplierId: value.supplierId!,
        retailerId: value.retailerId!,
        startDate: toIsoDate(value.startDate!),
        endDate: toIsoDate(value.endDate!),
      })
      .subscribe({
        next: (created) => {
          this.saving.set(false);
          this._notifications.success('partnerships.created', {
            supplier: created.supplierName,
            retailer: created.retailerName,
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
