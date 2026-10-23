import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { Component } from '@angular/core';
import { DashboardNavService, DashboardView } from './dashboard-nav.service';

@Component({ standalone: true, template: '' })
class StubComponent {}

describe('DashboardNavService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([{ path: 'dashboard/:view', component: StubComponent }])]
    });
  });

  it('tracks the active view from the URL and emits when it changes', async () => {
    const nav = TestBed.inject(DashboardNavService);
    const entered: DashboardView[] = [];
    nav.entered$.subscribe(v => entered.push(v));

    await TestBed.inject(Router).navigateByUrl('/dashboard/analytics');
    expect(nav.activeNav()).toBe('analytics');

    await TestBed.inject(Router).navigateByUrl('/dashboard/not-a-view');
    expect(nav.activeNav()).toBe('board');
    expect(entered).toEqual(['analytics', 'board']);
  });
});
