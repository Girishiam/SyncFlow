import { Component, ViewEncapsulation } from '@angular/core';
import { SfIconComponent } from '../../../shared/icon/icon.component';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DragDropModule } from '@angular/cdk/drag-drop';
import { DashboardViewBase } from '../dashboard-view.base';

/** Lazily loaded 'board' view; styled by the dashboard shell stylesheet (scoped under .sf-dashboard). */
@Component({
  selector: 'sf-board-view',
  standalone: true,
  imports: [CommonModule, FormsModule, DragDropModule, SfIconComponent],
  templateUrl: './board-view.component.html',
  encapsulation: ViewEncapsulation.None,
  // The host is the flex child of .main-content; it must pass the bounded height down to the view.
  host: { class: 'dashboard-view-host' }
})
export class BoardViewComponent extends DashboardViewBase {}
