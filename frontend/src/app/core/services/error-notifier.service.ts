import { Injectable, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ApiError } from '../models/auth.models';

export interface Toast {
  id: number;
  message: string;
  details: string[];
  /** Errors are red; info toasts confirm that something worked. */
  kind: 'error' | 'info';
}

/** Shows short toasts: errors from failed API calls, and confirmations of completed actions. */
@Injectable({ providedIn: 'root' })
export class ErrorNotifierService {
  readonly toasts = signal<Toast[]>([]);
  private nextId = 1;

  /** Extracts the structured server error, falling back to a generic message. */
  static apiError(err: unknown): ApiError | null {
    if (err instanceof HttpErrorResponse && err.error && typeof err.error === 'object' && 'code' in err.error) {
      return err.error as ApiError;
    }
    return null;
  }

  static message(err: unknown): string {
    const api = ErrorNotifierService.apiError(err);
    if (api?.message) return api.message;
    if (err instanceof HttpErrorResponse && err.status === 0) return 'Cannot reach the SyncFlow server.';
    return 'Something went wrong. Please try again.';
  }

  show(message: string, details: string[] = [], kind: Toast['kind'] = 'error'): void {
    const toast: Toast = { id: this.nextId++, message, details, kind };
    this.toasts.update(list => [...list.slice(-3), toast]);
    setTimeout(() => this.dismiss(toast.id), 6000);
  }

  showInfo(message: string): void {
    this.show(message, [], 'info');
  }

  showError(err: unknown): void {
    this.show(ErrorNotifierService.message(err), ErrorNotifierService.apiError(err)?.details ?? []);
  }

  /**
   * For component-level error handlers: shows validation/conflict errors (4xx). 401, 403 and 5xx are
   * already surfaced by the HTTP interceptor, so they are skipped here to avoid duplicate toasts.
   */
  showClientError(err: unknown): void {
    const status = err instanceof HttpErrorResponse ? err.status : 0;
    if (status >= 400 && status < 500 && status !== 401 && status !== 403) {
      this.showError(err);
    }
  }

  dismiss(id: number): void {
    this.toasts.update(list => list.filter(t => t.id !== id));
  }
}
