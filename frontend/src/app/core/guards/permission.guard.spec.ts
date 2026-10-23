import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { Component } from '@angular/core';
import { permissionGuard } from './permission.guard';
import { AuthService } from '../services/auth.service';

@Component({ standalone: true, template: '' })
class StubComponent {}

describe('permissionGuard', () => {
  const setup = (granted: string[]) => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([
          { path: 'dashboard/board', component: StubComponent },
          { path: 'dashboard/compliance', canMatch: [permissionGuard('AUDIT:VIEW')], component: StubComponent }
        ]),
        { provide: AuthService, useValue: { hasPermission: (p: string) => granted.includes(p) } }
      ]
    });
    return TestBed.inject(Router);
  };

  it('sends people without the permission to the board', async () => {
    const router = setup(['TASK:VIEW']);
    await router.navigateByUrl('/dashboard/compliance');
    expect(router.url).toBe('/dashboard/board');
  });

  it('opens the page for people who hold the permission', async () => {
    const router = setup(['AUDIT:VIEW']);
    await router.navigateByUrl('/dashboard/compliance');
    expect(router.url).toBe('/dashboard/compliance');
  });
});
