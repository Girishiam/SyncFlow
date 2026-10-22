import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Project, ProjectBoard, TimelineTask, Sprint, AutomationRule } from '../models/project.models';
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

  createProject(name: string, description?: string): Observable<Project> {
    return this.http.post<Project>(this.apiUrl, { name, description });
  }

  getProjectBoard(projectId: number): Observable<ProjectBoard> {
    return this.http.get<ProjectBoard>(`${this.apiUrl}/${projectId}/board`);
  }

  createStage(projectId: number, name: string, isDoneStage?: boolean): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/${projectId}/stages`, { name, isDoneStage });
  }

  reorderStages(projectId: number, stageIds: number[]): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${projectId}/stages/reorder`, { stageIds });
  }

  getBurndownAnalytics(projectId: number): Observable<import('../models/project.models').BurndownAnalytics> {
    return this.http.get<import('../models/project.models').BurndownAnalytics>(`${this.apiUrl}/${projectId}/analytics/burndown`);
  }

  // --- Roadmap & Gantt Timeline ---
  getTimeline(projectId: number): Observable<TimelineTask[]> {
    return this.http.get<TimelineTask[]>(`${this.apiUrl}/${projectId}/timeline`);
  }

  // --- Agile Sprints & Planning ---
  getSprints(projectId: number): Observable<Sprint[]> {
    return this.http.get<Sprint[]>(`${this.apiUrl}/${projectId}/sprints`);
  }

  createSprint(projectId: number, request: Partial<Sprint>): Observable<Sprint> {
    return this.http.post<Sprint>(`${this.apiUrl}/${projectId}/sprints`, request);
  }

  startSprint(sprintId: number): Observable<Sprint> {
    return this.http.put<Sprint>(`${this.apiUrl}/sprints/${sprintId}/start`, {});
  }

  completeSprint(sprintId: number): Observable<Sprint> {
    return this.http.put<Sprint>(`${this.apiUrl}/sprints/${sprintId}/complete`, {});
  }

  // --- Butler Automation Rules ---
  getAutomationRules(projectId: number): Observable<AutomationRule[]> {
    return this.http.get<AutomationRule[]>(`${this.apiUrl}/${projectId}/automations`);
  }

  toggleAutomationRule(ruleId: number): Observable<AutomationRule> {
    return this.http.put<AutomationRule>(`${this.apiUrl}/automations/${ruleId}/toggle`, {});
  }
}
