import { Routes } from '@angular/router';
import { LoginComponent } from './pages/login/login.component';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'login'
  },
  {
    path: 'login',
    component: LoginComponent
  },
  {
    path: 'dashboard',
    component: DashboardComponent,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'board' },
      { path: 'board', loadComponent: () => import('./pages/dashboard/views/board-view.component').then(m => m.BoardViewComponent) },
      { path: 'roadmap', loadComponent: () => import('./pages/dashboard/views/roadmap-view.component').then(m => m.RoadmapViewComponent) },
      { path: 'backlog', loadComponent: () => import('./pages/dashboard/views/backlog-view.component').then(m => m.BacklogViewComponent) },
      { path: 'discussions', loadComponent: () => import('./pages/dashboard/views/discussions-view.component').then(m => m.DiscussionsViewComponent) },
      { path: 'analytics', loadComponent: () => import('./pages/dashboard/views/analytics-view.component').then(m => m.AnalyticsViewComponent) },
      { path: 'rbac', loadComponent: () => import('./pages/dashboard/views/rbac-view.component').then(m => m.RbacViewComponent) },
      { path: 'compliance', loadComponent: () => import('./pages/dashboard/views/compliance-view.component').then(m => m.ComplianceViewComponent) },
      { path: 'financials', loadComponent: () => import('./pages/dashboard/views/financials-view.component').then(m => m.FinancialsViewComponent) },
      { path: 'capacity', loadComponent: () => import('./pages/dashboard/views/capacity-view.component').then(m => m.CapacityViewComponent) },
      { path: 'webhooks', loadComponent: () => import('./pages/dashboard/views/webhooks-view.component').then(m => m.WebhooksViewComponent) },
      { path: '**', redirectTo: 'board' }
    ]
  },
  {
    path: '**',
    redirectTo: 'login'
  }
];
