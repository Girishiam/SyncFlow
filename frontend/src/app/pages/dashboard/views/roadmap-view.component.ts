import { Component, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DashboardViewBase } from '../dashboard-view.base';

/** Lazily loaded 'roadmap' view; styled by the dashboard shell stylesheet (scoped under .sf-dashboard). */
@Component({
  selector: 'sf-roadmap-view',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './roadmap-view.component.html',
  encapsulation: ViewEncapsulation.None
})
export class RoadmapViewComponent extends DashboardViewBase {}
