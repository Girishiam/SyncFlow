import { CanMatchFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

/** Lets a route match only for users holding the permission; everyone else lands on the board. */
export const permissionGuard = (permission: string): CanMatchFn => () =>
  inject(AuthService).hasPermission(permission) || inject(Router).createUrlTree(['/dashboard/board']);
