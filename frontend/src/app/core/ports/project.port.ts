import { Observable } from 'rxjs';
import { NewProjectRequest, Project, ProjectBoard } from '../models/project.models';

export interface IProjectPort {
  getAllProjects(): Observable<Project[]>;
  createProject(request: NewProjectRequest): Observable<Project>;
  getProjectBoard(projectId: number): Observable<ProjectBoard>;
  createStage(projectId: number, name: string, isDoneStage?: boolean): Observable<any>;
  reorderStages(projectId: number, stageIds: number[]): Observable<void>;
}
