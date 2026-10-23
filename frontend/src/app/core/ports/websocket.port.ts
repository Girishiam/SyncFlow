import { Observable } from 'rxjs';

export interface IWebSocketPort {
  isConnected: () => boolean;
  watchProject(projectId: number): void;
  getBoardUpdates(): Observable<any>;
  getBlockerAlerts(): Observable<any>;
  subscribeToChannel(channelId: number): Observable<any>;
  subscribeToNotifications(): Observable<any>;
  disconnect(): void;
}
