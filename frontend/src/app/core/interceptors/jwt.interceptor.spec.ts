import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { jwtInterceptor } from './jwt.interceptor';
import { AuthService } from '../services/auth.service';
import { ErrorNotifierService } from '../services/error-notifier.service';

/** Builds an unsigned JWT whose payload expires {@code seconds} from now. */
function jwt(seconds: number): string {
  const payload = btoa(JSON.stringify({ sub: 'dev@syncflow.bank', exp: Math.floor(Date.now() / 1000) + seconds }));
  return `h.${payload}.s`;
}

describe('jwtInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;
  let router: jasmine.SpyObj<Router>;

  function login(accessToken: string) {
    localStorage.setItem('syncflow_jwt_token', accessToken);
    localStorage.setItem('syncflow_refresh_token', 'refresh-1');
    localStorage.setItem('syncflow_user', JSON.stringify({ id: 'u1', role: { name: 'DEVELOPER' }, permissions: [] }));
  }

  function setup() {
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([jwtInterceptor])),
        provideHttpClientTesting(),
        { provide: Router, useValue: router }
      ]
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
  }

  afterEach(() => {
    backend.verify();
    localStorage.clear();
  });

  it('attaches the bearer token', () => {
    const token = jwt(600);
    login(token);
    setup();

    http.get('/api/v1/projects').subscribe();
    const req = backend.expectOne('/api/v1/projects');
    expect(req.request.headers.get('Authorization')).toBe(`Bearer ${token}`);
    req.flush([]);
  });

  it('refreshes once after a 401 and retries the request with the new token', () => {
    login(jwt(600));
    setup();
    const fresh = jwt(900);

    let result: unknown;
    http.get('/api/v1/projects').subscribe(r => (result = r));
    backend.expectOne('/api/v1/projects').flush({ code: 'UNAUTHORIZED' }, { status: 401, statusText: 'Unauthorized' });

    const refresh = backend.expectOne(r => r.url.endsWith('/v1/auth/refresh'));
    expect(refresh.request.body).toEqual({ refreshToken: 'refresh-1' });
    refresh.flush({ token: fresh, refreshToken: 'refresh-2', expiresIn: 900, type: 'Bearer', user: { id: 'u1' } });

    const retry = backend.expectOne('/api/v1/projects');
    expect(retry.request.headers.get('Authorization')).toBe(`Bearer ${fresh}`);
    retry.flush(['ok']);
    expect(result).toEqual(['ok']);
    expect(localStorage.getItem('syncflow_refresh_token')).toBe('refresh-2');
  });

  it('refreshes proactively when the access token is about to expire', () => {
    login(jwt(5));
    setup();

    http.get('/api/v1/projects').subscribe();
    backend.expectOne(r => r.url.endsWith('/v1/auth/refresh'))
      .flush({ token: jwt(900), refreshToken: 'refresh-2', expiresIn: 900, type: 'Bearer', user: { id: 'u1' } });
    backend.expectOne('/api/v1/projects').flush([]);
    expect(localStorage.getItem('syncflow_refresh_token')).toBe('refresh-2');
  });

  it('ends the session when the refresh token is rejected', () => {
    login(jwt(600));
    setup();

    http.get('/api/v1/projects').subscribe({ error: () => undefined });
    backend.expectOne('/api/v1/projects').flush({}, { status: 401, statusText: 'Unauthorized' });
    backend.expectOne(r => r.url.endsWith('/v1/auth/refresh'))
      .flush({ code: 'INVALID_REFRESH_TOKEN' }, { status: 401, statusText: 'Unauthorized' });

    expect(TestBed.inject(AuthService).isAuthenticated()).toBeFalse();
    expect(router.navigate).toHaveBeenCalledWith(['/login'], { queryParams: { expired: 1 } });
  });

  it('surfaces 403 responses as a toast', () => {
    login(jwt(600));
    setup();

    http.put('/api/v1/auth/users/x/role', {}).subscribe({ error: () => undefined });
    backend.expectOne('/api/v1/auth/users/x/role')
      .flush({ code: 'FORBIDDEN', message: 'You do not have permission to perform this action', details: [] },
        { status: 403, statusText: 'Forbidden' });

    expect(TestBed.inject(ErrorNotifierService).toasts()[0].message)
      .toBe('You do not have permission to perform this action');
  });
});
