import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Project,
  ProjectBoard,
  TimelineTask,
  Sprint,
  AutomationRule,
  ProjectFinancials,
  TeamCapacity,
  WebhookSubscription,
  ProjectTimeSummary,
  StageGateFlags
} from '../models/project.models';
import { IProjectPort } from '../ports/project.port';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ProjectService implements IProjectPort {
  private readonly apiUrl = `${environment.apiBaseUrl}/v1/projects`;

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

  createStage(projectId: number, name: string, isDoneStage?: boolean, gates?: StageGateFlags): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/${projectId}/stages`, { name, isDoneStage, ...gates });
  }

  /** Rename a stage or change the quality gates enforced when tasks enter it. */
  updateStage(stageId: number, changes: { name?: string; isDoneStage?: boolean } & StageGateFlags): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/stages/${stageId}`, changes);
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

  // --- Executive Financial Budget & Resource Cost Tracking ---
  getFinancials(projectId: number): Observable<ProjectFinancials> {
    return this.http.get<ProjectFinancials>(`${this.apiUrl}/${projectId}/financials`);
  }

  // --- Team Workload Capacity & Resource Allocation Matrix ---
  getTeamCapacity(projectId: number): Observable<TeamCapacity> {
    return this.http.get<TeamCapacity>(`${this.apiUrl}/${projectId}/capacity`);
  }

  // --- Real-Time Webhooks & Enterprise Connectors ---
  getWebhooks(projectId: number): Observable<WebhookSubscription[]> {
    return this.http.get<WebhookSubscription[]>(`${this.apiUrl}/${projectId}/webhooks`);
  }

  createWebhook(projectId: number, request: Partial<WebhookSubscription>): Observable<WebhookSubscription> {
    return this.http.post<WebhookSubscription>(`${this.apiUrl}/${projectId}/webhooks`, request);
  }

  toggleWebhook(webhookId: number): Observable<WebhookSubscription> {
    return this.http.put<WebhookSubscription>(`${this.apiUrl}/webhooks/${webhookId}/toggle`, {});
  }

  testWebhook(webhookId: number): Observable<WebhookSubscription> {
    return this.http.post<WebhookSubscription>(`${this.apiUrl}/webhooks/${webhookId}/test`, {});
  }

  deleteWebhook(webhookId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/webhooks/${webhookId}`);
  }

  // --- Collective Project Time Tracking ---
  getProjectTimeSummary(projectId: number): Observable<ProjectTimeSummary> {
    return this.http.get<ProjectTimeSummary>(`${this.apiUrl}/${projectId}/time-summary`);
  }
}
