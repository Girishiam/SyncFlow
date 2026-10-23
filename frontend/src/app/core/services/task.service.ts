import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { TaskCard, BankUser, TaskDependency } from '../models/project.models';
import { ITaskPort, CreateTaskPayload, TaskComment } from '../ports/task.port';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class TaskService implements ITaskPort {
  private readonly apiUrl = `${environment.apiBaseUrl}/v1/tasks`;
  private readonly authApiUrl = `${environment.apiBaseUrl}/v1/auth`;

  constructor(private http: HttpClient) {}

  getTaskDetails(taskId: number): Observable<TaskCard> {
    return this.http.get<TaskCard>(`${this.apiUrl}/${taskId}`);
  }

  createTask(payload: CreateTaskPayload): Observable<TaskCard> {
    return this.http.post<TaskCard>(this.apiUrl, payload);
  }

  moveTask(taskId: number, targetStageId: number, overrideGate?: boolean, overrideReason?: string, elevationToken?: string): Observable<TaskCard> {
    return this.http.put<TaskCard>(`${this.apiUrl}/${taskId}/move`, { targetStageId, overrideGate, overrideReason, elevationToken });
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

  // Subtasks & Deliverable QA Checklist
  getSubtasks(taskId: number): Observable<import('../models/project.models').SubtaskItem[]> {
    return this.http.get<import('../models/project.models').SubtaskItem[]>(`${this.apiUrl}/${taskId}/subtasks`);
  }

  addSubtask(taskId: number, title: string): Observable<import('../models/project.models').SubtaskItem> {
    return this.http.post<import('../models/project.models').SubtaskItem>(`${this.apiUrl}/${taskId}/subtasks`, { title });
  }

  toggleSubtask(taskId: number, subtaskId: number): Observable<import('../models/project.models').SubtaskItem> {
    return this.http.patch<import('../models/project.models').SubtaskItem>(`${this.apiUrl}/${taskId}/subtasks/${subtaskId}/toggle`, {});
  }

  deleteSubtask(taskId: number, subtaskId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${taskId}/subtasks/${subtaskId}`);
  }

  // Task Attachments
  getAttachments(taskId: number): Observable<import('../models/project.models').TaskAttachmentItem[]> {
    return this.http.get<import('../models/project.models').TaskAttachmentItem[]>(`${this.apiUrl}/${taskId}/attachments`);
  }

  /** Links an external http(s) deliverable. */
  addLinkAttachment(taskId: number, fileName: string, fileUrl: string): Observable<import('../models/project.models').TaskAttachmentItem> {
    return this.http.post<import('../models/project.models').TaskAttachmentItem>(`${this.apiUrl}/${taskId}/attachments`, { fileName, fileUrl });
  }

  /** Uploads a file (stored server-side, max 25 MB). */
  uploadAttachment(taskId: number, file: File): Observable<import('../models/project.models').TaskAttachmentItem> {
    const form = new FormData();
    form.append('file', file, file.name);
    return this.http.post<import('../models/project.models').TaskAttachmentItem>(`${this.apiUrl}/${taskId}/attachments`, form);
  }

  /** Downloads a stored file with the caller's credentials. */
  downloadAttachment(taskId: number, attachmentId: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${taskId}/attachments/${attachmentId}/download`, { responseType: 'blob' });
  }

  deleteAttachment(taskId: number, attachmentId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${taskId}/attachments/${attachmentId}`);
  }

  // --- Task Archiving & Board Lifecycle ---
  archiveTask(taskId: number): Observable<TaskCard> {
    return this.http.put<TaskCard>(`${this.apiUrl}/${taskId}/archive`, {});
  }

  restoreTask(taskId: number): Observable<TaskCard> {
    return this.http.put<TaskCard>(`${this.apiUrl}/${taskId}/restore`, {});
  }

  getArchivedTasks(projectId: number): Observable<TaskCard[]> {
    return this.http.get<TaskCard[]>(`${this.apiUrl}/archived`, { params: { projectId: String(projectId) } });
  }

  // --- Task Dependencies ---
  getDependencies(taskId: number): Observable<TaskDependency[]> {
    return this.http.get<TaskDependency[]>(`${this.apiUrl}/${taskId}/dependencies`);
  }

  addDependency(taskId: number, targetTaskId: number, dependencyType: string): Observable<TaskDependency> {
    return this.http.post<TaskDependency>(`${this.apiUrl}/${taskId}/dependencies`, {
      targetTaskId: String(targetTaskId),
      dependencyType
    });
  }

  deleteDependency(dependencyId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/dependencies/${dependencyId}`);
  }

  // --- Story Points & Sprint Assignment ---
  updateStoryPoints(taskId: number, storyPoints: number): Observable<TaskCard> {
    return this.http.put<TaskCard>(`${this.apiUrl}/${taskId}/story-points`, { storyPoints });
  }

  assignSprint(taskId: number, sprintId: number | null): Observable<TaskCard> {
    return this.http.put<TaskCard>(`${this.apiUrl}/${taskId}/sprint`, { sprintId });
  }

  // --- Enterprise Transition Policy & Quality Gate Check ---
  validateTransitionGate(taskId: number, targetStageId: number): Observable<import('../models/project.models').WorkflowGateValidationResult> {
    return this.http.get<import('../models/project.models').WorkflowGateValidationResult>(`${this.apiUrl}/${taskId}/validate-gate`, {
      params: { targetStageId: String(targetStageId) }
    });
  }

  // --- Git Repository & Pull Request Integration ---
  linkGit(taskId: number, payload: { gitBranch?: string; gitCommitHash?: string; gitPrUrl?: string }): Observable<TaskCard> {
    return this.http.put<TaskCard>(`${this.apiUrl}/${taskId}/git`, payload);
  }

  // --- Task Work Logs & Time Tracking ---
  logTime(taskId: number, payload: { hoursSpent: number; description?: string; loggedDate?: string; estimatedHours?: number }): Observable<import('../models/project.models').TaskTimeLog> {
    return this.http.post<import('../models/project.models').TaskTimeLog>(`${this.apiUrl}/${taskId}/time-logs`, payload);
  }

  getTaskTimeLogs(taskId: number): Observable<import('../models/project.models').TaskTimeLog[]> {
    return this.http.get<import('../models/project.models').TaskTimeLog[]>(`${this.apiUrl}/${taskId}/time-logs`);
  }

  deleteTimeLog(logId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/time-logs/${logId}`);
  }
}
