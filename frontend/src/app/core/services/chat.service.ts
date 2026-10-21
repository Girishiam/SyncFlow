import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Channel, ChatMessage, BankUser } from '../models/project.models';
import { IChatPort, CreateChannelPayload, UpdateChannelPayload } from '../ports/chat.port';

@Injectable({
  providedIn: 'root'
})
export class ChatService implements IChatPort {
  private readonly apiUrl = 'http://localhost:9090/api/v1/channels';

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

  getMessages(channelId: number): Observable<ChatMessage[]> {
    return this.http.get<ChatMessage[]>(`${this.apiUrl}/${channelId}/messages`);
  }

  sendMessage(channelId: number, content: string): Observable<ChatMessage> {
    return this.http.post<ChatMessage>(`${this.apiUrl}/${channelId}/messages`, { content });
  }
}

