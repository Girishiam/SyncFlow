import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ErrorNotifierService } from './core/services/error-notifier.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent {
  readonly notifier = inject(ErrorNotifierService);
}
