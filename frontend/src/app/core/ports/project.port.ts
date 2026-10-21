import { Observable } from 'rxjs';
import { Project, ProjectBoard } from '../models/project.models';

export interface IProjectPort {
  getAllProjects(): Observable<Project[]>;
  getProjectBoard(projectId: number): Observable<ProjectBoard>;
}
