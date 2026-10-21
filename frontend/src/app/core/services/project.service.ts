import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Project, ProjectBoard } from '../models/project.models';
import { IProjectPort } from '../ports/project.port';

@Injectable({
  providedIn: 'root'
})
export class ProjectService implements IProjectPort {
  private readonly apiUrl = 'http://localhost:9090/api/v1/projects';

  constructor(private http: HttpClient) {}

  getAllProjects(): Observable<Project[]> {
    return this.http.get<Project[]>(this.apiUrl);
  }

  getProjectBoard(projectId: number): Observable<ProjectBoard> {
    return this.http.get<ProjectBoard>(`${this.apiUrl}/${projectId}/board`);
  }
}
