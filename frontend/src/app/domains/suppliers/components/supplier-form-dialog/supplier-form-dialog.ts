import { Component, computed, effect, inject, input, model, signal, untracked } from '@angular/core';
import { AbstractControl, NonNullableFormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { TranslocoDirective } from '@jsverse/transloco';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { SelectModule } from 'primeng/select';
import { countryOptions } from '../../../../shared/countries/countries';
import { LanguageStore } from '../../../../shared/i18n/language.store';
import { NotificationService } from '../../../../shared/notifications/notification.service';
import { SupplierStore } from '../../data-access/supplier.store';
import { Supplier, SupplierDraft } from '../../models/supplier.model';

const PHONE_PATTERN = /^\+?[0-9 ().-]{6,20}$/;

/** Create / edit dialog. Pass `supplier` to edit, leave it null to create. */
@Component({
  selector: 'app-supplier-form-dialog',
  imports: [ReactiveFormsModule, TranslocoDirective, DialogModule, ButtonModule, InputTextModule, SelectModule, MessageModule],
  templateUrl: './supplier-form-dialog.html',
  styleUrl: './supplier-form-dialog.scss',
})
export class SupplierFormDialog {
  private readonly store = inject(SupplierStore);
  private readonly notifications = inject(NotificationService);
  private readonly language = inject(LanguageStore).language;

  readonly visible = model(false);
  readonly supplier = input<Supplier | null>(null);

  protected readonly isEdit = computed(() => this.supplier() !== null);
  protected readonly saving = signal(false);
  protected readonly countries = computed(() => countryOptions(this.language()));

  protected readonly form = inject(NonNullableFormBuilder).group({
    companyName: ['', [Validators.required, Validators.maxLength(120), this.uniqueNameValidator()]],
    contactName: ['', [Validators.required, Validators.maxLength(120)]],
    contactEmail: ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
    contactPhone: ['', [Validators.required, Validators.pattern(PHONE_PATTERN)]],
    countryCode: ['', Validators.required],
  });

  constructor() {
    // Refill the form each time the dialog opens. The reset runs untracked: validators and
    // form controls read signals of their own, which must not re-trigger this effect.
    effect(() => {
      if (!this.visible()) return;
      const supplier = this.supplier();
      untracked(() =>
        this.form.reset({
          companyName: supplier?.companyName ?? '',
          contactName: supplier?.contact.name ?? '',
          contactEmail: supplier?.contact.email ?? '',
          contactPhone: supplier?.contact.phone ?? '',
          countryCode: supplier?.countryCode ?? '',
        }),
      );
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

    const id = this.supplier()?.id;
    const draft = this.toDraft();
    const request = id ? this.store.update(id, draft) : this.store.create(draft);

    this.saving.set(true);
    request.subscribe({
      next: (saved) => {
        this.saving.set(false);
        this.notifications.success(id ? 'suppliers.updated' : 'suppliers.created', { name: saved.companyName });
        this.visible.set(false);
      },
      error: (error) => {
        this.saving.set(false);
        this.notifications.error(error);
      },
    });
  }

  private toDraft(): SupplierDraft {
    const value = this.form.getRawValue();
    return {
      companyName: value.companyName.trim(),
      contact: {
        name: value.contactName.trim(),
        email: value.contactEmail.trim(),
        phone: value.contactPhone.trim(),
      },
      countryCode: value.countryCode,
    };
  }

  private uniqueNameValidator() {
    return (control: AbstractControl<string>): ValidationErrors | null =>
      this.store.isNameTaken(control.value ?? '', this.supplier()?.id) ? { nameTaken: true } : null;
  }
}
