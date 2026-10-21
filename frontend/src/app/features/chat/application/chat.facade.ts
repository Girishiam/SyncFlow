import { Injectable, signal, OnDestroy } from '@angular/core';
import { Subscription } from 'rxjs';
import { ChatService } from '../../../core/services/chat.service';
import { WebSocketService } from '../../../core/services/websocket.service';
import { Channel, ChatMessage } from '../../../core/models/project.models';

@Injectable({
  providedIn: 'root'
})
export class ChatFacade implements OnDestroy {
  public channels = signal<Channel[]>([]);
  public activeChannel = signal<Channel | null>(null);
  public messages = signal<ChatMessage[]>([]);
  public chatInput = signal('');

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
}
