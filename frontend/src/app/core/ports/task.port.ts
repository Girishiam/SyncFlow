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

export interface ITaskPort {
  createTask(payload: CreateTaskPayload): Observable<TaskCard>;
  moveTask(taskId: number, targetStageId: number): Observable<TaskCard>;
  updateProgress(taskId: number, progress: number): Observable<TaskCard>;
  setBlocker(taskId: number, isBlocked: boolean, blockerReason?: string): Observable<TaskCard>;
}
