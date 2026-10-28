import { ChangeDetectionStrategy, Component, computed, effect, inject, input, model, signal, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { translateObjectSignal, TranslocoDirective } from '@jsverse/transloco';
import { ButtonModule } from 'primeng/button';
import { DatePickerModule } from 'primeng/datepicker';
import { DialogModule } from 'primeng/dialog';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { SelectModule } from 'primeng/select';
import { TextareaModule } from 'primeng/textarea';
import { addDays, fromIsoDate, today, toIsoDate } from '@shared/dates/local-date';
import { NotificationService } from '@shared/notifications/notification.service';
import { CURRENCIES, DISCOUNT_TYPES, DiscountType, Offer, OfferDraft } from '../../models/offer.model';
import { OfferService } from '../../services/offer.service';

const REFERENCE_PATTERN = /^[A-Za-z0-9._-]{1,40}$/;

type Labels = Record<string, string> | undefined;

/** BR-09 hints: percentage in (0, 100]; valid to on or after valid from. The API is the real guard. */
const offerValidator: ValidatorFn = (group: AbstractControl): ValidationErrors | null => {
  const type = group.get('discountType')?.value as DiscountType;
  const value = group.get('discountValue')?.value as number | null;
  const from = group.get('validFrom')?.value as Date | null;
  const to = group.get('validTo')?.value as Date | null;
  const errors: ValidationErrors = {};
  if (value !== null && (value <= 0 || (type === 'PERCENTAGE_DISCOUNT' && value > 100))) {
    errors['discountRange'] = true;
  }
  if (from && to && to < from) {
    errors['validToBeforeFrom'] = true;
  }
  return Object.keys(errors).length ? errors : null;
};

/** Create a draft, or edit one (BR-08: only drafts are editable). A currency is needed for fixed amounts. */
@Component({
  selector: 'app-offer-form-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    TranslocoDirective,
    DialogModule,
    ButtonModule,
    InputTextModule,
    InputNumberModule,
    TextareaModule,
    SelectModule,
    DatePickerModule,
    MessageModule,
  ],
  templateUrl: './offer-form-dialog.html',
})
export class OfferFormDialog {
  private readonly _offerService = inject(OfferService);
  private readonly _notifications = inject(NotificationService);
  private readonly typeLabels = translateObjectSignal('offers.discountTypes');

  readonly visible = model(false);
  readonly offer = input<Offer | null>(null);

  protected readonly isEdit = computed(() => this.offer() !== null);
  protected readonly saving = signal(false);
  protected readonly currencies = [...CURRENCIES];
  protected readonly discountTypes = computed(() => {
    const labels = this.typeLabels() as Labels;
    return DISCOUNT_TYPES.map((type) => ({ label: labels?.[type] ?? type, value: type }));
  });

  protected readonly form = new FormGroup(
    {
      reference: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.pattern(REFERENCE_PATTERN)] }),
      title: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(120)] }),
      description: new FormControl('', { nonNullable: true, validators: Validators.maxLength(2000) }),
      discountType: new FormControl<DiscountType>('PERCENTAGE_DISCOUNT', { nonNullable: true }),
      discountValue: new FormControl<number | null>(null, Validators.required),
      currency: new FormControl<string | null>(null),
      minimumOrderQuantity: new FormControl<number | null>(null, Validators.min(1)),
      validFrom: new FormControl<Date | null>(null, Validators.required),
      validTo: new FormControl<Date | null>(null, Validators.required),
    },
    { validators: offerValidator },
  );

  private readonly discountType = toSignal(this.form.controls.discountType.valueChanges, {
    initialValue: this.form.controls.discountType.value,
  });
  protected readonly isFixedAmount = computed(() => this.discountType() === 'FIXED_AMOUNT_DISCOUNT');

  constructor() {
    effect(() => {
      if (!this.visible()) return;
      const offer = this.offer();
      untracked(() =>
        this.form.reset({
          reference: offer?.reference ?? '',
          title: offer?.title ?? '',
          description: offer?.description ?? '',
          discountType: offer?.discountType ?? 'PERCENTAGE_DISCOUNT',
          discountValue: offer?.discountValue ?? null,
          currency: offer?.currency ?? 'EUR',
          minimumOrderQuantity: offer?.minimumOrderQuantity ?? null,
          validFrom: offer ? fromIsoDate(offer.validFrom) : today(),
          validTo: offer ? fromIsoDate(offer.validTo) : addDays(today(), 30),
        }),
      );
    });
    // A currency is required for fixed amounts only.
    effect(() => {
      const fixed = this.isFixedAmount();
      untracked(() => {
        const currency = this.form.controls.currency;
        if (fixed) {
          currency.addValidators(Validators.required);
        } else {
          currency.removeValidators(Validators.required);
        }
        currency.updateValueAndValidity({ emitEvent: false });
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
    const id = this.offer()?.id;
    const draft = this.toDraft();
    const request = id ? this._offerService.edit(id, draft) : this._offerService.create(draft);
    this.saving.set(true);
    request.subscribe({
      next: (saved) => {
        this.saving.set(false);
        this._notifications.success(id ? 'offers.updated' : 'offers.created', { reference: saved.reference });
        this.visible.set(false);
      },
      error: (error) => {
        this.saving.set(false);
        this._notifications.error(error);
      },
    });
  }

  private toDraft(): OfferDraft {
    const value = this.form.getRawValue();
    return {
      reference: value.reference.trim(),
      title: value.title.trim(),
      description: value.description.trim() || null,
      discountType: value.discountType,
      discountValue: value.discountValue!,
      currency: this.isFixedAmount() ? value.currency : null,
      minimumOrderQuantity: value.minimumOrderQuantity,
      validFrom: toIsoDate(value.validFrom!),
      validTo: toIsoDate(value.validTo!),
    };
  }
}
