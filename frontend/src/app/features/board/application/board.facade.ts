import { Injectable, signal, computed, OnDestroy } from '@angular/core';
import { Subscription } from 'rxjs';
import { ProjectService } from '../../../core/services/project.service';
import { TaskService } from '../../../core/services/task.service';
import { WebSocketService } from '../../../core/services/websocket.service';
import { WorkLogService } from '../../../core/services/worklog.service';
import { AuditService } from '../../../core/services/audit.service';
import {
  Project,
  KanbanColumn,
  TaskCard,
  BankUser,
  DailyWorkLog,
  SubmitWorkLogPayload,
  SubtaskItem,
  TaskAttachmentItem,
  BurndownAnalytics,
  AuditLogItem,
  AuditMetrics
} from '../../../core/models/project.models';
import { TaskComment } from '../../../core/ports/task.port';
import { CdkDragDrop, moveItemInArray, transferArrayItem } from '@angular/cdk/drag-drop';

@Injectable({
  providedIn: 'root'
})
export class BoardFacade implements OnDestroy {
  // State Signals
  public projects = signal<Project[]>([]);
  public selectedProjectId = signal<number>(1);
  public columns = signal<KanbanColumn[]>([]);

  // Metric Signals
  public totalTasksCount = signal<number>(0);
  public blockedCount = signal<number>(0);
  public completionPercentage = signal<number>(0);

  // Blocker Notification Banner Signal
  public activeBlockerBanner = signal<{ title: string; reason: string } | null>(null);

  // Bank Members
  public bankUsers = signal<BankUser[]>([]);

  // Search & Filter State
  public searchQuery = signal<string>('');
  public filterMode = signal<'ALL' | 'MY_TASKS' | 'BLOCKED' | 'CRITICAL_HIGH'>('ALL');
  public currentUserName = signal<string>('Tariq Manager');

  // Modals State
  public isCreateModalOpen = signal(false);
  public newTaskTitle = signal('');
  public newTaskDescription = signal('');
  public newTaskPriority = signal<'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'>('MEDIUM');
  public newTaskStageId = signal<number | null>(null);
  public newTaskAssigneeId = signal<string>('');
  public newTaskDeadline = signal<string>('');

  public selectedTask = signal<TaskCard | null>(null);
  public blockerReasonInput = signal('');

  // Task Detail Drawer State
  public activeDetailTask = signal<TaskCard | null>(null);
  public detailAssigneeId = signal<string>('');
  public detailDeadline = signal<string>('');
  public taskComments = signal<TaskComment[]>([]);
  public commentInput = signal('');
  public isLoadingComments = signal(false);
  public completionCelebrationMsg = signal<string | null>(null);

  // Project Creator Modal State
  public isCreateProjectModalOpen = signal(false);
  public newProjectName = signal('');
  public newProjectDescription = signal('');
  public isCreatingProject = signal(false);

  // Stage Creator Modal State
  public isCreateStageModalOpen = signal(false);
  public newStageName = signal('');
  public newStageIsDone = signal(false);

  // Daily Standup & EOD Work Log State
  public isWorkLogDrawerOpen = signal(false);
  public myTodayLog = signal<DailyWorkLog | null>(null);
  public teamLogs = signal<DailyWorkLog[]>([]);
  public workLogSummary = signal('');
  public workLogCompletedCount = signal<number>(0);
  public workLogDate = signal<string>(new Date().toISOString().substring(0, 10));
  public workLogActiveTab = signal<'MY_LOG' | 'TEAM_LOGS'>('MY_LOG');
  public isSavingWorkLog = signal(false);
  public workLogSaveSuccess = signal(false);

  // Subtasks & QA Checklist State
  public taskSubtasks = signal<SubtaskItem[]>([]);
  public newSubtaskTitle = signal<string>('');
  public isAddingSubtask = signal<boolean>(false);

  public subtaskProgress = computed(() => {
    const items = this.taskSubtasks();
    if (!items || items.length === 0) return 0;
    const completed = items.filter(s => s.isCompleted).length;
    return Math.round((completed / items.length) * 100);
  });

  // Task Deliverable Attachments State
  public taskAttachments = signal<TaskAttachmentItem[]>([]);
  public newAttachmentName = signal<string>('');
  public newAttachmentUrl = signal<string>('');
  public newAttachmentType = signal<'SPEC_DOC' | 'DESIGN' | 'SCREENSHOT' | 'PR_LINK' | 'LOGS' | 'OTHER'>('SPEC_DOC');
  public isAddingAttachment = signal<boolean>(false);

  // Sprint Burndown & Velocity Analytics State
  public burndownAnalytics = signal<BurndownAnalytics | null>(null);
  public isLoadingBurndown = signal<boolean>(false);

  // Compliance & Immutable Audit Trail Ledger State
  public auditLogs = signal<AuditLogItem[]>([]);
  public auditMetrics = signal<AuditMetrics | null>(null);
  public isLoadingAudit = signal<boolean>(false);
  public auditFilterAction = signal<string>('ALL');
  public auditFilterEntity = signal<string>('ALL');
  public isExportingCsv = signal<boolean>(false);

  public filteredAuditLogs = computed(() => {
    const logs = this.auditLogs();
    const action = this.auditFilterAction();
    const entity = this.auditFilterEntity();
    return logs.filter(l => {
      const matchAction = !action || action === 'ALL' || l.action === action;
      const matchEntity = !entity || entity === 'ALL' || l.entityType === entity;
      return matchAction && matchEntity;
    });
  });

  // Computed Filtered Columns
  public filteredColumns = computed(() => {
    const query = this.searchQuery().trim().toLowerCase();
    const mode = this.filterMode();
    const cols = this.columns();

    return cols.map(col => {
      let tasks = col.tasks;

      // Text Search Filter
      if (query) {
        tasks = tasks.filter(t =>
          (t.title && t.title.toLowerCase().includes(query)) ||
          (t.description && t.description.toLowerCase().includes(query)) ||
          (t.assignee && t.assignee.toLowerCase().includes(query)) ||
          (t.projectName && t.projectName.toLowerCase().includes(query))
        );
      }

      // Quick Filter Mode
      if (mode === 'MY_TASKS') {
        const myName = this.currentUserName().toLowerCase();
        tasks = tasks.filter(t => t.assignee && t.assignee.toLowerCase().includes(myName.split(' ')[0]));
      } else if (mode === 'BLOCKED') {
        tasks = tasks.filter(t => t.isBlocked);
      } else if (mode === 'CRITICAL_HIGH') {
        tasks = tasks.filter(t => t.priority === 'CRITICAL' || t.priority === 'HIGH');
      }

      return {
        ...col,
        tasks
      };
    });
  });

  private subscriptions: Subscription[] = [];

  constructor(
    private projectService: ProjectService,
    private taskService: TaskService,
    private wsService: WebSocketService,
    private workLogService: WorkLogService,
    private auditService: AuditService
  ) {
    this.initWebSocketListeners();
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  public init(): void {
    this.loadProjects();
    this.loadBankUsers();
    this.loadAuditData();
  }

  public loadBankUsers(): void {
    this.taskService.getBankUsers().subscribe({
      next: (users) => this.bankUsers.set(users),
      error: (err) => console.error('BoardFacade: Failed to load bank users', err)
    });
  }

  public loadProjects(): void {
    this.projectService.getAllProjects().subscribe({
      next: (projs) => {
        this.projects.set(projs);
        if (projs.length > 0) {
          this.selectedProjectId.set(projs[0].id);
          this.loadBoard(projs[0].id);
        }
      },
      error: (err) => console.error('BoardFacade: Failed to load projects', err)
    });
  }

  public selectProject(projectId: number): void {
    this.selectedProjectId.set(projectId);
    this.loadBoard(projectId);
  }

  public loadBoard(projectId: number): void {
    this.loadBurndown(projectId);
    this.projectService.getProjectBoard(projectId).subscribe({
      next: (board) => {
        const cols: KanbanColumn[] = board.stages.map(s => ({
          id: 'stage-' + s.id,
          numericId: s.id,
          title: s.name,
          isDoneStage: s.isDoneStage,
          tasks: s.tasks || []
        }));
        this.columns.set(cols);
        this.totalTasksCount.set(board.totalTasks);
        this.blockedCount.set(board.blockedTasks);
        this.completionPercentage.set(board.completionPercentage);
      },
      error: (err) => console.error('BoardFacade: Failed to load project board', err)
    });
  }

  private initWebSocketListeners(): void {
    const boardSub = this.wsService.getBoardUpdates().subscribe({
      next: (updatedTask: TaskCard) => {
        this.applyTaskUpdateLocally(updatedTask);
      }
    });
    this.subscriptions.push(boardSub);

    const blockerSub = this.wsService.getBlockerAlerts().subscribe({
      next: (alertTask: TaskCard) => {
        this.activeBlockerBanner.set({
          title: alertTask.title,
          reason: alertTask.blockerReason || 'Critical blocker encountered'
        });
        setTimeout(() => this.activeBlockerBanner.set(null), 8000);
      }
    });
    this.subscriptions.push(blockerSub);
  }

  public applyTaskUpdateLocally(task: TaskCard): void {
    if (task.projectId && task.projectId !== this.selectedProjectId()) {
      return;
    }
    this.columns.update(cols => {
      return cols.map(col => {
        const remaining = col.tasks.filter(t => t.id !== task.id);
        if (col.numericId === task.stageId) {
          return { ...col, tasks: [...remaining, task] };
        }
        return { ...col, tasks: remaining };
      });
    });
    this.recalculateMetrics();
  }

  public recalculateMetrics(): void {
    let total = 0;
    let blocked = 0;
    let completed = 0;

    for (const col of this.columns()) {
      total += col.tasks.length;
      for (const t of col.tasks) {
        if (t.isBlocked) blocked++;
        if (col.isDoneStage || t.progress === 100) completed++;
      }
    }

    this.totalTasksCount.set(total);
    this.blockedCount.set(blocked);
    this.completionPercentage.set(total > 0 ? Math.round((completed / total) * 100) : 0);
  }

  public onDrop(event: CdkDragDrop<TaskCard[]>): void {
    if (event.previousContainer === event.container) {
      moveItemInArray(event.container.data, event.previousIndex, event.currentIndex);
    } else {
      transferArrayItem(
        event.previousContainer.data,
        event.container.data,
        event.previousIndex,
        event.currentIndex
      );
      const movedTask = event.container.data[event.currentIndex];
      const targetCol = this.columns().find(c => c.id === event.container.id);

      if (targetCol) {
        movedTask.stageId = targetCol.numericId;
        if (targetCol.isDoneStage) {
          movedTask.progress = 100;
          movedTask.isBlocked = false;
        }

        this.taskService.moveTask(movedTask.id, targetCol.numericId).subscribe({
          next: () => this.recalculateMetrics(),
          error: (err) => console.error('BoardFacade: Failed to persist task move', err)
        });
      }
    }
  }

  public onProgressChange(task: TaskCard): void {
    this.taskService.updateProgress(task.id, task.progress).subscribe({
      next: () => this.recalculateMetrics(),
      error: (err) => console.error('BoardFacade: Failed to update progress', err)
    });
  }

  // Create Task Modal Controls
  public openCreateTask(stageId: number): void {
    this.newTaskStageId.set(stageId);
    this.newTaskTitle.set('');
    this.newTaskDescription.set('');
    this.newTaskPriority.set('MEDIUM');
    this.newTaskAssigneeId.set('');
    this.newTaskDeadline.set('');
    this.isCreateModalOpen.set(true);
  }

  public closeCreateTask(): void {
    this.isCreateModalOpen.set(false);
  }

  public submitCreateTask(): void {
    const title = this.newTaskTitle().trim();
    const stageId = this.newTaskStageId();
    const projectId = this.selectedProjectId();

    if (!title || !stageId) return;

    const assigneeVal = this.newTaskAssigneeId() ? this.newTaskAssigneeId() : undefined;
    const deadlineVal = this.newTaskDeadline() ? new Date(this.newTaskDeadline()).toISOString() : undefined;

    this.taskService.createTask({
      projectId,
      stageId,
      title,
      description: this.newTaskDescription().trim(),
      priority: this.newTaskPriority(),
      assigneeId: assigneeVal,
      deadline: deadlineVal
    }).subscribe({
      next: (created) => {
        this.applyTaskUpdateLocally(created);
        this.closeCreateTask();
      },
      error: (err) => console.error('BoardFacade: Failed to create task', err)
    });
  }

  // Blocker Dialog Controls
  public openBlocker(task: TaskCard): void {
    this.selectedTask.set(task);
    this.blockerReasonInput.set(task.blockerReason || '');
  }

  public closeBlocker(): void {
    this.selectedTask.set(null);
    this.blockerReasonInput.set('');
  }

  public saveBlocker(): void {
    const task = this.selectedTask();
    if (!task) return;

    const reason = this.blockerReasonInput().trim();
    const isBlocked = !!reason;

    this.taskService.setBlocker(task.id, isBlocked, reason).subscribe({
      next: (updated) => {
        task.isBlocked = updated.isBlocked;
        task.blockerReason = updated.blockerReason;
        this.recalculateMetrics();
        this.closeBlocker();
      },
      error: (err) => console.error('BoardFacade: Failed to update blocker', err)
    });
  }

  // Task Detail Drawer
  public openTaskDetail(task: TaskCard): void {
    // Resolve assignee ID if missing but assignee name is present
    let initialAssigneeId = task.assigneeId || '';
    if (!initialAssigneeId && task.assignee && task.assignee !== 'Unassigned') {
      const match = this.bankUsers().find(u => u.fullName.toLowerCase() === task.assignee!.toLowerCase());
      if (match) {
        initialAssigneeId = match.id;
        task.assigneeId = match.id;
      }
    }
    this.detailAssigneeId.set(initialAssigneeId);

    this.activeDetailTask.set(task);
    this.commentInput.set('');
    this.detailDeadline.set(task.deadline ? task.deadline.substring(0, 10) : '');
    this.loadTaskComments(task.id);
    this.loadSubtasks(task.id);
    this.loadAttachments(task.id);
  }

  public closeTaskDetail(): void {
    this.activeDetailTask.set(null);
    this.detailAssigneeId.set('');
    this.taskComments.set([]);
    this.commentInput.set('');
    this.detailDeadline.set('');
    this.taskSubtasks.set([]);
    this.newSubtaskTitle.set('');
    this.taskAttachments.set([]);
    this.newAttachmentName.set('');
    this.newAttachmentUrl.set('');
  }

  public assignTaskTo(task: TaskCard, assigneeId: string | null): void {
    this.taskService.assignTask(task.id, assigneeId).subscribe({
      next: (updated) => {
        task.assignee = updated.assignee;
        task.assigneeId = updated.assigneeId;
        task.avatar = updated.avatar;
        this.detailAssigneeId.set(updated.assigneeId || '');
        if (this.activeDetailTask() && this.activeDetailTask()!.id === task.id) {
          this.activeDetailTask.update(t => t ? {
            ...t,
            assignee: updated.assignee,
            assigneeId: updated.assigneeId,
            avatar: updated.avatar
          } : null);
        }
      },
      error: (err) => console.error('BoardFacade: Failed to assign task', err)
    });
  }

  public markTaskCompleted(task: TaskCard): void {
    const doneCol = this.columns().find(c => c.isDoneStage) || this.columns()[this.columns().length - 1];
    task.progress = 100;
    task.isBlocked = false;
    task.blockerReason = undefined;

    if (doneCol) {
      task.stageId = doneCol.numericId;
      task.stageName = doneCol.title;
    }

    if (this.activeDetailTask() && this.activeDetailTask()!.id === task.id) {
      this.activeDetailTask.update(t => t ? {
        ...t,
        progress: 100,
        isBlocked: false,
        blockerReason: undefined,
        stageId: doneCol ? doneCol.numericId : t.stageId,
        stageName: doneCol ? doneCol.title : t.stageName
      } : null);
    }

    this.taskService.updateProgress(task.id, 100).subscribe({
      next: () => {
        if (doneCol) {
          this.taskService.moveTask(task.id, doneCol.numericId).subscribe({
            next: () => {
              this.loadBoard(this.selectedProjectId());
              this.showCelebrationBanner(`🎉 Task Completed: "${task.title}" is finished & verified!`);
              this.submitAuditComment(task.id, '✅ Task marked as COMPLETED and verified.');
            }
          });
        } else {
          this.recalculateMetrics();
          this.showCelebrationBanner(`🎉 Task Completed: "${task.title}" is finished & verified!`);
          this.submitAuditComment(task.id, '✅ Task marked as COMPLETED and verified.');
        }
      },
      error: (err) => console.error('BoardFacade: Failed to mark task completed', err)
    });
  }

  public reopenTask(task: TaskCard): void {
    const devCol = this.columns().find(c => !c.isDoneStage && c.numericId !== 1) || this.columns()[0];
    task.progress = 50;

    if (devCol) {
      task.stageId = devCol.numericId;
      task.stageName = devCol.title;
    }

    if (this.activeDetailTask() && this.activeDetailTask()!.id === task.id) {
      this.activeDetailTask.update(t => t ? {
        ...t,
        progress: 50,
        stageId: devCol ? devCol.numericId : t.stageId,
        stageName: devCol ? devCol.title : t.stageName
      } : null);
    }

    this.taskService.updateProgress(task.id, 50).subscribe({
      next: () => {
        if (devCol) {
          this.taskService.moveTask(task.id, devCol.numericId).subscribe({
            next: () => {
              this.loadBoard(this.selectedProjectId());
              this.submitAuditComment(task.id, '🔄 Task reopened for further review and development.');
            }
          });
        } else {
          this.recalculateMetrics();
          this.submitAuditComment(task.id, '🔄 Task reopened for further review and development.');
        }
      },
      error: (err) => console.error('BoardFacade: Failed to reopen task', err)
    });
  }

  private showCelebrationBanner(msg: string): void {
    this.completionCelebrationMsg.set(msg);
    setTimeout(() => this.completionCelebrationMsg.set(null), 6000);
  }

  private submitAuditComment(taskId: number, content: string): void {
    this.taskService.addComment(taskId, content).subscribe({
      next: (comment) => {
        if (this.activeDetailTask() && this.activeDetailTask()!.id === taskId) {
          this.taskComments.update(c => [...c, comment]);
        }
      },
      error: (err) => console.error('BoardFacade: Failed to post audit comment', err)
    });
  }

  public updateTaskDeadline(task: TaskCard, deadlineStr: string | null): void {
    const isoDeadline = deadlineStr ? new Date(deadlineStr).toISOString() : null;
    this.taskService.updateDeadline(task.id, isoDeadline).subscribe({
      next: (updated) => {
        task.deadline = updated.deadline;
        if (this.activeDetailTask() && this.activeDetailTask()!.id === task.id) {
          this.activeDetailTask.update(t => t ? { ...t, deadline: updated.deadline } : null);
        }
      },
      error: (err) => console.error('BoardFacade: Failed to update task deadline', err)
    });
  }

  public loadTaskComments(taskId: number): void {
    this.isLoadingComments.set(true);
    this.taskService.getTaskComments(taskId).subscribe({
      next: (comments) => {
        this.isLoadingComments.set(false);
        this.taskComments.set(comments);
      },
      error: (err) => {
        this.isLoadingComments.set(false);
        console.error('BoardFacade: Failed to load task comments', err);
      }
    });
  }

  public submitComment(): void {
    const task = this.activeDetailTask();
    const content = this.commentInput().trim();
    if (!task || !content) return;

    this.taskService.addComment(task.id, content).subscribe({
      next: (comment) => {
        this.taskComments.update(c => [...c, comment]);
        this.commentInput.set('');
      },
      error: (err) => console.error('BoardFacade: Failed to add comment', err)
    });
  }

  // Custom Stage / Pipeline Builder
  public openCreateStageModal(): void {
    this.newStageName.set('');
    this.newStageIsDone.set(false);
    this.isCreateStageModalOpen.set(true);
  }

  public closeCreateStageModal(): void {
    this.isCreateStageModalOpen.set(false);
  }

  public submitCreateStage(): void {
    const name = this.newStageName().trim();
    const projectId = this.selectedProjectId();
    if (!name || !projectId) return;

    const isDone = this.newStageIsDone();
    this.projectService.createStage(projectId, name, isDone).subscribe({
      next: (stage) => {
        const newCol: KanbanColumn = {
          id: 'stage-' + stage.id,
          numericId: stage.id,
          title: stage.name,
          isDoneStage: stage.isDoneStage,
          tasks: []
        };
        this.columns.update(cols => [...cols, newCol]);
        this.closeCreateStageModal();
      },
      error: (err) => console.error('BoardFacade: Failed to create stage', err)
    });
  }

  // Work Log Actions
  public openWorkLogDrawer(): void {
    this.isWorkLogDrawerOpen.set(true);
    this.workLogSaveSuccess.set(false);
    const today = this.workLogDate();
    this.workLogService.getMyWorkLog(today).subscribe({
      next: (log) => {
        this.myTodayLog.set(log);
        if (log) {
          this.workLogSummary.set(log.summaryText);
          this.workLogCompletedCount.set(log.completedTasksCount);
        } else {
          this.workLogSummary.set('');
          this.workLogCompletedCount.set(0);
        }
      },
      error: (err) => console.error('BoardFacade: Failed to load my work log', err)
    });

    this.loadTeamWorkLogs();
  }

  public closeWorkLogDrawer(): void {
    this.isWorkLogDrawerOpen.set(false);
  }

  public setWorkLogTab(tab: 'MY_LOG' | 'TEAM_LOGS'): void {
    this.workLogActiveTab.set(tab);
    if (tab === 'TEAM_LOGS') {
      this.loadTeamWorkLogs();
    }
  }

  public loadTeamWorkLogs(): void {
    this.workLogService.getRecentLogs().subscribe({
      next: (logs) => this.teamLogs.set(logs),
      error: (err) => console.error('BoardFacade: Failed to load team work logs', err)
    });
  }

  public submitDailyWorkLog(): void {
    const text = this.workLogSummary().trim();
    if (!text) return;

    this.isSavingWorkLog.set(true);
    const payload: SubmitWorkLogPayload = {
      logDate: this.workLogDate(),
      summaryText: text,
      completedTasksCount: this.workLogCompletedCount()
    };

    this.workLogService.submitWorkLog(payload).subscribe({
      next: (savedLog) => {
        this.isSavingWorkLog.set(false);
        this.myTodayLog.set(savedLog);
        this.workLogSaveSuccess.set(true);
        this.loadTeamWorkLogs();
        setTimeout(() => this.workLogSaveSuccess.set(false), 3000);
      },
      error: (err) => {
        this.isSavingWorkLog.set(false);
        console.error('BoardFacade: Failed to submit work log', err);
      }
    });
  }

  // Project Creation Actions
  public openCreateProjectModal(): void {
    this.newProjectName.set('');
    this.newProjectDescription.set('');
    this.isCreateProjectModalOpen.set(true);
  }

  public closeCreateProjectModal(): void {
    this.isCreateProjectModalOpen.set(false);
  }

  public submitCreateProject(): void {
    const name = this.newProjectName().trim();
    if (!name) return;

    this.isCreatingProject.set(true);
    const desc = this.newProjectDescription().trim();

    this.projectService.createProject(name, desc).subscribe({
      next: (createdProject) => {
        this.isCreatingProject.set(false);
        this.projects.update(projs => [...projs, createdProject]);
        this.selectedProjectId.set(createdProject.id);
        this.loadBoard(createdProject.id);
        this.closeCreateProjectModal();
      },
      error: (err) => {
        this.isCreatingProject.set(false);
        console.error('BoardFacade: Failed to create project', err);
      }
    });
  }

  // --- Subtasks QA Checklist Actions ---
  public loadSubtasks(taskId: number): void {
    this.taskService.getSubtasks(taskId).subscribe({
      next: (items) => this.taskSubtasks.set(items),
      error: (err) => console.error('BoardFacade: Failed to load subtasks', err)
    });
  }

  public addSubtask(): void {
    const task = this.activeDetailTask();
    const title = this.newSubtaskTitle().trim();
    if (!task || !title) return;

    this.isAddingSubtask.set(true);
    this.taskService.addSubtask(task.id, title).subscribe({
      next: (created) => {
        this.taskSubtasks.update(list => [...list, created]);
        this.newSubtaskTitle.set('');
        this.isAddingSubtask.set(false);
      },
      error: (err) => {
        this.isAddingSubtask.set(false);
        console.error('BoardFacade: Failed to add subtask', err);
      }
    });
  }

  public toggleSubtask(item: SubtaskItem): void {
    const task = this.activeDetailTask();
    if (!task) return;

    this.taskService.toggleSubtask(task.id, item.id).subscribe({
      next: (updated) => {
        this.taskSubtasks.update(list => list.map(s => s.id === updated.id ? updated : s));
      },
      error: (err) => console.error('BoardFacade: Failed to toggle subtask', err)
    });
  }

  public deleteSubtask(subtaskId: number): void {
    const task = this.activeDetailTask();
    if (!task) return;

    this.taskService.deleteSubtask(task.id, subtaskId).subscribe({
      next: () => {
        this.taskSubtasks.update(list => list.filter(s => s.id !== subtaskId));
      },
      error: (err) => console.error('BoardFacade: Failed to delete subtask', err)
    });
  }

  // --- Task Deliverable Attachments Actions ---
  public loadAttachments(taskId: number): void {
    this.taskService.getAttachments(taskId).subscribe({
      next: (attachments) => this.taskAttachments.set(attachments),
      error: (err) => console.error('BoardFacade: Failed to load attachments', err)
    });
  }

  public addAttachment(): void {
    const task = this.activeDetailTask();
    const name = this.newAttachmentName().trim();
    const url = this.newAttachmentUrl().trim();
    if (!task || !name || !url) return;

    this.isAddingAttachment.set(true);
    const type = this.newAttachmentType();

    this.taskService.addAttachment(task.id, {
      fileName: name,
      fileType: type,
      fileUrl: url,
      fileSize: '1.2 MB'
    }).subscribe({
      next: (created) => {
        this.taskAttachments.update(list => [...list, created]);
        this.newAttachmentName.set('');
        this.newAttachmentUrl.set('');
        this.isAddingAttachment.set(false);
      },
      error: (err) => {
        this.isAddingAttachment.set(false);
        console.error('BoardFacade: Failed to add attachment', err);
      }
    });
  }

  public deleteAttachment(attachmentId: number): void {
    const task = this.activeDetailTask();
    if (!task) return;

    this.taskService.deleteAttachment(task.id, attachmentId).subscribe({
      next: () => {
        this.taskAttachments.update(list => list.filter(a => a.id !== attachmentId));
      },
      error: (err) => console.error('BoardFacade: Failed to delete attachment', err)
    });
  }

  // --- Sprint Burndown & Velocity Analytics Actions ---
  public loadBurndown(projectId?: number): void {
    const pId = projectId || this.selectedProjectId();
    if (!pId) return;

    this.isLoadingBurndown.set(true);
    this.projectService.getBurndownAnalytics(pId).subscribe({
      next: (data) => {
        this.burndownAnalytics.set(data);
        this.isLoadingBurndown.set(false);
      },
      error: (err) => {
        this.isLoadingBurndown.set(false);
        console.error('BoardFacade: Failed to load burndown analytics', err);
      }
    });
  }

  // --- Compliance & Immutable Audit Trail Ledger Actions ---
  public loadAuditData(): void {
    this.isLoadingAudit.set(true);
    this.auditService.getAuditMetrics().subscribe({
      next: (metrics) => this.auditMetrics.set(metrics),
      error: (err) => console.error('BoardFacade: Failed to load audit metrics', err)
    });

    this.auditService.getAuditLogs().subscribe({
      next: (logs) => {
        this.auditLogs.set(logs);
        this.isLoadingAudit.set(false);
      },
      error: (err) => {
        this.isLoadingAudit.set(false);
        console.error('BoardFacade: Failed to load audit logs', err);
      }
    });
  }

  public filterAuditLogs(action: string, entityType: string): void {
    this.auditFilterAction.set(action);
    this.auditFilterEntity.set(entityType);
  }

  public exportAuditCsv(): void {
    this.isExportingCsv.set(true);
    const action = this.auditFilterAction();
    const entity = this.auditFilterEntity();

    this.auditService.exportCsv(action, entity).subscribe({
      next: (blob) => {
        this.isExportingCsv.set(false);
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `syncflow-compliance-audit-${new Date().toISOString().substring(0, 10)}.csv`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
      },
      error: (err) => {
        this.isExportingCsv.set(false);
        console.error('BoardFacade: Failed to export audit CSV', err);
      }
    });
  }
}


