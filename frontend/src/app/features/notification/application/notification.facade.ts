import { Injectable, signal, computed, OnDestroy } from '@angular/core';
import { Subscription } from 'rxjs';
import { NotificationService } from '../../../core/services/notification.service';
import { WebSocketService } from '../../../core/services/websocket.service';
import { NotificationItem, NotificationSummary } from '../../../core/models/notification.models';

@Injectable({
  providedIn: 'root'
})
export class NotificationFacade implements OnDestroy {
  public notifications = signal<NotificationItem[]>([]);
  public summary = signal<NotificationSummary>({
    unreadTotal: 0,
    unreadMentions: 0,
    unreadTasks: 0,
    unreadDeadlines: 0,
    unreadBlockers: 0
  });

  public isDrawerOpen = signal(false);
  public activeFilter = signal<'ALL' | 'UNREAD' | 'MENTIONS' | 'TASKS' | 'DEADLINES'>('ALL');
  public isLoading = signal(false);
  public latestToast = signal<NotificationItem | null>(null);

  private toastTimeout: any = null;
  private subscriptions: Subscription[] = [];

  // Computed signals
  public unreadCount = computed(() => {
    return this.notifications().filter(n => !n.isRead).length;
  });

  public unreadMentionsCount = computed(() => {
    return this.notifications().filter(n => !n.isRead && n.type === 'CHANNEL_MENTION').length;
  });

  public unreadTasksCount = computed(() => {
    return this.notifications().filter(n => !n.isRead && n.type === 'TASK_ASSIGNED').length;
  });

  public unreadDeadlinesCount = computed(() => {
    return this.notifications().filter(n => !n.isRead && (n.type === 'TASK_DUE_SOON' || n.type === 'TASK_OVERDUE')).length;
  });

  public filteredNotifications = computed(() => {
    const list = this.notifications();
    const filter = this.activeFilter();

    switch (filter) {
      case 'UNREAD':
        return list.filter(n => !n.isRead);
      case 'MENTIONS':
        return list.filter(n => n.type === 'CHANNEL_MENTION');
      case 'TASKS':
        return list.filter(n => n.type === 'TASK_ASSIGNED');
      case 'DEADLINES':
        return list.filter(n => n.type === 'TASK_DUE_SOON' || n.type === 'TASK_OVERDUE');
      case 'ALL':
      default:
        return list;
    }
  });

  constructor(
    private notificationService: NotificationService,
    private wsService: WebSocketService
  ) {}

  ngOnDestroy(): void {
    this.subscriptions.forEach(s => s.unsubscribe());
    if (this.toastTimeout) {
      clearTimeout(this.toastTimeout);
    }
  }

  public init(userId?: string, roleName?: string): void {
    this.loadNotifications();
    this.loadSummary();

    if (userId) {
      const sub = this.wsService.subscribeToNotifications().subscribe({
        next: (incoming: NotificationItem) => {
          if (!incoming || !incoming.id) return;

          // Check if already in list
          this.notifications.update(list => {
            if (list.some(n => n.id === incoming.id)) return list;
            return [incoming, ...list];
          });

          // Show floating toast
          this.showToast(incoming);
        },
        error: (err) => console.error('NotificationFacade: WS error', err)
      });
      this.subscriptions.push(sub);
    }
  }

  public loadNotifications(): void {
    this.isLoading.set(true);
    this.notificationService.getNotifications().subscribe({
      next: (items) => {
        this.notifications.set(items);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('NotificationFacade: Failed to load notifications', err);
        this.isLoading.set(false);
      }
    });
  }

  public loadSummary(): void {
    this.notificationService.getSummary().subscribe({
      next: (sum) => this.summary.set(sum),
      error: (err) => console.error('NotificationFacade: Failed to load summary', err)
    });
  }

  public markAsRead(notificationId: number): void {
    this.notifications.update(list =>
      list.map(n => n.id === notificationId ? { ...n, isRead: true } : n)
    );

    this.notificationService.markAsRead(notificationId).subscribe({
      next: () => this.loadSummary(),
      error: (err) => console.error('NotificationFacade: Failed to mark as read', err)
    });
  }

  public markAllAsRead(): void {
    this.notifications.update(list =>
      list.map(n => ({ ...n, isRead: true }))
    );

    this.notificationService.markAllAsRead().subscribe({
      next: () => this.loadSummary(),
      error: (err) => console.error('NotificationFacade: Failed to mark all as read', err)
    });
  }

  public toggleDrawer(): void {
    this.isDrawerOpen.update(v => !v);
    if (this.isDrawerOpen()) {
      this.loadNotifications();
    }
  }

  public closeDrawer(): void {
    this.isDrawerOpen.set(false);
  }

  public setFilter(filter: 'ALL' | 'UNREAD' | 'MENTIONS' | 'TASKS' | 'DEADLINES'): void {
    this.activeFilter.set(filter);
  }

  public triggerDueScan(): void {
    this.notificationService.triggerDueCheck().subscribe({
      next: () => this.loadNotifications(),
      error: (err) => console.error('NotificationFacade: Failed to trigger due scan', err)
    });
  }

  private showToast(item: NotificationItem): void {
    this.latestToast.set(item);
    if (this.toastTimeout) {
      clearTimeout(this.toastTimeout);
    }
    this.toastTimeout = setTimeout(() => {
      this.latestToast.set(null);
    }, 6000);
  }

  public dismissToast(): void {
    this.latestToast.set(null);
    if (this.toastTimeout) {
      clearTimeout(this.toastTimeout);
    }
  }
}
