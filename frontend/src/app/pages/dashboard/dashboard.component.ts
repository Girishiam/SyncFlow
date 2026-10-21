import { Component, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CdkDragDrop, DragDropModule, moveItemInArray, transferArrayItem } from '@angular/cdk/drag-drop';
import { AuthService } from '../../core/services/auth.service';

interface TaskCard {
  id: number;
  title: string;
  project: string;
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  progress: number;
  assignee: string;
  avatar: string;
  isBlocked: boolean;
  blockerReason?: string;
}

interface KanbanColumn {
  id: string;
  title: string;
  tasks: TaskCard[];
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, DragDropModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent {
  currentUser = computed(() => this.authService.currentUserSignal());

  // Active navigation view
  activeNav = signal<'overview' | 'board' | 'discussions' | 'analytics' | 'rbac'>('board');
  activeChannel = signal<string>('general');

  // Meeting modal state
  isMeetingOpen = signal(false);
  meetingRoom = signal('syncflow-sprint-room');

  // Blocker modal state
  selectedTask = signal<TaskCard | null>(null);
  blockerReasonInput = signal('');

  // Discussions channels
  channels = [
    { id: 'general', name: 'general', unread: 0 },
    { id: 'core-banking-api', name: 'core-banking-api', unread: 2 },
    { id: 'design-tokens', name: 'design-tokens', unread: 0 },
    { id: 'database-migration', name: 'database-migration', unread: 1 }
  ];

  // Channel messages
  messages = signal([
    { sender: 'Tariq Manager', text: 'Sprint goals are locked in for the payment gateway migration.', time: '09:15 AM', seenBy: ['Arman Lead', 'Farhan Dev'] },
    { sender: 'Arman Lead', text: 'Pipeline stages are configured. API development is underway.', time: '09:22 AM', seenBy: ['Farhan Dev'] },
    { sender: 'Farhan Dev', text: 'MSSQL connection pool and Virtual Threads setup complete.', time: '09:30 AM', seenBy: ['Arman Lead'] }
  ]);
  chatInput = signal('');

  // Kanban Columns
  columns = signal<KanbanColumn[]>([
    {
      id: 'backlog',
      title: 'Backlog',
      tasks: [
        { id: 101, title: 'Implement Biometric 2FA Hook', project: 'Security Suite', priority: 'HIGH', progress: 0, assignee: 'Farhan Dev', avatar: 'FD', isBlocked: false },
        { id: 102, title: 'Audit Trail Export to CSV', project: 'Compliance', priority: 'LOW', progress: 0, assignee: 'Sarah SQA', avatar: 'SS', isBlocked: false }
      ]
    },
    {
      id: 'db_design',
      title: 'DB Design',
      tasks: [
        { id: 201, title: 'MSSQL Partitioning for Ledger', project: 'Core Ledger', priority: 'CRITICAL', progress: 30, assignee: 'Farhan Dev', avatar: 'FD', isBlocked: false }
      ]
    },
    {
      id: 'api',
      title: 'API Dev',
      tasks: [
        { id: 301, title: 'WebSocket STOMP Broker & Event Hook', project: 'Realtime Ops', priority: 'HIGH', progress: 65, assignee: 'Arman Lead', avatar: 'AL', isBlocked: true, blockerReason: 'Waiting for SSL certificate handshake setup' }
      ]
    },
    {
      id: 'ui',
      title: 'UI & Layout',
      tasks: [
        { id: 401, title: 'Emerald Reserve Design Tokens', project: 'SyncFlow UI', priority: 'CRITICAL', progress: 90, assignee: 'Farhan Dev', avatar: 'FD', isBlocked: false }
      ]
    },
    {
      id: 'review',
      title: 'Review / QA',
      tasks: [
        { id: 501, title: 'JWT Refresh Filter Test Suite', project: 'Security Suite', priority: 'MEDIUM', progress: 95, assignee: 'Sarah SQA', avatar: 'SS', isBlocked: false }
      ]
    },
    {
      id: 'done',
      title: 'Done',
      tasks: [
        { id: 601, title: 'Spring Boot 3.3.5 & Java 21 Loom Scaffolding', project: 'SyncFlow Core', priority: 'HIGH', progress: 100, assignee: 'Farhan Dev', avatar: 'FD', isBlocked: false }
      ]
    }
  ]);

  // Executive KPI stats
  totalTasksCount = computed(() => {
    return this.columns().reduce((acc, col) => acc + col.tasks.length, 0);
  });

  blockedCount = computed(() => {
    return this.columns().reduce((acc, col) => acc + col.tasks.filter(t => t.isBlocked).length, 0);
  });

  completedCount = computed(() => {
    const doneCol = this.columns().find(c => c.id === 'done');
    return doneCol ? doneCol.tasks.length : 0;
  });

  completionPercentage = computed(() => {
    const total = this.totalTasksCount();
    if (total === 0) return 0;
    return Math.round((this.completedCount() / total) * 100);
  });

  constructor(public authService: AuthService) {}

  // Drag & drop
  onDrop(event: CdkDragDrop<TaskCard[]>) {
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
      if (event.container.id === 'done') {
        movedTask.progress = 100;
        movedTask.isBlocked = false;
      }
    }
  }

  // Blocker operations
  openBlocker(task: TaskCard) {
    this.selectedTask.set(task);
    this.blockerReasonInput.set(task.blockerReason || '');
  }

  saveBlocker() {
    const task = this.selectedTask();
    if (!task) return;

    if (this.blockerReasonInput().trim()) {
      task.isBlocked = true;
      task.blockerReason = this.blockerReasonInput().trim();
    } else {
      task.isBlocked = false;
      task.blockerReason = undefined;
    }
    this.selectedTask.set(null);
    this.blockerReasonInput.set('');
  }

  closeBlocker() {
    this.selectedTask.set(null);
    this.blockerReasonInput.set('');
  }

  // Toggle user availability
  toggleAvailability() {
    const user = this.currentUser();
    if (user) {
      user.onLeave = !user.onLeave;
    }
  }

  // Send message
  sendMessage() {
    const text = this.chatInput().trim();
    if (!text) return;

    const user = this.currentUser();
    this.messages.update(msgs => [
      ...msgs,
      {
        sender: user?.fullName || 'User',
        text: text,
        time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        seenBy: ['Just now']
      }
    ]);
    this.chatInput.set('');
  }
}
