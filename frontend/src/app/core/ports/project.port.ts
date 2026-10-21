import { Observable } from 'rxjs';
import { Project, ProjectBoard } from '../models/project.models';

export interface IProjectPort {
  getAllProjects(): Observable<Project[]>;
  createProject(name: string, description?: string): Observable<Project>;
  getProjectBoard(projectId: number): Observable<ProjectBoard>;
  createStage(projectId: number, name: string, isDoneStage?: boolean): Observable<any>;
  reorderStages(projectId: number, stageIds: number[]): Observable<void>;
}
