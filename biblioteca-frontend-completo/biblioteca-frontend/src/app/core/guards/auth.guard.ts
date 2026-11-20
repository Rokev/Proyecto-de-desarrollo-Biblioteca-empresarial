import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { RolNombre } from '../models/biblioteca.models';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.autenticado() ? true : router.createUrlTree(['/login']);
};

export function roleGuard(roles: RolNombre[]): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    const router = inject(Router);
    if (!auth.autenticado()) return router.createUrlTree(['/login']);
    const r = auth.rol();
    return r && roles.includes(r) ? true : router.createUrlTree(['/acceso-denegado']);
  };
}

export const subscriptionGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.autenticado()) return router.createUrlTree(['/login']);
  return auth.esSuscriptor() ? true : router.createUrlTree(['/planes'], { queryParams: { motivo: 'suscripcion' } });
};
