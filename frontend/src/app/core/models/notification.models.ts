export type NotificationType =
  | 'TASK_ASSIGNED'
  | 'TASK_DUE_SOON'
  | 'TASK_OVERDUE'
  | 'CHANNEL_MENTION'
  | 'BLOCKER_ALERT'
  | 'TASK_COMPLETED'
  | 'ROLE_UPDATE'
  | 'PROJECT_ADDED'
  | 'SYSTEM';

export type NotificationPriority = 'INFO' | 'WARNING' | 'CRITICAL';

export interface NotificationItem {
  id: number;
  recipientId?: string;
  recipientName?: string;
  title: string;
  message: string;
  type: NotificationType;
  priority: NotificationPriority;
  isRead: boolean;
  linkUrl?: string;
  actionType?: 'NAVIGATE_TASK' | 'NAVIGATE_CHANNEL' | 'NAVIGATE_RBAC' | 'NAVIGATE_PROJECT' | 'NONE';
  referenceId?: number;
  senderName?: string;
  senderAvatar?: string;
  createdAt: string;
  readAt?: string;
  timeAgo: string;
}

export interface NotificationSummary {
  unreadTotal: number;
  unreadMentions: number;
  unreadTasks: number;
  unreadDeadlines: number;
  unreadBlockers: number;
}
