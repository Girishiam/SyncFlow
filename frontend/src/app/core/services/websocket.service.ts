import { Injectable, signal } from '@angular/core';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { Observable, Subject } from 'rxjs';
import { IWebSocketPort } from '../ports/websocket.port';

@Injectable({
  providedIn: 'root'
})
export class WebSocketService implements IWebSocketPort {
  private client: Client;
  public isConnected = signal(false);

  private boardUpdates$ = new Subject<any>();
  private blockerAlerts$ = new Subject<any>();
  private activeChannelSubject: Subject<any> | null = null;
  private activeChannelSubId: any = null;

  constructor() {
    this.client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:9090/api/ws'),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      debug: (msg) => {
        // quiet in production
      }
    });

    this.client.onConnect = () => {
      this.isConnected.set(true);
      console.log('SyncFlow STOMP Broker Connected');

      // 1. Board updates topic
      this.client.subscribe('/topic/board', (msg: IMessage) => {
        try {
          this.boardUpdates$.next(JSON.parse(msg.body));
        } catch (err) {
          console.error('Failed to parse board message', err);
        }
      });

      // 2. Real-time blocker flash alerts
      this.client.subscribe('/topic/blocker-alerts', (msg: IMessage) => {
        try {
          this.blockerAlerts$.next(JSON.parse(msg.body));
        } catch (err) {
          console.error('Failed to parse blocker alert', err);
        }
      });

      // 3. Resubscribe active channel if any
      if (this.currentChannelId !== null) {
        this.bindChannelSubscription(this.currentChannelId);
      }

      // 4. Resubscribe user/role notifications if any
      if (this.currentUserId !== null) {
        this.bindNotificationSubscriptions();
      }
    };

    this.client.onDisconnect = () => {
      this.isConnected.set(false);
    };

    this.client.activate();
  }

  private currentChannelId: number | null = null;
  private channelMessages$ = new Subject<any>();

  private notifications$ = new Subject<any>();
  private userNotificationSubId: any = null;
  private roleNotificationSubId: any = null;
  private currentUserId: string | null = null;
  private currentRoleName: string | null = null;

  private bindChannelSubscription(channelId: number): void {
    if (this.activeChannelSubId) {
      try {
        this.activeChannelSubId.unsubscribe();
      } catch (e) {}
      this.activeChannelSubId = null;
    }

    if (this.client.connected) {
      this.activeChannelSubId = this.client.subscribe(`/topic/channel.${channelId}`, (msg: IMessage) => {
        try {
          this.channelMessages$.next(JSON.parse(msg.body));
        } catch (e) {
          console.error('Failed to parse channel message', e);
        }
      });
    }
  }

  public subscribeToUserNotifications(userId: string, roleName?: string): Observable<any> {
    this.currentUserId = userId;
    this.currentRoleName = roleName || null;
    this.bindNotificationSubscriptions();
    return this.notifications$.asObservable();
  }

  private bindNotificationSubscriptions(): void {
    if (this.userNotificationSubId) {
      try { this.userNotificationSubId.unsubscribe(); } catch (e) {}
      this.userNotificationSubId = null;
    }
    if (this.roleNotificationSubId) {
      try { this.roleNotificationSubId.unsubscribe(); } catch (e) {}
      this.roleNotificationSubId = null;
    }

    if (this.client.connected && this.currentUserId) {
      this.userNotificationSubId = this.client.subscribe(`/topic/notifications.${this.currentUserId}`, (msg: IMessage) => {
        try {
          this.notifications$.next(JSON.parse(msg.body));
        } catch (e) {
          console.error('Failed to parse user notification', e);
        }
      });

      if (this.currentRoleName) {
        this.roleNotificationSubId = this.client.subscribe(`/topic/notifications.role.${this.currentRoleName}`, (msg: IMessage) => {
          try {
            this.notifications$.next(JSON.parse(msg.body));
          } catch (e) {
            console.error('Failed to parse role notification', e);
          }
        });
      }
    }
  }

  getBoardUpdates(): Observable<any> {
    return this.boardUpdates$.asObservable();
  }

  getBlockerAlerts(): Observable<any> {
    return this.blockerAlerts$.asObservable();
  }

  getNotifications(): Observable<any> {
    return this.notifications$.asObservable();
  }

  subscribeToChannel(channelId: number): Observable<any> {
    this.currentChannelId = channelId;
    this.bindChannelSubscription(channelId);
    return this.channelMessages$.asObservable();
  }

  publish(destination: string, body: any): void {
    if (this.client.connected) {
      this.client.publish({
        destination,
        body: JSON.stringify(body)
      });
    }
  }

  disconnect(): void {
    if (this.client.active) {
      this.client.deactivate();
    }
  }
}
