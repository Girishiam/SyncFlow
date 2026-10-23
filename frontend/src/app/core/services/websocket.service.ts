import { Injectable, effect, inject, signal } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import { Observable, Subject, firstValueFrom } from 'rxjs';
import { IWebSocketPort } from '../ports/websocket.port';
import { AuthService } from './auth.service';
import { environment } from '../../../environments/environment';

/**
 * Authenticated STOMP connection. The access token is sent on CONNECT; the server authorises every
 * subscription (project board topics, readable channels, the user's private notification queue).
 * The connection is opened when a user is signed in and closed when they sign out.
 */
@Injectable({
  providedIn: 'root'
})
export class WebSocketService implements IWebSocketPort {
  private readonly auth = inject(AuthService);
  private readonly client: Client;
  public isConnected = signal(false);

  private readonly boardUpdates$ = new Subject<any>();
  private readonly blockerAlerts$ = new Subject<any>();
  private readonly channelMessages$ = new Subject<any>();
  private readonly notifications$ = new Subject<any>();

  private projectId: number | null = null;
  private channelId: number | null = null;
  private wantNotifications = false;

  private projectSubs: StompSubscription[] = [];
  private channelSub: StompSubscription | null = null;
  private notificationSub: StompSubscription | null = null;

  constructor() {
    this.client = new Client({
      brokerURL: WebSocketService.resolveBrokerUrl(environment.wsUrl),
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      debug: () => undefined,
      beforeConnect: async () => {
        // Make sure the CONNECT frame carries a fresh access token.
        if (this.auth.isAuthenticated() && this.auth.isAccessTokenExpiring()) {
          try {
            await firstValueFrom(this.auth.refreshAccessToken());
          } catch {
            this.auth.clearSession();
          }
        }
        this.client.connectHeaders = { Authorization: `Bearer ${this.auth.getToken() ?? ''}` };
      }
    });

    this.client.onConnect = () => {
      this.isConnected.set(true);
      this.bindProject();
      this.bindChannel();
      this.bindNotifications();
    };
    this.client.onDisconnect = () => this.isConnected.set(false);
    this.client.onWebSocketClose = () => this.isConnected.set(false);
    this.client.onStompError = frame => console.warn('STOMP error:', frame.headers['message']);

    effect(() => {
      if (this.auth.isAuthenticated()) {
        if (!this.client.active) this.client.activate();
      } else if (this.client.active) {
        this.resetState();
        this.client.deactivate();
      }
    });
  }

  /** Turns a relative path (production, same origin) into a ws(s):// URL; absolute URLs are used as-is. */
  static resolveBrokerUrl(url: string): string {
    if (url.startsWith('ws://') || url.startsWith('wss://')) return url;
    if (url.startsWith('http://') || url.startsWith('https://')) return url.replace(/^http/, 'ws');
    const scheme = location.protocol === 'https:' ? 'wss' : 'ws';
    return `${scheme}://${location.host}${url}`;
  }

  /** Follow live board changes and blocker alerts for one project. */
  watchProject(projectId: number): void {
    this.projectId = projectId;
    this.bindProject();
  }

  getBoardUpdates(): Observable<any> {
    return this.boardUpdates$.asObservable();
  }

  getBlockerAlerts(): Observable<any> {
    return this.blockerAlerts$.asObservable();
  }

  subscribeToChannel(channelId: number): Observable<any> {
    this.channelId = channelId;
    this.bindChannel();
    return this.channelMessages$.asObservable();
  }

  /** Personal notifications are delivered to the signed-in user's private queue. */
  subscribeToNotifications(): Observable<any> {
    this.wantNotifications = true;
    this.bindNotifications();
    return this.notifications$.asObservable();
  }

  disconnect(): void {
    this.resetState();
    if (this.client.active) {
      this.client.deactivate();
    }
  }

  private bindProject(): void {
    this.projectSubs.forEach(s => this.safeUnsubscribe(s));
    this.projectSubs = [];
    if (!this.client.connected || this.projectId === null) return;
    this.projectSubs = [
      this.client.subscribe(`/topic/projects.${this.projectId}.board`, m => this.emit(this.boardUpdates$, m)),
      this.client.subscribe(`/topic/projects.${this.projectId}.blockers`, m => this.emit(this.blockerAlerts$, m))
    ];
  }

  private bindChannel(): void {
    this.safeUnsubscribe(this.channelSub);
    this.channelSub = null;
    if (!this.client.connected || this.channelId === null) return;
    this.channelSub = this.client.subscribe(`/topic/channel.${this.channelId}`, m => this.emit(this.channelMessages$, m));
  }

  private bindNotifications(): void {
    this.safeUnsubscribe(this.notificationSub);
    this.notificationSub = null;
    if (!this.client.connected || !this.wantNotifications) return;
    this.notificationSub = this.client.subscribe('/user/queue/notifications', m => this.emit(this.notifications$, m));
  }

  private emit(target: Subject<any>, message: IMessage): void {
    try {
      target.next(JSON.parse(message.body));
    } catch (err) {
      console.error('Failed to parse STOMP message', err);
    }
  }

  private safeUnsubscribe(sub: StompSubscription | null): void {
    try {
      sub?.unsubscribe();
    } catch {
      // connection already gone
    }
  }

  private resetState(): void {
    this.projectId = null;
    this.channelId = null;
    this.wantNotifications = false;
    this.projectSubs = [];
    this.channelSub = null;
    this.notificationSub = null;
  }
}
