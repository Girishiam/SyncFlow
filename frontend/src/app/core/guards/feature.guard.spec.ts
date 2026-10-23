import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { Component } from '@angular/core';
import { featureGuard } from './feature.guard';
import { FeatureFlagsService } from '../services/feature-flags.service';

@Component({ standalone: true, template: '' })
class StubComponent {}

describe('featureGuard', () => {
  const stubConfig = (body: unknown, ok = true) =>
    spyOn(window, 'fetch').and.resolveTo(new Response(JSON.stringify(body), { status: ok ? 200 : 404 }));

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([
        { path: 'dashboard/board', component: StubComponent },
        { path: 'dashboard/financials', canMatch: [featureGuard('budget')], component: StubComponent }
      ])]
    });
  });

  it('keeps the budget page off by default and redirects to the board', async () => {
    stubConfig({}, false);
    await TestBed.inject(FeatureFlagsService).load();

    const router = TestBed.inject(Router);
    await router.navigateByUrl('/dashboard/financials');
    expect(router.url).toBe('/dashboard/board');
  });

  it('opens the budget page when app-config.json switches it on', async () => {
    stubConfig({ features: { budget: true } });
    await TestBed.inject(FeatureFlagsService).load();

    const router = TestBed.inject(Router);
    await router.navigateByUrl('/dashboard/financials');
    expect(router.url).toBe('/dashboard/financials');
  });
});
