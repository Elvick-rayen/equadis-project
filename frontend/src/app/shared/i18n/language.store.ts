import { DOCUMENT } from '@angular/common';
import { effect, inject, Injectable, signal } from '@angular/core';
import { TranslocoService } from '@jsverse/transloco';
import { PrimeNG } from 'primeng/config';
import { readStorage, writeStorage } from '../storage/local-storage';
import { DEFAULT_LANGUAGE, isLanguage, Language } from './languages';
import { PRIMENG_LOCALES } from './primeng-locales';

const STORAGE_KEY = 'equadis.language';

/** Current UI language, kept in sync with Transloco, <html lang> and localStorage. */
@Injectable({ providedIn: 'root' })
export class LanguageStore {
  private readonly _transloco = inject(TranslocoService);
  private readonly _document = inject(DOCUMENT);
  private readonly _primeng = inject(PrimeNG);

  private readonly _language = signal<Language>(this.initialLanguage());
  readonly language = this._language.asReadonly();

  constructor() {
    effect(() => {
      const language = this._language();
      this._transloco.setActiveLang(language);
      this._primeng.setTranslation(PRIMENG_LOCALES[language]);
      this._document.documentElement.lang = language;
      writeStorage(STORAGE_KEY, language);
    });
  }

  setLanguage(language: Language): void {
    this._language.set(language);
  }

  private initialLanguage(): Language {
    const stored = readStorage<string>(STORAGE_KEY);
    return isLanguage(stored) ? stored : DEFAULT_LANGUAGE;
  }
}
