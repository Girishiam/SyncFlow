import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Channel, ChatMessage, BankUser } from '../models/project.models';
import { IChatPort, CreateChannelPayload, UpdateChannelPayload } from '../ports/chat.port';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ChatService implements IChatPort {
  private readonly apiUrl = `${environment.apiBaseUrl}/v1/channels`;

  constructor(private http: HttpClient) {}

  getChannels(): Observable<Channel[]> {
    return this.http.get<Channel[]>(this.apiUrl);
  }

  createChannel(payload: CreateChannelPayload): Observable<Channel> {
    return this.http.post<Channel>(this.apiUrl, payload);
  }

  updateChannel(channelId: number, payload: UpdateChannelPayload): Observable<Channel> {
    return this.http.put<Channel>(`${this.apiUrl}/${channelId}`, payload);
  }

  deleteChannel(channelId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${channelId}`);
  }

  getChannelMembers(channelId: number): Observable<BankUser[]> {
    return this.http.get<BankUser[]>(`${this.apiUrl}/${channelId}/members`);
  }

  addMemberToChannel(channelId: number, userId: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${channelId}/members/${userId}`, {});
  }

  removeMemberFromChannel(channelId: number, userId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${channelId}/members/${userId}`);
  }

  /** Newest page of messages (oldest first); pass `before` (a message id) to load earlier history. */
  getMessages(channelId: number, before?: number, limit = 50): Observable<ChatMessage[]> {
    const params: Record<string, string> = { limit: String(limit) };
    if (before) params['before'] = String(before);
    return this.http.get<ChatMessage[]>(`${this.apiUrl}/${channelId}/messages`, { params });
  }

  sendMessage(channelId: number, content: string): Observable<ChatMessage> {
    return this.http.post<ChatMessage>(`${this.apiUrl}/${channelId}/messages`, { content });
  }
}

