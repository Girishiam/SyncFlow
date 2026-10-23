import { Component, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DashboardViewBase } from '../dashboard-view.base';

/** Lazily loaded 'analytics' view; styled by the dashboard shell stylesheet (scoped under .sf-dashboard). */
@Component({
  selector: 'sf-analytics-view',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './analytics-view.component.html',
  encapsulation: ViewEncapsulation.None
})
export class AnalyticsViewComponent extends DashboardViewBase {}
