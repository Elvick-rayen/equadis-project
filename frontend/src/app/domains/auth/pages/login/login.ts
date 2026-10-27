import { ChangeDetectionStrategy, Component, inject, isDevMode, signal } from '@angular/core';
import { FormsModule, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { TranslocoDirective } from '@jsverse/transloco';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { PasswordModule } from 'primeng/password';
import { SelectModule } from 'primeng/select';
import { AuthService } from '@shared/auth/auth.service';
import { toApiError } from '@shared/errors/api-error';
import { LanguageStore } from '@shared/i18n/language.store';
import { LANGUAGES } from '@shared/i18n/languages';
import { PersonaStore } from '@shared/persona/persona.store';

/** Sign in with username and password. */
@Component({
  selector: 'app-login',
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
  ],
  templateUrl: './login.html',
  styleUrl: '../auth-page.scss',
})
export class Login {
  private readonly _auth = inject(AuthService);
  private readonly _router = inject(Router);
  private readonly _personas = inject(PersonaStore);
  protected readonly languages = inject(LanguageStore);

  protected readonly languageOptions = LANGUAGES.map((code) => ({ label: code.toUpperCase(), value: code }));
  /** The demo accounts are only advertised in development builds. */
  protected readonly showDemoAccounts = isDevMode();
  protected readonly submitting = signal(false);
  protected readonly errorCode = signal<string | null>(null);

  protected readonly form = inject(NonNullableFormBuilder).group({
    username: ['', Validators.required],
    password: ['', Validators.required],
  });

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.errorCode.set(null);
    this.submitting.set(true);
    this._auth.login(this.form.getRawValue()).subscribe({
      next: () => void this._router.navigateByUrl(this._personas.homeRoute()),
      error: (error) => {
        this.submitting.set(false);
        this.errorCode.set(toApiError(error).code);
      },
    });
  }
}
