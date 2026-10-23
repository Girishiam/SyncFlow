import { Component, ViewEncapsulation } from '@angular/core';
import { SfIconComponent } from '../../../shared/icon/icon.component';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DragDropModule } from '@angular/cdk/drag-drop';
import { DashboardViewBase } from '../dashboard-view.base';

/** Lazily loaded 'backlog' view: sprint planning by drag and drop. Styled by the dashboard shell stylesheet. */
@Component({
  selector: 'sf-backlog-view',
  standalone: true,
  imports: [CommonModule, FormsModule, DragDropModule, SfIconComponent],
  templateUrl: './backlog-view.component.html',
  encapsulation: ViewEncapsulation.None,
  // The host is the flex child of .main-content; it must pass the bounded height down to the view.
  host: { class: 'dashboard-view-host' }
})
export class BacklogViewComponent extends DashboardViewBase {}
