import { Injectable, signal, computed, OnDestroy } from '@angular/core';
import { Subscription } from 'rxjs';
import { ProjectService } from '../../../core/services/project.service';
import { TaskService } from '../../../core/services/task.service';
import { WebSocketService } from '../../../core/services/websocket.service';
import { WorkLogService } from '../../../core/services/worklog.service';
import { Project, KanbanColumn, TaskCard, BankUser, DailyWorkLog, SubmitWorkLogPayload } from '../../../core/models/project.models';
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
  public detailDeadline = signal<string>('');
  public taskComments = signal<TaskComment[]>([]);
  public commentInput = signal('');
  public isLoadingComments = signal(false);

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
    private workLogService: WorkLogService
  ) {
    this.initWebSocketListeners();
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  public init(): void {
    this.loadProjects();
    this.loadBankUsers();
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
    this.activeDetailTask.set(task);
    this.commentInput.set('');
    this.detailDeadline.set(task.deadline ? task.deadline.substring(0, 10) : '');
    this.loadTaskComments(task.id);
  }

  public closeTaskDetail(): void {
    this.activeDetailTask.set(null);
    this.taskComments.set([]);
    this.commentInput.set('');
    this.detailDeadline.set('');
  }

  public assignTaskTo(task: TaskCard, assigneeId: string | null): void {
    this.taskService.assignTask(task.id, assigneeId).subscribe({
      next: (updated) => {
        task.assignee = updated.assignee;
        task.assigneeId = updated.assigneeId;
        task.avatar = updated.avatar;
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
}


