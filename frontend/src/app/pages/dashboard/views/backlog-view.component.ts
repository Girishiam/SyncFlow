import { Component, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DashboardViewBase } from '../dashboard-view.base';

/** Lazily loaded 'backlog' view; styled by the dashboard shell stylesheet (scoped under .sf-dashboard). */
@Component({
  selector: 'sf-backlog-view',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './backlog-view.component.html',
  encapsulation: ViewEncapsulation.None
})
export class BacklogViewComponent extends DashboardViewBase {}
