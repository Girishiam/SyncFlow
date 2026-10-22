package com.syncflow.project.application.port.in;

import com.syncflow.dto.*;

import java.util.List;

public interface ProjectUseCase {
    List<ProjectDTO> getAllProjects();
    ProjectDTO createProject(CreateProjectRequest request);
    ProjectBoardDTO getProjectBoard(Long projectId);
    StageDTO createStage(Long projectId, CreateStageRequest request);
    void reorderStages(Long projectId, ReorderStagesRequest request);
    BurndownAnalyticsDTO getBurndownAnalytics(Long projectId);

    // Roadmap & Gantt Timeline
    List<TimelineTaskDTO> getProjectTimeline(Long projectId);

    // Agile Sprints & Planning
    List<SprintDTO> getProjectSprints(Long projectId);
    SprintDTO createSprint(Long projectId, SprintDTO request);
    SprintDTO startSprint(Long sprintId);
    SprintDTO completeSprint(Long sprintId);

    // Butler Automation Rules
    List<AutomationRuleDTO> getAutomationRules(Long projectId);
    AutomationRuleDTO toggleAutomationRule(Long ruleId);

    // Executive Financial Budget & Resource Cost Tracking
    ProjectFinancialsDTO getProjectFinancials(Long projectId);

    // Team Workload Capacity & Resource Allocation Matrix
    TeamCapacityDTO getTeamCapacity(Long projectId);

    // Real-Time Webhooks & Enterprise Connectors
    List<WebhookSubscriptionDTO> getWebhooks(Long projectId);
    WebhookSubscriptionDTO createWebhook(Long projectId, CreateWebhookRequest request);
    WebhookSubscriptionDTO toggleWebhook(Long webhookId);
    WebhookSubscriptionDTO testWebhook(Long webhookId);
    void deleteWebhook(Long webhookId);
}
