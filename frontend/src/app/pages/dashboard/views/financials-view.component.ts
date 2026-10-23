import { Component, ViewEncapsulation } from '@angular/core';
import { SfIconComponent } from '../../../shared/icon/icon.component';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DashboardViewBase } from '../dashboard-view.base';

/** Lazily loaded 'financials' view; styled by the dashboard shell stylesheet (scoped under .sf-dashboard). */
@Component({
  selector: 'sf-financials-view',
  standalone: true,
  imports: [CommonModule, FormsModule, SfIconComponent],
  templateUrl: './financials-view.component.html',
  encapsulation: ViewEncapsulation.None,
  // The host is the flex child of .main-content; it must pass the bounded height down to the view.
  host: { class: 'dashboard-view-host' }
})
export class FinancialsViewComponent extends DashboardViewBase {}
