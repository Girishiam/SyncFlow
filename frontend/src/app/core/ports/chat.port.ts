import { Observable } from 'rxjs';
import { Channel, ChatMessage } from '../models/project.models';

export interface IChatPort {
  getChannels(): Observable<Channel[]>;
  getMessages(channelId: number): Observable<ChatMessage[]>;
  sendMessage(channelId: number, content: string): Observable<ChatMessage>;
}
