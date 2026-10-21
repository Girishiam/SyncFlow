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

  // Navigation tab
  activeTab = signal<'kanban' | 'channels' | 'analytics' | 'rbac'>('kanban');
  activeChannel = signal<string>('#general');

  // Meeting modal state
  isMeetingActive = signal(false);
  meetingRoomId = signal('syncflow-executive-room');

  // Blocker modal state
  selectedTaskForBlocker = signal<TaskCard | null>(null);
  blockerInputReason = signal('');

  // Discord-style Channels list
  channels = [
    { name: '#general', type: 'global', unread: 0 },
    { name: '#core-banking-api', type: 'project', unread: 2 },
    { name: '#ui-design-tokens', type: 'project', unread: 0 },
    { name: '#database-migration', type: 'project', unread: 1 }
  ];

  // Chat messages
  messages = signal([
    { sender: 'Manager', text: 'Good morning team! Sprint goals are locked in for the new payment gateway.', time: '09:15 AM', seenBy: ['Team Lead', 'Senior Dev'] },
    { sender: 'Team Lead', text: 'API schemas are drafted. Reviewing stage pipelines now.', time: '09:22 AM', seenBy: ['Senior Dev'] },
    { sender: 'Senior Dev', text: 'Started task on MSSQL connection pooling with Virtual Threads.', time: '09:30 AM', seenBy: ['Team Lead'] }
  ]);
  newChatMessage = signal('');

  // Kanban Columns with initial mock state
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
        { id: 201, title: 'MSSQL Partitioning for Ledger', project: 'Core Ledger', priority: 'CRITICAL', progress: 30, assignee: 'Senior Dev', avatar: 'SD', isBlocked: false }
      ]
    },
    {
      id: 'api',
      title: 'API Dev',
      tasks: [
        { id: 301, title: 'WebSocket STOMP Broker & Seen Hook', project: 'Realtime Ops', priority: 'HIGH', progress: 65, assignee: 'Farhan Dev', avatar: 'FD', isBlocked: true, blockerReason: 'Waiting for SSL certificate handshake setup' }
      ]
    },
    {
      id: 'ui',
      title: 'UI & Layout',
      tasks: [
        { id: 401, title: 'Emerald Reserve Design Token System', project: 'SyncFlow UI', priority: 'CRITICAL', progress: 90, assignee: 'Team Lead', avatar: 'TL', isBlocked: false }
      ]
    },
    {
      id: 'review',
      title: 'Review / QA',
      tasks: [
        { id: 501, title: 'JWT Secret Refresh Filter Test', project: 'Security Suite', priority: 'MEDIUM', progress: 95, assignee: 'Sarah SQA', avatar: 'SS', isBlocked: false }
      ]
    },
    {
      id: 'done',
      title: 'Done',
      tasks: [
        { id: 601, title: 'Spring Boot 3.3.5 Scaffolding with Loom', project: 'SyncFlow Core', priority: 'HIGH', progress: 100, assignee: 'Senior Dev', avatar: 'SD', isBlocked: false }
      ]
    }
  ]);

  // Executive Stats
  totalTasks = computed(() => {
    return this.columns().reduce((acc, col) => acc + col.tasks.length, 0);
  });

  blockedCount = computed(() => {
    return this.columns().reduce((acc, col) => acc + col.tasks.filter(t => t.isBlocked).length, 0);
  });

  completedCount = computed(() => {
    const doneCol = this.columns().find(c => c.id === 'done');
    return doneCol ? doneCol.tasks.length : 0;
  });

  completionRate = computed(() => {
    const total = this.totalTasks();
    if (total === 0) return 0;
    return Math.round((this.completedCount() / total) * 100);
  });

  constructor(public authService: AuthService) {}

  // Drag and drop handler
  drop(event: CdkDragDrop<TaskCard[]>) {
    if (event.previousContainer === event.container) {
      moveItemInArray(event.container.data, event.previousIndex, event.currentIndex);
    } else {
      transferArrayItem(
        event.previousContainer.data,
        event.container.data,
        event.previousIndex,
        event.currentIndex
      );
      // Automatically adjust progress on move to done
      const task = event.container.data[event.currentIndex];
      if (event.container.id === 'done') {
        task.progress = 100;
        task.isBlocked = false;
      }
    }
  }

  // Blocker Alert Actions
  openBlockerModal(task: TaskCard) {
    this.selectedTaskForBlocker.set(task);
    this.blockerInputReason.set(task.blockerReason || '');
  }

  confirmBlocker() {
    const task = this.selectedTaskForBlocker();
    if (!task) return;

    if (this.blockerInputReason().trim()) {
      task.isBlocked = true;
      task.blockerReason = this.blockerInputReason().trim();
    } else {
      task.isBlocked = false;
      task.blockerReason = undefined;
    }

    this.selectedTaskForBlocker.set(null);
    this.blockerInputReason.set('');
  }

  cancelBlocker() {
    this.selectedTaskForBlocker.set(null);
    this.blockerInputReason.set('');
  }

  // Toggle Leave Status
  toggleLeave() {
    const user = this.currentUser();
    if (user) {
      user.onLeave = !user.onLeave;
    }
  }

  // Meeting Trigger
  startMeeting() {
    this.isMeetingActive.set(true);
  }

  closeMeeting() {
    this.isMeetingActive.set(false);
  }

  // Chat message send
  sendChatMessage() {
    const text = this.newChatMessage().trim();
    if (!text) return;

    const user = this.currentUser();
    const sender = user?.fullName || 'User';

    this.messages.update(msgs => [
      ...msgs,
      {
        sender: sender,
        text: text,
        time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        seenBy: ['Just now']
      }
    ]);
    this.newChatMessage.set('');
  }
}
