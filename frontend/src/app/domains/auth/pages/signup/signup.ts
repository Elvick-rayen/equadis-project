import { ChangeDetectionStrategy, Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  FormsModule,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { translateObjectSignal, TranslocoDirective } from '@jsverse/transloco';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { PasswordModule } from 'primeng/password';
import { SelectModule } from 'primeng/select';
import { SelectButtonModule } from 'primeng/selectbutton';
import { SignUpRequest, StoreType } from '@shared/auth/auth.model';
import { AuthService } from '@shared/auth/auth.service';
import { countryOptions } from '@shared/countries/countries';
import { toApiError } from '@shared/errors/api-error';
import { LanguageStore } from '@shared/i18n/language.store';
import { LANGUAGES } from '@shared/i18n/languages';
import { NotificationService } from '@shared/notifications/notification.service';
import { PersonaStore } from '@shared/persona/persona.store';

type AccountType = 'SUPPLIER' | 'RETAILER';
type Labels = Record<string, string> | undefined;

const USERNAME_PATTERN = /^[A-Za-z0-9._-]{3,50}$/;
const PHONE_PATTERN = /^\+?[0-9 ().-]{6,20}$/;
const WEBSITE_PATTERN = /^https?:\/\/\S+\.\S+$/;
const STORE_TYPES: readonly StoreType[] = ['PHYSICAL', 'ONLINE', 'HYBRID'];

const passwordsMatch: ValidatorFn = (group: AbstractControl): ValidationErrors | null =>
  group.get('password')?.value === group.get('confirmPassword')?.value ? null : { passwordsMismatch: true };

/**
 * Sign up as a supplier or a retailer: creates the company and its account, then signs in.
 * Retailer location fields follow BR-02 (they depend on the store type); the API checks it too.
 */
@Component({
  selector: 'app-signup',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    FormsModule,
    RouterLink,
    TranslocoDirective,
    CardModule,
    InputTextModule,
    PasswordModule,
    ButtonModule,
    MessageModule,
    SelectModule,
    SelectButtonModule,
  ],
  templateUrl: './signup.html',
  styleUrl: '../auth-page.scss',
})
export class Signup {
  private readonly _auth = inject(AuthService);
  private readonly _router = inject(Router);
  private readonly _personas = inject(PersonaStore);
  private readonly _notifications = inject(NotificationService);
  protected readonly languages = inject(LanguageStore);

  private readonly personaLabels = translateObjectSignal('persona');
  private readonly storeTypeLabels = translateObjectSignal('retailers.storeTypes');

  protected readonly languageOptions = LANGUAGES.map((code) => ({ label: code.toUpperCase(), value: code }));
  protected readonly countries = computed(() => countryOptions(this.languages.language()));
  protected readonly accountTypes = computed(() => {
    const labels = this.personaLabels() as Labels;
    return [
      { label: labels?.['supplier'] ?? 'Supplier', value: 'SUPPLIER' as AccountType },
      { label: labels?.['retailer'] ?? 'Retailer', value: 'RETAILER' as AccountType },
    ];
  });
  protected readonly storeTypes = computed(() => {
    const labels = this.storeTypeLabels() as Labels;
    return STORE_TYPES.map((type) => ({ label: labels?.[type] ?? type, value: type }));
  });

  protected readonly accountType = signal<AccountType>('SUPPLIER');
  protected readonly submitting = signal(false);
  protected readonly errorCode = signal<string | null>(null);

  private readonly fb = inject(NonNullableFormBuilder);

  protected readonly account = this.fb.group(
    {
      username: ['', [Validators.required, Validators.pattern(USERNAME_PATTERN)]],
      password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
      confirmPassword: ['', Validators.required],
    },
    { validators: passwordsMatch },
  );

  protected readonly supplier = this.fb.group({
    companyName: ['', [Validators.required, Validators.maxLength(120)]],
    contactName: ['', [Validators.required, Validators.maxLength(120)]],
    contactEmail: ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
    contactPhone: ['', [Validators.required, Validators.pattern(PHONE_PATTERN)]],
    countryCode: ['', Validators.required],
  });

  protected readonly retailer = this.fb.group({
    storeName: ['', [Validators.required, Validators.maxLength(120)]],
    storeType: ['PHYSICAL' as StoreType, Validators.required],
    countryCode: ['', Validators.required],
    city: ['', Validators.maxLength(120)],
    address: ['', Validators.maxLength(200)],
    website: ['', [Validators.maxLength(255), Validators.pattern(WEBSITE_PATTERN)]],
    contactEmail: ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
  });

  private readonly storeType = toSignal(this.retailer.controls.storeType.valueChanges, {
    initialValue: this.retailer.controls.storeType.value,
  });
  /** BR-02: physical and hybrid stores need an address; online and hybrid stores need a website. */
  protected readonly needsAddress = computed(() => this.storeType() !== 'ONLINE');
  protected readonly needsWebsite = computed(() => this.storeType() !== 'PHYSICAL');

  constructor() {
    effect(() => {
      const needsAddress = this.needsAddress();
      const needsWebsite = this.needsWebsite();
      untracked(() => {
        const { city, address, website } = this.retailer.controls;
        toggleRequired(city, needsAddress);
        toggleRequired(address, needsAddress);
        toggleRequired(website, needsWebsite);
      });
    });
  }

  protected showError(control: AbstractControl): boolean {
    return control.invalid && (control.touched || control.dirty);
  }

  protected submit(): void {
    const company = this.accountType() === 'SUPPLIER' ? this.supplier : this.retailer;
    if (this.account.invalid || company.invalid) {
      this.account.markAllAsTouched();
      company.markAllAsTouched();
      return;
    }
    this.errorCode.set(null);
    this.submitting.set(true);
    this._auth.signUp(this.toRequest()).subscribe({
      next: (session) => {
        this._notifications.success('auth.welcome', { name: session.displayName });
        void this._router.navigateByUrl(this._personas.homeRoute());
      },
      error: (error) => {
        this.submitting.set(false);
        this.errorCode.set(toApiError(error).code);
      },
    });
  }

  private toRequest(): SignUpRequest {
    const { username, password } = this.account.getRawValue();
    if (this.accountType() === 'SUPPLIER') {
      const supplier = this.supplier.getRawValue();
      return {
        username: username.trim(),
        password,
        role: 'SUPPLIER',
        supplier: {
          companyName: supplier.companyName.trim(),
          contactName: supplier.contactName.trim(),
          contactEmail: supplier.contactEmail.trim(),
          contactPhone: supplier.contactPhone.trim(),
          countryCode: supplier.countryCode,
        },
      };
    }
    const retailer = this.retailer.getRawValue();
    return {
      username: username.trim(),
      password,
      role: 'RETAILER',
      retailer: {
        storeName: retailer.storeName.trim(),
        storeType: retailer.storeType,
        countryCode: retailer.countryCode,
        city: this.needsAddress() ? retailer.city.trim() : null,
        address: this.needsAddress() ? retailer.address.trim() : null,
        website: this.needsWebsite() ? retailer.website.trim() : null,
        contactEmail: retailer.contactEmail.trim(),
      },
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
