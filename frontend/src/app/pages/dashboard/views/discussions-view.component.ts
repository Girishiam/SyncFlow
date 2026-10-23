import { Component, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DashboardViewBase } from '../dashboard-view.base';

/** Lazily loaded 'discussions' view; styled by the dashboard shell stylesheet (scoped under .sf-dashboard). */
@Component({
  selector: 'sf-discussions-view',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './discussions-view.component.html',
  encapsulation: ViewEncapsulation.None
})
export class DiscussionsViewComponent extends DashboardViewBase {}
