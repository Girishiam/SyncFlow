import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { environment } from '../../../environments/environment';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent {
  loginForm: FormGroup;
  isLoading = signal(false);
  errorMessage = signal<string | null>(null);

  // Demo accounts exist only when the backend runs with the dev profile; never shown in production builds.
  demoRoles = environment.production ? [] : [
    { label: 'Manager', email: 'manager@syncflow.bank', pass: 'Manager@123' },
    { label: 'Team Lead', email: 'lead@syncflow.bank', pass: 'Lead@123' },
    { label: 'Developer', email: 'dev@syncflow.bank', pass: 'Dev@123' }
  ];

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    route: ActivatedRoute
  ) {
    if (route.snapshot.queryParamMap.get('expired')) {
      this.errorMessage.set('Your session has expired. Please sign in again.');
    }
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]]
    });
  }

  fillRole(role: { email: string; pass: string }) {
    this.loginForm.patchValue({
      email: role.email,
      password: role.pass
    });
    this.errorMessage.set(null);
  }

  onSubmit() {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.authService.login(this.loginForm.value).subscribe({
      next: () => {
        this.isLoading.set(false);
        const returnUrl = this.router.parseUrl(this.router.url).queryParams['returnUrl'];
        this.router.navigateByUrl(typeof returnUrl === 'string' && returnUrl.startsWith('/') ? returnUrl : '/dashboard');
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set(
          err.error?.message || 'Invalid email or password. Please try again.'
        );
      }
    });
  }
}
