export interface TaskCard {
  id: number;
  title: string;
  description?: string;
  priority: string; // LOW, MEDIUM, HIGH, CRITICAL
  progress: number;
  stageId?: number;
  stageName?: string;
  projectId?: number;
  projectName?: string;
  assigneeId?: string;
  assignee?: string;
  avatar?: string;
  isBlocked: boolean;
  blockerReason?: string;
  deadline?: string;
  isArchived?: boolean;
  archivedAt?: string;
  archivedBy?: string;
  startDate?: string;
  sprintId?: number;
  sprintName?: string;
  storyPoints?: number;
}

export interface BankUser {
  id: string;
  email: string;
  fullName: string;
  avatarUrl?: string;
  status: string;
  onLeave: boolean;
  role?: {
    id: number;
    name: string;
  };
}

export interface KanbanColumn {
  id: string;
  numericId: number;
  title: string;
  isDoneStage: boolean;
  tasks: TaskCard[];
}

export interface Project {
  id: number;
  name: string;
  description?: string;
  status: string;
}

export interface ProjectBoard {
  id: number;
  name: string;
  description?: string;
  status: string;
  stages: {
    id: number;
    name: string;
    position: number;
    isDoneStage: boolean;
    tasks: TaskCard[];
  }[];
  totalTasks: number;
  blockedTasks: number;
  completionPercentage: number;
}

export interface Channel {
  id: number;
  name: string;
  type?: string;
  unread?: number;
  description?: string;
  icon?: string;
  isPrivate?: boolean;
  memberCount?: number;
  projectId?: number;
  projectName?: string;
  createdByName?: string;
  createdAt?: string;
}

export interface ChatMessage {
  id?: number;
  channelId: number;
  channelName?: string;
  sender: string;
  text: string;
  time: string;
  seenBy: string[];
}

export interface DailyWorkLog {
  id: number;
  userId: string;
  userFullName: string;
  userEmail: string;
  userRole: string;
  logDate: string; // YYYY-MM-DD
  summaryText: string;
  completedTasksCount: number;
  createdAt: string;
}

export interface SubmitWorkLogPayload {
  logDate?: string;
  summaryText: string;
  completedTasksCount?: number;
}

export interface SubtaskItem {
  id: number;
  taskId: number;
  title: string;
  isCompleted: boolean;
  completedBy?: string;
  completedAt?: string;
  position: number;
}

export interface TaskAttachmentItem {
  id: number;
  taskId: number;
  fileName: string;
  fileType: string;
  fileSize?: string;
  fileUrl: string;
  uploadedBy: string;
  uploadedAt: string;
  timeAgo?: string;
}

export interface BurndownDay {
  day: number;
  label: string;
  idealRemaining: number;
  actualRemaining?: number;
  completedTasks: number;
}

export interface BurndownAnalytics {
  projectId: number;
  projectName: string;
  sprintDurationDays: number;
  currentDay: number;
  totalStoryPoints: number;
  remainingStoryPoints: number;
  completedStoryPoints?: number;
  completionPercentage: number;
  velocityPointsPerWeek: number;
  averageCycleTimeDays: number;
  daysRemainingInSprint: number;
  dailyProgress: BurndownDay[];
}

export interface AuditLogItem {
  id: number;
  action: string;
  entityType: string;
  entityId?: number;
  entityName?: string;
  actorName: string;
  actorEmail?: string;
  actorRole: string;
  actorAvatar?: string;
  details?: string;
  ipAddress?: string;
  verificationHash?: string;
  timestamp: string;
  formattedDate?: string;
  timeAgo?: string;
}

export interface AuditMetrics {
  totalAuditRecords: number;
  taskAuditRecords: number;
  rbacAuditRecords: number;
  complianceStatus: string;
  integrityAlgorithm: string;
}

export interface Sprint {
  id: number;
  projectId: number;
  name: string;
  goal?: string;
  startDate?: string;
  endDate?: string;
  status: 'PLANNING' | 'ACTIVE' | 'COMPLETED';
  targetStoryPoints: number;
  totalTasks: number;
  completedTasks: number;
  totalStoryPoints: number;
  completedStoryPoints: number;
}

export interface TaskDependency {
  id: number;
  sourceTaskId: number;
  targetTaskId: number;
  targetTaskTitle: string;
  dependencyType: 'BLOCKS' | 'BLOCKED_BY' | 'RELATES_TO';
  createdBy?: string;
  createdAt?: string;
}

export interface TimelineTask {
  id: number;
  title: string;
  priority: string;
  progress: number;
  stageId?: number;
  stageName?: string;
  isDoneStage: boolean;
  assignee?: string;
  avatar?: string;
  startDate: string;
  deadline: string;
  isBlocked: boolean;
  storyPoints: number;
  blockedByTaskIds: number[];
}

export interface AutomationRule {
  id: number;
  projectId: number;
  name: string;
  description: string;
  triggerEvent: string;
  actionType: string;
  targetRole?: string;
  isEnabled: boolean;
  executionCount: number;
  lastExecutedAt?: string;
}
