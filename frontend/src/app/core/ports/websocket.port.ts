import { Observable } from 'rxjs';

export interface IWebSocketPort {
  isConnected: () => boolean;
  getBoardUpdates(): Observable<any>;
  getBlockerAlerts(): Observable<any>;
  subscribeToChannel(channelId: number): Observable<any>;
  publish(destination: string, body: any): void;
  disconnect(): void;
}
