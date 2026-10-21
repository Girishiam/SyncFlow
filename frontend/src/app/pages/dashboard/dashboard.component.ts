import { Component, signal, computed, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CdkDragDrop, DragDropModule } from '@angular/cdk/drag-drop';
import { AuthService } from '../../core/services/auth.service';
import { WebSocketService } from '../../core/services/websocket.service';
import { BoardFacade } from '../../features/board/application/board.facade';
import { ChatFacade } from '../../features/chat/application/chat.facade';
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
  public wsService = inject(WebSocketService);

  currentUser = computed(() => this.authService.currentUserSignal());

  // Active navigation view
  activeNav = signal<'overview' | 'board' | 'discussions' | 'analytics' | 'rbac'>('board');

  // Board Facade State Delegates
  projects = this.boardFacade.projects;
  selectedProjectId = this.boardFacade.selectedProjectId;
  columns = this.boardFacade.columns;
  totalTasksCount = this.boardFacade.totalTasksCount;
  blockedCount = this.boardFacade.blockedCount;
  completionPercentage = this.boardFacade.completionPercentage;
  activeBlockerBanner = this.boardFacade.activeBlockerBanner;

  // Create Task Modal Delegates
  isCreateModalOpen = this.boardFacade.isCreateModalOpen;
  newTaskTitle = this.boardFacade.newTaskTitle;
  newTaskDescription = this.boardFacade.newTaskDescription;
  newTaskPriority = this.boardFacade.newTaskPriority;

  // Blocker Dialog Delegates
  selectedTask = this.boardFacade.selectedTask;
  blockerReasonInput = this.boardFacade.blockerReasonInput;

  // Chat Facade State Delegates
  channels = this.chatFacade.channels;
  activeChannel = this.chatFacade.activeChannel;
  messages = this.chatFacade.messages;
  chatInput = this.chatFacade.chatInput;

  // Meeting state
  isMeetingOpen = signal(false);
  meetingRoom = signal('syncflow-sprint-room');

  ngOnInit(): void {
    this.boardFacade.init();
    this.chatFacade.init();
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

  // Chat Operations
  selectChannel(channel: Channel): void {
    this.chatFacade.selectChannel(channel);
  }

  sendMessage(): void {
    this.chatFacade.sendMessage();
  }

  // Availability Toggle
  toggleAvailability(): void {
    const user = this.currentUser();
    if (user) {
      user.onLeave = !user.onLeave;
    }
  }
}
