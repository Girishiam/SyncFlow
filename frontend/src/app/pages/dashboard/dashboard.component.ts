import { Component, DestroyRef, OnInit, ViewEncapsulation, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterOutlet } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DashboardViewBase } from './dashboard-view.base';
import { DashboardView, DASHBOARD_VIEWS } from './dashboard-nav.service';

/**
 * Dashboard shell: sidebar, header, drawers and modals. The active view is a lazily loaded child
 * route (see app.routes.ts) rendered in the router outlet. Styles: dashboard.styles.scss (global,
 * scoped under .sf-dashboard).
 */
@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterOutlet],
  templateUrl: './dashboard.component.html',
  encapsulation: ViewEncapsulation.None
})
export class DashboardComponent extends DashboardViewBase implements OnInit {
  private destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    // Legacy deep links (?tab=rbac) keep working.
    this.route.queryParams.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(params => {
      const tab = params['tab'];
      if (DASHBOARD_VIEWS.includes(tab)) {
        this.nav.go(tab as DashboardView);
      }
    });

    this.nav.entered$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(view => this.loadViewData(view));

    this.boardFacade.init();
    this.chatFacade.init();
    this.rbacFacade.init();
    this.notificationFacade.init(this.currentUser()?.id, this.currentUser()?.role?.name);
    this.loadViewData(this.nav.activeNav());
  }

  /** Fetches the data a view needs when it becomes active. */
  private loadViewData(view: DashboardView): void {
    if (view === 'roadmap') this.boardFacade.loadTimeline();
    if (view === 'backlog') this.boardFacade.loadSprints();
    if (view === 'analytics') {
      this.boardFacade.loadBurndown();
      this.boardFacade.loadTeamCapacity();
    }
    if (view === 'compliance') this.boardFacade.loadAuditData();
    if (view === 'financials') this.boardFacade.loadFinancials();
    if (view === 'capacity') this.boardFacade.loadTeamCapacity();
    if (view === 'webhooks') this.boardFacade.loadWebhooks();
  }
}
