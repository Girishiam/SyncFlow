import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { AuthResponse, LoginRequest, User } from '../models/auth.models';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly apiUrl = 'http://localhost:9090/api/v1/auth';
  private readonly TOKEN_KEY = 'syncflow_jwt_token';
  private readonly USER_KEY = 'syncflow_user';

  // Angular 18 Signal State
  currentUserSignal = signal<User | null>(this.getStoredUser());
  tokenSignal = signal<string | null>(this.getStoredToken());

  isAuthenticated = computed(() => !!this.tokenSignal());
  userRole = computed(() => this.currentUserSignal()?.role?.name || '');

  constructor(private http: HttpClient, private router: Router) {}

  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, credentials).pipe(
      tap(response => {
        this.setSession(response);
      })
    );
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
    this.tokenSignal.set(null);
    this.currentUserSignal.set(null);
    this.router.navigate(['/login']);
  }

  hasPermission(permissionString: string): boolean {
    const user = this.currentUserSignal();
    if (!user) return false;
    if (user.role?.name === 'ADMIN') return true;
    return user.permissions?.includes(permissionString) ?? false;
  }

  getToken(): string | null {
    return this.tokenSignal();
  }

  private setSession(authResponse: AuthResponse): void {
    localStorage.setItem(this.TOKEN_KEY, authResponse.token);
    localStorage.setItem(this.USER_KEY, JSON.stringify(authResponse.user));
    this.tokenSignal.set(authResponse.token);
    this.currentUserSignal.set(authResponse.user);
  }

  private getStoredToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  private getStoredUser(): User | null {
    const raw = localStorage.getItem(this.USER_KEY);
    return raw ? JSON.parse(raw) : null;
  }
}
