import { Observable } from 'rxjs';
import { Channel, ChatMessage, BankUser } from '../models/project.models';

export interface CreateChannelPayload {
  name: string;
  description?: string;
  type?: string;
  icon?: string;
  isPrivate?: boolean;
  projectId?: number;
  memberIds?: string[];
}

export interface UpdateChannelPayload {
  name: string;
  description?: string;
  type?: string;
  icon?: string;
  isPrivate?: boolean;
}

export interface IChatPort {
  getChannels(): Observable<Channel[]>;
  createChannel(payload: CreateChannelPayload): Observable<Channel>;
  updateChannel(channelId: number, payload: UpdateChannelPayload): Observable<Channel>;
  deleteChannel(channelId: number): Observable<void>;
  getChannelMembers(channelId: number): Observable<BankUser[]>;
  addMemberToChannel(channelId: number, userId: string): Observable<void>;
  removeMemberFromChannel(channelId: number, userId: string): Observable<void>;
  getMessages(channelId: number): Observable<ChatMessage[]>;
  sendMessage(channelId: number, content: string): Observable<ChatMessage>;
}

