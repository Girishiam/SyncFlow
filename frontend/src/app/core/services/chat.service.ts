import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Channel, ChatMessage } from '../models/project.models';
import { IChatPort } from '../ports/chat.port';

@Injectable({
  providedIn: 'root'
})
export class ChatService implements IChatPort {
  private readonly apiUrl = 'http://localhost:9090/api/v1/channels';

  constructor(private http: HttpClient) {}

  getChannels(): Observable<Channel[]> {
    return this.http.get<Channel[]>(this.apiUrl);
  }

  getMessages(channelId: number): Observable<ChatMessage[]> {
    return this.http.get<ChatMessage[]>(`${this.apiUrl}/${channelId}/messages`);
  }

  sendMessage(channelId: number, content: string): Observable<ChatMessage> {
    return this.http.post<ChatMessage>(`${this.apiUrl}/${channelId}/messages`, { content });
  }
}
