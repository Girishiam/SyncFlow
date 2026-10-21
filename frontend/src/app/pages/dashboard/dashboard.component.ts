import { Component, signal, computed, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CdkDragDrop, DragDropModule, moveItemInArray, transferArrayItem } from '@angular/cdk/drag-drop';
import { Subscription } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { ProjectService } from '../../core/services/project.service';
import { TaskService } from '../../core/services/task.service';
import { ChatService } from '../../core/services/chat.service';
import { WebSocketService } from '../../core/services/websocket.service';
import { Project, KanbanColumn, TaskCard, Channel, ChatMessage } from '../../core/models/project.models';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, DragDropModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit, OnDestroy {
  currentUser = computed(() => this.authService.currentUserSignal());

  // Active navigation view
  activeNav = signal<'overview' | 'board' | 'discussions' | 'analytics' | 'rbac'>('board');

  // Projects
  projects = signal<Project[]>([]);
  selectedProjectId = signal<number>(1);

  // Kanban Columns
  columns = signal<KanbanColumn[]>([]);

  // Executive KPI stats
  totalTasksCount = signal<number>(0);
  blockedCount = signal<number>(0);
  completionPercentage = signal<number>(0);

  // Blocker banner notification
  activeBlockerBanner = signal<{ title: string; reason: string } | null>(null);

  // Discussions channels
  channels = signal<Channel[]>([]);
  activeChannel = signal<Channel | null>(null);
  messages = signal<ChatMessage[]>([]);
  chatInput = signal('');

  // Meeting modal state
  isMeetingOpen = signal(false);
  meetingRoom = signal('syncflow-sprint-room');

  // Blocker modal state
  selectedTask = signal<TaskCard | null>(null);
  blockerReasonInput = signal('');

  // Create Task modal state
  isCreateModalOpen = signal(false);
  newTaskTitle = signal('');
  newTaskDescription = signal('');
  newTaskPriority = signal<'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'>('MEDIUM');
  newTaskStageId = signal<number | null>(null);

  private subscriptions: Subscription[] = [];

  constructor(
    public authService: AuthService,
    private projectService: ProjectService,
    private taskService: TaskService,
    private chatService: ChatService,
    public wsService: WebSocketService
  ) {}

  ngOnInit(): void {
    this.loadProjects();
    this.loadChannels();
    this.setupWebSocketListeners();
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  // 1. Projects & Board Loading
  loadProjects(): void {
    this.projectService.getAllProjects().subscribe({
      next: (projs) => {
        this.projects.set(projs);
        if (projs.length > 0) {
          this.selectedProjectId.set(projs[0].id);
          this.loadBoard(projs[0].id);
        }
      },
      error: (err) => console.error('Failed to load projects', err)
    });
  }

  onProjectChange(event: Event): void {
    const select = event.target as HTMLSelectElement;
    const projId = Number(select.value);
    this.selectedProjectId.set(projId);
    this.loadBoard(projId);
  }

  loadBoard(projectId: number): void {
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
      error: (err) => console.error('Failed to load project board', err)
    });
  }

  // 2. Discussions Loading
  loadChannels(): void {
    this.chatService.getChannels().subscribe({
      next: (chs) => {
        this.channels.set(chs);
        if (chs.length > 0) {
          this.selectChannel(chs[0]);
        }
      },
      error: (err) => console.error('Failed to load channels', err)
    });
  }

  selectChannel(channel: Channel): void {
    this.activeChannel.set(channel);
    this.chatService.getMessages(channel.id).subscribe({
      next: (msgs) => {
        this.messages.set(msgs);
      },
      error: (err) => console.error('Failed to load messages for channel ' + channel.id, err)
    });

    // Listen to real-time channel messages via WebSocket
    const sub = this.wsService.subscribeToChannel(channel.id).subscribe({
      next: (newMsg: ChatMessage) => {
        if (newMsg && newMsg.channelId === channel.id) {
          this.messages.update(msgs => {
            if (newMsg.id && msgs.some(m => m.id === newMsg.id)) return msgs;
            return [...msgs, newMsg];
          });
        }
      }
    });
    this.subscriptions.push(sub);
  }

  // 3. WebSocket Real-time Synchronization
  setupWebSocketListeners(): void {
    // A. Real-time board movements & task updates from other users
    const boardSub = this.wsService.getBoardUpdates().subscribe({
      next: (updatedTask: TaskCard) => {
        this.applyTaskUpdateLocally(updatedTask);
      }
    });
    this.subscriptions.push(boardSub);

    // B. Real-time Blocker Alert Flash
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

  private applyTaskUpdateLocally(task: TaskCard): void {
    if (task.projectId && task.projectId !== this.selectedProjectId()) {
      return;
    }
    this.columns.update(cols => {
      const newCols = cols.map(col => {
        const remaining = col.tasks.filter(t => t.id !== task.id);
        if (col.numericId === task.stageId) {
          return { ...col, tasks: [...remaining, task] };
        }
        return { ...col, tasks: remaining };
      });
      return newCols;
    });
    this.recalculateMetrics();
  }

  private recalculateMetrics(): void {
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

  // 4. Drag & drop Task Cards
  onDrop(event: CdkDragDrop<TaskCard[]>): void {
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

        // Call backend API to persist stage move
        this.taskService.moveTask(movedTask.id, targetCol.numericId).subscribe({
          next: () => this.recalculateMetrics(),
          error: (err) => console.error('Failed to persist task move', err)
        });
      }
    }
  }

  // 5. Progress Slider
  onProgressChange(task: TaskCard): void {
    this.taskService.updateProgress(task.id, task.progress).subscribe({
      next: () => this.recalculateMetrics(),
      error: (err) => console.error('Failed to update progress', err)
    });
  }

  // 6. Blocker Operations
  openBlocker(task: TaskCard): void {
    this.selectedTask.set(task);
    this.blockerReasonInput.set(task.blockerReason || '');
  }

  saveBlocker(): void {
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
      error: (err) => console.error('Failed to update blocker', err)
    });
  }

  closeBlocker(): void {
    this.selectedTask.set(null);
    this.blockerReasonInput.set('');
  }

  // 7. Create Task Modal
  openCreateTask(stageId: number): void {
    this.newTaskStageId.set(stageId);
    this.newTaskTitle.set('');
    this.newTaskDescription.set('');
    this.newTaskPriority.set('MEDIUM');
    this.isCreateModalOpen.set(true);
  }

  closeCreateTask(): void {
    this.isCreateModalOpen.set(false);
  }

  submitCreateTask(): void {
    const title = this.newTaskTitle().trim();
    const stageId = this.newTaskStageId();
    const projectId = this.selectedProjectId();

    if (!title || !stageId) return;

    this.taskService.createTask({
      projectId,
      stageId,
      title,
      description: this.newTaskDescription().trim(),
      priority: this.newTaskPriority()
    }).subscribe({
      next: (created) => {
        this.applyTaskUpdateLocally(created);
        this.closeCreateTask();
      },
      error: (err) => console.error('Failed to create task', err)
    });
  }

  // 8. Discussions Messaging
  sendMessage(): void {
    const text = this.chatInput().trim();
    const channel = this.activeChannel();
    if (!text || !channel) return;

    this.chatService.sendMessage(channel.id, text).subscribe({
      next: (msg) => {
        this.chatInput.set('');
      },
      error: (err) => console.error('Failed to send message', err)
    });
  }

  // 9. Availability Toggle
  toggleAvailability(): void {
    const user = this.currentUser();
    if (user) {
      user.onLeave = !user.onLeave;
    }
  }
}
