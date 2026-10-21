import { Component, signal, computed, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { CdkDragDrop, DragDropModule } from '@angular/cdk/drag-drop';
import { AuthService } from '../../core/services/auth.service';
import { WebSocketService } from '../../core/services/websocket.service';
import { BoardFacade } from '../../features/board/application/board.facade';
import { ChatFacade } from '../../features/chat/application/chat.facade';
import { RbacFacade } from '../../features/rbac/application/rbac.facade';
import { Channel, TaskCard, BankUser } from '../../core/models/project.models';
import { NotificationFacade } from '../../features/notification/application/notification.facade';
import { NotificationItem } from '../../core/models/notification.models';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, DragDropModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {
  public authService = inject(AuthService);
  public boardFacade = inject(BoardFacade);
  public chatFacade = inject(ChatFacade);
  public rbacFacade = inject(RbacFacade);
  public notificationFacade = inject(NotificationFacade);
  public wsService = inject(WebSocketService);
  private route = inject(ActivatedRoute);

  currentUser = computed(() => this.authService.currentUserSignal());

  // Active navigation view
  activeNav = signal<'overview' | 'board' | 'discussions' | 'analytics' | 'rbac' | 'compliance'>('board');

  // Subtasks & Deliverables Checklist Delegates
  taskSubtasks = this.boardFacade.taskSubtasks;
  newSubtaskTitle = this.boardFacade.newSubtaskTitle;
  isAddingSubtask = this.boardFacade.isAddingSubtask;
  subtaskProgress = this.boardFacade.subtaskProgress;

  // Task Deliverable Attachments Delegates
  taskAttachments = this.boardFacade.taskAttachments;
  newAttachmentName = this.boardFacade.newAttachmentName;
  newAttachmentUrl = this.boardFacade.newAttachmentUrl;
  newAttachmentType = this.boardFacade.newAttachmentType;
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
  isMeetingOpen = signal(false);
  meetingRoom = signal('syncflow-sprint-room');

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      const tab = params['tab'];
      if (tab === 'rbac' || tab === 'discussions' || tab === 'analytics' || tab === 'board' || tab === 'compliance') {
        this.activeNav.set(tab);
      }
    });

    this.boardFacade.init();
    this.chatFacade.init();
    this.rbacFacade.init();
    this.notificationFacade.init(this.currentUser()?.id, this.currentUser()?.role?.name);
  }

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

  openBlocker(task: TaskCard): void {
    this.boardFacade.openBlocker(task);
  }

  saveBlocker(): void {
    this.boardFacade.saveBlocker();
  }

  closeBlocker(): void {
    this.boardFacade.closeBlocker();
  }

  openCreateTask(stageId: number): void {
    this.boardFacade.openCreateTask(stageId);
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

  getChannelIconSymbol(iconId?: string): string {
    const found = this.availableChannelIcons.find(i => i.id === iconId);
    return found ? found.icon : '#';
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
      this.activeNav.set('discussions');
      const target = this.channels().find(c => c.id === item.referenceId);
      if (target) {
        this.selectChannel(target);
      }
    } else if (item.actionType === 'NAVIGATE_TASK') {
      this.activeNav.set('board');
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
      this.activeNav.set('rbac');
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
}
