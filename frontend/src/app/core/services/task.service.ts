import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { TaskCard } from '../models/project.models';

@Injectable({
  providedIn: 'root'
})
export class TaskService {
  private readonly apiUrl = 'http://localhost:9090/api/v1/tasks';

  constructor(private http: HttpClient) {}

  createTask(payload: {
    projectId: number;
    stageId: number;
    title: string;
    description?: string;
    priority?: string;
    assigneeId?: string;
  }): Observable<TaskCard> {
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
}
