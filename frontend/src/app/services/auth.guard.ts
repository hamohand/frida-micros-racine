import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from './auth.service';
import { map, catchError } from 'rxjs/operators';
import { of } from 'rxjs';

export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isLoggedIn()) {
    return true;
  } else {
    return authService.getSecurityStatus().pipe(
      map(res => {
        if (res.demoMode === true || res.demoMode === 'true') {
          authService.loginDemo();
          return true;
        }
        router.navigate(['/login']);
        return false;
      }),
      catchError(() => {
        router.navigate(['/login']);
        return of(false);
      })
    );
  }
};

export const maitreGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isLoggedIn() && authService.isMaitre()) {
    return true;
  } else {
    // Si pas connecté, authGuard s'occupera d'abord du mode démo sur les autres routes
    // Mais s'il atterrit ici directement
    if (!authService.isLoggedIn()) {
      return authService.getSecurityStatus().pipe(
        map(res => {
          if (res.demoMode === true || res.demoMode === 'true') {
            authService.loginDemo();
            return true; // En mode démo, l'utilisateur est MAITRE
          }
          router.navigate(['/login']);
          return false;
        }),
        catchError(() => {
          router.navigate(['/login']);
          return of(false);
        })
      );
    }

    router.navigate(['/list']);
    return false;
  }
};
