import { Component, signal, computed, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CdkDragDrop, DragDropModule } from '@angular/cdk/drag-drop';
import { AuthService } from '../../core/services/auth.service';
import { WebSocketService } from '../../core/services/websocket.service';
import { BoardFacade } from '../../features/board/application/board.facade';
import { ChatFacade } from '../../features/chat/application/chat.facade';
import { RbacFacade } from '../../features/rbac/application/rbac.facade';
import { Channel, TaskCard } from '../../core/models/project.models';

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
  public wsService = inject(WebSocketService);

  currentUser = computed(() => this.authService.currentUserSignal());

  // Active navigation view
  activeNav = signal<'overview' | 'board' | 'discussions' | 'analytics' | 'rbac'>('board');

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
  detailDeadline = this.boardFacade.detailDeadline;
  taskComments = this.boardFacade.taskComments;
  commentInput = this.boardFacade.commentInput;
  isLoadingComments = this.boardFacade.isLoadingComments;

  // Stage Creator Modal Delegates
  isCreateStageModalOpen = this.boardFacade.isCreateStageModalOpen;
  newStageName = this.boardFacade.newStageName;
  newStageIsDone = this.boardFacade.newStageIsDone;

  // Chat Facade State Delegates
  channels = this.chatFacade.channels;
  activeChannel = this.chatFacade.activeChannel;
  messages = this.chatFacade.messages;
  chatInput = this.chatFacade.chatInput;

  // RBAC Facade State Delegates
  roles = this.rbacFacade.roles;
  permissions = this.rbacFacade.permissions;
  selectedRoleId = this.rbacFacade.selectedRoleId;
  selectedRole = this.rbacFacade.selectedRole;
  modules = this.rbacFacade.modules;
  activePermissionIds = this.rbacFacade.activePermissionIds;
  isSavingRbac = this.rbacFacade.isSaving;
  rbacSuccessMsg = this.rbacFacade.saveSuccessMessage;

  // Custom Role Modal Delegates
  isCreateRoleModalOpen = this.rbacFacade.isCreateRoleModalOpen;
  newRoleName = this.rbacFacade.newRoleName;
  newRoleDescription = this.rbacFacade.newRoleDescription;

  // Meeting state
  isMeetingOpen = signal(false);
  meetingRoom = signal('syncflow-sprint-room');

  ngOnInit(): void {
    this.boardFacade.init();
    this.chatFacade.init();
    this.rbacFacade.init();
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

  onAssigneeChange(task: TaskCard, event: Event): void {
    const select = event.target as HTMLSelectElement;
    const val = select.value ? select.value : null;
    this.boardFacade.assignTaskTo(task, val);
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

  // Chat Operations
  selectChannel(channel: Channel): void {
    this.chatFacade.selectChannel(channel);
  }

  sendMessage(): void {
    this.chatFacade.sendMessage();
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

  // Availability Toggle
  toggleAvailability(): void {
    const user = this.currentUser();
    if (user) {
      user.onLeave = !user.onLeave;
    }
  }
}
