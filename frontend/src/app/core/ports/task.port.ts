import { Observable } from 'rxjs';
import { TaskCard } from '../models/project.models';

export interface CreateTaskPayload {
  projectId: number;
  stageId: number;
  title: string;
  description?: string;
  priority?: string;
  assigneeId?: string;
}

export interface TaskComment {
  id: number;
  taskId: number;
  authorId: string;
  authorName: string;
  authorRole: string;
  authorAvatar: string;
  content: string;
  time: string;
  attachmentUrl?: string;
}

export interface ITaskPort {
  getTaskDetails(taskId: number): Observable<TaskCard>;
  createTask(payload: CreateTaskPayload): Observable<TaskCard>;
  moveTask(taskId: number, targetStageId: number): Observable<TaskCard>;
  updateProgress(taskId: number, progress: number): Observable<TaskCard>;
  setBlocker(taskId: number, isBlocked: boolean, blockerReason?: string): Observable<TaskCard>;
  getTaskComments(taskId: number): Observable<TaskComment[]>;
  addComment(taskId: number, content: string, attachmentUrl?: string): Observable<TaskComment>;
}
