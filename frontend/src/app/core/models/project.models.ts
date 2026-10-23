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
  /** When the task entered a done stage; absent while open. */
  completedAt?: string;
  sprintId?: number;
  sprintName?: string;
  storyPoints?: number;
  gitBranch?: string;
  gitCommitHash?: string;
  gitPrUrl?: string;
  estimatedHours?: number;
  totalHoursSpent?: number;
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
  requiresChecklistComplete?: boolean;
  requiresDeliverable?: boolean;
  requiresNoBlockers?: boolean;
  tasks: TaskCard[];
}

/** Quality gates enforced when a task enters a stage. */
export interface StageGateFlags {
  requiresChecklistComplete?: boolean;
  requiresDeliverable?: boolean;
  requiresNoBlockers?: boolean;
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
    requiresChecklistComplete?: boolean;
    requiresDeliverable?: boolean;
    requiresNoBlockers?: boolean;
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
  /** Sprint the chart covers; null when the last 14 days of the project are shown. */
  sprintId?: number | null;
  sprintName?: string | null;
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
  chainVerifiedRecords?: number;
  chainBrokenRecords?: number;
  legacyUnchainedRecords?: number;
  firstBrokenRecordId?: number | null;
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

export interface ProjectFinancials {
  projectId: number;
  projectName: string;
  currency: string;
  /** Null when no budget has been set for the project. */
  totalBudget: number | null;
  allocatedBudget: number | null;
  spentBudget: number;
  remainingBudget: number | null;
  burnRatePercent: number | null;
  /** Null until at least one story point has been completed. */
  estimatedCostPerStoryPoint: number | null;
  totalStoryPoints: number;
  completedStoryPoints: number;
  roleHourlyRates: { [role: string]: number };
  spentByRole: { [role: string]: number };
  projectedCostAtCompletion: number;
  totalHoursLogged?: number;
  blendedHourlyRate?: number;
}

export interface WorkflowGateValidationResult {
  allowed: boolean;
  targetStageName: string;
  requiresOverride: boolean;
  violations: string[];
  blockingTaskTitles: string[];
  uncompletedSubtasksCount: number;
  hasDeliverableAttachment: boolean;
}

export interface WebhookSubscription {
  id: number;
  projectId: number;
  name: string;
  targetUrl: string;
  secretToken?: string;
  events: string[];
  isEnabled: boolean;
  deliveryCount: number;
  lastDeliveredAt?: string;
  lastResponseStatus?: string;
}

export interface MemberCapacity {
  userId: string;
  fullName: string;
  role: string;
  avatar: string;
  maxCapacityStoryPoints: number;
  assignedStoryPoints: number;
  utilizationPercent: number;
  assignedTaskCount: number;
  isOverloaded: boolean;
  onLeave: boolean;
  status: 'OPTIMAL' | 'OVERLOADED' | 'UNDERUTILIZED' | 'ON_LEAVE';
  assignedTaskTitles: string[];
}

export interface TeamCapacity {
  projectId: number;
  projectName: string;
  totalSprintCapacity: number;
  totalAssignedPoints: number;
  overallUtilizationPercent: number;
  overloadedMembersCount: number;
  members: MemberCapacity[];
}

export interface TaskTimeLog {
  id: number;
  taskId: number;
  taskTitle?: string;
  userId: string;
  userName: string;
  userEmail?: string;
  userRole?: string;
  hoursSpent: number;
  description?: string;
  loggedDate: string;
  createdAt: string;
}

export interface DeveloperTimeBreakdown {
  userId: string;
  fullName: string;
  email: string;
  role: string;
  totalHours: number;
  logsCount: number;
  percentageOfTotal: number;
}

export interface StageTimeBreakdown {
  stageId: number;
  stageName: string;
  totalHours: number;
  tasksCount: number;
  percentageOfTotal: number;
}

export interface ProjectTimeSummary {
  projectId: number;
  projectName: string;
  totalHoursSpent: number;
  totalEstimatedHours: number;
  hoursRemaining: number;
  progressPercentage: number;
  totalLogsCount: number;
  tasksWithLogsCount: number;
  developerBreakdown: DeveloperTimeBreakdown[];
  stageBreakdown: StageTimeBreakdown[];
  recentLogs: TaskTimeLog[];
}

