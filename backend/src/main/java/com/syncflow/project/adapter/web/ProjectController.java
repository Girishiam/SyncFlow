package com.syncflow.project.adapter.web;

import com.syncflow.dto.*;
import com.syncflow.project.application.port.in.ProjectUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/projects")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class ProjectController {

    private final ProjectUseCase projectUseCase;

    @GetMapping
    public ResponseEntity<List<ProjectDTO>> getAllProjects() {
        return ResponseEntity.ok(projectUseCase.getAllProjects());
    }

    @PostMapping
    public ResponseEntity<ProjectDTO> createProject(@Valid @RequestBody CreateProjectRequest request) {
        return ResponseEntity.ok(projectUseCase.createProject(request));
    }

    @GetMapping("/{id}/board")
    public ResponseEntity<ProjectBoardDTO> getProjectBoard(@PathVariable Long id) {
        return ResponseEntity.ok(projectUseCase.getProjectBoard(id));
    }

    @PostMapping("/{id}/stages")
    public ResponseEntity<StageDTO> createStage(
            @PathVariable Long id,
            @Valid @RequestBody CreateStageRequest request) {
        return ResponseEntity.ok(projectUseCase.createStage(id, request));
    }

    @PutMapping("/{id}/stages/reorder")
    public ResponseEntity<Void> reorderStages(
            @PathVariable Long id,
            @Valid @RequestBody ReorderStagesRequest request) {
        projectUseCase.reorderStages(id, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/analytics/burndown")
    public ResponseEntity<BurndownAnalyticsDTO> getBurndownAnalytics(@PathVariable Long id) {
        return ResponseEntity.ok(projectUseCase.getBurndownAnalytics(id));
    }

    // --- Roadmap & Gantt Timeline ---
    @GetMapping("/{id}/timeline")
    public ResponseEntity<List<TimelineTaskDTO>> getProjectTimeline(@PathVariable Long id) {
        return ResponseEntity.ok(projectUseCase.getProjectTimeline(id));
    }

    // --- Agile Sprints & Planning ---
    @GetMapping("/{id}/sprints")
    public ResponseEntity<List<SprintDTO>> getProjectSprints(@PathVariable Long id) {
        return ResponseEntity.ok(projectUseCase.getProjectSprints(id));
    }

    @PostMapping("/{id}/sprints")
    public ResponseEntity<SprintDTO> createSprint(
            @PathVariable Long id,
            @RequestBody SprintDTO request) {
        return ResponseEntity.ok(projectUseCase.createSprint(id, request));
    }

    @PutMapping("/sprints/{sprintId}/start")
    public ResponseEntity<SprintDTO> startSprint(@PathVariable Long sprintId) {
        return ResponseEntity.ok(projectUseCase.startSprint(sprintId));
    }

    @PutMapping("/sprints/{sprintId}/complete")
    public ResponseEntity<SprintDTO> completeSprint(@PathVariable Long sprintId) {
        return ResponseEntity.ok(projectUseCase.completeSprint(sprintId));
    }

    // --- Butler Automation Rules ---
    @GetMapping("/{id}/automations")
    public ResponseEntity<List<AutomationRuleDTO>> getAutomationRules(@PathVariable Long id) {
        return ResponseEntity.ok(projectUseCase.getAutomationRules(id));
    }

    @PutMapping("/automations/{ruleId}/toggle")
    public ResponseEntity<AutomationRuleDTO> toggleAutomationRule(@PathVariable Long ruleId) {
        return ResponseEntity.ok(projectUseCase.toggleAutomationRule(ruleId));
    }

    // --- Executive Financial Budget & Resource Cost Tracking ---
    @GetMapping("/{id}/financials")
    public ResponseEntity<com.syncflow.dto.ProjectFinancialsDTO> getProjectFinancials(@PathVariable Long id) {
        return ResponseEntity.ok(projectUseCase.getProjectFinancials(id));
    }

    // --- Team Workload Capacity & Resource Allocation Matrix ---
    @GetMapping("/{id}/capacity")
    public ResponseEntity<com.syncflow.dto.TeamCapacityDTO> getTeamCapacity(@PathVariable Long id) {
        return ResponseEntity.ok(projectUseCase.getTeamCapacity(id));
    }

    // --- Real-Time Webhooks & Enterprise Connectors ---
    @GetMapping("/{id}/webhooks")
    public ResponseEntity<List<com.syncflow.dto.WebhookSubscriptionDTO>> getWebhooks(@PathVariable Long id) {
        return ResponseEntity.ok(projectUseCase.getWebhooks(id));
    }

    @PostMapping("/{id}/webhooks")
    public ResponseEntity<com.syncflow.dto.WebhookSubscriptionDTO> createWebhook(
            @PathVariable Long id,
            @RequestBody com.syncflow.dto.CreateWebhookRequest request) {
        return ResponseEntity.ok(projectUseCase.createWebhook(id, request));
    }

    @PutMapping("/webhooks/{webhookId}/toggle")
    public ResponseEntity<com.syncflow.dto.WebhookSubscriptionDTO> toggleWebhook(@PathVariable Long webhookId) {
        return ResponseEntity.ok(projectUseCase.toggleWebhook(webhookId));
    }

    @PostMapping("/webhooks/{webhookId}/test")
    public ResponseEntity<com.syncflow.dto.WebhookSubscriptionDTO> testWebhook(@PathVariable Long webhookId) {
        return ResponseEntity.ok(projectUseCase.testWebhook(webhookId));
    }

    @DeleteMapping("/webhooks/{webhookId}")
    public ResponseEntity<Void> deleteWebhook(@PathVariable Long webhookId) {
        projectUseCase.deleteWebhook(webhookId);
        return ResponseEntity.ok().build();
    }

    // --- Collective Project Time Tracking ---
    @GetMapping("/{id}/time-summary")
    public ResponseEntity<com.syncflow.dto.ProjectTimeSummaryDTO> getProjectTimeSummary(@PathVariable Long id) {
        return ResponseEntity.ok(projectUseCase.getProjectTimeSummary(id));
    }
}
