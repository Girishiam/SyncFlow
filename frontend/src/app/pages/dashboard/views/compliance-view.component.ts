import { Component, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DashboardViewBase } from '../dashboard-view.base';

/** Lazily loaded 'compliance' view; styled by the dashboard shell stylesheet (scoped under .sf-dashboard). */
@Component({
  selector: 'sf-compliance-view',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './compliance-view.component.html',
  encapsulation: ViewEncapsulation.None
})
export class ComplianceViewComponent extends DashboardViewBase {}
