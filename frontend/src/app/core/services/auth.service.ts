import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, finalize, map, shareReplay, tap } from 'rxjs';
import { AuthResponse, ElevationResponse, LoginRequest, User } from '../models/auth.models';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly apiUrl = `${environment.apiBaseUrl}/v1/auth`;
  private readonly TOKEN_KEY = 'syncflow_jwt_token';
  private readonly REFRESH_KEY = 'syncflow_refresh_token';
  private readonly USER_KEY = 'syncflow_user';

  currentUserSignal = signal<User | null>(this.getStoredUser());
  tokenSignal = signal<string | null>(localStorage.getItem(this.TOKEN_KEY));
  private refreshTokenSignal = signal<string | null>(localStorage.getItem(this.REFRESH_KEY));

  /** A session exists while we hold a refresh token; the access token is renewed on demand. */
  isAuthenticated = computed(() => !!this.tokenSignal() && !!this.refreshTokenSignal());
  userRole = computed(() => this.currentUserSignal()?.role?.name || '');

  private refreshInFlight$: Observable<string> | null = null;

  constructor(private http: HttpClient, private router: Router) {}

  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, credentials).pipe(
      tap(response => this.setSession(response))
    );
  }

  /**
   * Exchanges the refresh token for a new token pair. Concurrent callers share one request so a
   * burst of 401s triggers a single refresh (refresh tokens are single-use on the server).
   */
  refreshAccessToken(): Observable<string> {
    if (!this.refreshInFlight$) {
      const refreshToken = this.refreshTokenSignal();
      this.refreshInFlight$ = this.http
        .post<AuthResponse>(`${this.apiUrl}/refresh`, { refreshToken })
        .pipe(
          tap(response => this.setSession(response)),
          map(response => response.token),
          finalize(() => (this.refreshInFlight$ = null)),
          shareReplay(1)
        );
    }
    return this.refreshInFlight$;
  }

  logout(): void {
    const refreshToken = this.refreshTokenSignal();
    if (refreshToken) {
      // Best effort server-side revocation; the local session is cleared regardless.
      this.http.post(`${this.apiUrl}/logout`, { refreshToken }).subscribe({ error: () => undefined });
    }
    this.clearSession();
    this.router.navigate(['/login']);
  }

  /** Clears local state without contacting the server (used when the session is already invalid). */
  clearSession(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.REFRESH_KEY);
    localStorage.removeItem(this.USER_KEY);
    this.tokenSignal.set(null);
    this.refreshTokenSignal.set(null);
    this.currentUserSignal.set(null);
  }

  /** Re-enter the password to unlock a privileged action (e.g. overriding a workflow gate) for a few minutes. */
  elevate(password: string, scope: string): Observable<ElevationResponse> {
    return this.http.post<ElevationResponse>(`${this.apiUrl}/elevate`, { password, scope });
  }

  /** UI hint only; the server enforces every permission. */
  hasPermission(permissionString: string): boolean {
    const user = this.currentUserSignal();
    if (!user) return false;
    if (user.role?.name === 'ADMIN') return true;
    return user.permissions?.includes(permissionString) ?? false;
  }

  /**
   * Re-reads the signed-in user (role and permissions) from the server, so a role change made by an
   * admin shows up without signing out.
   */
  refreshProfile(): void {
    if (!this.isAuthenticated()) return;
    this.http.get<User>(`${this.apiUrl}/me`).subscribe({
      next: user => {
        localStorage.setItem(this.USER_KEY, JSON.stringify(user));
        this.currentUserSignal.set(user);
      },
      error: () => { /* keep the stored profile; the interceptor handles expired sessions */ }
    });
  }

  getToken(): string | null {
    return this.tokenSignal();
  }

  /** True when the access token expires within {@code skewSeconds}. */
  isAccessTokenExpiring(skewSeconds = 30): boolean {
    const token = this.tokenSignal();
    if (!token) return true;
    try {
      const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
      return payload.exp * 1000 - Date.now() < skewSeconds * 1000;
    } catch {
      return true;
    }
  }

  private setSession(authResponse: AuthResponse): void {
    localStorage.setItem(this.TOKEN_KEY, authResponse.token);
    localStorage.setItem(this.REFRESH_KEY, authResponse.refreshToken);
    localStorage.setItem(this.USER_KEY, JSON.stringify(authResponse.user));
    this.tokenSignal.set(authResponse.token);
    this.refreshTokenSignal.set(authResponse.refreshToken);
    this.currentUserSignal.set(authResponse.user);
  }

  private getStoredUser(): User | null {
    const raw = localStorage.getItem(this.USER_KEY);
    try {
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  }
}
