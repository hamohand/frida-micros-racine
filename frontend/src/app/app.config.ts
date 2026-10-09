import { ApplicationConfig } from '@angular/core';
import { NavigationError, provideRouter, withNavigationErrorHandler } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideTranslateService } from '@ngx-translate/core';
import { provideTranslateHttpLoader } from '@ngx-translate/http-loader';
import { routes } from './app.routes';
import { authInterceptor } from './services/auth.interceptor';

function rechargerSiVersionObsolete(erreur: NavigationError) {
  const message = String(erreur.error?.message ?? erreur.error ?? '');
  const fichierManquant = /dynamically imported module|Importing a module script failed|Loading chunk/i.test(message);
  if (!fichierManquant) return;
  const dernier = Number(sessionStorage.getItem('rechargementVersion') || 0);
  if (Date.now() - dernier < 60_000) return;
  sessionStorage.setItem('rechargementVersion', String(Date.now()));
  window.location.assign(erreur.url);
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes, withNavigationErrorHandler(rechargerSiVersionObsolete)),
    provideHttpClient(withInterceptors([authInterceptor])),
    provideTranslateService({
      fallbackLang: 'fr',
      lang: 'fr'
    }),
    provideTranslateHttpLoader({
      prefix: './assets/i18n/',
      suffix: '.json'
    })
  ]
};
