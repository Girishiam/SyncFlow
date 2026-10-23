import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { ErrorNotifierService } from '../services/error-notifier.service';

const PUBLIC_AUTH_PATHS = ['/v1/auth/login', '/v1/auth/refresh', '/v1/auth/logout'];

const withToken = (req: HttpRequest<unknown>, token: string | null) =>
  token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

/**
 * Attaches the access token, refreshes it shortly before expiry or after a 401 (retrying the request
 * once), ends the session when refresh fails, and surfaces 403/5xx errors as toasts.
 */
export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const notifier = inject(ErrorNotifierService);

  if (PUBLIC_AUTH_PATHS.some(p => req.url.includes(p))) {
    return next(req);
  }

  const endSession = (err: unknown) => {
    auth.clearSession();
    router.navigate(['/login'], { queryParams: { expired: 1 } });
    return throwError(() => err);
  };

  const send = (token: string | null) =>
    next(withToken(req, token)).pipe(
      catchError((err: HttpErrorResponse) => {
        if (err.status === 403) {
          notifier.showError(err);
        } else if (err.status >= 500) {
          notifier.show('The server could not complete the request.');
        }
        return throwError(() => err);
      })
    );

  if (!auth.isAuthenticated()) {
    return next(req);
  }

  const start$ = auth.isAccessTokenExpiring()
    ? auth.refreshAccessToken().pipe(catchError(endSession), switchMap(token => send(token)))
    : send(auth.getToken());

  return start$.pipe(
    catchError((err: unknown) => {
      if (err instanceof HttpErrorResponse && err.status === 401) {
        return auth.refreshAccessToken().pipe(
          catchError(endSession),
          switchMap(token => send(token))
        );
      }
      return throwError(() => err);
    })
  );
};
