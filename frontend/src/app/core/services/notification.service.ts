import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { INotificationPort } from '../ports/notification.port';
import { NotificationItem, NotificationSummary } from '../models/notification.models';

@Injectable({
  providedIn: 'root'
})
export class NotificationService implements INotificationPort {
  private readonly baseUrl = 'http://localhost:9090/api/v1/notifications';

  constructor(private http: HttpClient) {}

  getNotifications(): Observable<NotificationItem[]> {
    return this.http.get<NotificationItem[]>(this.baseUrl);
  }

  getSummary(): Observable<NotificationSummary> {
    return this.http.get<NotificationSummary>(`${this.baseUrl}/summary`);
  }

  markAsRead(id: number): Observable<NotificationItem> {
    return this.http.patch<NotificationItem>(`${this.baseUrl}/${id}/read`, {});
  }

  markAllAsRead(): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.baseUrl}/mark-all-read`, {});
  }

  triggerDueCheck(): Observable<{ scannedCount: number; message: string }> {
    return this.http.post<{ scannedCount: number; message: string }>(`${this.baseUrl}/trigger-due-check`, {});
  }
}
