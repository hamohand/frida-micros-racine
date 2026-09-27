import { ApplicationConfig } from '@angular/core';
import { NavigationError, provideRouter, withNavigationErrorHandler } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { routes } from './app.routes';
import { authInterceptor } from './services/auth.interceptor';

/**
 * Un onglet ouvert avant une mise à jour réclame des fichiers JS qui n'existent plus
 * (leur nom change à chaque compilation) : la navigation échouait alors sans message.
 * On recharge la page sur l'URL visée pour récupérer la nouvelle version.
 */
function rechargerSiVersionObsolete(erreur: NavigationError) {
  const message = String(erreur.error?.message ?? erreur.error ?? '');
  const fichierManquant = /dynamically imported module|Importing a module script failed|Loading chunk/i.test(message);
  if (!fichierManquant) return;

  // Garde-fou : pas plus d'un rechargement par minute, pour ne jamais boucler
  const dernier = Number(sessionStorage.getItem('rechargementVersion') || 0);
  if (Date.now() - dernier < 60_000) return;
  sessionStorage.setItem('rechargementVersion', String(Date.now()));
  window.location.assign(erreur.url);
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes, withNavigationErrorHandler(rechargerSiVersionObsolete)),
    provideHttpClient(withInterceptors([authInterceptor]))
  ]
};
