import { Observable } from 'rxjs';
import { NotificationItem, NotificationSummary } from '../models/notification.models';

export interface INotificationPort {
  getNotifications(): Observable<NotificationItem[]>;
  getSummary(): Observable<NotificationSummary>;
  markAsRead(id: number): Observable<NotificationItem>;
  markAllAsRead(): Observable<{ message: string }>;
  triggerDueCheck(): Observable<{ scannedCount: number; message: string }>;
}
