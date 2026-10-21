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

