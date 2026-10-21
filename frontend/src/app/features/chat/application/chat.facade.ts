import { Injectable, signal, OnDestroy } from '@angular/core';
import { Subscription } from 'rxjs';
import { ChatService } from '../../../core/services/chat.service';
import { WebSocketService } from '../../../core/services/websocket.service';
import { Channel, ChatMessage, BankUser } from '../../../core/models/project.models';

@Injectable({
  providedIn: 'root'
})
export class ChatFacade implements OnDestroy {
  public channels = signal<Channel[]>([]);
  public activeChannel = signal<Channel | null>(null);
  public messages = signal<ChatMessage[]>([]);
  public chatInput = signal('');

  // Create Channel Modal State
  public isCreateModalOpen = signal(false);
  public newChannelName = signal('');
  public newChannelDescription = signal('');
  public newChannelType = signal<'TEXT' | 'VOICE' | 'ANNOUNCEMENT'>('TEXT');
  public newChannelIcon = signal('hash');
  public newChannelIsPrivate = signal(false);
  public newChannelProjectId = signal<number | null>(null);
  public isCreatingChannel = signal(false);
  public createChannelError = signal<string | null>(null);

  // Edit Channel Modal State
  public isEditModalOpen = signal(false);
  public channelToEdit = signal<Channel | null>(null);
  public editChannelName = signal('');
  public editChannelDescription = signal('');
  public editChannelType = signal('TEXT');
  public editChannelIcon = signal('hash');
  public editChannelIsPrivate = signal(false);
  public isUpdatingChannel = signal(false);
  public editChannelError = signal<string | null>(null);

  // Delete Channel Modal State
  public isDeleteModalOpen = signal(false);
  public channelToDelete = signal<Channel | null>(null);
  public deleteConfirmText = signal('');
  public deleteChannelError = signal<string | null>(null);

  // Channel Members State
  public isMembersModalOpen = signal(false);
  public channelMembers = signal<BankUser[]>([]);
  public isLoadingMembers = signal(false);
  public selectedMemberToAdd = signal<string>('');

  // Available Icon Options for Channels
  public availableIcons = [
    { id: 'hash', label: 'General / Text', icon: '#' },
    { id: 'chat-dots', label: 'Discussions', icon: '💬' },
    { id: 'shield-lock', label: 'Security & Auth', icon: '🛡️' },
    { id: 'bank', label: 'Treasury & Core', icon: '🏦' },
    { id: 'code-slash', label: 'Engineering', icon: '💻' },
    { id: 'megaphone', label: 'Announcements', icon: '📢' },
    { id: 'cpu', label: 'Infrastructure', icon: '⚡' },
    { id: 'terminal', label: 'DevOps & CI/CD', icon: '🚀' }
  ];

  private subscriptions: Subscription[] = [];
  private currentChannelSub: Subscription | null = null;

  constructor(
    private chatService: ChatService,
    private wsService: WebSocketService
  ) {}

  ngOnDestroy(): void {
    this.subscriptions.forEach(s => s.unsubscribe());
    if (this.currentChannelSub) {
      this.currentChannelSub.unsubscribe();
    }
  }

  public init(): void {
    this.loadChannels();
  }

  public loadChannels(): void {
    this.chatService.getChannels().subscribe({
      next: (chs) => {
        this.channels.set(chs);
        if (chs.length > 0 && !this.activeChannel()) {
          this.selectChannel(chs[0]);
        } else if (this.activeChannel()) {
          const match = chs.find(c => c.id === this.activeChannel()!.id);
          if (match) {
            this.activeChannel.set(match);
          }
        }
      },
      error: (err) => console.error('ChatFacade: Failed to load channels', err)
    });
  }

  public selectChannel(channel: Channel): void {
    this.activeChannel.set(channel);
    this.chatService.getMessages(channel.id).subscribe({
      next: (msgs) => {
        this.messages.set(msgs);
      },
      error: (err) => console.error('ChatFacade: Failed to load messages for channel ' + channel.id, err)
    });

    if (this.currentChannelSub) {
      this.currentChannelSub.unsubscribe();
    }

    this.currentChannelSub = this.wsService.subscribeToChannel(channel.id).subscribe({
      next: (newMsg: ChatMessage) => {
        if (newMsg && newMsg.channelId === channel.id) {
          this.messages.update(msgs => {
            if (newMsg.id && msgs.some(m => m.id === newMsg.id)) return msgs;
            return [...msgs, newMsg];
          });
        }
      }
    });

    // Also load member list for header count
    this.loadChannelMembers(channel.id);
  }

  public sendMessage(): void {
    const text = this.chatInput().trim();
    const channel = this.activeChannel();
    if (!text || !channel) return;

    this.chatService.sendMessage(channel.id, text).subscribe({
      next: () => {
        this.chatInput.set('');
      },
      error: (err) => console.error('ChatFacade: Failed to send message', err)
    });
  }

  // --- Channel Creation Operations ---
  public openCreateChannelModal(): void {
    this.newChannelName.set('');
    this.newChannelDescription.set('');
    this.newChannelType.set('TEXT');
    this.newChannelIcon.set('hash');
    this.newChannelIsPrivate.set(false);
    this.newChannelProjectId.set(null);
    this.createChannelError.set(null);
    this.isCreateModalOpen.set(true);
  }

  public closeCreateChannelModal(): void {
    this.isCreateModalOpen.set(false);
  }

  public submitCreateChannel(): void {
    const raw = this.newChannelName().trim();
    if (!raw) {
      this.createChannelError.set('Channel name is required.');
      return;
    }

    this.isCreatingChannel.set(true);
    this.createChannelError.set(null);

    this.chatService.createChannel({
      name: raw,
      description: this.newChannelDescription().trim() || undefined,
      type: this.newChannelType(),
      icon: this.newChannelIcon(),
      isPrivate: this.newChannelIsPrivate(),
      projectId: this.newChannelProjectId() || undefined
    }).subscribe({
      next: (created) => {
        this.isCreatingChannel.set(false);
        this.channels.update(chs => [...chs, created]);
        this.selectChannel(created);
        this.closeCreateChannelModal();
      },
      error: (err) => {
        this.isCreatingChannel.set(false);
        console.error('ChatFacade: Failed to create channel', err);
        this.createChannelError.set(err.error?.message || 'Failed to create channel.');
      }
    });
  }

  // --- Channel Editing Operations ---
  public openEditChannelModal(channel?: Channel): void {
    const target = channel || this.activeChannel();
    if (!target) return;

    this.channelToEdit.set(target);
    this.editChannelName.set(target.name);
    this.editChannelDescription.set(target.description || '');
    this.editChannelType.set(target.type || 'TEXT');
    this.editChannelIcon.set(target.icon || 'hash');
    this.editChannelIsPrivate.set(!!target.isPrivate);
    this.editChannelError.set(null);
    this.isEditModalOpen.set(true);
  }

  public closeEditChannelModal(): void {
    this.isEditModalOpen.set(false);
    this.channelToEdit.set(null);
  }

  public submitEditChannel(): void {
    const target = this.channelToEdit();
    if (!target) return;

    const raw = this.editChannelName().trim();
    if (!raw) {
      this.editChannelError.set('Channel name is required.');
      return;
    }

    this.isUpdatingChannel.set(true);
    this.editChannelError.set(null);

    this.chatService.updateChannel(target.id, {
      name: raw,
      description: this.editChannelDescription().trim() || undefined,
      type: this.editChannelType(),
      icon: this.editChannelIcon(),
      isPrivate: this.editChannelIsPrivate()
    }).subscribe({
      next: (updated) => {
        this.isUpdatingChannel.set(false);
        this.channels.update(chs => chs.map(c => c.id === updated.id ? updated : c));
        if (this.activeChannel()?.id === updated.id) {
          this.activeChannel.set(updated);
        }
        this.closeEditChannelModal();
      },
      error: (err) => {
        this.isUpdatingChannel.set(false);
        console.error('ChatFacade: Failed to update channel', err);
        this.editChannelError.set(err.error?.message || 'Failed to update channel.');
      }
    });
  }

  // --- Channel Deletion Operations ---
  public openDeleteChannelModal(channel?: Channel): void {
    const target = channel || this.activeChannel();
    if (!target) return;

    this.channelToDelete.set(target);
    this.deleteConfirmText.set('');
    this.deleteChannelError.set(null);
    this.isDeleteModalOpen.set(true);
  }

  public closeDeleteChannelModal(): void {
    this.isDeleteModalOpen.set(false);
    this.channelToDelete.set(null);
  }

  public confirmDeleteChannel(): void {
    const target = this.channelToDelete();
    if (!target) return;

    if (target.name.toLowerCase() === 'general') {
      this.deleteChannelError.set('Default #general channel cannot be deleted.');
      return;
    }

    if (this.deleteConfirmText().trim() !== target.name) {
      this.deleteChannelError.set(`Type "${target.name}" to confirm.`);
      return;
    }

    this.chatService.deleteChannel(target.id).subscribe({
      next: () => {
        this.channels.update(chs => chs.filter(c => c.id !== target.id));
        if (this.activeChannel()?.id === target.id) {
          const remaining = this.channels();
          if (remaining.length > 0) {
            this.selectChannel(remaining[0]);
          } else {
            this.activeChannel.set(null);
          }
        }
        this.closeDeleteChannelModal();
      },
      error: (err) => {
        console.error('ChatFacade: Failed to delete channel', err);
        this.deleteChannelError.set(err.error?.message || 'Failed to delete channel.');
      }
    });
  }

  // --- Channel Members Operations ---
  public openMembersModal(): void {
    const channel = this.activeChannel();
    if (!channel) return;
    this.isMembersModalOpen.set(true);
    this.selectedMemberToAdd.set('');
    this.loadChannelMembers(channel.id);
  }

  public closeMembersModal(): void {
    this.isMembersModalOpen.set(false);
  }

  public loadChannelMembers(channelId: number): void {
    this.isLoadingMembers.set(true);
    this.chatService.getChannelMembers(channelId).subscribe({
      next: (members) => {
        this.channelMembers.set(members);
        this.isLoadingMembers.set(false);
        // update channel member count in activeChannel
        const current = this.activeChannel();
        if (current && current.id === channelId) {
          current.memberCount = members.length;
          this.activeChannel.set({ ...current });
        }
      },
      error: (err) => {
        this.isLoadingMembers.set(false);
        console.error('ChatFacade: Failed to load channel members', err);
      }
    });
  }

  public addMemberToActiveChannel(userId: string): void {
    const channel = this.activeChannel();
    if (!channel || !userId) return;

    this.chatService.addMemberToChannel(channel.id, userId).subscribe({
      next: () => {
        this.loadChannelMembers(channel.id);
        this.selectedMemberToAdd.set('');
      },
      error: (err) => console.error('ChatFacade: Failed to add member', err)
    });
  }

  public removeMemberFromActiveChannel(userId: string): void {
    const channel = this.activeChannel();
    if (!channel || !userId) return;

    this.chatService.removeMemberFromChannel(channel.id, userId).subscribe({
      next: () => {
        this.loadChannelMembers(channel.id);
      },
      error: (err) => console.error('ChatFacade: Failed to remove member', err)
    });
  }
}

