import { DOCUMENT } from '@angular/common';
import { effect, inject, Injectable, signal } from '@angular/core';
import { TranslocoService } from '@jsverse/transloco';
import { readStorage, writeStorage } from '../storage/local-storage';
import { DEFAULT_LANGUAGE, isLanguage, Language } from './languages';

const STORAGE_KEY = 'equadis.language';

/** Current UI language, kept in sync with Transloco, <html lang> and localStorage. */
@Injectable({ providedIn: 'root' })
export class LanguageStore {
  private readonly transloco = inject(TranslocoService);
  private readonly document = inject(DOCUMENT);

  private readonly _language = signal<Language>(this.initialLanguage());
  readonly language = this._language.asReadonly();

  constructor() {
    effect(() => {
      const language = this._language();
      this.transloco.setActiveLang(language);
      this.document.documentElement.lang = language;
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
