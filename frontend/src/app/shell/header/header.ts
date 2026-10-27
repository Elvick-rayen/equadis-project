import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { translateObjectSignal, TranslocoDirective } from '@jsverse/transloco';
import { MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { MenubarModule } from 'primeng/menubar';
import { SelectModule } from 'primeng/select';
import { RetailerService } from '@domains/retailers/services/retailer.service';
import { SupplierService } from '@domains/suppliers/services/supplier.service';
import { AuthService } from '@shared/auth/auth.service';
import { LanguageStore } from '@shared/i18n/language.store';
import { Language, LANGUAGES } from '@shared/i18n/languages';
import { NotificationService } from '@shared/notifications/notification.service';
import { PersonaStore } from '@shared/persona/persona.store';
import { PERSONA_MENU } from '../navigation/persona-menu';

interface PersonaOption {
  label: string;
  value: string;
}

interface PersonaGroup {
  label: string;
  items: PersonaOption[];
}

type Labels = Record<string, string> | undefined;

/**
 * Navy top bar: persona menu on the left; language, persona and sign-out on the right.
 * Administrators get the "Acting as" selector; suppliers and retailers see their company.
 */
@Component({
  selector: 'app-header',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, TranslocoDirective, MenubarModule, SelectModule, ButtonModule],
  templateUrl: './header.html',
  styleUrl: './header.scss',
})
export class Header {
  private readonly _router = inject(Router);
  private readonly _suppliers = inject(SupplierService);
  private readonly _retailers = inject(RetailerService);
  private readonly _notifications = inject(NotificationService);
  protected readonly auth = inject(AuthService);
  protected readonly personas = inject(PersonaStore);
  protected readonly languages = inject(LanguageStore);

  private readonly navLabels = translateObjectSignal('nav');
  private readonly personaLabels = translateObjectSignal('persona');

  protected readonly languageOptions = LANGUAGES.map((code) => ({ label: code.toUpperCase(), value: code }));

  protected readonly menuItems = computed<MenuItem[]>(() => {
    const labels = this.navLabels() as Labels;
    return PERSONA_MENU[this.personas.role()].map((item) => ({
      label: labels?.[item.labelKey] ?? item.labelKey,
      icon: item.icon,
      routerLink: item.route,
    }));
  });

  protected readonly personaGroups = computed<PersonaGroup[]>(() => {
    const labels = this.personaLabels() as Labels;
    return [
      {
        label: labels?.['administration'] ?? '',
        items: [{ label: labels?.['administrator'] ?? '', value: 'ADMIN' }],
      },
      {
        label: labels?.['suppliers'] ?? '',
        // Both lists come sorted by name from the API.
        items: this._suppliers.summaries().map((s) => ({ label: s.companyName, value: `SUPPLIER:${s.id}` })),
      },
      {
        label: labels?.['retailers'] ?? '',
        items: this._retailers.summaries().map((r) => ({ label: r.storeName, value: `RETAILER:${r.id}` })),
      },
    ];
  });

  /** "Administrator", or e.g. "Supplier · Alpina Foods". */
  protected readonly actingAsLabel = computed(() => {
    const labels = this.personaLabels() as Labels;
    switch (this.personas.role()) {
      case 'ADMIN':
        return labels?.['administrator'] ?? '';
      case 'SUPPLIER':
        return `${labels?.['supplier'] ?? ''} · ${this.personas.displayName() ?? ''}`;
      case 'RETAILER':
        return `${labels?.['retailer'] ?? ''} · ${this.personas.displayName() ?? ''}`;
    }
  });

  protected actAs(personaKey: string): void {
    if (personaKey === this.personas.key()) return;
    this.auth.actAs(personaKey).subscribe({
      next: () => void this._router.navigateByUrl(this.personas.homeRoute()),
      error: (error) => this._notifications.error(error),
    });
  }

  protected signOut(): void {
    this.auth.logout();
    void this._router.navigateByUrl('/auth/login');
  }

  protected goHome(): void {
    void this._router.navigateByUrl(this.personas.homeRoute());
  }

  protected setLanguage(language: Language): void {
    this.languages.setLanguage(language);
  }
}
