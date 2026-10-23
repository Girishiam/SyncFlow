import { Injectable, inject, signal } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { Subject, filter } from 'rxjs';

export const DASHBOARD_VIEWS = ['board', 'roadmap', 'backlog', 'discussions', 'analytics', 'rbac',
  'compliance', 'financials', 'capacity', 'webhooks'] as const;
export type DashboardView = typeof DASHBOARD_VIEWS[number];

/** The active dashboard view, derived from the URL (/dashboard/<view>). */
@Injectable({ providedIn: 'root' })
export class DashboardNavService {
  private readonly router = inject(Router);
  readonly activeNav = signal<DashboardView>(this.parse(this.router.url));
  /** Emits each time a different view becomes active. */
  readonly entered$ = new Subject<DashboardView>();

  constructor() {
    this.router.events.pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd)).subscribe(e => {
      const view = this.parse(e.urlAfterRedirects);
      if (view !== this.activeNav()) {
        this.activeNav.set(view);
        this.entered$.next(view);
      }
    });
  }

  go(view: DashboardView): void {
    this.router.navigate(['/dashboard', view]);
  }

  private parse(url: string): DashboardView {
    const segment = url.split('?')[0].split('/')[2] as DashboardView;
    return DASHBOARD_VIEWS.includes(segment) ? segment : 'board';
  }
}
