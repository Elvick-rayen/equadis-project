import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, inject, isDevMode, provideAppInitializer, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideTransloco } from '@jsverse/transloco';
import { ConfirmationService, MessageService } from 'primeng/api';
import { providePrimeNG } from 'primeng/config';

import { routes } from './app.routes';
import { actingAsInterceptor } from './shared/http/acting-as.interceptor';
import { apiErrorInterceptor } from './shared/http/api-error.interceptor';
import { LanguageStore } from './shared/i18n/language.store';
import { DEFAULT_LANGUAGE, LANGUAGES } from './shared/i18n/languages';
import { TranslocoHttpLoader } from './shared/i18n/transloco-http.loader';
import { EquadisPreset } from './shared/theme/equadis.preset';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withInterceptors([actingAsInterceptor, apiErrorInterceptor])),
    provideTransloco({
      config: {
        availableLangs: [...LANGUAGES],
        defaultLang: DEFAULT_LANGUAGE,
        fallbackLang: DEFAULT_LANGUAGE,
        reRenderOnLangChange: true,
        prodMode: !isDevMode(),
      },
      loader: TranslocoHttpLoader,
    }),
    // Applies the remembered language before the first render.
    provideAppInitializer(() => {
      inject(LanguageStore);
    }),
    providePrimeNG({
      theme: {
        preset: EquadisPreset,
        options: { darkModeSelector: false },
      },
    }),
    MessageService,
    ConfirmationService,
  ],
};
