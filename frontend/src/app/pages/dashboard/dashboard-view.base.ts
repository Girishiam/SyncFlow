import { Directive, signal, computed, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { DashboardNavService, DashboardView } from './dashboard-nav.service';
import { CdkDragDrop } from '@angular/cdk/drag-drop';
import { AuthService } from '../../core/services/auth.service';
import { WebSocketService } from '../../core/services/websocket.service';
import { BoardFacade } from '../../features/board/application/board.facade';
import { ChatFacade } from '../../features/chat/application/chat.facade';
import { RbacFacade } from '../../features/rbac/application/rbac.facade';
import { Channel, TaskCard, BankUser, TaskAttachmentItem } from '../../core/models/project.models';
import { NotificationFacade } from '../../features/notification/application/notification.facade';
import { NotificationItem } from '../../core/models/notification.models';

/**
 * State and actions shared by the dashboard shell and every lazily loaded view. All state lives in
 * root-provided facades/services, so each subclass instance sees the same data.
 */
@Directive()
export abstract class DashboardViewBase {
  public authService = inject(AuthService);
  public boardFacade = inject(BoardFacade);
  public chatFacade = inject(ChatFacade);
  public rbacFacade = inject(RbacFacade);
  public notificationFacade = inject(NotificationFacade);
  public wsService = inject(WebSocketService);
  protected route = inject(ActivatedRoute);
  protected nav = inject(DashboardNavService);
  readonly Math = Math;

  currentUser = computed(() => this.authService.currentUserSignal());

  // Active navigation view
  activeNav = this.nav.activeNav;

  // Subtasks & Deliverables Checklist Delegates
  taskSubtasks = this.boardFacade.taskSubtasks;
  newSubtaskTitle = this.boardFacade.newSubtaskTitle;
  isAddingSubtask = this.boardFacade.isAddingSubtask;
  subtaskProgress = this.boardFacade.subtaskProgress;

  // Task Deliverable Attachments Delegates
  taskAttachments = this.boardFacade.taskAttachments;
  newAttachmentName = this.boardFacade.newAttachmentName;
  newAttachmentUrl = this.boardFacade.newAttachmentUrl;
  isAddingAttachment = this.boardFacade.isAddingAttachment;

  // Sprint Burndown & Velocity Analytics Delegates
  burndownAnalytics = this.boardFacade.burndownAnalytics;
  isLoadingBurndown = this.boardFacade.isLoadingBurndown;

  // Compliance & Audit Trail Ledger Delegates
  auditLogs = this.boardFacade.filteredAuditLogs;
  auditMetrics = this.boardFacade.auditMetrics;
  isLoadingAudit = this.boardFacade.isLoadingAudit;
  auditFilterAction = this.boardFacade.auditFilterAction;
  auditFilterEntity = this.boardFacade.auditFilterEntity;
  isExportingCsv = this.boardFacade.isExportingCsv;

  // --- Feature 1: Card Archiving & "Active Only" View Toggle Delegates ---
  archivedTasks = this.boardFacade.archivedTasks;
  isArchivedDrawerOpen = this.boardFacade.isArchivedDrawerOpen;
  hideCompletedFilter = this.boardFacade.hideCompletedFilter;

  // --- Feature 2: Interactive Sprint Roadmap & Gantt Timeline Delegates ---
  timelineTasks = this.boardFacade.timelineTasks;
  isLoadingTimeline = this.boardFacade.isLoadingTimeline;

  // --- Feature 3: Task Dependency Engine & Blocker Links Delegates ---
  taskDependencies = this.boardFacade.taskDependencies;
  isLoadingDependencies = this.boardFacade.isLoadingDependencies;
  newDependencyTargetId = this.boardFacade.newDependencyTargetId;
  newDependencyType = this.boardFacade.newDependencyType;

  // --- Feature 4: Sprint Planning & Dedicated Backlog Grooming Delegates ---
  projectSprints = this.boardFacade.projectSprints;
  isLoadingSprints = this.boardFacade.isLoadingSprints;
  isCreateSprintModalOpen = this.boardFacade.isCreateSprintModalOpen;
  newSprintName = this.boardFacade.newSprintName;
  newSprintGoal = this.boardFacade.newSprintGoal;
  newSprintTargetPoints = this.boardFacade.newSprintTargetPoints;

  // --- Feature 5: Smart Automation Rules Engine (Butler Automations) Delegates ---
  automationRules = this.boardFacade.automationRules;
  isAutomationsModalOpen = this.boardFacade.isAutomationsModalOpen;
  isTogglingRule = this.boardFacade.isTogglingRule;

  // --- Enterprise Feature 1: Executive Financial Budget & Cost Tracking Delegates ---
  projectFinancials = this.boardFacade.projectFinancials;
  isLoadingFinancials = this.boardFacade.isLoadingFinancials;

  // --- Enterprise Feature 2: Strict Workflow Transition Gates Delegates ---
  gateValidationResult = this.boardFacade.gateValidationResult;
  isGateViolationModalOpen = this.boardFacade.isGateViolationModalOpen;
  pendingGateMove = this.boardFacade.pendingGateMove;
  gateOverrideReason = this.boardFacade.gateOverrideReason;
  isSubmittingGateOverride = this.boardFacade.isSubmittingGateOverride;

  // --- Enterprise Feature 3: Real-Time Webhooks & Connectors Delegates ---
  webhooks = this.boardFacade.webhooks;
  isWebhooksModalOpen = this.boardFacade.isWebhooksModalOpen;
  isLoadingWebhooks = this.boardFacade.isLoadingWebhooks;
  newWebhookName = this.boardFacade.newWebhookName;
  newWebhookUrl = this.boardFacade.newWebhookUrl;
  newWebhookSecret = this.boardFacade.newWebhookSecret;
  isTestingWebhook = this.boardFacade.isTestingWebhook;
  webhookTestResult = this.boardFacade.webhookTestResult;
  detailGitBranch = this.boardFacade.detailGitBranch;
  detailGitCommit = this.boardFacade.detailGitCommit;
  detailGitPrUrl = this.boardFacade.detailGitPrUrl;
  isSavingGit = this.boardFacade.isSavingGit;

  // --- Gate override password confirmation ---
  gateOverridePassword = this.boardFacade.gateOverridePassword;
  gateOverrideError = this.boardFacade.gateOverrideError;

  // --- Enterprise Feature 5: Team Workload Capacity & Resource Matrix Delegates ---
  teamCapacity = this.boardFacade.teamCapacity;
  isLoadingCapacity = this.boardFacade.isLoadingCapacity;

  // Notification State Delegates
  notifications = this.notificationFacade.filteredNotifications;
  unreadNotificationCount = this.notificationFacade.unreadCount;
  unreadDiscussionsCount = this.notificationFacade.unreadMentionsCount;
  unreadTasksCount = this.notificationFacade.unreadTasksCount;
  unreadDeadlinesCount = this.notificationFacade.unreadDeadlinesCount;
  isNotificationDrawerOpen = this.notificationFacade.isDrawerOpen;
  notificationFilter = this.notificationFacade.activeFilter;
  latestNotificationToast = this.notificationFacade.latestToast;

  // Mention Autocomplete State
  showMentionPopup = signal(false);
  mentionSearchTerm = signal('');
  mentionCandidates = computed(() => {
    const query = this.mentionSearchTerm().toLowerCase();
    const members = this.bankUsers();
    if (!query) return members.slice(0, 6);
    return members.filter(u =>
      u.fullName.toLowerCase().includes(query) ||
      (u.role?.name && u.role.name.toLowerCase().includes(query))
    ).slice(0, 6);
  });

  // Board Facade State Delegates
  projects = this.boardFacade.projects;
  selectedProjectId = this.boardFacade.selectedProjectId;
  columns = this.boardFacade.columns;
  filteredColumns = this.boardFacade.filteredColumns;
  bankUsers = this.boardFacade.bankUsers;
  searchQuery = this.boardFacade.searchQuery;
  filterMode = this.boardFacade.filterMode;
  totalTasksCount = this.boardFacade.totalTasksCount;
  blockedCount = this.boardFacade.blockedCount;
  completionPercentage = this.boardFacade.completionPercentage;
  activeBlockerBanner = this.boardFacade.activeBlockerBanner;

  // Create Task Modal Delegates
  isCreateModalOpen = this.boardFacade.isCreateModalOpen;
  newTaskTitle = this.boardFacade.newTaskTitle;
  newTaskDescription = this.boardFacade.newTaskDescription;
  newTaskPriority = this.boardFacade.newTaskPriority;
  newTaskAssigneeId = this.boardFacade.newTaskAssigneeId;
  newTaskDeadline = this.boardFacade.newTaskDeadline;
  newTaskSprintId = this.boardFacade.newTaskSprintId;

  // Blocker Dialog Delegates
  selectedTask = this.boardFacade.selectedTask;
  blockerReasonInput = this.boardFacade.blockerReasonInput;

  // Task Detail Drawer Delegates
  activeDetailTask = this.boardFacade.activeDetailTask;
  detailAssigneeId = this.boardFacade.detailAssigneeId;
  detailDeadline = this.boardFacade.detailDeadline;
  taskComments = this.boardFacade.taskComments;
  commentInput = this.boardFacade.commentInput;
  isLoadingComments = this.boardFacade.isLoadingComments;
  completionCelebrationMsg = this.boardFacade.completionCelebrationMsg;

  // Project Creator Modal Delegates
  isCreateProjectModalOpen = this.boardFacade.isCreateProjectModalOpen;
  newProjectName = this.boardFacade.newProjectName;
  newProjectDescription = this.boardFacade.newProjectDescription;
  isCreatingProject = this.boardFacade.isCreatingProject;

  // Stage Creator Modal Delegates
  isCreateStageModalOpen = this.boardFacade.isCreateStageModalOpen;
  newStageName = this.boardFacade.newStageName;
  newStageIsDone = this.boardFacade.newStageIsDone;
  newStageRequiresChecklist = this.boardFacade.newStageRequiresChecklist;
  newStageRequiresDeliverable = this.boardFacade.newStageRequiresDeliverable;
  newStageRequiresNoBlockers = this.boardFacade.newStageRequiresNoBlockers;

  // Daily Work Log Delegates
  isWorkLogDrawerOpen = this.boardFacade.isWorkLogDrawerOpen;
  myTodayLog = this.boardFacade.myTodayLog;
  teamLogs = this.boardFacade.teamLogs;
  workLogSummary = this.boardFacade.workLogSummary;
  workLogCompletedCount = this.boardFacade.workLogCompletedCount;
  workLogDate = this.boardFacade.workLogDate;
  workLogActiveTab = this.boardFacade.workLogActiveTab;
  isSavingWorkLog = this.boardFacade.isSavingWorkLog;
  workLogSaveSuccess = this.boardFacade.workLogSaveSuccess;

  // Task Work Logs & Collective Project Time Tracking
  projectTimeSummary = this.boardFacade.projectTimeSummary;
  activeTaskTimeLogs = this.boardFacade.activeTaskTimeLogs;
  isProjectTimeModalOpen = this.boardFacade.isProjectTimeModalOpen;
  isLoadingTimeLogs = this.boardFacade.isLoadingTimeLogs;
  isTimerActive = this.boardFacade.isTimerActive;
  timerTaskId = this.boardFacade.timerTaskId;
  timerElapsedSeconds = this.boardFacade.timerElapsedSeconds;
  logHoursInput = this.boardFacade.logHoursInput;
  logNoteInput = this.boardFacade.logNoteInput;
  logEstimatedHoursInput = this.boardFacade.logEstimatedHoursInput;
  isLoggingTime = this.boardFacade.isLoggingTime;

  // Chat Facade State Delegates
  channels = this.chatFacade.channels;
  activeChannel = this.chatFacade.activeChannel;
  messages = this.chatFacade.messages;
  chatInput = this.chatFacade.chatInput;

  // Channel Creation Modal State
  isCreateChannelModalOpen = this.chatFacade.isCreateModalOpen;
  newChannelName = this.chatFacade.newChannelName;
  newChannelDescription = this.chatFacade.newChannelDescription;
  newChannelType = this.chatFacade.newChannelType;
  newChannelIcon = this.chatFacade.newChannelIcon;
  newChannelIsPrivate = this.chatFacade.newChannelIsPrivate;
  newChannelProjectId = this.chatFacade.newChannelProjectId;
  isCreatingChannel = this.chatFacade.isCreatingChannel;
  createChannelError = this.chatFacade.createChannelError;

  // Channel Edit Modal State
  isEditChannelModalOpen = this.chatFacade.isEditModalOpen;
  channelToEdit = this.chatFacade.channelToEdit;
  editChannelName = this.chatFacade.editChannelName;
  editChannelDescription = this.chatFacade.editChannelDescription;
  editChannelType = this.chatFacade.editChannelType;
  editChannelIcon = this.chatFacade.editChannelIcon;
  editChannelIsPrivate = this.chatFacade.editChannelIsPrivate;
  isUpdatingChannel = this.chatFacade.isUpdatingChannel;
  editChannelError = this.chatFacade.editChannelError;

  // Channel Delete Modal State
  isDeleteChannelModalOpen = this.chatFacade.isDeleteModalOpen;
  channelToDelete = this.chatFacade.channelToDelete;
  deleteChannelConfirmText = this.chatFacade.deleteConfirmText;
  deleteChannelError = this.chatFacade.deleteChannelError;

  // Channel Members State
  isChannelMembersModalOpen = this.chatFacade.isMembersModalOpen;
  channelMembers = this.chatFacade.channelMembers;
  isLoadingChannelMembers = this.chatFacade.isLoadingMembers;
  selectedMemberToAdd = this.chatFacade.selectedMemberToAdd;

  availableChannelIcons = this.chatFacade.availableIcons;

  // RBAC Permission Computations for Channel Governance
  canCreateChannel = computed(() => this.authService.hasPermission('CHAT:CREATE_CHANNEL'));
  canEditChannel = computed(() => this.authService.hasPermission('CHAT:EDIT_CHANNEL'));
  canDeleteChannel = computed(() => this.authService.hasPermission('CHAT:DELETE_CHANNEL'));
  isAdmin = computed(() => this.authService.userRole() === 'ADMIN');
  canManageMembers = computed(() => this.authService.hasPermission('CHAT:MANAGE_MEMBERS'));

  // Available bank users who can be added to the channel
  availableBankUsersForChannel = computed(() => {
    const currentMembers = this.channelMembers();
    const allUsers = this.bankUsers();
    const currentMemberIds = new Set(currentMembers.map(m => m.id));
    return allUsers.filter(u => !currentMemberIds.has(u.id));
  });

  // RBAC Facade State Delegates
  roles = this.rbacFacade.roles;
  permissions = this.rbacFacade.permissions;
  selectedRoleId = this.rbacFacade.selectedRoleId;
  selectedRole = this.rbacFacade.selectedRole;
  modules = this.rbacFacade.modules;
  activePermissionIds = this.rbacFacade.activePermissionIds;
  isSavingRbac = this.rbacFacade.isSaving;
  rbacSuccessMsg = this.rbacFacade.saveSuccessMessage;

  // RBAC Search, Filter & KPI Delegates
  rbacSearchQuery = this.rbacFacade.searchQuery;
  rbacStatusFilter = this.rbacFacade.statusFilter;
  filteredRoles = this.rbacFacade.filteredRoles;
  totalRolesCount = this.rbacFacade.totalRolesCount;
  activeRolesCount = this.rbacFacade.activeRolesCount;
  deactivatedRolesCount = this.rbacFacade.deactivatedRolesCount;
  totalAssignedUsersCount = this.rbacFacade.totalAssignedUsersCount;

  // Custom Role Modal Delegates
  isCreateRoleModalOpen = this.rbacFacade.isCreateRoleModalOpen;
  newRoleName = this.rbacFacade.newRoleName;
  newRoleDescription = this.rbacFacade.newRoleDescription;
  rbacBankUsers = this.rbacFacade.bankUsers;
  activeRbacTab = signal<'MATRIX' | 'MEMBERS'>('MATRIX');

  // Delete Role Modal Delegates
  isDeleteRoleModalOpen = this.rbacFacade.isDeleteModalOpen;
  roleToDelete = this.rbacFacade.roleToDelete;
  deleteConfirmText = this.rbacFacade.deleteConfirmText;
  deleteErrorMessage = this.rbacFacade.deleteErrorMessage;

  // Meeting state
  isMeetingOpen = this.chatFacade.isMeetingOpen;
  meetingRoom = this.chatFacade.meetingRoom;


  // Board Operations
  onProjectChange(event: Event): void {
    const select = event.target as HTMLSelectElement;
    this.boardFacade.selectProject(Number(select.value));
  }

  onDrop(event: CdkDragDrop<TaskCard[]>): void {
    this.boardFacade.onDrop(event);
  }

  onProgressChange(task: TaskCard): void {
    this.boardFacade.onProgressChange(task);
  }

  cycleTaskProgress(task: TaskCard, event: Event): void {
    event.stopPropagation();
    const nextProgress = task.progress >= 100 ? 0 : Math.min(100, task.progress + 25);
    task.progress = nextProgress;
    this.boardFacade.onProgressChange(task);
  }

  setTaskProgress(prog: number): void {
    const task = this.activeDetailTask();
    if (task) {
      task.progress = prog;
      this.boardFacade.onProgressChange(task);
    }
  }

  openBlocker(task: TaskCard): void {
    this.boardFacade.openBlocker(task);
  }

  saveBlocker(): void {
    this.boardFacade.saveBlocker();
  }

  closeBlocker(): void {
    this.boardFacade.closeBlocker();
  }

  openCreateTask(stageId?: number, sprintId?: number): void {
    const sId = stageId || (this.columns().length > 0 ? this.columns()[0].numericId : 1);
    this.boardFacade.openCreateTask(sId, sprintId);
  }

  getTasksForSprint(sprintId: number): TaskCard[] {
    const res: TaskCard[] = [];
    for (const col of this.columns()) {
      for (const t of col.tasks) {
        if (t.sprintId === sprintId) {
          res.push(t);
        }
      }
    }
    return res;
  }

  closeCreateTask(): void {
    this.boardFacade.closeCreateTask();
  }

  submitCreateTask(): void {
    this.boardFacade.submitCreateTask();
  }

  // Task Details & Comments
  openTaskDetail(task: TaskCard): void {
    this.boardFacade.openTaskDetail(task);
  }

  closeTaskDetail(): void {
    this.boardFacade.closeTaskDetail();
  }

  submitComment(): void {
    this.boardFacade.submitComment();
  }

  onAssigneeSelectChange(newAssigneeId: string): void {
    const task = this.activeDetailTask();
    if (!task) return;
    const val = newAssigneeId ? newAssigneeId : null;
    this.boardFacade.assignTaskTo(task, val);
  }

  onAssigneeChange(task: TaskCard, event: Event): void {
    const select = event.target as HTMLSelectElement;
    const val = select.value ? select.value : null;
    this.boardFacade.assignTaskTo(task, val);
  }

  isTaskCompleted(task: TaskCard | null): boolean {
    if (!task) return false;
    if (task.progress === 100) return true;
    if (task.stageName && task.stageName.toLowerCase().includes('done')) return true;
    const doneCol = this.columns().find(c => c.isDoneStage);
    return doneCol ? task.stageId === doneCol.numericId : false;
  }

  markTaskCompleted(task: TaskCard): void {
    this.boardFacade.markTaskCompleted(task);
  }

  reopenTask(task: TaskCard): void {
    this.boardFacade.reopenTask(task);
  }

  onDeadlineChange(task: TaskCard, event: Event): void {
    const input = event.target as HTMLInputElement;
    const val = input.value ? input.value : null;
    this.boardFacade.updateTaskDeadline(task, val);
  }

  setFilterMode(mode: 'ALL' | 'MY_TASKS' | 'BLOCKED' | 'CRITICAL_HIGH'): void {
    this.boardFacade.filterMode.set(mode);
  }

  // Stage Operations
  openCreateStage(): void {
    this.boardFacade.openCreateStageModal();
  }

  closeCreateStage(): void {
    this.boardFacade.closeCreateStageModal();
  }

  submitCreateStage(): void {
    this.boardFacade.submitCreateStage();
  }

  // Daily Work Log Operations
  openWorkLogDrawer(): void {
    this.boardFacade.openWorkLogDrawer();
  }

  closeWorkLogDrawer(): void {
    this.boardFacade.closeWorkLogDrawer();
  }

  setWorkLogTab(tab: 'MY_LOG' | 'TEAM_LOGS'): void {
    this.boardFacade.setWorkLogTab(tab);
  }

  submitDailyWorkLog(): void {
    this.boardFacade.submitDailyWorkLog();
  }

  // Chat Operations
  selectChannel(channel: Channel): void {
    this.chatFacade.selectChannel(channel);
  }

  sendMessage(): void {
    this.chatFacade.sendMessage();
  }

  openCreateChannel(): void {
    this.chatFacade.openCreateChannelModal();
  }

  closeCreateChannel(): void {
    this.chatFacade.closeCreateChannelModal();
  }

  submitCreateChannel(): void {
    this.chatFacade.submitCreateChannel();
  }

  openEditChannel(channel?: Channel): void {
    this.chatFacade.openEditChannelModal(channel);
  }

  closeEditChannel(): void {
    this.chatFacade.closeEditChannelModal();
  }

  submitEditChannel(): void {
    this.chatFacade.submitEditChannel();
  }

  openDeleteChannel(channel?: Channel): void {
    this.chatFacade.openDeleteChannelModal(channel);
  }

  closeDeleteChannel(): void {
    this.chatFacade.closeDeleteChannelModal();
  }

  confirmDeleteChannel(): void {
    this.chatFacade.confirmDeleteChannel();
  }

  openChannelMembers(): void {
    this.chatFacade.openMembersModal();
  }

  closeChannelMembers(): void {
    this.chatFacade.closeMembersModal();
  }

  addMemberToChannel(): void {
    const userId = this.selectedMemberToAdd();
    if (userId) {
      this.chatFacade.addMemberToActiveChannel(userId);
    }
  }

  removeMemberFromChannel(userId: string): void {
    this.chatFacade.removeMemberFromActiveChannel(userId);
  }

  getChannelIconName(iconId?: string): string {
    const found = this.availableChannelIcons.find(i => i.id === iconId);
    return found ? found.icon : 'hash';
  }

  notificationIcon(type: string): string {
    switch (type) {
      case 'TASK_ASSIGNED': return 'pin';
      case 'TASK_DUE_SOON': return 'clock';
      case 'TASK_OVERDUE': return 'alert';
      case 'CHANNEL_MENTION': return 'message';
      case 'BLOCKER_ALERT': return 'ban';
      case 'TASK_COMPLETED': return 'check-circle';
      case 'ROLE_UPDATE': return 'shield';
      default: return 'bell';
    }
  }

  notificationLabel(type: string): string {
    switch (type) {
      case 'TASK_ASSIGNED': return 'Assigned';
      case 'TASK_DUE_SOON': return 'Due soon';
      case 'TASK_OVERDUE': return 'Overdue';
      case 'CHANNEL_MENTION': return 'Mention';
      case 'BLOCKER_ALERT': return 'Blocked';
      case 'TASK_COMPLETED': return 'Done';
      case 'ROLE_UPDATE': return 'Role change';
      default: return 'Update';
    }
  }

  // RBAC Operations
  selectRole(roleId: number): void {
    this.rbacFacade.selectRole(roleId);
  }

  hasPermission(permissionId: number): boolean {
    return this.rbacFacade.hasPermission(permissionId);
  }

  togglePermission(permissionId: number): void {
    this.rbacFacade.togglePermission(permissionId);
  }

  getPermissionsForModule(moduleName: string) {
    return this.rbacFacade.getPermissionsForModule(moduleName);
  }

  saveRolePermissions(): void {
    this.rbacFacade.saveRolePermissions();
  }

  openCreateRole(): void {
    this.rbacFacade.openCreateRoleModal();
  }

  closeCreateRole(): void {
    this.rbacFacade.closeCreateRoleModal();
  }

  submitCreateRole(): void {
    this.rbacFacade.submitCreateRole();
  }

  // Batch Permission Controls
  selectAllPermissions(): void {
    this.rbacFacade.selectAllPermissions();
  }

  clearAllPermissions(): void {
    this.rbacFacade.clearAllPermissions();
  }

  isModuleAllSelected(moduleName: string): boolean {
    return this.rbacFacade.isModuleAllSelected(moduleName);
  }

  toggleModulePermissions(moduleName: string): void {
    this.rbacFacade.toggleModulePermissions(moduleName);
  }

  // Role Status & Delete Operations
  toggleRoleStatus(roleId: number): void {
    this.rbacFacade.toggleRoleStatus(roleId);
  }

  openDeleteRoleModal(role: any): void {
    this.rbacFacade.openDeleteModal(role);
  }

  closeDeleteRoleModal(): void {
    this.rbacFacade.closeDeleteModal();
  }

  confirmDeleteRole(): void {
    this.rbacFacade.confirmDeleteRole();
  }

  getModuleIcon(module: string): string {
    return this.rbacFacade.getModuleIcon(module);
  }

  getModuleDescription(module: string): string {
    return this.rbacFacade.getModuleDescription(module);
  }

  setRbacStatusFilter(filter: 'ALL' | 'ACTIVE' | 'INACTIVE'): void {
    this.rbacFacade.statusFilter.set(filter);
  }

  onMemberRoleChange(userId: string, event: Event): void {
    const select = event.target as HTMLSelectElement;
    const roleId = Number(select.value);
    if (roleId) {
      this.rbacFacade.changeUserRole(userId, roleId);
    }
  }

  // Project Creation Operations
  openCreateProject(): void {
    this.boardFacade.openCreateProjectModal();
  }

  closeCreateProject(): void {
    this.boardFacade.closeCreateProjectModal();
  }

  submitCreateProject(): void {
    this.boardFacade.submitCreateProject();
  }

  // Availability Toggle
  toggleAvailability(): void {
    const user = this.currentUser();
    if (user) {
      user.onLeave = !user.onLeave;
    }
  }

  // --- Notification Center Actions ---
  toggleNotificationCenter(): void {
    this.notificationFacade.toggleDrawer();
  }

  closeNotificationCenter(): void {
    this.notificationFacade.closeDrawer();
  }

  setNotificationFilter(filter: 'ALL' | 'UNREAD' | 'MENTIONS' | 'TASKS' | 'DEADLINES'): void {
    this.notificationFacade.setFilter(filter);
  }

  markNotificationAsRead(id: number, event?: Event): void {
    if (event) {
      event.stopPropagation();
    }
    this.notificationFacade.markAsRead(id);
  }

  markAllNotificationsAsRead(): void {
    this.notificationFacade.markAllAsRead();
  }

  dismissNotificationToast(): void {
    this.notificationFacade.dismissToast();
  }

  triggerDueScan(): void {
    this.notificationFacade.triggerDueScan();
  }

  onNotificationClick(item: NotificationItem): void {
    this.notificationFacade.markAsRead(item.id);
    this.notificationFacade.closeDrawer();

    if (item.actionType === 'NAVIGATE_CHANNEL' && item.referenceId) {
      this.switchNav('discussions');
      const target = this.channels().find(c => c.id === item.referenceId);
      if (target) {
        this.selectChannel(target);
      }
    } else if (item.actionType === 'NAVIGATE_TASK') {
      this.switchNav('board');
      if (item.referenceId) {
        for (const col of this.columns()) {
          const match = col.tasks?.find(t => t.id === item.referenceId);
          if (match) {
            this.openTaskDetail(match);
            break;
          }
        }
      }
    } else if (item.actionType === 'NAVIGATE_RBAC') {
      this.switchNav('rbac');
    }
  }

  // --- Mention Autocomplete for Chat Composer ---
  onChatInput(event: Event): void {
    const text = this.chatInput();
    const lastAtIdx = text.lastIndexOf('@');
    if (lastAtIdx !== -1 && lastAtIdx >= text.length - 20) {
      const query = text.substring(lastAtIdx + 1);
      if (!query.includes(' ')) {
        this.mentionSearchTerm.set(query);
        this.showMentionPopup.set(true);
        return;
      }
    }
    this.showMentionPopup.set(false);
  }

  insertMention(user: BankUser): void {
    const text = this.chatInput();
    const lastAtIdx = text.lastIndexOf('@');
    if (lastAtIdx !== -1) {
      const prefix = text.substring(0, lastAtIdx);
      this.chatInput.set(`${prefix}@${user.fullName} `);
    } else {
      this.chatInput.set(`${text}@${user.fullName} `);
    }
    this.showMentionPopup.set(false);
  }

  // --- Subtasks QA Checklist Actions ---
  addSubtask(): void {
    this.boardFacade.addSubtask();
  }

  toggleSubtask(item: any): void {
    this.boardFacade.toggleSubtask(item);
  }

  deleteSubtask(subtaskId: number): void {
    this.boardFacade.deleteSubtask(subtaskId);
  }

  // --- Task Deliverable Attachments Actions ---
  addAttachment(): void {
    this.boardFacade.addAttachment();
  }

  deleteAttachment(attachmentId: number): void {
    this.boardFacade.deleteAttachment(attachmentId);
  }

  // --- Sprint Burndown & Velocity Analytics Actions ---
  refreshBurndown(): void {
    this.boardFacade.loadBurndown();
  }

  // --- Compliance & Audit Trail Ledger Actions ---
  refreshAudit(): void {
    this.boardFacade.loadAuditData();
  }

  filterAudit(action: string, entity: string): void {
    this.boardFacade.filterAuditLogs(action, entity);
  }

  exportAuditCsv(): void {
    this.boardFacade.exportAuditCsv();
  }

  // --- Navigation Switcher ---
  switchNav(nav: DashboardView): void {
    this.nav.go(nav);
  }

  // --- Feature 1: Card Archiving & Active Only Methods ---
  archiveTask(task: TaskCard): void {
    this.boardFacade.archiveTask(task);
  }

  restoreTask(task: TaskCard): void {
    this.boardFacade.restoreTask(task);
  }

  openArchivedDrawer(): void {
    this.boardFacade.isArchivedDrawerOpen.set(true);
    this.boardFacade.loadArchivedTasks();
  }

  closeArchivedDrawer(): void {
    this.boardFacade.isArchivedDrawerOpen.set(false);
  }

  toggleHideCompleted(): void {
    this.boardFacade.hideCompletedFilter.update(v => !v);
  }

  // --- Feature 2: Interactive Sprint Roadmap & Gantt Timeline Methods ---
  refreshTimeline(): void {
    this.boardFacade.loadTimeline();
  }

  getGanttBarLeft(startDateStr: string): number {
    if (!startDateStr) return 10;
    const start = new Date(startDateStr).getTime();
    const minTime = Date.now() - (7 * 86400000);
    const totalSpan = 28 * 86400000;
    const diff = Math.max(0, start - minTime);
    return Math.min(85, Math.round((diff / totalSpan) * 100));
  }

  getGanttBarWidth(startDateStr: string, deadlineStr: string): number {
    if (!startDateStr || !deadlineStr) return 20;
    const start = new Date(startDateStr).getTime();
    const end = new Date(deadlineStr).getTime();
    const duration = Math.max(86400000, end - start);
    const totalSpan = 28 * 86400000;
    return Math.min(60, Math.max(12, Math.round((duration / totalSpan) * 100)));
  }

  // --- Feature 3: Task Dependency Engine & Blocker Links Methods ---
  addDependency(): void {
    const targetId = this.newDependencyTargetId();
    const type = this.newDependencyType();
    if (!targetId) return;
    this.boardFacade.addDependency(targetId, type);
  }

  deleteDependency(depId: number): void {
    this.boardFacade.deleteDependency(depId);
  }

  // --- Feature 4: Sprint Planning & Dedicated Backlog Grooming Methods ---
  openCreateSprintModal(): void {
    this.boardFacade.isCreateSprintModalOpen.set(true);
  }

  closeCreateSprintModal(): void {
    this.boardFacade.isCreateSprintModalOpen.set(false);
  }

  submitCreateSprint(): void {
    this.boardFacade.createSprint();
  }

  startSprint(sprintId: number): void {
    this.boardFacade.startSprint(sprintId);
  }

  completeSprint(sprintId: number): void {
    this.boardFacade.completeSprint(sprintId);
  }

  updateTaskStoryPoints(taskId: number, event: Event): void {
    const select = event.target as HTMLSelectElement;
    this.boardFacade.updateStoryPoints(taskId, Number(select.value));
  }

  assignTaskSprint(taskId: number, event: Event): void {
    const select = event.target as HTMLSelectElement;
    this.boardFacade.assignSprint(taskId, Number(select.value));
  }

  // --- Feature 5: Smart Automation Rules Engine (Butler Automations) Methods ---
  openAutomationsModal(): void {
    this.boardFacade.isAutomationsModalOpen.set(true);
    this.boardFacade.loadAutomationRules();
  }

  closeAutomationsModal(): void {
    this.boardFacade.isAutomationsModalOpen.set(false);
  }

  toggleAutomationRule(ruleId: number): void {
    this.boardFacade.toggleAutomationRule(ruleId);
  }


  // --- Enterprise Feature 1: Financials Helpers ---
  refreshFinancials(): void {
    this.boardFacade.loadFinancials();
  }

  // --- Enterprise Feature 2: Quality Gate Override Helpers ---
  confirmGateOverride(): void {
    this.boardFacade.confirmGateOverride();
  }

  cancelGateOverride(): void {
    this.boardFacade.cancelGateOverride();
  }

  // --- Enterprise Feature 3: Webhooks & Git Linker Helpers ---
  openWebhooksModal(): void {
    this.boardFacade.isWebhooksModalOpen.set(true);
    this.boardFacade.loadWebhooks();
  }

  closeWebhooksModal(): void {
    this.boardFacade.isWebhooksModalOpen.set(false);
  }

  createWebhook(): void {
    this.boardFacade.createWebhook();
  }

  toggleWebhook(id: number): void {
    this.boardFacade.toggleWebhook(id);
  }

  testWebhook(id: number): void {
    this.boardFacade.testWebhook(id);
  }

  deleteWebhook(id: number): void {
    this.boardFacade.deleteWebhook(id);
  }

  saveGitLink(): void {
    this.boardFacade.saveGitLink();
  }

  /**
   * SVG geometry for the burndown chart (plot area x 60..780, y 40..205), derived from the
   * server's daily ideal/actual remaining points.
   */
  burndownChart = computed(() => {
    const data = this.burndownAnalytics();
    const days = data?.dailyProgress ?? [];
    if (!data || days.length === 0) return null;

    const maxValue = Math.max(1, data.totalStoryPoints, ...days.map(d => d.actualRemaining ?? 0));
    const x = (i: number) => (days.length === 1 ? 420 : 60 + (i * 720) / (days.length - 1));
    const y = (v: number) => 205 - (v / maxValue) * 165;
    const pt = (i: number, v: number) => `${x(i).toFixed(1)},${y(v).toFixed(1)}`;

    const actual = days
      .map((d, i) => ({ i, v: d.actualRemaining }))
      .filter((p): p is { i: number; v: number } => p.v !== null && p.v !== undefined);
    const actualPoints = actual.map(p => pt(p.i, p.v)).join(' ');
    const last = actual[actual.length - 1];
    const labelStep = Math.max(1, Math.ceil(days.length / 7));

    return {
      idealPoints: days.map((d, i) => pt(i, d.idealRemaining)).join(' '),
      actualPoints: actual.length ? actualPoints : '',
      actualArea: actual.length
        ? `${actualPoints} ${x(last.i).toFixed(1)},205 ${x(actual[0].i).toFixed(1)},205`
        : '',
      actualDots: actual.map(p => ({ x: x(p.i), y: y(p.v) })),
      current: last ? { x: x(last.i), y: y(last.v), value: last.v } : null,
      yTicks: [1, 2 / 3, 1 / 3, 0].map(f => ({ y: y(maxValue * f), label: Math.round(maxValue * f) })),
      xLabels: days
        .map((d, i) => ({ i, text: 'Day ' + d.day }))
        .filter(l => l.i % labelStep === 0 || l.i === days.length - 1)
        .map(l => ({ x: x(l.i), text: l.text }))
    };
  });

  /** Real counts for the roadmap header. */
  roadmapStats = computed(() => {
    const tasks = this.timelineTasks();
    const now = Date.now();
    return {
      blocked: tasks.filter(t => t.isBlocked || (t.blockedByTaskIds?.length ?? 0) > 0).length,
      done: tasks.filter(t => t.isDoneStage).length,
      overdue: tasks.filter(t => !t.isDoneStage && t.deadline && new Date(t.deadline).getTime() < now).length
    };
  });

  /** Share of completed tasks with a deadline that were finished on or before it (active board). */
  onTimeDelivery = computed(() => {
    const done = this.columns()
      .filter(c => c.isDoneStage)
      .flatMap(c => c.tasks)
      .filter(t => t.deadline && t.completedAt);
    if (done.length === 0) return { rate: null as number | null, sample: 0 };
    const onTime = done.filter(t => new Date(t.completedAt!).getTime() <= new Date(t.deadline!).getTime()).length;
    return { rate: Math.round((onTime / done.length) * 1000) / 10, sample: done.length };
  });

  /** Spend-by-role rows for the financials view, largest first. */
  financialRoleRows(): { role: string; rate: number; spent: number; share: number }[] {
    const fin = this.projectFinancials();
    if (!fin) return [];
    const total = fin.spentBudget || 0;
    return Object.entries(fin.spentByRole || {})
      .map(([role, spent]) => ({
        role,
        rate: fin.roleHourlyRates?.[role] ?? fin.blendedHourlyRate ?? 0,
        spent,
        share: total > 0 ? Math.round((spent / total) * 100) : 0
      }))
      .sort((a, b) => b.spent - a.spent);
  }

  canOverrideGates(): boolean {
    return this.boardFacade.canOverrideGates();
  }

  uploadAttachment(file: File | null | undefined): void {
    this.boardFacade.uploadAttachment(file);
  }

  openAttachment(item: TaskAttachmentItem): void {
    this.boardFacade.openAttachment(item);
  }

  // --- Enterprise Feature 5: Team Capacity Matrix Helpers ---
  refreshCapacity(): void {
    this.boardFacade.loadTeamCapacity();
  }

  rebalanceTask(fromUser: string, toUser: string, taskId: number): void {
    this.boardFacade.rebalanceMemberTask(fromUser, toUser, taskId);
  }

  // --- Task Work Logs & Time Tracking Helpers ---
  openProjectTimeModal(): void {
    this.boardFacade.isProjectTimeModalOpen.set(true);
    this.boardFacade.loadProjectTimeSummary();
  }

  closeProjectTimeModal(): void {
    this.boardFacade.isProjectTimeModalOpen.set(false);
  }

  startTimer(taskId: number, event?: Event): void {
    if (event) event.stopPropagation();
    this.boardFacade.startTimer(taskId);
  }

  pauseTimer(event?: Event): void {
    if (event) event.stopPropagation();
    this.boardFacade.pauseTimer();
  }

  stopTimerAndLog(taskId: number, event?: Event): void {
    if (event) event.stopPropagation();
    this.boardFacade.stopTimerAndLog(taskId);
  }

  resetTimer(): void {
    this.boardFacade.resetTimer();
  }

  logTaskTime(taskId: number): void {
    this.boardFacade.logTaskTime(taskId);
  }

  deleteTaskTimeLog(logId: number, taskId: number): void {
    this.boardFacade.deleteTaskTimeLog(logId, taskId);
  }

  formatTimerDisplay(seconds: number): string {
    return this.boardFacade.formatTimerDisplay(seconds);
  }
}
