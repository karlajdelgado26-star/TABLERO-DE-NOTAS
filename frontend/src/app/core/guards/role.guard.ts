import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { Role } from '../../shared/models/user.model';

/** Permite entrar solo a los roles indicados; el resto vuelve a la lista de tableros. */
export const roleGuard = (allowedRoles: Role[]): CanActivateFn => () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isAuthenticated()) {
    return router.parseUrl('/login');
  }

  return authService.hasAnyRole(allowedRoles) ? true : router.parseUrl('/boards');
};
