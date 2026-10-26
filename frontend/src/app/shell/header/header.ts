import { Component, computed, effect, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { translateObjectSignal, TranslocoDirective } from '@jsverse/transloco';
import { MenuItem } from 'primeng/api';
import { MenubarModule } from 'primeng/menubar';
import { SelectModule } from 'primeng/select';
import { RetailerStore } from '../../domains/retailers/data-access/retailer.store';
import { SupplierStore } from '../../domains/suppliers/data-access/supplier.store';
import { LanguageStore } from '../../shared/i18n/language.store';
import { Language, LANGUAGES } from '../../shared/i18n/languages';
import { personaFromKey } from '../../shared/persona/persona.model';
import { PersonaStore } from '../../shared/persona/persona.store';
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

/** Navy top bar: persona menu on the left, language and "Acting as" selectors on the right. */
@Component({
  selector: 'app-header',
  imports: [FormsModule, TranslocoDirective, MenubarModule, SelectModule],
  templateUrl: './header.html',
  styleUrl: './header.scss',
})
export class Header {
  private readonly router = inject(Router);
  private readonly suppliers = inject(SupplierStore);
  private readonly retailers = inject(RetailerStore);
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
        // Already sorted by name by the API.
        items: this.suppliers.summaries().map((s) => ({ label: s.companyName, value: `SUPPLIER:${s.id}` })),
      },
      {
        label: labels?.['retailers'] ?? '',
        items: [...this.retailers.retailers()]
          .sort((a, b) => a.storeName.localeCompare(b.storeName))
          .map((r) => ({ label: r.storeName, value: `RETAILER:${r.id}` })),
      },
    ];
  });

  /** Text shown in the closed selector, e.g. "Supplier · Alpina Foods". */
  protected readonly actingAsLabel = computed(() => {
    const labels = this.personaLabels() as Labels;
    const persona = this.personas.persona();
    switch (persona.role) {
      case 'ADMIN':
        return labels?.['administrator'] ?? '';
      case 'SUPPLIER':
        return `${labels?.['supplier'] ?? ''} · ${this.suppliers.summaryById().get(persona.supplierId)?.companyName ?? '…'}`;
      case 'RETAILER':
        return `${labels?.['retailer'] ?? ''} · ${this.retailers.byId().get(persona.retailerId)?.storeName ?? '…'}`;
    }
  });

  constructor() {
    this.retailers.load();

    // A remembered persona may point to a company that no longer exists: fall back to administrator.
    effect(() => {
      if (!this.suppliers.summariesLoaded() || !this.retailers.loaded()) return;
      const persona = this.personas.persona();
      const exists =
        persona.role === 'ADMIN' ||
        (persona.role === 'SUPPLIER' && this.suppliers.summaryById().has(persona.supplierId)) ||
        (persona.role === 'RETAILER' && this.retailers.byId().has(persona.retailerId));
      if (!exists) {
        this.personas.reset();
        void this.router.navigateByUrl(this.personas.homeRoute());
      }
    });
  }

  protected actAs(key: string): void {
    const persona = personaFromKey(key);
    if (!persona) return;
    this.personas.actAs(persona);
    void this.router.navigateByUrl(this.personas.homeRoute());
  }

  protected goHome(): void {
    void this.router.navigateByUrl(this.personas.homeRoute());
  }

  protected setLanguage(language: Language): void {
    this.languages.setLanguage(language);
  }
}
