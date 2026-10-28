import { ChangeDetectionStrategy, Component, computed, effect, inject, input, model, signal, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { AbstractControl, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { translateObjectSignal, TranslocoDirective } from '@jsverse/transloco';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { SelectModule } from 'primeng/select';
import { countryOptions } from '@shared/countries/countries';
import { LanguageStore } from '@shared/i18n/language.store';
import { NotificationService } from '@shared/notifications/notification.service';
import { needsStreetAddress, needsWebsite, Retailer, RetailerDraft, STORE_TYPES, StoreType } from '../../models/retailer.model';
import { RetailerService } from '../../services/retailer.service';

const WEBSITE_PATTERN = /^https?:\/\/\S+\.\S+$/;

type Labels = Record<string, string> | undefined;

/** Create / edit dialog. The location fields follow the store type (BR-02); the API checks it too. */
@Component({
  selector: 'app-retailer-form-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, TranslocoDirective, DialogModule, ButtonModule, InputTextModule, SelectModule, MessageModule],
  templateUrl: './retailer-form-dialog.html',
})
export class RetailerFormDialog {
  private readonly _retailerService = inject(RetailerService);
  private readonly _notifications = inject(NotificationService);
  private readonly language = inject(LanguageStore).language;
  private readonly storeTypeLabels = translateObjectSignal('retailers.storeTypes');

  readonly visible = model(false);
  readonly retailer = input<Retailer | null>(null);

  protected readonly isEdit = computed(() => this.retailer() !== null);
  protected readonly saving = signal(false);
  protected readonly countries = computed(() => countryOptions(this.language()));
  protected readonly storeTypes = computed(() => {
    const labels = this.storeTypeLabels() as Labels;
    return STORE_TYPES.map((type) => ({ label: labels?.[type] ?? type, value: type }));
  });

  protected readonly form = inject(NonNullableFormBuilder).group({
    storeName: ['', [Validators.required, Validators.maxLength(120)]],
    storeType: ['PHYSICAL' as StoreType, Validators.required],
    countryCode: ['', Validators.required],
    city: ['', Validators.maxLength(120)],
    address: ['', Validators.maxLength(200)],
    website: ['', [Validators.maxLength(255), Validators.pattern(WEBSITE_PATTERN)]],
    contactEmail: ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
  });

  private readonly storeType = toSignal(this.form.controls.storeType.valueChanges, {
    initialValue: this.form.controls.storeType.value,
  });
  protected readonly needsAddress = computed(() => needsStreetAddress(this.storeType()));
  protected readonly needsWebsite = computed(() => needsWebsite(this.storeType()));

  constructor() {
    // Refill the form each time the dialog opens (untracked: form controls read signals of their own).
    effect(() => {
      if (!this.visible()) return;
      const retailer = this.retailer();
      untracked(() =>
        this.form.reset({
          storeName: retailer?.storeName ?? '',
          storeType: retailer?.storeType ?? 'PHYSICAL',
          countryCode: retailer?.location.countryCode ?? '',
          city: retailer?.location.city ?? '',
          address: retailer?.location.address ?? '',
          website: retailer?.location.website ?? '',
          contactEmail: retailer?.contactEmail ?? '',
        }),
      );
    });
    // BR-02: which location fields are required depends on the store type.
    effect(() => {
      const address = this.needsAddress();
      const website = this.needsWebsite();
      untracked(() => {
        toggleRequired(this.form.controls.city, address);
        toggleRequired(this.form.controls.address, address);
        toggleRequired(this.form.controls.website, website);
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
    const id = this.retailer()?.id;
    const draft = this.toDraft();
    const request = id ? this._retailerService.update(id, draft) : this._retailerService.create(draft);

    this.saving.set(true);
    request.subscribe({
      next: (saved) => {
        this.saving.set(false);
        this._notifications.success(id ? 'retailers.updated' : 'retailers.created', { name: saved.storeName });
        this.visible.set(false);
      },
      error: (error) => {
        this.saving.set(false);
        this._notifications.error(error);
      },
    });
  }

  private toDraft(): RetailerDraft {
    const value = this.form.getRawValue();
    return {
      storeName: value.storeName.trim(),
      storeType: value.storeType,
      countryCode: value.countryCode,
      city: this.needsAddress() ? value.city.trim() : null,
      address: this.needsAddress() ? value.address.trim() : null,
      website: this.needsWebsite() ? value.website.trim() : null,
      contactEmail: value.contactEmail.trim(),
    };
  }
}

function toggleRequired(control: AbstractControl, required: boolean): void {
  if (required) {
    control.addValidators(Validators.required);
  } else {
    control.removeValidators(Validators.required);
  }
  control.updateValueAndValidity({ emitEvent: false });
}
