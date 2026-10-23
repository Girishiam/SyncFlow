package com.syncflow.project.application.port.in;

import com.syncflow.dto.*;

import java.util.List;

public interface ProjectUseCase {
    /** Projects visible to the current user. */
    List<ProjectDTO> getAllProjects();
    ProjectDTO createProject(CreateProjectRequest request);
    ProjectBoardDTO getProjectBoard(Long projectId);
    StageDTO createStage(Long projectId, CreateStageRequest request);
    StageDTO updateStage(Long stageId, UpdateStageRequest request);
    void reorderStages(Long projectId, ReorderStagesRequest request);
    BurndownAnalyticsDTO getBurndownAnalytics(Long projectId);

    // Roadmap & Gantt Timeline
    List<TimelineTaskDTO> getProjectTimeline(Long projectId);

    // Agile Sprints & Planning
    List<SprintDTO> getProjectSprints(Long projectId);
    SprintDTO createSprint(Long projectId, SprintDTO request);
    SprintDTO startSprint(Long sprintId);
    SprintDTO completeSprint(Long sprintId);

    // Automation Rules
    List<AutomationRuleDTO> getAutomationRules(Long projectId);
    AutomationRuleDTO toggleAutomationRule(Long ruleId);

    // Budget & Cost Tracking
    ProjectFinancialsDTO getProjectFinancials(Long projectId);

    // Team Workload Capacity
    TeamCapacityDTO getTeamCapacity(Long projectId);

    // Webhooks
    List<WebhookSubscriptionDTO> getWebhooks(Long projectId);
    WebhookSubscriptionDTO createWebhook(Long projectId, CreateWebhookRequest request);
    WebhookSubscriptionDTO toggleWebhook(Long webhookId);
    WebhookSubscriptionDTO testWebhook(Long webhookId);
    void deleteWebhook(Long webhookId);

    // Collective Project Time Tracking
    ProjectTimeSummaryDTO getProjectTimeSummary(Long projectId);
}
