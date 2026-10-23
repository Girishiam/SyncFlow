import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AppComponent } from './app.component';
import { ErrorNotifierService } from './core/services/error-notifier.service';

describe('AppComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [provideRouter([])]
    }).compileComponents();
  });

  it('renders queued error toasts and lets the user dismiss them', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const notifier = TestBed.inject(ErrorNotifierService);

    notifier.show('Workflow gate requirements are not met', ['Checklist incomplete']);
    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.toast')?.textContent).toContain('Workflow gate requirements are not met');
    expect(el.querySelector('.toast li')?.textContent).toContain('Checklist incomplete');

    (el.querySelector('.toast-close') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(el.querySelector('.toast')).toBeNull();
  });
});
