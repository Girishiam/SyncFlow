import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
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

  // Demo accounts for instant testing
  demoAccounts = [
    { role: 'Reporting Manager', email: 'manager@syncflow.bank', pass: 'Manager@123', badge: 'Executive Control' },
    { role: 'Team Lead', email: 'lead@syncflow.bank', pass: 'Lead@123', badge: 'Pipeline Builder' },
    { role: 'Senior Developer', email: 'dev@syncflow.bank', pass: 'Dev@123', badge: 'Workspace & Kanban' }
  ];

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]]
    });
  }

  fillDemo(account: { email: string; pass: string }) {
    this.loginForm.patchValue({
      email: account.email,
      password: account.pass
    });
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
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set(
          err.error?.message || 'Invalid credentials or unauthorized access. Please verify and try again.'
        );
      }
    });
  }
}
