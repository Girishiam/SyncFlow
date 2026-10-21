import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { TaskCard, BankUser } from '../models/project.models';
import { ITaskPort, CreateTaskPayload, TaskComment } from '../ports/task.port';

@Injectable({
  providedIn: 'root'
})
export class TaskService implements ITaskPort {
  private readonly apiUrl = 'http://localhost:9090/api/v1/tasks';
  private readonly authApiUrl = 'http://localhost:9090/api/v1/auth';

  constructor(private http: HttpClient) {}

  getTaskDetails(taskId: number): Observable<TaskCard> {
    return this.http.get<TaskCard>(`${this.apiUrl}/${taskId}`);
  }

  createTask(payload: CreateTaskPayload): Observable<TaskCard> {
    return this.http.post<TaskCard>(this.apiUrl, payload);
  }

  moveTask(taskId: number, targetStageId: number): Observable<TaskCard> {
    return this.http.put<TaskCard>(`${this.apiUrl}/${taskId}/move`, { targetStageId });
  }

  updateProgress(taskId: number, progress: number): Observable<TaskCard> {
    return this.http.put<TaskCard>(`${this.apiUrl}/${taskId}/progress`, { progress });
  }

  setBlocker(taskId: number, isBlocked: boolean, blockerReason?: string): Observable<TaskCard> {
    return this.http.put<TaskCard>(`${this.apiUrl}/${taskId}/blocker`, { isBlocked, blockerReason });
  }

  getTaskComments(taskId: number): Observable<TaskComment[]> {
    return this.http.get<TaskComment[]>(`${this.apiUrl}/${taskId}/comments`);
  }

  addComment(taskId: number, content: string, attachmentUrl?: string): Observable<TaskComment> {
    return this.http.post<TaskComment>(`${this.apiUrl}/${taskId}/comments`, { content, attachmentUrl });
  }

  assignTask(taskId: number, assigneeId: string | null): Observable<TaskCard> {
    return this.http.put<TaskCard>(`${this.apiUrl}/${taskId}/assignee`, { assigneeId });
  }

  updateDeadline(taskId: number, deadline: string | null): Observable<TaskCard> {
    return this.http.put<TaskCard>(`${this.apiUrl}/${taskId}/deadline`, { deadline });
  }

  getBankUsers(): Observable<BankUser[]> {
    return this.http.get<BankUser[]>(`${this.authApiUrl}/users`);
  }
}
